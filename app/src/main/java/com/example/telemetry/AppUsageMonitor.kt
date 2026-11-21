package com.example.telemetry

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Detects the currently open foreground Android application using UsageStatsManager
 * (with explicit user opt-in via Android Usage Access settings) and resolves human-readable
 * app names for Instagram, WhatsApp, YouTube, Spotify, Chrome, and all installed apps.
 */
object AppUsageMonitor {

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        val targeted = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val generic = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(targeted)
        } catch (_: Exception) {
            try {
                context.startActivity(generic)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Returns the friendly display name of the currently open foreground app (e.g. "Instagram",
     * "WhatsApp", "YouTube", "Duet"), or empty string if on Home Screen / Usage Access not enabled.
     */
    fun detectCurrentForegroundAppName(context: Context, isDuetInForeground: Boolean): String {
        if (isDuetInForeground) return "Duet"
        if (!hasUsageStatsPermission(context)) return ""

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ""

        val now = System.currentTimeMillis()
        val pkg = findLatestForegroundPackage(usm, now - 5 * 60_000L, now)
            ?: findLatestForegroundPackage(usm, now - 60 * 60_000L, now)
            ?: findFallbackFromUsageStats(usm, now - 15 * 60_000L, now)
            ?: return ""

        if (pkg == context.packageName) return "Duet"
        if (isHomeLauncher(context, pkg) || isIgnoredSystemOverlay(pkg)) return ""
        return resolveFriendlyAppName(context, pkg)
    }

    private fun findLatestForegroundPackage(
        usm: UsageStatsManager,
        startMs: Long,
        endMs: Long
    ): String? {
        return try {
            val events = usm.queryEvents(startMs, endMs) ?: return null
            val event = UsageEvents.Event()
            var latestPkg: String? = null
            var latestTimestamp = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val isForegroundEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                        @Suppress("DEPRECATION")
                        event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                } else {
                    @Suppress("DEPRECATION")
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                }
                if (isForegroundEvent) {
                    val pkg = event.packageName
                    if (!pkg.isNullOrBlank() && !isIgnoredSystemOverlay(pkg) && event.timeStamp >= latestTimestamp) {
                        latestTimestamp = event.timeStamp
                        latestPkg = pkg
                    }
                }
            }
            latestPkg
        } catch (_: Exception) {
            null
        }
    }

    private fun findFallbackFromUsageStats(
        usm: UsageStatsManager,
        startMs: Long,
        endMs: Long
    ): String? {
        return try {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMs, endMs)
                ?: return null
            stats
                .filter { !it.packageName.isNullOrBlank() && !isIgnoredSystemOverlay(it.packageName) }
                .maxByOrNull { it.lastTimeUsed }
                ?.takeIf { endMs - it.lastTimeUsed <= 15 * 60_000L }
                ?.packageName
        } catch (_: Exception) {
            null
        }
    }

    private fun isHomeLauncher(context: Context, packageName: String): Boolean {
        val lower = packageName.lowercase()
        if (lower.contains("launcher") || lower.endsWith(".home") || lower == "com.miui.home") {
            return true
        }
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(
                    homeIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            resolved?.activityInfo?.packageName == packageName
        } catch (_: Exception) {
            false
        }
    }

    private fun isIgnoredSystemOverlay(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower == "android" ||
            lower == "com.android.systemui" ||
            lower.contains("inputmethod") ||
            lower.contains("keyboard") ||
            lower.contains("gboard") ||
            lower.contains("permissioncontroller") ||
            lower.contains("packageinstaller") ||
            lower.contains("biometric")
    }

    fun resolveFriendlyAppName(context: Context, packageName: String): String {
        KNOWN_APPS[packageName]?.let { return it }

        // Resolve localized label using Play-compliant <queries> launcher visibility
        try {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            val label = pm.getApplicationLabel(appInfo)?.toString()?.trim()
            if (!label.isNullOrBlank() && !label.contains(".")) {
                return label.take(40)
            }
        } catch (_: Exception) {
        }

        // Clean package fallback (e.g. com.example.myapp -> Myapp)
        val parts = packageName.split('.').filter {
            it.isNotBlank() && it !in setOf("com", "org", "net", "in", "io", "app", "android", "mobile", "client", "apps")
        }
        val best = parts.lastOrNull() ?: packageName.substringAfterLast('.')
        return best.replace('_', ' ')
            .replace('-', ' ')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            .take(40)
    }

    private val KNOWN_APPS: Map<String, String> = mapOf(
        "com.instagram.android" to "Instagram",
        "com.instagram.barcelona" to "Threads",
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.google.android.youtube" to "YouTube",
        "com.google.android.apps.youtube.music" to "YouTube Music",
        "com.snapchat.android" to "Snapchat",
        "com.facebook.katana" to "Facebook",
        "com.facebook.orca" to "Messenger",
        "com.facebook.lite" to "Facebook Lite",
        "org.telegram.messenger" to "Telegram",
        "com.twitter.android" to "X",
        "com.x.android" to "X",
        "com.spotify.music" to "Spotify",
        "com.netflix.mediaclient" to "Netflix",
        "com.amazon.avod.thirdpartyclient" to "Prime Video",
        "in.startv.hotstar" to "JioHotstar",
        "com.jio.media.ondemand" to "JioCinema",
        "com.android.chrome" to "Chrome",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok",
        "com.reddit.frontpage" to "Reddit",
        "com.discord" to "Discord",
        "com.pinterest" to "Pinterest",
        "com.linkedin.android" to "LinkedIn",
        "com.google.android.gm" to "Gmail",
        "com.google.android.apps.maps" to "Google Maps",
        "com.google.android.apps.photos" to "Google Photos",
        "com.google.android.apps.messaging" to "Messages",
        "com.google.android.dialer" to "Phone",
        "com.samsung.android.dialer" to "Phone",
        "com.samsung.android.messaging" to "Messages",
        "com.android.settings" to "Settings",
        "com.google.android.GoogleCamera" to "Camera",
        "com.sec.android.app.camera" to "Camera",
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.amazon.mShop.android.shopping" to "Amazon",
        "com.flipkart.android" to "Flipkart",
        "in.swiggy.android" to "Swiggy",
        "com.application.zomato" to "Zomato",
        "com.pubg.imobile" to "BGMI",
        "com.dts.freefireth" to "Free Fire",
        "com.dts.freefiremax" to "Free Fire MAX",
        "com.openai.chatgpt" to "ChatGPT"
    )
}
