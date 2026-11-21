package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.crypto.DuetCryptoManager
import com.example.data.CareCheckInCatalog
import com.example.data.CountdownCalculator
import com.example.data.CoupleSpace
import com.example.data.DailyPromptDeck
import com.example.data.DuetRepository
import com.example.data.LiveAppConfig
import com.example.data.MoodCatalog
import com.example.data.PartnerBannedNotice
import com.example.data.PartnerPresence
import com.example.notifications.DuetNotificationHelper
import com.example.notifications.DuetNotificationStateStore
import com.example.telemetry.DeviceTelemetryCollector
import com.example.ui.theme.DuetAppearanceStore
import com.example.widget.DuetCoupleHubWidgetProvider
import com.example.widget.DuetQuestionWidgetProvider
import com.example.widget.WidgetStateStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Persistent foreground dataSync service that keeps Duet alive in the background:
 * 1. Fires instant notifications (with Direct Inline Reply & Quick Actions) whenever your partner
 *    sends a message, DM photo, love pulse, mood update, comfort hug, moment photo, daily prompt
 *    answer, special date, or message reaction.
 * 2. Publishes real-time device telemetry (battery %, charging, network, lock state, and currently
 *    open foreground app such as Instagram or WhatsApp) while preserving WhatsApp-style Last Seen.
 * 3. Keeps the cute Android home screen widget refreshed even when the app UI is closed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DuetSyncService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var activeUid: String? = null
    private lateinit var stateStore: DuetNotificationStateStore
    private lateinit var widgetStore: WidgetStateStore

    override fun onCreate() {
        super.onCreate()
        stateStore = DuetNotificationStateStore(this)
        widgetStore = WidgetStateStore(this)
        DuetNotificationHelper.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!stateStore.isBackgroundSyncEnabled()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val uid = Firebase.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        promoteToForeground("Duet Live Sync", "Keeping encrypted chat & partner status connected")

        if (monitorJob == null || monitorJob?.isActive != true || activeUid != uid) {
            activeUid = uid
            monitorJob?.cancel()
            monitorJob = serviceScope.launch {
                runRealtimeMonitors(uid)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun promoteToForeground(title: String, subtitle: String) {
        val notification = DuetNotificationHelper.buildForegroundSyncNotification(this, title, subtitle)
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                DuetNotificationHelper.NOTIFICATION_ID_SYNC,
                notification,
                type
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to enter foreground service state", e)
        }
    }

    private fun updateOngoingSyncNotification(title: String, subtitle: String) {
        if (!DuetNotificationHelper.hasNotificationPermission(this)) return
        try {
            val notification = DuetNotificationHelper.buildForegroundSyncNotification(this, title, subtitle)
            NotificationManagerCompat.from(this).notify(DuetNotificationHelper.NOTIFICATION_ID_SYNC, notification)
        } catch (_: SecurityException) {
        }
    }

    private suspend fun runRealtimeMonitors(uid: String) {
        val repository = DuetRepository(this)
        val crypto = DuetCryptoManager(this, uid)
        val telemetryCollector = DeviceTelemetryCollector(this)
        val latestCouple = MutableStateFlow<CoupleSpace?>(null)
        val latestShareStatus = MutableStateFlow(true)
        val latestPartnerNickname = MutableStateFlow<String?>(null)
        val latestMyPresence = MutableStateFlow<PartnerPresence?>(null)
        val latestLiveConfig = MutableStateFlow(LiveAppConfig())
        val latestPartnerBanned = MutableStateFlow<PartnerBannedNotice?>(null)
        var lastSyncedKey: String? = null
        var lastSyncedAtMs: Long = 0L

        val profileFlow = repository.observeUserProfile(uid)
            .catch { Log.w(TAG, "Background profile flow error", it) }

        val coupleFlow = profileFlow
            .map { profile ->
                if (profile?.isAccountBanned() == true) {
                    Log.w(TAG, "Account suspended/banned; stopping background sync.")
                    stopSelf()
                    return@map null
                }
                latestShareStatus.value = profile?.shareDeviceStatus ?: true
                latestPartnerNickname.value = profile?.partnerNickname
                profile?.coupleId?.takeIf { it.isNotBlank() }
            }
            .flatMapLatest { coupleId ->
                if (coupleId == null) flowOf(null)
                else repository.observeCoupleSpace(coupleId, uid)
                    .catch { Log.w(TAG, "Background couple flow error", it) }
            }

        // 0. Observe Live Bondly OTA App Config (New Updates, Maintenance & Announcements)
        serviceScope.launch {
            repository.observeLiveAppConfig()
                .catch { Log.w(TAG, "Background live app_config flow error", it) }
                .collect { config ->
                    latestLiveConfig.value = config
                    val hasUpdate = config.hasAnyUpdateForNotification(
                        currentVersionName = BuildConfig.VERSION_NAME,
                        currentVersionCode = BuildConfig.VERSION_CODE
                    )
                    if (hasUpdate) {
                        val updateKey = "ota_update_${config.updateSignature()}"
                        if (!stateStore.hasNotified(updateKey)) {
                            stateStore.markNotified(updateKey)
                            val targetVer = config.notificationUpdateVersion(BuildConfig.VERSION_NAME)
                            val isForced = config.isForceUpdateRequired(
                                currentVersionName = BuildConfig.VERSION_NAME,
                                currentVersionCode = BuildConfig.VERSION_CODE
                            )
                            DuetNotificationHelper.showAppUpdateNotification(
                                context = this@DuetSyncService,
                                newVersion = targetVer,
                                title = config.updateTitle,
                                releaseNotes = config.updateMessage,
                                updateLink = config.effectiveUpdateLink(packageName),
                                isForced = isForced
                            )
                        }
                    }

                    if (config.maintenanceMode && !DuetNotificationStateStore.isAppInForeground) {
                        val maintKey = "ota_maint_${config.maintenanceTitle.trim()}|${config.maintenanceMessage.trim()}"
                        if (!stateStore.hasNotified(maintKey)) {
                            stateStore.markNotified(maintKey)
                            DuetNotificationHelper.showAdminNoticeNotification(
                                context = this@DuetSyncService,
                                title = config.maintenanceTitle.ifBlank { "Duet Under Maintenance" },
                                body = config.maintenanceMessage,
                                isMaintenance = true
                            )
                        }
                    } else if (config.upcomingMaintenance &&
                        config.upcomingMaintenanceText.isNotBlank() &&
                        !DuetNotificationStateStore.isAppInForeground
                    ) {
                        val upcomingKey = "ota_upcoming_${config.upcomingMaintenanceSignature()}"
                        if (!stateStore.hasNotified(upcomingKey)) {
                            stateStore.markNotified(upcomingKey)
                            DuetNotificationHelper.showAdminNoticeNotification(
                                context = this@DuetSyncService,
                                title = "Scheduled Duet Maintenance",
                                body = config.upcomingMaintenanceText,
                                isMaintenance = true
                            )
                        }
                    }

                    if (config.shouldShowAnnouncementBanner() &&
                        !config.maintenanceMode &&
                        !DuetNotificationStateStore.isAppInForeground
                    ) {
                        val bannerKey = "ota_banner_${config.bannerSignature()}"
                        if (!stateStore.hasNotified(bannerKey)) {
                            stateStore.markNotified(bannerKey)
                            DuetNotificationHelper.showAdminNoticeNotification(
                                context = this@DuetSyncService,
                                title = config.bannerTitle.ifBlank { "Bondly Announcement" },
                                body = config.resolvedBannerText(),
                                isMaintenance = false,
                                notificationId = DuetNotificationHelper.NOTIFICATION_ID_ADMIN_NOTICE
                            )
                        }
                    }

                    if (config.shouldShowPromoBanner() &&
                        !config.maintenanceMode &&
                        !DuetNotificationStateStore.isAppInForeground
                    ) {
                        val promoKey = "ota_promo_${config.promoSignature()}"
                        if (!stateStore.hasNotified(promoKey)) {
                            stateStore.markNotified(promoKey)
                            DuetNotificationHelper.showAdminNoticeNotification(
                                context = this@DuetSyncService,
                                title = config.promoTitle.ifBlank { "Bondly Special Offer" },
                                body = config.resolvedPromoText(),
                                isMaintenance = false,
                                notificationId = DuetNotificationHelper.NOTIFICATION_ID_PROMO_NOTICE
                            )
                        }
                    }
                }
        }

        // 1. Observe CoupleSpace (Dissolution auto-reset + Love Pulses + baseline tracking)
        serviceScope.launch {
            var initializedCoupleId: String? = null
            var handledDissolvedCoupleId: String? = null
            coupleFlow.collect { couple ->
                latestCouple.value = couple
                if (couple == null) {
                    if (initializedCoupleId != null) {
                        widgetStore.resetToUnpaired()
                        WidgetStateStore.updateAllDuetWidgets(this@DuetSyncService)
                        stopSelf()
                    }
                    return@collect
                }

                if (couple.status == "dissolved") {
                    if (handledDissolvedCoupleId != couple.id) {
                        handledDissolvedCoupleId = couple.id
                        if (couple.dissolvedByUid != uid) {
                            val removerName = couple.dissolvedByName?.takeIf { it.isNotBlank() }
                                ?: couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                            val dissolvedNotifyKey = "dissolved_${couple.id}"
                            if (!stateStore.hasNotified(dissolvedNotifyKey)) {
                                stateStore.markNotified(dissolvedNotifyKey)
                                DuetNotificationHelper.showPartnerRemovedNotification(
                                    context = this@DuetSyncService,
                                    removerName = removerName
                                )
                            }
                            if (!DuetNotificationStateStore.isAppInForeground) {
                                stateStore.setLastExcludedInviteCode(couple.inviteCode)
                                stateStore.setPendingRemovalNotice(
                                    "$removerName removed the partner link. All shared chats & moments were reset — enter or create a new 6-digit code to start fresh."
                                )
                                val freshPubKey = crypto.rotateKeyPair()
                                repository.completePartnerRemovalForRemovedUser(couple, freshPubKey)
                                DuetAppearanceStore.clearCustomChatWallpaper(this@DuetSyncService)
                                widgetStore.resetToUnpaired()
                                WidgetStateStore.updateAllDuetWidgets(this@DuetSyncService)
                                stateStore.clearCoupleNotifications()
                                stopSelf()
                            }
                        }
                    }
                    return@collect
                }

                val partnerLabel = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                val storedCount = stateStore.getLastPulseCount(couple.id)
                if (initializedCoupleId != couple.id) {
                    initializedCoupleId = couple.id
                    if (storedCount < 0L) {
                        stateStore.setLastPulseCount(couple.id, couple.lovePulseCount)
                    } else if (couple.lovePulseCount > storedCount && couple.lastPulseByUid != uid) {
                        stateStore.setLastPulseCount(couple.id, couple.lovePulseCount)
                        DuetNotificationHelper.showPulseNotification(
                            context = this@DuetSyncService,
                            coupleId = couple.id,
                            partnerName = partnerLabel,
                            totalCount = couple.lovePulseCount
                        )
                    } else {
                        stateStore.setLastPulseCount(couple.id, couple.lovePulseCount)
                    }
                } else {
                    val prev = stateStore.getLastPulseCount(couple.id)
                    if (couple.lovePulseCount > prev) {
                        stateStore.setLastPulseCount(couple.id, couple.lovePulseCount)
                        if (couple.lastPulseByUid != null && couple.lastPulseByUid != uid) {
                            DuetNotificationHelper.showPulseNotification(
                                context = this@DuetSyncService,
                                coupleId = couple.id,
                                partnerName = partnerLabel,
                                totalCount = couple.lovePulseCount
                            )
                        }
                    }
                }
            }
        }

        // 1b. Observe Paired Partner's Account Ban Status (Notify once with admin reason + mark UI/widget)
        serviceScope.launch {
            latestCouple
                .map { couple ->
                    if (couple == null || couple.status != "paired") null
                    else couple.otherUid(uid)?.takeIf { it.isNotBlank() }
                }
                .distinctUntilChanged()
                .flatMapLatest { partnerUid ->
                    if (partnerUid == null) flowOf(null)
                    else repository.observePartnerUserProfile(partnerUid)
                }
                .collect { partnerProfile ->
                    val couple = latestCouple.value
                    val partnerUid = couple?.otherUid(uid)?.takeIf { it.isNotBlank() }
                    if (couple == null || couple.status != "paired" || partnerUid == null ||
                        partnerProfile == null || partnerProfile.userId != partnerUid || !partnerProfile.isAccountBanned()
                    ) {
                        latestPartnerBanned.value = null
                        return@collect
                    }
                    val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                    val reason = partnerProfile.resolvedBanReason()
                    val bannedAtMillis = partnerProfile.bannedAt?.toDate()?.time
                    latestPartnerBanned.value = PartnerBannedNotice(
                        partnerUid = partnerUid,
                        partnerName = partnerName,
                        reason = reason,
                        bannedAtMillis = bannedAtMillis
                    )
                    updateOngoingSyncNotification(
                        title = "Duet • $partnerName",
                        subtitle = "Account banned by admin"
                    )
                    refreshHomeWidget(
                        couple = couple,
                        myUid = uid,
                        partner = null,
                        mine = latestMyPresence.value,
                        customPartnerNickname = latestPartnerNickname.value,
                        isPartnerBanned = true
                    )
                    val notifyKey = "partner_banned_${couple.id}_${partnerUid}_${bannedAtMillis ?: 0L}_${reason.orEmpty()}"
                    if (!stateStore.hasNotified(notifyKey)) {
                        stateStore.markNotified(notifyKey)
                        DuetNotificationHelper.showPartnerBannedNotification(
                            context = this@DuetSyncService,
                            partnerName = partnerName,
                            reason = reason
                        )
                    }
                }
        }

        // 2. Observe Messages & Reactions -> Decrypt & Notify with Inline Reply
        serviceScope.launch {
            var baselineCoupleId: String? = null
            val seenPartnerReactions = mutableMapOf<String, String>()
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeMessages(coupleId, uid)
                        .catch { Log.w(TAG, "Background messages flow error", it) }
                }
                .collect { messages ->
                    val couple = latestCouple.value ?: return@collect
                    val partnerUid = couple.otherUid(uid)
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        seenPartnerReactions.clear()
                        messages.forEach { msg ->
                            stateStore.markNotified("msg_${msg.id}")
                            val existingPartnerReaction = partnerUid?.let { msg.reactions[it] }
                            if (!existingPartnerReaction.isNullOrBlank()) {
                                seenPartnerReactions[msg.id] = existingPartnerReaction
                            }
                        }
                        return@collect
                    }

                    val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                    val chatSuppressed = DuetNotificationStateStore.isAppInForeground &&
                        DuetNotificationStateStore.isChatTabVisible

                    for (msg in messages) {
                        val msgKey = "msg_${msg.id}"
                        if (msg.senderUid != uid && !stateStore.hasNotified(msgKey)) {
                            stateStore.markNotified(msgKey)
                            if (!chatSuppressed) {
                                val plain = crypto.decrypt(
                                    ciphertext = msg.ciphertext,
                                    ivBase64 = msg.iv,
                                    keyMode = msg.keyMode,
                                    coupleId = couple.id,
                                    inviteCode = couple.inviteCode,
                                    wrappedVaultKey = couple.wrappedVaultKey,
                                    partnerPublicKey = couple.otherPublicKey(uid)
                                )
                                val preview = extractJsonField(plain, "text")
                                    .ifBlank { if (msg.messageType == "IMAGE") "" else "New encrypted message" }
                                DuetNotificationHelper.showMessageNotification(
                                    context = this@DuetSyncService,
                                    coupleId = couple.id,
                                    partnerName = partnerName,
                                    messagePreview = preview,
                                    isPhoto = msg.messageType == "IMAGE",
                                    sentAtMillis = msg.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                                )
                            }
                        }

                        // Notify ONLY when the PARTNER (partnerUid != uid) adds or changes their reaction.
                        // Never notify for the current user's own reaction!
                        val partnerReaction = partnerUid?.takeIf { it != uid }?.let { msg.reactions[it] }
                        if (partnerReaction.isNullOrBlank()) {
                            seenPartnerReactions.remove(msg.id)
                        } else if (seenPartnerReactions[msg.id] != partnerReaction) {
                            seenPartnerReactions[msg.id] = partnerReaction
                            val reactKey = "react_${msg.id}_${partnerUid}_$partnerReaction"
                            if (!stateStore.hasNotified(reactKey)) {
                                stateStore.markNotified(reactKey)
                                if (!chatSuppressed) {
                                    DuetNotificationHelper.showReactionNotification(
                                        context = this@DuetSyncService,
                                        coupleId = couple.id,
                                        partnerName = partnerName,
                                        reactionEmoji = reactionEmoji(partnerReaction)
                                    )
                                }
                            }
                        }
                    }

                    // Keep Quick-Chat Home Screen Widget refreshed in background
                    val lastTwo = messages.takeLast(2)
                    if (lastTwo.isNotEmpty()) {
                        val lines = lastTwo.map { m ->
                            val plain = crypto.decrypt(
                                ciphertext = m.ciphertext,
                                ivBase64 = m.iv,
                                keyMode = m.keyMode,
                                coupleId = couple.id,
                                inviteCode = couple.inviteCode,
                                wrappedVaultKey = couple.wrappedVaultKey,
                                partnerPublicKey = couple.otherPublicKey(uid)
                            )
                            val body = extractJsonField(plain, "text").ifBlank {
                                if (m.messageType == "IMAGE") "📷 Encrypted photo" else "New message"
                            }
                            val prefix = if (m.senderUid == uid) "You" else partnerName.substringBefore(" ")
                            "$prefix: $body"
                        }
                        val latest = lastTwo.last()
                        val receipt = when {
                            latest.senderUid != uid -> ""
                            latest.isSeenByOther(uid) -> "✓✓ Seen"
                            else -> "✓ Sent"
                        }
                        widgetStore.updateChatPreview(
                            msg1 = if (lines.size >= 2) lines[0] else "🔒 End-to-end encrypted",
                            msg2 = lines.last(),
                            receipt = receipt,
                            coupleId = couple.id,
                            partnerName = partnerName
                        )
                        WidgetStateStore.updateAllDuetWidgets(this@DuetSyncService)
                    }
                }
        }

        // 3. Observe Shared Moods & Comfort Hugs
        serviceScope.launch {
            var baselineCoupleId: String? = null
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeMoods(coupleId, uid)
                        .catch { Log.w(TAG, "Background moods flow error", it) }
                }
                .collect { moods ->
                    val couple = latestCouple.value ?: return@collect
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        moods.forEach { m ->
                            stateStore.markNotified("mood_${m.id}")
                            if (!m.comfortSentByUid.isNullOrBlank()) {
                                stateStore.markNotified("hug_${m.id}_${m.comfortSentByUid}")
                            }
                        }
                        return@collect
                    }

                    val partnerName = couple.partnerDisplayName(uid)
                    if (!latestLiveConfig.value.moodSyncEnabled) return@collect
                    for (entry in moods) {
                        val moodKey = "mood_${entry.id}"
                        if (entry.userId != uid && !stateStore.hasNotified(moodKey)) {
                            stateStore.markNotified(moodKey)
                            val option = MoodCatalog.byKey(entry.moodKey)
                            DuetNotificationHelper.showMoodNotification(
                                context = this@DuetSyncService,
                                coupleId = couple.id,
                                partnerName = entry.authorName.ifBlank { partnerName },
                                moodId = entry.id,
                                moodEmoji = option.emoji,
                                moodLabel = option.label,
                                moodNote = entry.note
                            )
                        }
                        val hugger = entry.comfortSentByUid
                        if (entry.userId == uid && !hugger.isNullOrBlank() && hugger != uid) {
                            val hugKey = "hug_${entry.id}_$hugger"
                            if (!stateStore.hasNotified(hugKey)) {
                                stateStore.markNotified(hugKey)
                                val option = MoodCatalog.byKey(entry.moodKey)
                                DuetNotificationHelper.showComfortHugNotification(
                                    context = this@DuetSyncService,
                                    coupleId = couple.id,
                                    partnerName = partnerName,
                                    moodLabel = option.label
                                )
                            }
                        }
                    }
                }
        }

        // 4. Observe Encrypted Photo Moments
        serviceScope.launch {
            var baselineCoupleId: String? = null
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeMoments(coupleId, uid)
                        .catch { Log.w(TAG, "Background moments flow error", it) }
                }
                .collect { moments ->
                    val couple = latestCouple.value ?: return@collect
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        moments.forEach { stateStore.markNotified("mom_${it.id}") }
                        return@collect
                    }
                    val partnerName = couple.partnerDisplayName(uid)
                    if (!latestLiveConfig.value.sharedJournalEnabled) return@collect
                    for (moment in moments) {
                        val key = "mom_${moment.id}"
                        if (moment.uploaderUid != uid && !stateStore.hasNotified(key)) {
                            stateStore.markNotified(key)
                            val plain = crypto.decrypt(
                                ciphertext = moment.ciphertext,
                                ivBase64 = moment.iv,
                                keyMode = moment.keyMode,
                                coupleId = couple.id,
                                inviteCode = couple.inviteCode,
                                wrappedVaultKey = couple.wrappedVaultKey,
                                partnerPublicKey = couple.otherPublicKey(uid)
                            )
                            val caption = extractJsonField(plain, "caption")
                            DuetNotificationHelper.showMomentNotification(
                                context = this@DuetSyncService,
                                coupleId = couple.id,
                                partnerName = moment.uploaderName.ifBlank { partnerName },
                                caption = caption
                            )
                        }
                    }
                }
        }

        // 5. Observe Daily Couple Prompt (midnight-aware + 1-hour expiry alert if unanswered + partner-answered alert)
        val latestDailyPromptState = MutableStateFlow<Pair<String, com.example.data.DailyPromptEntry?>?>(null)
        serviceScope.launch {
            var baselineTag: String? = null
            val dateKeyTicker = flow {
                while (currentCoroutineContext().isActive) {
                    emit(DailyPromptDeck.todayDateKey())
                    delay(30_000L)
                }
            }.distinctUntilChanged()

            combine(
                latestCouple.map { it?.id?.takeIf { id -> id.isNotBlank() } }.distinctUntilChanged(),
                dateKeyTicker
            ) { coupleId, dateKey -> coupleId to dateKey }
                .flatMapLatest { (coupleId, dateKey) ->
                    if (coupleId == null) flowOf(dateKey to null)
                    else repository.observeDailyPrompt(coupleId, dateKey, uid)
                        .map { entry -> dateKey to entry }
                        .catch { Log.w(TAG, "Background daily prompt error", it) }
                }
                .collect { (dateKey, prompt) ->
                    latestDailyPromptState.value = dateKey to prompt
                    val couple = latestCouple.value ?: return@collect
                    val isCreator = couple.creatorUid == uid
                    val theirAnswer = if (isCreator) prompt?.partnerAnswer else prompt?.creatorAnswer
                    val myAnswer = if (isCreator) prompt?.creatorAnswer else prompt?.partnerAnswer
                    val answeredTag = "${couple.id}_$dateKey"
                    val eventKey = "prompt_${couple.id}_${dateKey}_partner_answered"

                    // If current user already answered, suppress today's 1-hour expiry alert forever
                    if (!myAnswer.isNullOrBlank()) {
                        stateStore.markNotified("prompt_expiry_${couple.id}_$dateKey")
                    }

                    // Keep Today's Question Home Widget updated in background
                    val promptIdx = prompt?.promptIndex?.toInt() ?: DailyPromptDeck.todayPromptIndex()
                    val qText = DailyPromptDeck.questionForIndex(promptIdx)
                    val qStatus = when {
                        !myAnswer.isNullOrBlank() && !theirAnswer.isNullOrBlank() -> "✨ Both answered — tap to read!"
                        !myAnswer.isNullOrBlank() -> "✓ Answered — waiting for partner"
                        !theirAnswer.isNullOrBlank() -> "✨ Partner answered — reply to unlock!"
                        else -> "Tap to answer Today's Question"
                    }
                    val qExpiry = "Expires in ${DailyPromptDeck.computeExpiryCountdown().formattedCompact} (IST)"
                    widgetStore.updateDailyQuestionPreview(qText, qStatus, qExpiry)
                    DuetQuestionWidgetProvider.updateAllWidgets(this@DuetSyncService)

                    if (baselineTag != answeredTag) {
                        baselineTag = answeredTag
                        if (!theirAnswer.isNullOrBlank()) stateStore.markNotified(eventKey)
                        return@collect
                    }
                    if (!theirAnswer.isNullOrBlank() && !stateStore.hasNotified(eventKey)) {
                        stateStore.markNotified(eventKey)
                        if (latestLiveConfig.value.dailyPromptEnabled) {
                            DuetNotificationHelper.showDailyPromptNotification(
                                context = this@DuetSyncService,
                                partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value),
                                bothUnlocked = !myAnswer.isNullOrBlank()
                            )
                        }
                    }
                }
        }

        // 5b. 1-Hour Expiry Reminder Watcher: fires ONLY if <= 60 min left AND current user has NOT answered yet
        serviceScope.launch {
            while (currentCoroutineContext().isActive) {
                val couple = latestCouple.value
                val snapshot = latestDailyPromptState.value
                if (couple != null && couple.status != "dissolved" && snapshot != null) {
                    val todayKey = DailyPromptDeck.todayDateKey()
                    if (snapshot.first == todayKey) {
                        val prompt = snapshot.second
                        val isCreator = couple.creatorUid == uid
                        val myAnswer = if (isCreator) prompt?.creatorAnswer else prompt?.partnerAnswer
                        val expiryKey = "prompt_expiry_${couple.id}_$todayKey"
                        if (!myAnswer.isNullOrBlank()) {
                            // User already answered — never send expiry alert
                            stateStore.markNotified(expiryKey)
                        } else {
                            val countdown = DailyPromptDeck.computeExpiryCountdown()
                            if (countdown.isExpiringSoon && !stateStore.hasNotified(expiryKey)) {
                                stateStore.markNotified(expiryKey)
                                if (latestLiveConfig.value.dailyPromptEnabled) {
                                    val minsLeft = ((countdown.millisRemaining + 59_999L) / 60_000L).coerceIn(1L, 60L)
                                    DuetNotificationHelper.showDailyPromptExpiryNotification(
                                        context = this@DuetSyncService,
                                        minutesRemaining = minsLeft
                                    )
                                }
                            }
                        }
                    }
                }
                delay(30_000L)
            }
        }

        // 5c. Observe Shared Bucket List Dreams for Partner Updates
        serviceScope.launch {
            var baselineCoupleId: String? = null
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeBucketItems(coupleId, uid)
                        .catch { Log.w(TAG, "Background bucket list flow error", it) }
                }
                .collect { items ->
                    val couple = latestCouple.value ?: return@collect
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        items.forEach { item ->
                            stateStore.markNotified("bucket_add_${item.id}")
                            if (item.completed) stateStore.markNotified("bucket_done_${item.id}")
                        }
                        return@collect
                    }
                    val doneCount = items.count { it.completed }
                    widgetStore.updateBucketSummary("$doneCount/${items.size} Dreams Done")
                    DuetCoupleHubWidgetProvider.updateAllWidgets(this@DuetSyncService)

                    val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                    for (item in items) {
                        val addKey = "bucket_add_${item.id}"
                        if (item.creatorUid != uid && !stateStore.hasNotified(addKey)) {
                            stateStore.markNotified(addKey)
                            DuetNotificationHelper.showBucketListNotification(
                                context = this@DuetSyncService,
                                partnerName = partnerName,
                                dreamTitle = item.title,
                                completed = false
                            )
                        }
                        val doneKey = "bucket_done_${item.id}"
                        if (item.completed && item.completedByUid != null && item.completedByUid != uid && !stateStore.hasNotified(doneKey)) {
                            stateStore.markNotified(doneKey)
                            DuetNotificationHelper.showBucketListNotification(
                                context = this@DuetSyncService,
                                partnerName = partnerName,
                                dreamTitle = item.title,
                                completed = true
                            )
                        }
                    }
                }
        }

        // 5d. Observe Secret Love Letters & Time Capsules
        serviceScope.launch {
            var baselineCoupleId: String? = null
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeLoveLetters(coupleId, uid)
                        .catch { Log.w(TAG, "Background love letters flow error", it) }
                }
                .collect { letters ->
                    val couple = latestCouple.value ?: return@collect
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        letters.forEach { stateStore.markNotified("letter_${it.id}") }
                        return@collect
                    }
                    val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                    if (!latestLiveConfig.value.sharedJournalEnabled) return@collect
                    for (letter in letters) {
                        val key = "letter_${letter.id}"
                        if (letter.senderUid != uid && !stateStore.hasNotified(key)) {
                            stateStore.markNotified(key)
                            DuetNotificationHelper.showLoveLetterNotification(
                                context = this@DuetSyncService,
                                coupleId = couple.id,
                                partnerName = partnerName,
                                letterTitle = letter.title,
                                isTimeCapsuleLocked = !letter.isUnlocked()
                            )
                        }
                    }
                }
        }

        // 5e. Observe Shared Wish & Love Jar
        serviceScope.launch {
            var baselineCoupleId: String? = null
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeLoveJarNotes(coupleId, uid)
                        .catch { Log.w(TAG, "Background love jar flow error", it) }
                }
                .collect { notes ->
                    val couple = latestCouple.value ?: return@collect
                    notes.firstOrNull()?.let { latest ->
                        widgetStore.updateLoveJarHighlight("🫙 ${latest.text}")
                        DuetCoupleHubWidgetProvider.updateAllWidgets(this@DuetSyncService)
                    }
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        notes.forEach { n ->
                            stateStore.markNotified("jar_add_${n.id}")
                            if (n.granted) stateStore.markNotified("jar_grant_${n.id}")
                        }
                        return@collect
                    }
                    val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                    if (!latestLiveConfig.value.sharedJournalEnabled) return@collect
                    for (note in notes) {
                        val addKey = "jar_add_${note.id}"
                        if (note.authorUid != uid && !stateStore.hasNotified(addKey)) {
                            stateStore.markNotified(addKey)
                            DuetNotificationHelper.showLoveJarNotification(
                                context = this@DuetSyncService,
                                partnerName = partnerName,
                                noteText = note.text,
                                granted = false
                            )
                        }
                        val grantKey = "jar_grant_${note.id}"
                        if (note.granted && note.grantedByUid != null && note.grantedByUid != uid && !stateStore.hasNotified(grantKey)) {
                            stateStore.markNotified(grantKey)
                            DuetNotificationHelper.showLoveJarNotification(
                                context = this@DuetSyncService,
                                partnerName = partnerName,
                                noteText = note.text,
                                granted = true
                            )
                        }
                    }
                }
        }

        // 5f. Observe Shared Music & Vibe Dedications
        serviceScope.launch {
            var baselineCoupleId: String? = null
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeVibeDedications(coupleId, uid)
                        .catch { Log.w(TAG, "Background vibe dedications flow error", it) }
                }
                .collect { vibes ->
                    val couple = latestCouple.value ?: return@collect
                    vibes.firstOrNull()?.let { latest ->
                        widgetStore.updateSongVibeHighlight("${latest.vibeEmoji} ${latest.songTitle} • ${latest.artistName}")
                        DuetCoupleHubWidgetProvider.updateAllWidgets(this@DuetSyncService)
                    }
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        vibes.forEach { stateStore.markNotified("vibe_${it.id}") }
                        return@collect
                    }
                    val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                    for (vibe in vibes) {
                        val key = "vibe_${vibe.id}"
                        if (vibe.senderUid != uid && !stateStore.hasNotified(key)) {
                            stateStore.markNotified(key)
                            DuetNotificationHelper.showSongDedicationNotification(
                                context = this@DuetSyncService,
                                coupleId = couple.id,
                                partnerName = partnerName,
                                songTitle = vibe.songTitle,
                                artistName = vibe.artistName,
                                vibeEmoji = vibe.vibeEmoji
                            )
                        }
                    }
                }
        }

        // 5g. Observe Care & Wellness Check-Ins
        serviceScope.launch {
            var baselineCoupleId: String? = null
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeCareCheckIns(coupleId, uid)
                        .catch { Log.w(TAG, "Background care check-ins flow error", it) }
                }
                .collect { checkIns ->
                    val couple = latestCouple.value ?: return@collect
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        checkIns.forEach { c ->
                            stateStore.markNotified("care_add_${c.id}")
                            if (c.acknowledged) stateStore.markNotified("care_ack_${c.id}")
                        }
                        return@collect
                    }
                    val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                    for (item in checkIns) {
                        val preset = CareCheckInCatalog.byKey(item.careType)
                        val addKey = "care_add_${item.id}"
                        if (item.senderUid != uid && !stateStore.hasNotified(addKey)) {
                            stateStore.markNotified(addKey)
                            DuetNotificationHelper.showCareCheckInNotification(
                                context = this@DuetSyncService,
                                coupleId = couple.id,
                                partnerName = partnerName,
                                emoji = preset.emoji,
                                message = item.message,
                                acknowledged = false
                            )
                        }
                        val ackKey = "care_ack_${item.id}"
                        if (item.acknowledged && item.acknowledgedByUid != null && item.acknowledgedByUid != uid && !stateStore.hasNotified(ackKey)) {
                            stateStore.markNotified(ackKey)
                            DuetNotificationHelper.showCareCheckInNotification(
                                context = this@DuetSyncService,
                                coupleId = couple.id,
                                partnerName = partnerName,
                                emoji = preset.emoji,
                                message = item.message,
                                acknowledged = true
                            )
                        }
                    }
                }
        }

        // 5h. Observe Shared Love Doodles
        serviceScope.launch {
            var baselineCoupleId: String? = null
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeLoveDoodles(coupleId, uid)
                        .catch { Log.w(TAG, "Background love doodles flow error", it) }
                }
                .collect { doodles ->
                    val couple = latestCouple.value ?: return@collect
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        doodles.forEach { stateStore.markNotified("doodle_${it.id}") }
                        return@collect
                    }
                    val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                    for (doodle in doodles) {
                        val key = "doodle_${doodle.id}"
                        if (doodle.artistUid != uid && !stateStore.hasNotified(key)) {
                            stateStore.markNotified(key)
                            DuetNotificationHelper.showDoodleNotification(
                                context = this@DuetSyncService,
                                coupleId = couple.id,
                                partnerName = partnerName,
                                titleText = doodle.title
                            )
                        }
                    }
                }
        }

        // 5i. Observe Couple Promises & Pinky-Swears
        serviceScope.launch {
            var baselineCoupleId: String? = null
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeCouplePromises(coupleId, uid)
                        .catch { Log.w(TAG, "Background couple promises flow error", it) }
                }
                .collect { promises ->
                    val couple = latestCouple.value ?: return@collect
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        promises.forEach { p ->
                            stateStore.markNotified("promise_add_${p.id}")
                            if (p.isFullyCoSigned()) stateStore.markNotified("promise_sign_${p.id}")
                        }
                        return@collect
                    }
                    val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                    for (promise in promises) {
                        val addKey = "promise_add_${promise.id}"
                        if (promise.creatorUid != uid && !stateStore.hasNotified(addKey)) {
                            stateStore.markNotified(addKey)
                            DuetNotificationHelper.showPromiseNotification(
                                context = this@DuetSyncService,
                                partnerName = partnerName,
                                promiseTitle = promise.title,
                                coSigned = false
                            )
                        }
                        val signKey = "promise_sign_${promise.id}"
                        if (promise.isFullyCoSigned() && promise.creatorUid == uid && !stateStore.hasNotified(signKey)) {
                            stateStore.markNotified(signKey)
                            DuetNotificationHelper.showPromiseNotification(
                                context = this@DuetSyncService,
                                partnerName = partnerName,
                                promiseTitle = promise.title,
                                coSigned = true
                            )
                        }
                    }
                }
        }

        serviceScope.launch {
            var baselineCoupleId: String? = null
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observeEvents(coupleId, uid)
                        .catch { Log.w(TAG, "Background events flow error", it) }
                }
                .collect { events ->
                    val couple = latestCouple.value ?: return@collect
                    if (baselineCoupleId != couple.id) {
                        baselineCoupleId = couple.id
                        events.forEach { stateStore.markNotified("evt_${it.id}") }
                        return@collect
                    }
                    val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                    for (evt in events) {
                        val key = "evt_${evt.id}"
                        if (evt.creatorUid != uid && !stateStore.hasNotified(key)) {
                            stateStore.markNotified(key)
                            DuetNotificationHelper.showSpecialDateNotification(
                                context = this@DuetSyncService,
                                partnerName = partnerName,
                                eventTitle = evt.title
                            )
                        }
                    }
                }
        }

        // 6. Observe Presences -> Refresh ongoing sync notification + Home Widget in background
        serviceScope.launch {
            latestCouple
                .map { it?.id?.takeIf { id -> id.isNotBlank() } }
                .flatMapLatest { coupleId ->
                    if (coupleId == null) flowOf(emptyList())
                    else repository.observePresences(coupleId, uid)
                        .catch { Log.w(TAG, "Background presence flow error", it) }
                }
                .collect { presences ->
                    val couple = latestCouple.value ?: return@collect
                    if (couple.status == "dissolved") return@collect
                    latestMyPresence.value = presences.firstOrNull { it.userId == uid }
                    val partnerUid = couple.otherUid(uid)
                    val partnerPresence = partnerUid?.let { pUid -> presences.firstOrNull { it.userId == pUid } }

                    val bannedNotice = latestPartnerBanned.value
                    if (couple.status == "paired" && bannedNotice != null) {
                        val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                        updateOngoingSyncNotification(
                            title = "Duet • $partnerName",
                            subtitle = "Account banned by admin"
                        )
                    } else if (couple.status == "paired" && partnerPresence != null) {
                        val partnerName = couple.resolvedPartnerName(uid, latestPartnerNickname.value)
                        val batteryStr = if (partnerPresence.statusShared && partnerPresence.batteryPercent >= 0) {
                            val bolt = if (partnerPresence.charging) "⚡" else ""
                            "${partnerPresence.batteryPercent}%$bolt • "
                        } else {
                            ""
                        }
                        updateOngoingSyncNotification(
                            title = "Duet • Connected with $partnerName",
                            subtitle = "$batteryStr${partnerPresence.liveActivityLabel()}"
                        )
                    } else {
                        updateOngoingSyncNotification(
                            title = "Duet Live Sync",
                            subtitle = "Always-on encrypted sync active"
                        )
                    }

                    refreshHomeWidget(
                        couple = couple,
                        myUid = uid,
                        partner = partnerPresence,
                        mine = latestMyPresence.value,
                        customPartnerNickname = latestPartnerNickname.value,
                        isPartnerBanned = bannedNotice != null
                    )
                }
        }

        // 7. Continuous Real Device Telemetry + Currently Open App Publisher (Battery, Network, Lock, Active App)
        val heartbeatFlow = flow {
            var tick = 0L
            while (currentCoroutineContext().isActive) {
                emit(tick++)
                delay(20_000L)
            }
        }

        combine(
            latestCouple,
            latestShareStatus,
            telemetryCollector.observeTelemetry(pollIntervalMs = 2_500L),
            latestMyPresence,
            combine(DuetNotificationStateStore.appForegroundFlow, heartbeatFlow) { fg, tick -> fg to tick }
        ) { couple, shareStatus, rawTelemetry, myPresence, (inFg, _) ->
            telemetryCollector.setAppForeground(inFg)
            val corrected = if (inFg && rawTelemetry.deviceStatus != "LOCKED") {
                rawTelemetry.copy(deviceStatus = "ACTIVE_IN_DUET", activeAppName = "Duet")
            } else {
                rawTelemetry
            }
            Quad(couple, shareStatus, corrected, myPresence)
        }.collect { (couple, shareStatus, telemetry, myPresence) ->
            if (couple == null || couple.status == "dissolved") return@collect
            val moodKey = myPresence?.moodKey ?: "IN_LOVE"
            val moodNote = myPresence?.moodNote ?: ""
            val displayName = couple.myDisplayName(uid)
            val syncKey = "${couple.id}|${couple.memberUids.joinToString(",")}|$shareStatus|$telemetry|$moodKey|$moodNote|$displayName"
            val now = System.currentTimeMillis()
            val heartbeatDue = (now - lastSyncedAtMs) >= 20_000L
            if (syncKey == lastSyncedKey && !heartbeatDue) return@collect

            val writeResult = repository.upsertMyPresence(
                couple = couple,
                displayName = displayName,
                telemetry = telemetry,
                moodKey = moodKey,
                moodNote = moodNote,
                shareStatus = shareStatus,
                markLastSeenNow = telemetry.deviceStatus == "ACTIVE_IN_DUET"
            )
            if (writeResult.isSuccess) {
                lastSyncedKey = syncKey
                lastSyncedAtMs = now
            }
        }
    }

    private fun refreshHomeWidget(
        couple: CoupleSpace,
        myUid: String,
        partner: PartnerPresence?,
        mine: PartnerPresence?,
        customPartnerNickname: String?,
        isPartnerBanned: Boolean = false
    ) {
        val displayPresence = partner ?: mine
        val moodOption = MoodCatalog.byKey(displayPresence?.moodKey)
        val partnerName = couple.resolvedPartnerName(myUid, customPartnerNickname)
        val coupleTitle = if (couple.status == "paired") {
            "${couple.myDisplayName(myUid)} & $partnerName"
        } else {
            "Duet • Code ${couple.inviteCode}"
        }
        val batteryBadge = when {
            isPartnerBanned -> "Banned"
            displayPresence == null -> "Live"
            !displayPresence.statusShared || displayPresence.batteryPercent < 0 -> "Private"
            displayPresence.charging -> "${displayPresence.batteryPercent}% ⚡"
            else -> "${displayPresence.batteryPercent}%"
        }
        val whoLabel = if (partner != null) partnerName else "You"
        val moodLine = if (isPartnerBanned) {
            "⛔ $partnerName's account banned by admin"
        } else {
            "${moodOption.emoji} $whoLabel: ${moodOption.label}"
        }
        val countdownLine = couple.anniversaryMonth?.let { m ->
            couple.anniversaryDay?.let { d ->
                val c = CountdownCalculator.computeYearlyCountdown(m, d)
                if (c.isToday) "Happy Anniversary Today!" else "${c.days}d until Anniversary"
            }
        } ?: couple.partnerBirthday(myUid)?.let { (m, d) ->
            val b = CountdownCalculator.computeYearlyCountdown(m, d)
            if (b.isToday) "Partner's Birthday Today!" else "${b.days}d until Partner's Birthday"
        } ?: "Tap to set anniversary & birthday"

        val statusFooter = if (isPartnerBanned) {
            "$partnerName: Account banned by admin"
        } else if (displayPresence != null) {
            "$whoLabel: ${displayPresence.liveActivityLabel()}"
        } else {
            "End-to-end encrypted for two"
        }

        val existing = widgetStore.readSnapshot()
        val partnerStatus = when {
            isPartnerBanned -> "⛔ Account Banned"
            couple.status != "paired" -> "Invite code ${couple.inviteCode}"
            partner == null -> "End-to-end encrypted"
            partner.isPartnerActivelyTyping() -> "Typing…"
            partner.deviceStatus == "ACTIVE_IN_DUET" -> "Online in Duet"
            else -> partner.liveActivityLabel()
        }
        val daysTogetherLabel = if (couple.anniversaryYear != null && couple.anniversaryMonth != null && couple.anniversaryDay != null) {
            val d = CountdownCalculator.daysTogether(couple.anniversaryYear, couple.anniversaryMonth, couple.anniversaryDay)
            "$d Days Together ❤️"
        } else {
            "Set Anniversary"
        }
        val annCd = couple.anniversaryMonth?.let { m ->
            couple.anniversaryDay?.let { d ->
                val c = CountdownCalculator.computeYearlyCountdown(m, d)
                if (c.isToday) "Today! 🎉" else "${c.days}d ${c.hours}h"
            }
        } ?: "Tap to set"
        val bdayCd = couple.partnerBirthday(myUid)?.let { (m, d) ->
            val b = CountdownCalculator.computeYearlyCountdown(m, d)
            if (b.isToday) "Today! 🎂" else "${b.days}d ${b.hours}h"
        } ?: "Tap to set"
        val partnerNet = when {
            displayPresence == null || !displayPresence.statusShared -> "Private"
            else -> displayPresence.networkType.ifBlank { "Live" }
        }
        val partnerApp = displayPresence?.liveActivityLabel() ?: "Duet"
        val partnerDevice = displayPresence?.loggedInDeviceLabel()?.ifBlank { "Partner Phone" } ?: "Partner Phone"

        widgetStore.saveSnapshot(
            existing.copy(
                coupleId = couple.id,
                coupleTitle = coupleTitle,
                partnerName = partnerName,
                partnerStatus = partnerStatus,
                partnerTyping = partner?.isPartnerActivelyTyping() == true,
                batteryBadge = batteryBadge,
                moodLine = moodLine,
                countdownLine = countdownLine,
                statusFooter = statusFooter,
                daysTogetherLabel = daysTogetherLabel,
                anniversaryCountdown = annCd,
                birthdayCountdown = bdayCd,
                partnerNetwork = partnerNet,
                partnerActiveApp = partnerApp,
                partnerDeviceName = partnerDevice
            )
        )
        WidgetStateStore.updateAllDuetWidgets(this)
    }

    private fun extractJsonField(plain: String?, key: String): String {
        if (plain.isNullOrBlank()) return ""
        return try {
            JSONObject(plain).optString(key, "")
        } catch (_: Exception) {
            if (key == "text") plain else ""
        }
    }

    private fun reactionEmoji(key: String): String = when (key) {
        "HEART" -> "❤️"
        "LAUGH" -> "😂"
        "WOW" -> "😮"
        "SAD" -> "😢"
        "PRAY" -> "🙏"
        "FIRE" -> "🔥"
        "KISS" -> "😘"
        "SPARKLE" -> "✨"
        "HUG" -> "🫂"
        else -> "❤️"
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    companion object {
        private const val TAG = "DuetSyncService"
        private const val ACTION_START = "com.example.service.START_SYNC"
        private const val ACTION_STOP = "com.example.service.STOP_SYNC"

        fun start(context: Context) {
            val appCtx = context.applicationContext
            if (!DuetNotificationStateStore(appCtx).isBackgroundSyncEnabled()) return
            if (Firebase.auth.currentUser == null) return
            val intent = Intent(appCtx, DuetSyncService::class.java).apply { action = ACTION_START }
            try {
                ContextCompat.startForegroundService(appCtx, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to start foreground DuetSyncService", e)
            }
        }

        fun stop(context: Context) {
            val appCtx = context.applicationContext
            try {
                appCtx.stopService(Intent(appCtx, DuetSyncService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
