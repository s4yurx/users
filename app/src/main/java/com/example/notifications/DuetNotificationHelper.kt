package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppConfigUrlValidator

/**
 * Manages all Duet notification channels, persistent background sync notification,
 * and interactive notifications with Direct Inline Reply (RemoteInput), Send Pulse Back,
 * and Send Comfort Hug actions that execute directly in the background.
 */
object DuetNotificationHelper {
    const val CHANNEL_SYNC = "duet_background_sync"
    const val CHANNEL_MESSAGES = "duet_messages"
    const val CHANNEL_PULSES = "duet_pulses"
    const val CHANNEL_MOODS = "duet_moods"
    const val CHANNEL_EVENTS = "duet_moments_events"
    const val CHANNEL_QUESTIONS = "duet_daily_questions"
    const val CHANNEL_UPDATES = "duet_app_updates"

    const val NOTIFICATION_ID_SYNC = 1600
    const val NOTIFICATION_ID_MESSAGE = 1601
    const val NOTIFICATION_ID_PULSE = 1602
    const val NOTIFICATION_ID_MOOD = 1603
    const val NOTIFICATION_ID_MOMENT = 1604
    const val NOTIFICATION_ID_PROMPT = 1605
    const val NOTIFICATION_ID_EVENT = 1606
    const val NOTIFICATION_ID_REACTION = 1607
    const val NOTIFICATION_ID_PROMPT_EXPIRY = 1608
    const val NOTIFICATION_ID_BUCKET = 1609
    const val NOTIFICATION_ID_LOVE_LETTER = 1610
    const val NOTIFICATION_ID_LOVE_JAR = 1611
    const val NOTIFICATION_ID_SONG_VIBE = 1612
    const val NOTIFICATION_ID_CARE_CHECKIN = 1613
    const val NOTIFICATION_ID_DOODLE = 1614
    const val NOTIFICATION_ID_PROMISE = 1615
    const val NOTIFICATION_ID_PARTNER_REMOVED = 1616
    const val NOTIFICATION_ID_APP_UPDATE = 1618
    const val NOTIFICATION_ID_ADMIN_NOTICE = 1619
    const val NOTIFICATION_ID_PROMO_NOTICE = 1620
    const val NOTIFICATION_ID_PARTNER_BANNED = 1621

    const val EXTRA_OPEN_TAB = "duet_open_tab"
    const val EXTRA_OPEN_UPDATE_PROMPT = "duet_open_update_prompt"
    const val KEY_TEXT_REPLY = "duet_inline_reply_text"

    const val ACTION_INLINE_REPLY = "com.example.action.INLINE_REPLY"
    const val ACTION_SEND_PULSE_BACK = "com.example.action.SEND_PULSE_BACK"
    const val ACTION_SEND_HUG = "com.example.action.SEND_HUG"

