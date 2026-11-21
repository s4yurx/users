package com.example.widget

import android.content.Context
import com.example.ui.theme.AppThemePreset

enum class WidgetThemeStyle(
    val key: String,
    val label: String,
    val emoji: String,
    val isExclusive: Boolean = false
) {
    ROSE("ROSE", "Blush Rose", "🌸"),
    VELVET("VELVET", "Velvet Dusk", "🍷"),
    MIDNIGHT("MIDNIGHT", "Midnight Ocean", "🌊"),
    LAVENDER("LAVENDER", "Lavender Dream", "💜"),
    PEACH("PEACH", "Sunset Peach", "🍑"),
    EMERALD("EMERALD", "Emerald Night", "🌿"),
    SAKURA("SAKURA", "Kyoto Sakura", "🌺"),
    ROYAL_GOLD("ROYAL_GOLD", "Royal Obsidian", "👑", isExclusive = true),
    AURORA("AURORA", "Aurora Cyber", "🌌", isExclusive = true),
    AMETHYST("AMETHYST", "Amethyst Eclipse", "🔮", isExclusive = true),
    CHAMPAGNE("CHAMPAGNE", "Champagne Ivory", "🥂", isExclusive = true),
    SOLARIS("SOLARIS", "Solaris Rose Gold", "🌅", isExclusive = true),
    SAPPHIRE("SAPPHIRE", "Celestial Sapphire", "💎", isExclusive = true),
    RUBY("RUBY", "Crimson Ruby", "🌹", isExclusive = true),
    LOTUS("LOTUS", "Neon Lotus Prism", "🪷", isExclusive = true);

    fun toAppThemePreset(): AppThemePreset = AppThemePreset.fromKey(key)

    companion object {
        fun fromKey(raw: String?): WidgetThemeStyle =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: ROSE
    }
}

data class DuetWidgetSnapshot(
    val coupleId: String = "",
    val coupleTitle: String = "Duet",
    val partnerName: String = "Private Chat",
    val partnerStatus: String = "End-to-end encrypted",
    val partnerTyping: Boolean = false,
    val batteryBadge: String = "Live",
    val moodLine: String = "Tap to share your mood",
    val countdownLine: String = "Set anniversary & birthday",
    val statusFooter: String = "End-to-end encrypted for two",
    val lastMsg1: String = "No private messages yet",
    val lastMsg2: String = "Tap below to chat privately from home screen",
    val lastMsgReceipt: String = "",
    val daysTogetherLabel: String = "Set Anniversary",
    val anniversaryCountdown: String = "Tap to set",
    val birthdayCountdown: String = "Tap to set",
    val dailyQuestionText: String = "Tap to answer Today's Question",
    val dailyQuestionStatus: String = "Open Duet to answer",
    val dailyQuestionExpiry: String = "Expires at 12:00 AM IST",
    val partnerNetwork: String = "Live",
    val partnerActiveApp: String = "Duet",
    val partnerDeviceName: String = "Partner Phone",
    val loveJarHighlight: String = "Drop a sweet note in your Love Jar",
    val songVibeHighlight: String = "Dedicate a song to each other",
    val bucketSummary: String = "0/0 Dreams Done",
    val themeStyle: WidgetThemeStyle = WidgetThemeStyle.ROSE
)

class WidgetStateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSnapshot(snapshot: DuetWidgetSnapshot) {
        prefs.edit()
            .putString(KEY_COUPLE_ID, snapshot.coupleId)
            .putString(KEY_TITLE, snapshot.coupleTitle)
            .putString(KEY_PARTNER_NAME, snapshot.partnerName)
            .putString(KEY_PARTNER_STATUS, snapshot.partnerStatus)
            .putBoolean(KEY_PARTNER_TYPING, snapshot.partnerTyping)
            .putString(KEY_BATTERY, snapshot.batteryBadge)
            .putString(KEY_MOOD, snapshot.moodLine)
            .putString(KEY_COUNTDOWN, snapshot.countdownLine)
            .putString(KEY_FOOTER, snapshot.statusFooter)
            .putString(KEY_MSG_1, snapshot.lastMsg1)
            .putString(KEY_MSG_2, snapshot.lastMsg2)
            .putString(KEY_MSG_RECEIPT, snapshot.lastMsgReceipt)
            .putString(KEY_DAYS_TOGETHER, snapshot.daysTogetherLabel)
            .putString(KEY_ANNIVERSARY_CD, snapshot.anniversaryCountdown)
            .putString(KEY_BIRTHDAY_CD, snapshot.birthdayCountdown)
            .putString(KEY_DAILY_Q_TEXT, snapshot.dailyQuestionText)
            .putString(KEY_DAILY_Q_STATUS, snapshot.dailyQuestionStatus)
            .putString(KEY_DAILY_Q_EXPIRY, snapshot.dailyQuestionExpiry)
            .putString(KEY_PARTNER_NET, snapshot.partnerNetwork)
            .putString(KEY_PARTNER_APP, snapshot.partnerActiveApp)
            .putString(KEY_PARTNER_DEVICE, snapshot.partnerDeviceName)
            .putString(KEY_LOVE_JAR, snapshot.loveJarHighlight)
            .putString(KEY_SONG_VIBE, snapshot.songVibeHighlight)
            .putString(KEY_BUCKET_SUMMARY, snapshot.bucketSummary)
            .putString(KEY_THEME, snapshot.themeStyle.key)
            .apply()
    }

    fun updateChatPreview(
        msg1: String,
        msg2: String,
        receipt: String,
        coupleId: String? = null,
        partnerName: String? = null
    ) {
        prefs.edit().apply {
            putString(KEY_MSG_1, msg1)
            putString(KEY_MSG_2, msg2)
            putString(KEY_MSG_RECEIPT, receipt)
            if (!coupleId.isNullOrBlank()) putString(KEY_COUPLE_ID, coupleId)
            if (!partnerName.isNullOrBlank()) putString(KEY_PARTNER_NAME, partnerName)
            apply()
        }
    }

    fun updateLoveJarHighlight(text: String) {
        prefs.edit().putString(KEY_LOVE_JAR, text).apply()
    }

    fun updateSongVibeHighlight(text: String) {
        prefs.edit().putString(KEY_SONG_VIBE, text).apply()
    }

    fun updateBucketSummary(text: String) {
        prefs.edit().putString(KEY_BUCKET_SUMMARY, text).apply()
    }

    fun updateDailyQuestionPreview(question: String, status: String, expiry: String) {
        prefs.edit()
            .putString(KEY_DAILY_Q_TEXT, question)
            .putString(KEY_DAILY_Q_STATUS, status)
            .putString(KEY_DAILY_Q_EXPIRY, expiry)
            .apply()
    }

    fun setThemeStyle(style: WidgetThemeStyle) {
        prefs.edit().putString(KEY_THEME, style.key).apply()
    }

    fun resetToUnpaired() {
        val activeTheme = readSnapshot().themeStyle
        saveSnapshot(DuetWidgetSnapshot(themeStyle = activeTheme))
    }

    fun readSnapshot(): DuetWidgetSnapshot = DuetWidgetSnapshot(
        coupleId = prefs.getString(KEY_COUPLE_ID, "") ?: "",
        coupleTitle = prefs.getString(KEY_TITLE, "Duet") ?: "Duet",
        partnerName = prefs.getString(KEY_PARTNER_NAME, "Private Chat") ?: "Private Chat",
        partnerStatus = prefs.getString(KEY_PARTNER_STATUS, "End-to-end encrypted") ?: "End-to-end encrypted",
        partnerTyping = prefs.getBoolean(KEY_PARTNER_TYPING, false),
        batteryBadge = prefs.getString(KEY_BATTERY, "Live") ?: "Live",
        moodLine = prefs.getString(KEY_MOOD, "Tap to share your mood") ?: "Tap to share your mood",
        countdownLine = prefs.getString(KEY_COUNTDOWN, "Set anniversary & birthday") ?: "Set anniversary & birthday",
        statusFooter = prefs.getString(KEY_FOOTER, "End-to-end encrypted for two") ?: "End-to-end encrypted for two",
        lastMsg1 = prefs.getString(KEY_MSG_1, "No private messages yet") ?: "No private messages yet",
        lastMsg2 = prefs.getString(KEY_MSG_2, "Tap below to chat privately from home screen")
            ?: "Tap below to chat privately from home screen",
        lastMsgReceipt = prefs.getString(KEY_MSG_RECEIPT, "") ?: "",
        daysTogetherLabel = prefs.getString(KEY_DAYS_TOGETHER, "Set Anniversary") ?: "Set Anniversary",
        anniversaryCountdown = prefs.getString(KEY_ANNIVERSARY_CD, "Tap to set") ?: "Tap to set",
        birthdayCountdown = prefs.getString(KEY_BIRTHDAY_CD, "Tap to set") ?: "Tap to set",
        dailyQuestionText = prefs.getString(KEY_DAILY_Q_TEXT, "Tap to answer Today's Question")
            ?: "Tap to answer Today's Question",
        dailyQuestionStatus = prefs.getString(KEY_DAILY_Q_STATUS, "Open Duet to answer") ?: "Open Duet to answer",
        dailyQuestionExpiry = prefs.getString(KEY_DAILY_Q_EXPIRY, "Expires at 12:00 AM IST")
            ?: "Expires at 12:00 AM IST",
        partnerNetwork = prefs.getString(KEY_PARTNER_NET, "Live") ?: "Live",
        partnerActiveApp = prefs.getString(KEY_PARTNER_APP, "Duet") ?: "Duet",
        partnerDeviceName = prefs.getString(KEY_PARTNER_DEVICE, "Partner Phone") ?: "Partner Phone",
        loveJarHighlight = prefs.getString(KEY_LOVE_JAR, "Drop a sweet note in your Love Jar")
            ?: "Drop a sweet note in your Love Jar",
        songVibeHighlight = prefs.getString(KEY_SONG_VIBE, "Dedicate a song to each other")
            ?: "Dedicate a song to each other",
        bucketSummary = prefs.getString(KEY_BUCKET_SUMMARY, "0/0 Dreams Done") ?: "0/0 Dreams Done",
        themeStyle = WidgetThemeStyle.fromKey(prefs.getString(KEY_THEME, WidgetThemeStyle.ROSE.key))
    )

    companion object {
        private const val PREFS_NAME = "duet_widget_state"
        private const val KEY_COUPLE_ID = "couple_id"
        private const val KEY_TITLE = "title"
        private const val KEY_PARTNER_NAME = "partner_name"
        private const val KEY_PARTNER_STATUS = "partner_status"
        private const val KEY_PARTNER_TYPING = "partner_typing"
        private const val KEY_BATTERY = "battery"
        private const val KEY_MOOD = "mood"
        private const val KEY_COUNTDOWN = "countdown"
        private const val KEY_FOOTER = "footer"
        private const val KEY_MSG_1 = "last_msg_1"
        private const val KEY_MSG_2 = "last_msg_2"
        private const val KEY_MSG_RECEIPT = "last_msg_receipt"
        private const val KEY_DAYS_TOGETHER = "days_together"
        private const val KEY_ANNIVERSARY_CD = "anniversary_cd"
        private const val KEY_BIRTHDAY_CD = "birthday_cd"
        private const val KEY_DAILY_Q_TEXT = "daily_q_text"
        private const val KEY_DAILY_Q_STATUS = "daily_q_status"
        private const val KEY_DAILY_Q_EXPIRY = "daily_q_expiry"
        private const val KEY_PARTNER_NET = "partner_net"
        private const val KEY_PARTNER_APP = "partner_app"
        private const val KEY_PARTNER_DEVICE = "partner_device"
        private const val KEY_LOVE_JAR = "love_jar_highlight"
        private const val KEY_SONG_VIBE = "song_vibe_highlight"
        private const val KEY_BUCKET_SUMMARY = "bucket_summary"
        private const val KEY_THEME = "theme"

        fun updateAllDuetWidgets(context: Context) {
            DuetLoveWidgetProvider.updateAllWidgets(context)
            DuetQuickChatWidgetProvider.updateAllWidgets(context)
            DuetThemeWidgetProvider.updateAllWidgets(context)
            DuetCountdownWidgetProvider.updateAllWidgets(context)
            DuetMoodWidgetProvider.updateAllWidgets(context)
            DuetQuestionWidgetProvider.updateAllWidgets(context)
            DuetRadarWidgetProvider.updateAllWidgets(context)
            DuetCoupleHubWidgetProvider.updateAllWidgets(context)
        }
    }
}
