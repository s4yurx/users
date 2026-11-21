package com.example.telemetry

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.notifications.DuetNotificationStateStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LocalDeviceTelemetry(
    val batteryPercent: Int,
    val charging: Boolean,
    val networkType: String,
    val deviceStatus: String,
    val activeAppName: String = "",
    val deviceName: String = "",
    val deviceSpecsJson: String = ""
)

/**
 * Reads genuine Android device telemetry using standard OS APIs:
 * - BatteryManager & ACTION_BATTERY_CHANGED for instant battery percentage and charging state
 * - ConnectivityManager / NetworkCapabilities for Wi-Fi / Cellular / Ethernet / Offline
 * - KeyguardManager & PowerManager for Active-in-Duet / Mobile-In-Use / Mobile-Locked state
 * - UsageStatsManager (when opted-in via Usage Access) for currently open foreground app name
 * - DeviceSpecsCollector for complete A-to-Z device identity, OS, security patch & hardware versions
 */
class DeviceTelemetryCollector(context: Context) {
    private val appContext = context.applicationContext

    @Volatile
    private var appInForeground: Boolean = DuetNotificationStateStore.isAppInForeground

    @Volatile
    private var cachedSpecs: DeviceSpecsSnapshot? = null

    @Volatile
    private var cachedSpecsJson: String = ""

    @Volatile
    private var lastSpecsCollectedAtMs: Long = 0L

    fun setAppForeground(inForeground: Boolean) {
        appInForeground = inForeground
    }

    fun readDeviceSpecs(forceRefresh: Boolean = false): DeviceSpecsSnapshot {
        val now = System.currentTimeMillis()
        val existing = cachedSpecs
        if (!forceRefresh && existing != null && (now - lastSpecsCollectedAtMs) < SPECS_REFRESH_INTERVAL_MS) {
            return existing
        }
        val fresh = DeviceSpecsCollector.collect(appContext)
        cachedSpecs = fresh
        cachedSpecsJson = fresh.toCompactJson()
        lastSpecsCollectedAtMs = now
        return fresh
    }

    fun readSnapshot(forceRefreshSpecs: Boolean = false): LocalDeviceTelemetry {
        val (batteryPercent, charging) = readBattery()
        val networkType = readNetworkType()
        val deviceStatus = readDeviceStatus()
        val activeAppName = when (deviceStatus) {
            "LOCKED" -> ""
            "ACTIVE_IN_DUET" -> "Duet"
            else -> AppUsageMonitor.detectCurrentForegroundAppName(appContext, isDuetInForeground = false)
        }
        val specs = readDeviceSpecs(forceRefresh = forceRefreshSpecs)
        return LocalDeviceTelemetry(
            batteryPercent = batteryPercent,
            charging = charging,
            networkType = networkType,
            deviceStatus = deviceStatus,
            activeAppName = activeAppName,
            deviceName = specs.deviceName.take(100),
            deviceSpecsJson = cachedSpecsJson
        )
    }

    companion object {
        private const val SPECS_REFRESH_INTERVAL_MS = 10 * 60 * 1000L
    }

    fun observeTelemetry(pollIntervalMs: Long = 2_500L): Flow<LocalDeviceTelemetry> = callbackFlow {
        trySend(readSnapshot())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(readSnapshot())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        val receiverRegistered = runCatching {
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            true
        }.getOrDefault(false)

        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(readSnapshot())
            }

            override fun onLost(network: Network) {
                trySend(readSnapshot())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(readSnapshot())
            }
        }
        val registeredCallback = try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
            true
        } catch (_: Exception) {
            false
        }

        // Immediately emit when Duet moves to/from foreground
        val foregroundJob = launch {
            DuetNotificationStateStore.appForegroundFlow.collect { inFg ->
                appInForeground = inFg
                trySend(readSnapshot())
            }
        }

        // Continuous automatic telemetry polling (battery, lock, network, foreground app)
        val pollJob = launch {
            while (isActive) {
                delay(pollIntervalMs)
                trySend(readSnapshot())
            }
        }

        awaitClose {
            foregroundJob.cancel()
            pollJob.cancel()
            if (receiverRegistered) {
                try {
                    appContext.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
            }
            if (registeredCallback) {
                try {
                    connectivityManager?.unregisterNetworkCallback(networkCallback)
                } catch (_: Exception) {
                }
            }
        }
    }.distinctUntilChanged()

    private fun readBattery(): Pair<Int, Boolean> {
        return try {
            val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val bmCap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: Int.MIN_VALUE
            val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val intentPct = if (level >= 0 && scale > 0) {
                ((level * 100f) / scale.toFloat()).toInt().coerceIn(0, 100)
            } else {
                -1
            }
            val pct = when {
                bmCap in 0..100 -> bmCap
                intentPct in 0..100 -> intentPct
                else -> 0
            }
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0 ||
                (bm?.isCharging == true)
            pct to isCharging
        } catch (_: Exception) {
            0 to false
        }
    }

    private fun readNetworkType(): String {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "OFFLINE"
            val network = cm.activeNetwork ?: return "OFFLINE"
            val caps = cm.getNetworkCapabilities(network) ?: return "OFFLINE"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "OFFLINE"
            }
        } catch (_: Exception) {
            "OFFLINE"
        }
    }

    private fun readDeviceStatus(): String {
        return try {
            val keyguard = appContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val power = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val locked = keyguard?.isKeyguardLocked == true
            val interactive = power?.isInteractive ?: true
            val inFg = appInForeground || DuetNotificationStateStore.isAppInForeground
            when {
                locked || !interactive -> "LOCKED"
                inFg -> "ACTIVE_IN_DUET"
                else -> "IN_USE"
            }
        } catch (_: Exception) {
            "IN_USE"
        }
    }
}
