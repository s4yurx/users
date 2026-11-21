package com.example.telemetry

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Continuously monitors active internet connectivity across the entire app lifecycle.
 * Emits `true` when connected to a network with internet capability and `false` immediately
 * whenever connectivity drops (and automatically returns to `true` the moment it reconnects).
 */
class NetworkConnectivityMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun isCurrentlyOnline(): Boolean {
        val cm = connectivityManager ?: return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                hasUsableInternet(caps)
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.isConnected == true
            }
        } catch (_: Exception) {
            true
        }
    }

    fun observeIsOnline(backupPollIntervalMs: Long = 3_000L): Flow<Boolean> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }

        val validNetworks = ConcurrentHashMap.newKeySet<Network>()

        fun emitCurrent() {
            val online = validNetworks.isNotEmpty() || isCurrentlyOnline()
            trySend(online)
        }

        // Emit immediate initial status
        trySend(isCurrentlyOnline())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val caps = runCatching { cm.getNetworkCapabilities(network) }.getOrNull()
                if (caps == null || hasUsableInternet(caps)) {
                    validNetworks.add(network)
                }
                emitCurrent()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                if (hasUsableInternet(networkCapabilities)) {
                    validNetworks.add(network)
                } else {
                    validNetworks.remove(network)
                }
                emitCurrent()
            }

            override fun onLost(network: Network) {
                validNetworks.remove(network)
                // Re-evaluate directly against activeNetwork so handoffs (Wi-Fi -> 5G) stay seamless
                trySend(isCurrentlyOnline())
            }

            override fun onUnavailable() {
                validNetworks.clear()
                trySend(isCurrentlyOnline())
            }
        }

        val registered = try {
            cm.registerDefaultNetworkCallback(callback)
            true
        } catch (_: Exception) {
            false
        }

        // Lightweight safety poll catches quick airplane-mode toggles or ROMs that drop callbacks
        val pollJob = launch {
            while (isActive) {
                delay(backupPollIntervalMs)
                trySend(isCurrentlyOnline())
            }
        }

        awaitClose {
            pollJob.cancel()
            if (registered) {
                runCatching { cm.unregisterNetworkCallback(callback) }
            }
        }
    }.distinctUntilChanged()

    private fun hasUsableInternet(caps: NetworkCapabilities): Boolean {
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        return hasInternet && hasTransport
    }
}
