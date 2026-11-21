package com.example.telemetry

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.system.Os
import android.telephony.TelephonyManager
import android.view.WindowManager
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONObject

data class DeviceSpecItem(
    val label: String,
    val value: String,
    val monospace: Boolean = false
)

data class DeviceSpecSection(
    val title: String,
    val items: List<DeviceSpecItem>
)

data class DeviceSpecsSnapshot(
    val deviceName: String = "",
    val manufacturer: String = "",
    val brand: String = "",
    val model: String = "",
    val deviceCodeName: String = "",
    val productName: String = "",
    val loggedInAccount: String = "",
    val androidId: String = "",
    val imeiStatus: String = "",
    val androidVersion: String = "",
    val apiLevel: String = "",
    val securityPatch: String = "",
    val osCodename: String = "",
    val basebandVersion: String = "",
    val kernelVersion: String = "",
    val bootloaderVersion: String = "",
    val buildNumber: String = "",
    val buildId: String = "",
    val incrementalVersion: String = "",
    val buildTypeAndTags: String = "",
    val buildDate: String = "",
    val buildFingerprint: String = "",
    val javaVmVersion: String = "",
    val duetAppVersion: String = "",
    val hardware: String = "",
    val board: String = "",
    val socInfo: String = "",
    val cpuAbi: String = "",
    val cpuCores: String = "",
    val ramSummary: String = "",
    val storageSummary: String = "",
    val displaySummary: String = "",
    val batteryHealthDetails: String = "",
    val carrierName: String = "",
    val phoneType: String = "",
    val localeAndTimezone: String = ""
) {
    fun shortSummaryLine(): String {
        val parts = listOfNotNull(
            deviceName.takeIf { it.isNotBlank() },
            androidVersion.takeIf { it.isNotBlank() },
            securityPatch.takeIf { it.isNotBlank() && it != "Unavailable" }?.let { "Patch $it" }
        )
        return parts.joinToString(" • ").ifBlank { "Android Device" }
    }

    fun toSections(): List<DeviceSpecSection> = listOf(
        DeviceSpecSection(
            title = "Device Identity & Account",
            items = listOf(
                DeviceSpecItem("Device Name", deviceName.orNA()),
                DeviceSpecItem("Logged-In Account", loggedInAccount.orNA()),
                DeviceSpecItem("Manufacturer", manufacturer.orNA()),
                DeviceSpecItem("Brand", brand.orNA()),
                DeviceSpecItem("Model", model.orNA()),
                DeviceSpecItem("Device Codename", deviceCodeName.orNA(), monospace = true),
                DeviceSpecItem("Product Name", productName.orNA(), monospace = true),
                DeviceSpecItem("Device ID (Android SSAID)", androidId.orNA(), monospace = true),
                DeviceSpecItem("IMEI / Hardware ID", imeiStatus.orNA())
            )
        ),
        DeviceSpecSection(
            title = "Android OS & Security Patch",
            items = listOf(
                DeviceSpecItem("Android Version", androidVersion.orNA()),
                DeviceSpecItem("API / SDK Level", apiLevel.orNA(), monospace = true),
                DeviceSpecItem("Security Patch Update", securityPatch.orNA(), monospace = true),
                DeviceSpecItem("OS Codename", osCodename.orNA()),
                DeviceSpecItem("Duet App Version", duetAppVersion.orNA(), monospace = true),
                DeviceSpecItem("Java / ART Runtime", javaVmVersion.orNA(), monospace = true)
            )
        ),
        DeviceSpecSection(
            title = "All Firmware & Build Versions",
            items = listOf(
                DeviceSpecItem("Baseband / Radio Version", basebandVersion.orNA(), monospace = true),
                DeviceSpecItem("Kernel Version", kernelVersion.orNA(), monospace = true),
                DeviceSpecItem("Bootloader Version", bootloaderVersion.orNA(), monospace = true),
                DeviceSpecItem("Build Number (Display)", buildNumber.orNA(), monospace = true),
                DeviceSpecItem("Build ID", buildId.orNA(), monospace = true),
                DeviceSpecItem("Incremental Version", incrementalVersion.orNA(), monospace = true),
                DeviceSpecItem("Build Type & Tags", buildTypeAndTags.orNA(), monospace = true),
                DeviceSpecItem("Build Date", buildDate.orNA()),
                DeviceSpecItem("Build Fingerprint", buildFingerprint.orNA(), monospace = true)
            )
        ),
        DeviceSpecSection(
            title = "Hardware, Memory, Storage & Display",
            items = listOf(
                DeviceSpecItem("Processor / SoC", socInfo.orNA()),
                DeviceSpecItem("Hardware Platform", hardware.orNA(), monospace = true),
                DeviceSpecItem("Board", board.orNA(), monospace = true),
                DeviceSpecItem("CPU Cores", cpuCores.orNA()),
                DeviceSpecItem("Supported ABIs", cpuAbi.orNA(), monospace = true),
                DeviceSpecItem("RAM (Free / Total)", ramSummary.orNA()),
                DeviceSpecItem("Internal Storage", storageSummary.orNA()),
                DeviceSpecItem("Display & Refresh Rate", displaySummary.orNA()),
                DeviceSpecItem("Battery Hardware", batteryHealthDetails.orNA())
            )
        ),
        DeviceSpecSection(
            title = "Network, SIM & Region",
            items = listOf(
                DeviceSpecItem("SIM / Carrier Network", carrierName.orNA()),
                DeviceSpecItem("Radio / Phone Type", phoneType.orNA()),
                DeviceSpecItem("Locale & Timezone", localeAndTimezone.orNA())
            )
        )
    )

    fun toPlainTextReport(ownerLabel: String): String {
        val sb = StringBuilder()
        sb.appendLine("=== $ownerLabel — A to Z Device Details ===")
        toSections().forEach { section ->
            sb.appendLine()
            sb.appendLine("[${section.title}]")
            section.items.forEach { item ->
                sb.appendLine("${item.label}: ${item.value}")
            }
        }
        return sb.toString().trim()
    }

    fun toCompactJson(): String {
        val json = JSONObject().apply {
            put("dn", deviceName.take(96))
            put("mf", manufacturer.take(48))
            put("br", brand.take(48))
            put("md", model.take(64))
            put("dc", deviceCodeName.take(48))
            put("pr", productName.take(64))
            put("ac", loggedInAccount.take(80))
            put("ai", androidId.take(40))
            put("im", imeiStatus.take(96))
            put("av", androidVersion.take(40))
            put("ap", apiLevel.take(36))
            put("sp", securityPatch.take(32))
            put("oc", osCodename.take(32))
            put("bb", basebandVersion.take(96))
            put("kv", kernelVersion.take(120))
            put("bl", bootloaderVersion.take(64))
            put("bn", buildNumber.take(96))
            put("bi", buildId.take(48))
            put("iv", incrementalVersion.take(64))
            put("bt", buildTypeAndTags.take(48))
            put("bd", buildDate.take(48))
            put("fp", buildFingerprint.take(180))
            put("jv", javaVmVersion.take(64))
            put("dv", duetAppVersion.take(48))
            put("hw", hardware.take(48))
            put("bo", board.take(48))
            put("sc", socInfo.take(80))
            put("ca", cpuAbi.take(80))
            put("cc", cpuCores.take(32))
            put("rm", ramSummary.take(64))
            put("st", storageSummary.take(64))
            put("ds", displaySummary.take(80))
            put("bh", batteryHealthDetails.take(80))
            put("cn", carrierName.take(64))
            put("pt", phoneType.take(32))
            put("lt", localeAndTimezone.take(80))
        }
        val raw = json.toString()
        return if (raw.length <= 3950) raw else raw.take(3950)
    }

    companion object {
        fun fromJson(raw: String?): DeviceSpecsSnapshot? {
            if (raw.isNullOrBlank()) return null
            return try {
                val j = JSONObject(raw)
                DeviceSpecsSnapshot(
                    deviceName = j.optString("dn", ""),
                    manufacturer = j.optString("mf", ""),
                    brand = j.optString("br", ""),
                    model = j.optString("md", ""),
                    deviceCodeName = j.optString("dc", ""),
                    productName = j.optString("pr", ""),
                    loggedInAccount = j.optString("ac", ""),
                    androidId = j.optString("ai", ""),
                    imeiStatus = j.optString("im", ""),
                    androidVersion = j.optString("av", ""),
                    apiLevel = j.optString("ap", ""),
                    securityPatch = j.optString("sp", ""),
                    osCodename = j.optString("oc", ""),
                    basebandVersion = j.optString("bb", ""),
                    kernelVersion = j.optString("kv", ""),
                    bootloaderVersion = j.optString("bl", ""),
                    buildNumber = j.optString("bn", ""),
                    buildId = j.optString("bi", ""),
                    incrementalVersion = j.optString("iv", ""),
                    buildTypeAndTags = j.optString("bt", ""),
                    buildDate = j.optString("bd", ""),
                    buildFingerprint = j.optString("fp", ""),
                    javaVmVersion = j.optString("jv", ""),
                    duetAppVersion = j.optString("dv", ""),
                    hardware = j.optString("hw", ""),
                    board = j.optString("bo", ""),
                    socInfo = j.optString("sc", ""),
                    cpuAbi = j.optString("ca", ""),
                    cpuCores = j.optString("cc", ""),
                    ramSummary = j.optString("rm", ""),
                    storageSummary = j.optString("st", ""),
                    displaySummary = j.optString("ds", ""),
                    batteryHealthDetails = j.optString("bh", ""),
                    carrierName = j.optString("cn", ""),
                    phoneType = j.optString("pt", ""),
                    localeAndTimezone = j.optString("lt", "")
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

private fun String.orNA(): String = if (isBlank()) "Unavailable" else this

/**
 * Collects genuine A-to-Z Android device hardware, OS, security patch, firmware/build versions,
 * memory, storage, display, battery hardware, SIM carrier, and app-scoped device identifiers
 * strictly using standard, crash-safe Android OS APIs (zero mock values).
 */
object DeviceSpecsCollector {

    fun collect(context: Context): DeviceSpecsSnapshot {
        val appCtx = context.applicationContext
        val manufacturer = formatTitle(Build.MANUFACTURER)
        val brand = formatTitle(Build.BRAND)
        val model = Build.MODEL.orEmpty().trim()

        val customDeviceName = readDeviceName(appCtx, manufacturer, model)
        val androidId = readAndroidId(appCtx)
        val imeiStatus = readImeiOrPolicyNotice(appCtx)
        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH.orEmpty().ifBlank { "Unavailable" }
        } else {
            "Unavailable"
        }
        val baseband = runCatching { Build.getRadioVersion() }.getOrNull()
            ?.takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
            ?: "Wi-Fi / Unavailable"
        val kernel = readKernelVersion()
        val socInfo = readSocInfo()
        val ramSummary = readRamSummary(appCtx)
        val storageSummary = readStorageSummary()
        val displaySummary = readDisplaySummary(appCtx)
        val batteryHardware = readBatteryHardware(appCtx)
        val (carrierName, phoneType) = readCarrierAndPhoneType(appCtx)
        val buildDate = formatBuildDate(Build.TIME)
        val javaVm = listOfNotNull(
            System.getProperty("java.vm.name")?.takeIf { it.isNotBlank() },
            System.getProperty("java.vm.version")?.takeIf { it.isNotBlank() }
        ).joinToString(" ").ifBlank { "ART Runtime" }
        val apiLevel = buildString {
            append("API ${Build.VERSION.SDK_INT}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.VERSION.MEDIA_PERFORMANCE_CLASS > 0) {
                append(" (Perf Class ${Build.VERSION.MEDIA_PERFORMANCE_CLASS})")
            }
        }
        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
        val accountLabel = when {
            !user?.email.isNullOrBlank() -> user?.email.orEmpty()
            !user?.displayName.isNullOrBlank() -> user?.displayName.orEmpty()
            else -> "Signed-in Google Account"
        }

        return DeviceSpecsSnapshot(
            deviceName = customDeviceName,
            manufacturer = manufacturer,
            brand = brand,
            model = model,
            deviceCodeName = Build.DEVICE.orEmpty(),
            productName = Build.PRODUCT.orEmpty(),
            loggedInAccount = accountLabel,
            androidId = androidId,
            imeiStatus = imeiStatus,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            apiLevel = apiLevel,
            securityPatch = securityPatch,
            osCodename = Build.VERSION.CODENAME.orEmpty(),
            basebandVersion = baseband,
            kernelVersion = kernel,
            bootloaderVersion = Build.BOOTLOADER.orEmpty().ifBlank { "Unknown" },
            buildNumber = Build.DISPLAY.orEmpty(),
            buildId = Build.ID.orEmpty(),
            incrementalVersion = Build.VERSION.INCREMENTAL.orEmpty(),
            buildTypeAndTags = "${Build.TYPE} / ${Build.TAGS}",
            buildDate = buildDate,
            buildFingerprint = Build.FINGERPRINT.orEmpty(),
            javaVmVersion = javaVm,
            duetAppVersion = "v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})",
            hardware = Build.HARDWARE.orEmpty(),
            board = Build.BOARD.orEmpty(),
            socInfo = socInfo,
            cpuAbi = Build.SUPPORTED_ABIS.joinToString(", ").ifBlank { "Unknown" },
            cpuCores = "${Runtime.getRuntime().availableProcessors()} Cores",
            ramSummary = ramSummary,
            storageSummary = storageSummary,
            displaySummary = displaySummary,
            batteryHealthDetails = batteryHardware,
            carrierName = carrierName,
            phoneType = phoneType,
            localeAndTimezone = "${Locale.getDefault().displayName} • ${TimeZone.getDefault().id}"
        )
    }

    private fun readDeviceName(context: Context, manufacturer: String, model: String): String {
        val sysName = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            } else {
                null
            }
        }.getOrNull()?.trim().orEmpty()

        val fallback = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model".trim()
        }
        return when {
            sysName.isNotBlank() && !sysName.equals(model, ignoreCase = true) && !sysName.contains("sdk", ignoreCase = true) -> {
                if (sysName.contains(model, ignoreCase = true)) sysName else "$sysName ($model)"
            }
            sysName.isNotBlank() -> sysName
            else -> fallback.ifBlank { "Android Device" }
        }
    }

    @SuppressLint("HardwareIds")
    private fun readAndroidId(context: Context): String {
        return runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?.uppercase(Locale.US)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Unavailable"
    }

    /**
     * Android 10 (API 29) and higher restrict hardware IMEI exclusively to privileged system/carrier
     * apps (SecurityException for all third-party apps). Reports real IMEI only when permitted by OS,
     * and transparently displays the Android privacy protection notice otherwise.
     */
    @SuppressLint("HardwareIds", "MissingPermission")
    private fun readImeiOrPolicyNotice(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return "OS-Protected on Android ${Build.VERSION.RELEASE} (Dial *#06#)"
        }
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tm?.imei
            } else {
                @Suppress("DEPRECATION")
                tm?.deviceId
            }
            imei?.takeIf { it.isNotBlank() } ?: "OS-Protected (Dial *#06#)"
        } catch (_: SecurityException) {
            "OS-Protected by Android (Dial *#06#)"
        } catch (_: Exception) {
            "Unavailable (Dial *#06#)"
        }
    }

    private fun readKernelVersion(): String {
        val uname = runCatching {
            val u = Os.uname()
            "${u.sysname} ${u.release} (${u.machine})"
        }.getOrNull()
        if (!uname.isNullOrBlank()) return uname
        return System.getProperty("os.version")?.takeIf { it.isNotBlank() } ?: "Unavailable"
    }

    private fun readSocInfo(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mfg = Build.SOC_MANUFACTURER.orEmpty().takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
            val mod = Build.SOC_MODEL.orEmpty().takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
            val combined = listOfNotNull(mfg, mod).joinToString(" ")
            if (combined.isNotBlank()) return combined
        }
        return Build.HARDWARE.orEmpty().ifBlank { "Unknown" }
    }

    private fun readRamSummary(context: Context): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return "Unavailable"
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val availGb = mi.availMem / (1024.0 * 1024.0 * 1024.0)
            val totalGb = mi.totalMem / (1024.0 * 1024.0 * 1024.0)
            String.format(Locale.US, "%.1f GB free / %.1f GB total", availGb, totalGb)
        } catch (_: Exception) {
            "Unavailable"
        }
    }

    private fun readStorageSummary(): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val availGb = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
            val totalGb = stat.totalBytes / (1024.0 * 1024.0 * 1024.0)
            String.format(Locale.US, "%.1f GB free / %.1f GB total", availGb, totalGb)
        } catch (_: Exception) {
            "Unavailable"
        }
    }

    private fun readDisplaySummary(context: Context): String {
        return try {
            val dm = context.resources.displayMetrics
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val refreshHz = runCatching {
                @Suppress("DEPRECATION")
                wm?.defaultDisplay?.refreshRate?.toInt()
            }.getOrNull()?.takeIf { it in 24..240 }
            val resolution = "${dm.widthPixels} × ${dm.heightPixels} px"
            val dpi = "${dm.densityDpi} dpi"
            if (refreshHz != null) "$resolution • $dpi • ${refreshHz} Hz"
            else "$resolution • $dpi"
        } catch (_: Exception) {
            "Unavailable"
        }
    }

    private fun readBatteryHardware(context: Context): String {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: return "Unavailable"
            val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() } ?: "Li-ion"
            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            val health = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Degraded"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                else -> "Normal"
            }
            val parts = mutableListOf(health, tech)
            if (tempTenths > 0) {
                parts.add(String.format(Locale.US, "%.1f°C", tempTenths / 10.0))
            }
            if (voltageMv > 0) {
                parts.add(String.format(Locale.US, "%.2f V", voltageMv / 1000.0))
            }
            parts.joinToString(" • ")
        } catch (_: Exception) {
            "Unavailable"
        }
    }

    private fun readCarrierAndPhoneType(context: Context): Pair<String, String> {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return "Wi-Fi Only / No SIM" to "None"
            val netOp = tm.networkOperatorName.orEmpty().trim()
            val simOp = tm.simOperatorName.orEmpty().trim()
            val country = tm.simCountryIso.orEmpty().trim().uppercase(Locale.US)
            val opName = netOp.ifBlank { simOp }
            val carrierLabel = when {
                opName.isNotBlank() && country.isNotBlank() -> "$opName ($country)"
                opName.isNotBlank() -> opName
                else -> "Wi-Fi Only / No SIM"
            }
            val phoneType = when (tm.phoneType) {
                TelephonyManager.PHONE_TYPE_GSM -> "GSM / LTE / 5G"
                TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                TelephonyManager.PHONE_TYPE_SIP -> "SIP"
                else -> "Wi-Fi / Data Only"
            }
            carrierLabel to phoneType
        } catch (_: Exception) {
            "Wi-Fi Only / No SIM" to "Unavailable"
        }
    }

    private fun formatBuildDate(epochMillis: Long): String {
        if (epochMillis <= 0L) return "Unknown"
        return try {
            val fmt = SimpleDateFormat("dd MMM yyyy, h:mm a 'IST'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }
            fmt.format(Date(epochMillis))
        } catch (_: Exception) {
            "Unknown"
        }
    }

    private fun formatTitle(raw: String?): String {
        val clean = raw.orEmpty().trim()
        if (clean.isEmpty()) return "Unknown"
        return clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }
}