    const val EXTRA_COUPLE_ID = "extra_couple_id"
    const val EXTRA_PARTNER_NAME = "extra_partner_name"
    const val EXTRA_MOOD_ID = "extra_mood_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val syncChannel = NotificationChannel(
            CHANNEL_SYNC,
            "Duet Live Background Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps encrypted messages, love pulses, and live partner status synced in the background."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val messageChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Private Encrypted Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Instant encrypted messages and DM photos from your partner with quick inline reply."
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 120L, 80L, 120L)
        }

        val pulseChannel = NotificationChannel(
            CHANNEL_PULSES,
            "Love Pulses",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thinking-of-you heartbeat pulses from your partner."
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 150L, 90L, 240L, 100L, 150L)
        }

        val moodChannel = NotificationChannel(
            CHANNEL_MOODS,
            "Partner Moods & Hugs",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts whenever your partner updates their mood or sends you a comfort hug."
            enableVibration(true)
        }

        val eventChannel = NotificationChannel(
            CHANNEL_EVENTS,
            "Moments, Questions & Countdowns",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts for new shared photos, unlocked daily questions, and countdown dates."
            enableVibration(true)
        }

        val questionsChannel = NotificationChannel(
            CHANNEL_QUESTIONS,
            "Today's Question & Expiry Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when your partner answers Today's Question or when 1 hour remains before expiry."
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 120L, 80L, 160L)
        }

        val updatesChannel = NotificationChannel(
            CHANNEL_UPDATES,
            "App Updates & Service Notices",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important Bondly update releases, announcements, and maintenance alerts."
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 140L, 90L, 140L)
        }

        manager.createNotificationChannels(
            listOf(syncChannel, messageChannel, pulseChannel, moodChannel, eventChannel, questionsChannel, updatesChannel)
        )
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun buildForegroundSyncNotification(
        context: Context,
        title: String,
        subtitle: String
    ): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(createOpenAppIntent(context, "home", 900))
            .build()
    }

    fun showMessageNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        messagePreview: String,
        isPhoto: Boolean,
        sentAtMillis: Long = System.currentTimeMillis()
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val me = Person.Builder().setName("You").build()
        val partner = Person.Builder().setName(partnerName).build()
        val displayBody = when {
            isPhoto && messagePreview.isNotBlank() -> "Photo: $messagePreview"
            isPhoto -> "Sent you an encrypted photo"
            else -> messagePreview.ifBlank { "New encrypted message" }
        }

        val style = NotificationCompat.MessagingStyle(me)
            .setConversationTitle("$partnerName • Private Duet")
            .addMessage(displayBody, sentAtMillis, partner)

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createOpenAppIntent(context, "chat", 901))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_MESSAGE, 1001))
            .addAction(createPulseBackAction(context, coupleId, partnerName, NOTIFICATION_ID_MESSAGE, 1002))

        safeNotify(context, NOTIFICATION_ID_MESSAGE, builder.build())
    }

    fun showReplyConfirmedNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        replyText: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val me = Person.Builder().setName("You").build()
        val style = NotificationCompat.MessagingStyle(me)
            .setConversationTitle("$partnerName • Private Duet")
            .addMessage(replyText, System.currentTimeMillis(), me)

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setStyle(style)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(4500L)
            .setContentIntent(createOpenAppIntent(context, "chat", 901))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_MESSAGE, 1001))
            .build()

        safeNotify(context, NOTIFICATION_ID_MESSAGE, notification)
    }

    fun showPulseNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        totalCount: Long
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_PULSES)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle("$partnerName sent you a Love Pulse ❤️")
            .setContentText("Thinking of you right now • $totalCount pulses together")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createOpenAppIntent(context, "home", 902))
            .addAction(createPulseBackAction(context, coupleId, partnerName, NOTIFICATION_ID_PULSE, 1003))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_PULSE, 1004))
            .build()

        safeNotify(context, NOTIFICATION_ID_PULSE, notification)
    }

    fun showMoodNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        moodId: String,
        moodEmoji: String,
        moodLabel: String,
        moodNote: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val body = if (moodNote.isNotBlank()) {
            "$moodEmoji $moodLabel — “$moodNote”"
        } else {
            "$partnerName is feeling $moodEmoji $moodLabel right now"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_MOODS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle("$partnerName updated their mood")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createOpenAppIntent(context, "home", 903))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_MOOD, 1005))

        if (moodId.isNotBlank()) {
            builder.addAction(createSendHugAction(context, coupleId, moodId, NOTIFICATION_ID_MOOD, 1006))
        }

        safeNotify(context, NOTIFICATION_ID_MOOD, builder.build())
    }

    fun showComfortHugNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        moodLabel: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_MOODS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle("$partnerName sent you a warm hug 🫂")
            .setContentText("Comforting you after your \"$moodLabel\" check-in")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createOpenAppIntent(context, "home", 904))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_MOOD, 1007))
            .build()

        safeNotify(context, NOTIFICATION_ID_MOOD, notification)
    }

    fun showMomentNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        caption: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val body = if (caption.isNotBlank()) "“$caption”" else "Uploaded a new encrypted keepsake photo for you"
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle("$partnerName uploaded a new photo")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createOpenAppIntent(context, "moments", 905))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_MOMENT, 1008))
            .build()

        safeNotify(context, NOTIFICATION_ID_MOMENT, notification)
    }

    fun showDailyPromptNotification(
        context: Context,
        partnerName: String,
        bothUnlocked: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val title = if (bothUnlocked) {
            "Today's Question Unlocked! ✨"
        } else {
            "$partnerName answered Today's Question! ✨"
        }
        val body = if (bothUnlocked) {
            "Both of you answered — tap to read $partnerName's reply right now."
        } else {
            "$partnerName just sealed their answer! Tap to write yours before midnight."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_QUESTIONS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 906))
            .build()

        safeNotify(context, NOTIFICATION_ID_PROMPT, notification)
    }

    fun showDailyPromptExpiryNotification(
        context: Context,
        minutesRemaining: Long
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val mins = minutesRemaining.coerceIn(1L, 60L)
        val timeText = if (mins >= 60L) "1 hour" else "$mins minutes"
        val title = "Today's Question expires in $timeText! ⏳"
        val body = "You haven't answered Today's Question yet. Seal your reply before midnight IST!"

        val notification = NotificationCompat.Builder(context, CHANNEL_QUESTIONS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 909))
            .build()

        safeNotify(context, NOTIFICATION_ID_PROMPT_EXPIRY, notification)
    }

    fun showBucketListNotification(
        context: Context,
        partnerName: String,
        dreamTitle: String,
        completed: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val title = if (completed) {
            "$partnerName checked off a couple dream! 🎉"
        } else {
            "$partnerName added to your Bucket List ✨"
        }
        val body = if (completed) {
            "Completed together: “$dreamTitle”"
        } else {
            "New shared dream: “$dreamTitle”"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createOpenAppIntent(context, "home", 910))
            .build()

        safeNotify(context, NOTIFICATION_ID_BUCKET, notification)
    }

    fun showSpecialDateNotification(
        context: Context,
        partnerName: String,
        eventTitle: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle("$partnerName added a countdown")
            .setContentText("Counting down together to \"$eventTitle\"")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "dates", 907))
            .build()

        safeNotify(context, NOTIFICATION_ID_EVENT, notification)
    }

    fun showReactionNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        reactionEmoji: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle("$partnerName reacted $reactionEmoji")
            .setContentText("Reacted to your encrypted message")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "chat", 908))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_REACTION, 1009))
            .build()

        safeNotify(context, NOTIFICATION_ID_REACTION, notification)
    }

    fun showLoveLetterNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        letterTitle: String,
        isTimeCapsuleLocked: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val title = if (isTimeCapsuleLocked) {
            "$partnerName sealed a Time Capsule Letter ⏳💌"
        } else {
            "$partnerName sent you a Love Letter 💌"
        }
        val body = if (isTimeCapsuleLocked) {
            "“$letterTitle” — sealed until its special unlock time!"
        } else {
            "Tap to break the wax seal on “$letterTitle”"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 911))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_LOVE_LETTER, 1011))
            .build()
        safeNotify(context, NOTIFICATION_ID_LOVE_LETTER, notification)
    }

    fun showLoveJarNotification(
        context: Context,
        partnerName: String,
        noteText: String,
        granted: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val title = if (granted) {
            "$partnerName granted a Love Jar wish! 🌠"
        } else {
            "$partnerName dropped a note in your Love Jar 🫙💖"
        }
        val body = "“${noteText.take(120)}”"
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 912))
            .build()
        safeNotify(context, NOTIFICATION_ID_LOVE_JAR, notification)
    }

    fun showSongDedicationNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        songTitle: String,
        artistName: String,
        vibeEmoji: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val title = "$partnerName dedicated a song to you $vibeEmoji"
        val body = "“$songTitle” by $artistName — tap to feel the vibe"
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 913))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_SONG_VIBE, 1013))
            .build()
        safeNotify(context, NOTIFICATION_ID_SONG_VIBE, notification)
    }

    fun showCareCheckInNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        emoji: String,
        message: String,
        acknowledged: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val title = if (acknowledged) {
            "$partnerName completed your care check-in! ❤️"
        } else {
            "$emoji Care check-in from $partnerName"
        }
        val body = if (acknowledged) {
            "$partnerName says: Done! Thanks for taking care of me — “$message”"
        } else {
            message
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_MOODS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 914))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_CARE_CHECKIN, 1014))
            .build()
        safeNotify(context, NOTIFICATION_ID_CARE_CHECKIN, notification)
    }

    fun showDoodleNotification(
        context: Context,
        coupleId: String,
        partnerName: String,
        titleText: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val title = "$partnerName drew a Love Doodle for you 🎨"
        val body = if (titleText.isNotBlank()) "“$titleText” — tap to view in HD!" else "Tap to view their hand-drawn sketch!"
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 915))
            .addAction(createInlineReplyAction(context, coupleId, partnerName, NOTIFICATION_ID_DOODLE, 1015))
            .build()
        safeNotify(context, NOTIFICATION_ID_DOODLE, notification)
    }

    fun showPromiseNotification(
        context: Context,
        partnerName: String,
        promiseTitle: String,
        coSigned: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val title = if (coSigned) {
            "$partnerName pinky-swore your promise! 🤙💍"
        } else {
            "$partnerName made a Couple Promise 🤙"
        }
        val body = "“$promiseTitle”"
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 916))
            .build()
        safeNotify(context, NOTIFICATION_ID_PROMISE, notification)
    }

    fun showPartnerRemovedNotification(
        context: Context,
        removerName: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val cleanName = removerName.trim().ifBlank { "Your partner" }
        val title = "Partner link removed"
        val body = "$cleanName left your Duet space. All shared chats & moments were erased and your 6-digit code was reset. Tap to link with a new partner."
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 917))
            .build()
        safeNotify(context, NOTIFICATION_ID_PARTNER_REMOVED, notification)
    }

    fun showAppUpdateNotification(
        context: Context,
        newVersion: String,
        title: String,
        releaseNotes: String,
        updateLink: String,
        isForced: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val resolvedTitle = title.trim().ifBlank {
            "Update Available • Duet v$newVersion"
        }
        val resolvedBody = releaseNotes.trim().ifBlank {
            if (isForced) {
                "Version $newVersion is available and older versions are disabled. Tap Download Now to update."
            } else {
                "Version $newVersion is ready from Bondly. Tap Download Now to install the latest release."
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_UPDATE_PROMPT, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPi = PendingIntent.getActivity(
            context,
            918,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(resolvedTitle)
            .setContentText(resolvedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(resolvedBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openAppPi)

        if (AppConfigUrlValidator.isSafeWebUrl(updateLink)) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateLink.trim())).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val browserPi = PendingIntent.getActivity(
                context,
                1018,
                browserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_duet_notification,
                "Download Now",
                browserPi
            )
        }

        safeNotify(context, NOTIFICATION_ID_APP_UPDATE, builder.build())
    }

    fun showAdminNoticeNotification(
        context: Context,
        title: String,
        body: String,
        isMaintenance: Boolean,
        notificationId: Int = NOTIFICATION_ID_ADMIN_NOTICE
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val resolvedTitle = title.trim().ifBlank {
            if (isMaintenance) "Duet Maintenance Notice" else "Bondly Service Announcement"
        }
        val resolvedBody = body.trim().ifBlank {
            if (isMaintenance) {
                "Duet is currently undergoing maintenance. We will be back online shortly."
            } else {
                "Tap to open Duet and read the latest announcement."
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle(resolvedTitle)
            .setContentText(resolvedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(resolvedBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isMaintenance) NotificationCompat.CATEGORY_STATUS else NotificationCompat.CATEGORY_SOCIAL)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 900 + (notificationId % 100)))
            .build()

        safeNotify(context, notificationId, notification)
    }

    fun showPartnerBannedNotification(
        context: Context,
        partnerName: String,
        reason: String?
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val cleanName = partnerName.trim().ifBlank { "Your partner" }
        val cleanReason = reason?.trim().orEmpty()
        val body = if (cleanReason.isNotBlank()) {
            "$cleanName's account was banned by admin. Reason: $cleanReason"
        } else {
            "$cleanName's account was banned by admin. Open Duet to view status or remove shared data."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_duet_notification)
            .setContentTitle("$cleanName's Account Banned")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppIntent(context, "home", 921))
            .build()

        safeNotify(context, NOTIFICATION_ID_PARTNER_BANNED, notification)
    }

    fun showTestNotificationWithReply(context: Context, coupleId: String, partnerName: String) {
        showMessageNotification(
            context = context,
            coupleId = coupleId,
            partnerName = partnerName,
            messagePreview = "Notifications & background inline reply are active! Try replying right here.",
            isPhoto = false
        )
    }

    private fun createInlineReplyAction(
        context: Context,
        coupleId: String,
        partnerName: String,
        notificationId: Int,
        requestCode: Int
    ): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply privately…")
            .build()

        val replyIntent = Intent(context, DuetNotificationActionReceiver::class.java).apply {
            action = ACTION_INLINE_REPLY
            putExtra(EXTRA_COUPLE_ID, coupleId)
            putExtra(EXTRA_PARTNER_NAME, partnerName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val mutableFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            replyIntent,
            mutableFlags
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_duet_notification,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()
    }

    private fun createPulseBackAction(
        context: Context,
        coupleId: String,
        partnerName: String,
        notificationId: Int,
        requestCode: Int
    ): NotificationCompat.Action {
        val intent = Intent(context, DuetNotificationActionReceiver::class.java).apply {
            action = ACTION_SEND_PULSE_BACK
            putExtra(EXTRA_COUPLE_ID, coupleId)
            putExtra(EXTRA_PARTNER_NAME, partnerName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_duet_notification,
            "Pulse Back ❤️",
            pi
        ).build()
    }

    private fun createSendHugAction(
        context: Context,
        coupleId: String,
        moodId: String,
        notificationId: Int,
        requestCode: Int
    ): NotificationCompat.Action {
        val intent = Intent(context, DuetNotificationActionReceiver::class.java).apply {
            action = ACTION_SEND_HUG
            putExtra(EXTRA_COUPLE_ID, coupleId)
            putExtra(EXTRA_MOOD_ID, moodId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_duet_notification,
            "Send Hug 🫂",
            pi
        ).build()
    }

    private fun createOpenAppIntent(context: Context, tab: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_TAB, tab)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun safeNotify(context: Context, id: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        }
    }
}
