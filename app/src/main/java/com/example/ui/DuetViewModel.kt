package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.DuetCryptoManager
import com.example.crypto.PhotoCompressor
import com.example.data.BannedAccountNotice
import com.example.data.BucketListItem
import com.example.data.CareCheckIn
import com.example.data.CareCheckInCatalog
import com.example.data.CountdownCalculator
import com.example.data.CouplePromise
import com.example.data.CoupleSpace
import com.example.data.DailyPromptDeck
import com.example.data.DailyPromptEntry
import com.example.data.DecryptedMessageItem
import com.example.data.DecryptedMomentItem
import com.example.data.DuetRepository
import com.example.data.EncryptedMessage
import com.example.data.LoveDoodle
import com.example.data.LoveJarNote
import com.example.data.LoveLetter
import com.example.data.MoodCatalog
import com.example.data.MoodEntry
import com.example.data.MoodOption
import com.example.data.PartnerBannedNotice
import com.example.data.PartnerPresence
import com.example.data.SharedMoment
import com.example.data.SpecialDateEvent
import com.example.data.UserProfile
import com.example.data.VibeDedication
import com.example.notifications.DuetNotificationHelper
import com.example.notifications.DuetNotificationStateStore
import com.example.service.DuetSyncService
import com.example.telemetry.AppUsageMonitor
import com.example.telemetry.DeviceSpecsSnapshot
import com.example.telemetry.DeviceTelemetryCollector
import com.example.telemetry.LocalDeviceTelemetry
import com.example.telemetry.NetworkConnectivityMonitor
import com.example.ui.theme.DuetAppearanceStore
import com.example.widget.DuetWidgetSnapshot
import com.example.widget.WidgetStateStore
import com.example.widget.WidgetThemeStyle
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed interface SessionStage {
    data object Loading : SessionStage
    data object NeedsPairing : SessionStage
    data object Ready : SessionStage
    data class Error(val message: String) : SessionStage
}

data class DuetUiState(
    val stage: SessionStage = SessionStage.Loading,
    val currentUserId: String = "",
    val profile: UserProfile? = null,
    val couple: CoupleSpace? = null,
    val myPresence: PartnerPresence? = null,
    val partnerPresence: PartnerPresence? = null,
    val localTelemetry: LocalDeviceTelemetry = LocalDeviceTelemetry(0, false, "OFFLINE", "ACTIVE_IN_DUET", "Duet"),
    val myDeviceSpecs: DeviceSpecsSnapshot? = null,
    val messages: List<DecryptedMessageItem> = emptyList(),
    val moods: List<MoodEntry> = emptyList(),
    val moments: List<DecryptedMomentItem> = emptyList(),
    val events: List<SpecialDateEvent> = emptyList(),
    val bucketItems: List<BucketListItem> = emptyList(),
    val loveLetters: List<LoveLetter> = emptyList(),
    val loveJarNotes: List<LoveJarNote> = emptyList(),
    val vibeDedications: List<VibeDedication> = emptyList(),
    val careCheckIns: List<CareCheckIn> = emptyList(),
    val loveDoodles: List<LoveDoodle> = emptyList(),
    val couplePromises: List<CouplePromise> = emptyList(),
    val todayPrompt: DailyPromptEntry? = null,
    val todayPromptIndex: Int = DailyPromptDeck.todayPromptIndex(),
    val widgetTheme: WidgetThemeStyle = WidgetThemeStyle.ROSE,
    val safetyNumber: String = "",
    val myKeyFingerprint: String = "",
    val partnerKeyFingerprint: String? = null,
    val isOnline: Boolean = true,
    val backgroundSyncEnabled: Boolean = true,
    val hasNotificationPermission: Boolean = true,
    val hasUsageAccessPermission: Boolean = false,
    val preferEnterCodeTabOnPairing: Boolean = false,
    val busyAction: Boolean = false,
    val bannerMessage: String? = null,
    val bannedAccount: BannedAccountNotice? = null,
    val partnerBanned: PartnerBannedNotice? = null
) {
    fun resolvedPartnerName(): String =
        couple?.resolvedPartnerName(currentUserId, profile?.partnerNickname) ?: "Your Partner"
}

@OptIn(ExperimentalCoroutinesApi::class)
class DuetViewModel(
    private val repository: DuetRepository,
    private val crypto: DuetCryptoManager,
    private val telemetryCollector: DeviceTelemetryCollector,
    private val widgetStore: WidgetStateStore,
    private val appContext: Context,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    private val notificationStore = DuetNotificationStateStore(appContext)
    private val connectivityMonitor = NetworkConnectivityMonitor(appContext)
    private val _busy = MutableStateFlow(false)
    private val _bannerMessage = MutableStateFlow<String?>(null)
    private val _streamError = MutableStateFlow<String?>(null)
    private val _widgetTheme = MutableStateFlow(widgetStore.readSnapshot().themeStyle)
    private val _backgroundSyncEnabled = MutableStateFlow(notificationStore.isBackgroundSyncEnabled())
    private val _hasNotificationPermission = MutableStateFlow(DuetNotificationHelper.hasNotificationPermission(appContext))
    private val _hasUsageAccessPermission = MutableStateFlow(AppUsageMonitor.hasUsageStatsPermission(appContext))

    private var syncedCoupleMembersFor: String? = null
    private var lastTelemetrySyncKey: String? = null
    private var lastTelemetrySyncedAtMs: Long = 0L
    private var typingStopJob: Job? = null
    private var lastSentTypingState: Boolean = false
    private var lastTypingHeartbeatMs: Long = 0L
    private val markedSeenMessageIds = mutableSetOf<String>()
    private var handlingDissolutionForCoupleId: String? = null
    private var handlingMissingCoupleId: String? = null

    private val _myPublicKey = MutableStateFlow(crypto.getMyPublicKeyBase64())
    private val _myKeyFingerprint = MutableStateFlow(crypto.shortKeyFingerprint(_myPublicKey.value))
    private val _preferEnterCodeTabOnPairing = MutableStateFlow(false)
    private val _bannedAccount = MutableStateFlow<BannedAccountNotice?>(null)
    private var wasAccountBanned: Boolean = false

    private sealed interface CoupleLoadState {
        data object Unloaded : CoupleLoadState
        data object Missing : CoupleLoadState
        data class Loaded(val couple: CoupleSpace) : CoupleLoadState
    }

    private val profileFlow: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .safeCatch("Failed to observe profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val coupleLoadFlow: StateFlow<CoupleLoadState> = profileFlow
        .map { profile ->
            if (profile?.isAccountBanned() == true) null
            else profile?.coupleId?.takeIf { id -> id.isNotBlank() }
        }
        .distinctUntilChanged()
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf<CoupleLoadState>(CoupleLoadState.Unloaded)
            else repository.observeCoupleSpace(coupleId, currentUserId)
                .map { couple ->
                    if (couple != null) CoupleLoadState.Loaded(couple)
                    else CoupleLoadState.Missing
                }
                .safeCatch("Failed to observe couple space")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CoupleLoadState.Unloaded)

    private val coupleFlow: StateFlow<CoupleSpace?> = coupleLoadFlow
        .map { (it as? CoupleLoadState.Loaded)?.couple }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val partnerUidFlow: Flow<String?> = coupleFlow
        .map { couple ->
            if (couple == null || couple.status != "paired") null
            else couple.otherUid(currentUserId)?.takeIf { it.isNotBlank() }
        }
        .distinctUntilChanged()

    private val partnerProfileFlow: StateFlow<UserProfile?> = partnerUidFlow
        .flatMapLatest { partnerUid ->
            if (partnerUid == null) flowOf(null)
            else repository.observePartnerUserProfile(partnerUid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val activeCoupleIdFlow: Flow<String?> = coupleFlow
        .map { couple -> couple?.takeIf { it.status != "dissolved" }?.id?.takeIf { it.isNotBlank() } }
        .distinctUntilChanged()

    private val presencesFlow: StateFlow<List<PartnerPresence>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observePresences(coupleId, currentUserId)
                .safeCatch("Failed to observe partner status")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val rawMessagesFlow: StateFlow<List<EncryptedMessage>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeMessages(coupleId, currentUserId)
                .safeCatch("Failed to observe private messages")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val moodsFlow: StateFlow<List<MoodEntry>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeMoods(coupleId, currentUserId)
                .safeCatch("Failed to observe moods")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val rawMomentsFlow: StateFlow<List<SharedMoment>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeMoments(coupleId, currentUserId)
                .safeCatch("Failed to observe shared moments")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val eventsFlow: StateFlow<List<SpecialDateEvent>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeEvents(coupleId, currentUserId)
                .safeCatch("Failed to observe countdown dates")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val bucketItemsFlow: StateFlow<List<BucketListItem>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeBucketItems(coupleId, currentUserId)
                .safeCatch("Failed to observe bucket list")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val loveLettersFlow: StateFlow<List<LoveLetter>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeLoveLetters(coupleId, currentUserId)
                .safeCatch("Failed to observe love letters")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val loveJarFlow: StateFlow<List<LoveJarNote>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeLoveJarNotes(coupleId, currentUserId)
                .safeCatch("Failed to observe love jar")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val vibeDedicationsFlow: StateFlow<List<VibeDedication>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeVibeDedications(coupleId, currentUserId)
                .safeCatch("Failed to observe song dedications")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val careCheckInsFlow: StateFlow<List<CareCheckIn>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeCareCheckIns(coupleId, currentUserId)
                .safeCatch("Failed to observe care check-ins")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val loveDoodlesFlow: StateFlow<List<LoveDoodle>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeLoveDoodles(coupleId, currentUserId)
                .safeCatch("Failed to observe love doodles")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val couplePromisesFlow: StateFlow<List<CouplePromise>> = activeCoupleIdFlow
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList())
            else repository.observeCouplePromises(coupleId, currentUserId)
                .safeCatch("Failed to observe couple promises")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val todayDateKeyFlow: StateFlow<String> = flow {
        while (currentCoroutineContext().isActive) {
            emit(DailyPromptDeck.todayDateKey())
            delay(30_000L)
        }
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DailyPromptDeck.todayDateKey())

    private val todayPromptFlow: StateFlow<DailyPromptEntry?> = combine(
        activeCoupleIdFlow,
        todayDateKeyFlow
    ) { coupleId, dateKey -> coupleId to dateKey }
        .flatMapLatest { (coupleId, dateKey) ->
            if (coupleId == null) flowOf(null)
            else repository.observeDailyPrompt(coupleId, dateKey, currentUserId)
                .safeCatch("Failed to observe daily question")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val isOnlineFlow: StateFlow<Boolean> = connectivityMonitor.observeIsOnline()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            connectivityMonitor.isCurrentlyOnline()
        )

    private val localTelemetryFlow: StateFlow<LocalDeviceTelemetry> = telemetryCollector.observeTelemetry()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            telemetryCollector.readSnapshot()
        )

    private val _profileBootstrapped = MutableStateFlow(false)

    private val permissionsFlow = combine(
        _backgroundSyncEnabled,
        _hasNotificationPermission,
        _hasUsageAccessPermission,
        isOnlineFlow
    ) { bgSync, notifPerm, usagePerm, online ->
        PermissionsBundle(bgSync, notifPerm, usagePerm, online)
    }

    private val identityFlow = combine(
        _myPublicKey,
        _myKeyFingerprint,
        _preferEnterCodeTabOnPairing
    ) { pubKey, fp, preferJoin ->
        IdentityBundle(pubKey, fp, preferJoin)
    }

    private val extraContentFlow = combine(eventsFlow, bucketItemsFlow, todayPromptFlow) { evts, bucket, prompt ->
        Triple(evts, bucket, prompt)
    }

    private val facilitiesFlow = combine(
        combine(loveLettersFlow, loveJarFlow, vibeDedicationsFlow) { letters, jar, vibes ->
            Triple(letters, jar, vibes)
        },
        combine(careCheckInsFlow, loveDoodlesFlow, couplePromisesFlow) { care, doodles, promises ->
            Triple(care, doodles, promises)
        }
    ) { firstThree, secondThree ->
        FacilitiesBundle(
            loveLetters = firstThree.first,
            loveJarNotes = firstThree.second,
            vibeDedications = firstThree.third,
            careCheckIns = secondThree.first,
            loveDoodles = secondThree.second,
            couplePromises = secondThree.third
        )
    }

    val uiState: StateFlow<DuetUiState> = combine(
        combine(
            combine(profileFlow, coupleFlow) { p, c -> p to c },
            partnerProfileFlow,
            presencesFlow,
            localTelemetryFlow,
            _profileBootstrapped
        ) { (profile, couple), partnerProfile, presences, localTel, boot ->
            CoreBundle(profile, couple, partnerProfile, presences, localTel, boot)
        },
        combine(rawMessagesFlow, moodsFlow, rawMomentsFlow, extraContentFlow) { msgs, moods, moms, (evts, bucket, prompt) ->
            ContentBundle(msgs, moods, moms, evts, bucket, prompt)
        },
        facilitiesFlow,
        combine(_busy, _bannerMessage, _streamError, _widgetTheme, combine(permissionsFlow, identityFlow, _bannedAccount) { p, id, ban -> Triple(p, id, ban) }) { busy, banner, err, theme, triple ->
            MetaBundle(busy, banner, err, theme, triple.first, triple.second) to triple.third
        }
    ) { core, content, facilities, (meta, explicitBan) ->
        val profile = core.profile
        val couple = core.couple
        val partnerProfile = core.partnerProfile
        val activeBan = explicitBan ?: profile?.takeIf { it.isAccountBanned() }?.let {
            BannedAccountNotice(userId = currentUserId, reason = it.resolvedBanReason())
        }
        val stage = when {
            activeBan != null -> SessionStage.Loading
            meta.streamError != null && profile == null -> SessionStage.Error(meta.streamError)
            !core.bootstrapped && profile == null -> SessionStage.Loading
            profile?.coupleId.isNullOrBlank() -> SessionStage.NeedsPairing
            couple == null || couple.status == "dissolved" -> SessionStage.Loading
            else -> SessionStage.Ready
        }

        val myPresence = core.presences.firstOrNull { it.userId == currentUserId }
        val partnerUid = couple?.otherUid(currentUserId)
        val partnerPresence = if (partnerUid != null) {
            core.presences.firstOrNull { it.userId == partnerUid }
        } else {
            null
        }
        val partnerBannedNotice = if (
            activeBan == null &&
            couple != null &&
            couple.status == "paired" &&
            !partnerUid.isNullOrBlank() &&
            partnerProfile != null &&
            partnerProfile.userId == partnerUid &&
            partnerProfile.isAccountBanned()
        ) {
            PartnerBannedNotice(
                partnerUid = partnerUid,
                partnerName = couple.resolvedPartnerName(currentUserId, profile?.partnerNickname),
                reason = partnerProfile.resolvedBanReason(),
                bannedAtMillis = partnerProfile.bannedAt?.toDate()?.time
            )
        } else {
            null
        }

        val decryptedMessages = if (activeBan == null && couple != null && couple.status != "dissolved") {
            content.messages.map { decryptMessage(it, couple) }
        } else {
            emptyList()
        }

        val decryptedMoments = if (activeBan == null && couple != null && couple.status != "dissolved") {
            content.moments.map { decryptMoment(it, couple) }
        } else {
            emptyList()
        }

        val otherPubKey = couple?.otherPublicKey(currentUserId)
        val safetyNumber = crypto.computeSafetyNumber(meta.identity.myPublicKey, otherPubKey)
        val partnerFingerprint = otherPubKey?.takeIf { it.isNotBlank() }?.let { crypto.shortKeyFingerprint(it) }

        DuetUiState(
            stage = stage,
            currentUserId = currentUserId,
            profile = profile,
            couple = couple,
            myPresence = myPresence,
            partnerPresence = partnerPresence,
            localTelemetry = core.localTelemetry,
            myDeviceSpecs = telemetryCollector.readDeviceSpecs(forceRefresh = false),
            messages = decryptedMessages,
            moods = content.moods,
            moments = decryptedMoments,
            events = content.events,
            bucketItems = content.bucketItems,
            loveLetters = facilities.loveLetters,
            loveJarNotes = facilities.loveJarNotes,
            vibeDedications = facilities.vibeDedications,
            careCheckIns = facilities.careCheckIns,
            loveDoodles = facilities.loveDoodles,
            couplePromises = facilities.couplePromises,
            todayPrompt = content.prompt,
            todayPromptIndex = content.prompt?.promptIndex?.toInt() ?: DailyPromptDeck.todayPromptIndex(),
            widgetTheme = meta.widgetTheme,
            safetyNumber = safetyNumber,
            myKeyFingerprint = meta.identity.myKeyFingerprint,
            partnerKeyFingerprint = partnerFingerprint,
            isOnline = meta.permissions.isOnline,
            backgroundSyncEnabled = meta.permissions.backgroundSyncEnabled,
            hasNotificationPermission = meta.permissions.hasNotificationPermission,
            hasUsageAccessPermission = meta.permissions.hasUsageAccessPermission,
            preferEnterCodeTabOnPairing = meta.identity.preferEnterCodeTabOnPairing,
            busyAction = meta.busy,
            bannerMessage = meta.banner ?: meta.streamError,
            bannedAccount = activeBan,
            partnerBanned = partnerBannedNotice
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DuetUiState(
            stage = SessionStage.Loading,
            currentUserId = currentUserId,
            myKeyFingerprint = _myKeyFingerprint.value,
            isOnline = connectivityMonitor.isCurrentlyOnline(),
            backgroundSyncEnabled = notificationStore.isBackgroundSyncEnabled(),
            hasNotificationPermission = DuetNotificationHelper.hasNotificationPermission(appContext),
            hasUsageAccessPermission = AppUsageMonitor.hasUsageStatsPermission(appContext)
        )
    )

    init {
        telemetryCollector.setAppForeground(true)
        DuetNotificationStateStore.isAppInForeground = true
        DuetNotificationHelper.ensureChannels(appContext)
        consumePendingRemovalNoticeIfAny()
        bootstrapProfile()
        observeCoupleSideEffects()
        observePartnerBanNotifications()
        observeTelemetrySync()
        observeWidgetUpdates()
        observeAppearanceThemeSync()
        observeDailyPromptExpiryReminder()
    }

    private fun syncMyIdentityFromStore() {
        val storedPub = crypto.getMyPublicKeyBase64()
        if (storedPub != _myPublicKey.value) {
            _myPublicKey.value = storedPub
            _myKeyFingerprint.value = crypto.shortKeyFingerprint(storedPub)
        }
    }

    private fun rotateMyIdentityKeyPair(): String {
        val freshPub = crypto.rotateKeyPair()
        _myPublicKey.value = freshPub
        _myKeyFingerprint.value = crypto.shortKeyFingerprint(freshPub)
        return freshPub
    }

    private fun consumePendingRemovalNoticeIfAny() {
        syncMyIdentityFromStore()
        val notice = notificationStore.consumePendingRemovalNotice()
        if (!notice.isNullOrBlank()) {
            _preferEnterCodeTabOnPairing.value = true
            showBanner(notice)
        }
    }

    /**
     * Foreground safety watcher for Today's Question 1-hour expiry alert (deduplicated with DuetSyncService
     * via the exact same `prompt_expiry_{coupleId}_{dateKey}` key). Fires ONLY if the user has NOT answered yet.
     */
    private fun observeDailyPromptExpiryReminder() {
        viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                val state = uiState.value
                val couple = state.couple
                if (couple != null && couple.status != "dissolved") {
                    val todayKey = DailyPromptDeck.todayDateKey()
                    val prompt = state.todayPrompt
                    val isCreator = couple.creatorUid == currentUserId
                    val myAnswer = if (isCreator) prompt?.creatorAnswer else prompt?.partnerAnswer
                    val expiryKey = "prompt_expiry_${couple.id}_$todayKey"
                    if (!myAnswer.isNullOrBlank()) {
                        notificationStore.markNotified(expiryKey)
                    } else {
                        val countdown = DailyPromptDeck.computeExpiryCountdown()
                        if (countdown.isExpiringSoon && !notificationStore.hasNotified(expiryKey)) {
                            notificationStore.markNotified(expiryKey)
                            val minsLeft = ((countdown.millisRemaining + 59_999L) / 60_000L).coerceIn(1L, 60L)
                            DuetNotificationHelper.showDailyPromptExpiryNotification(
                                context = appContext,
                                minutesRemaining = minsLeft
                            )
                        }
                    }
                }
                delay(30_000L)
            }
        }
    }

    private fun observeAppearanceThemeSync() {
        viewModelScope.launch {
            DuetAppearanceStore.state(appContext).collect { appearance ->
                _widgetTheme.value = appearance.themePreset.toWidgetThemeStyle()
            }
        }
    }

    private fun handleBannedProfile(profile: UserProfile) {
        wasAccountBanned = true
        _bannedAccount.value = BannedAccountNotice(
            userId = currentUserId,
            reason = profile.resolvedBanReason()
        )
        typingStopJob?.cancel()
        lastSentTypingState = false
        telemetryCollector.setAppForeground(false)
        DuetNotificationStateStore.isAppInForeground = false
        DuetSyncService.stop(appContext)
    }

    private fun handleUnbannedProfile(profile: UserProfile) {
        val wasBanned = wasAccountBanned || _bannedAccount.value != null
        wasAccountBanned = false
        _bannedAccount.value = null
        _streamError.value = null
        lastTelemetrySyncKey = null
        lastTelemetrySyncedAtMs = 0L
        syncedCoupleMembersFor = null
        handlingDissolutionForCoupleId = null
        handlingMissingCoupleId = null
        DuetNotificationStateStore.isAppInForeground = true
        telemetryCollector.setAppForeground(true)
        refreshPermissionsState()
        if (wasBanned) {
            bootstrapProfile()
            showBanner("Your account has been reactivated! Welcome back to Duet.")
        }
        val hasCouple = !profile.coupleId.isNullOrBlank()
        if (hasCouple && _backgroundSyncEnabled.value) {
            DuetSyncService.start(appContext)
        }
    }

    fun refreshSuspendedAccountStatus() {
        runAction {
            val fresh = repository.fetchUserProfileFromServer(currentUserId)
            when {
                fresh == null -> showBanner("Unable to reach server. Please check your internet connection.")
                fresh.isAccountBanned() -> {
                    handleBannedProfile(fresh)
                    showBanner("Account is still suspended by admin.")
                }
                else -> handleUnbannedProfile(fresh)
            }
        }
    }

    private fun observePartnerBanNotifications() {
        viewModelScope.launch {
            combine(profileFlow, coupleFlow, partnerProfileFlow) { myProfile, couple, partnerProfile ->
                Triple(myProfile, couple, partnerProfile)
            }.collect { (myProfile, couple, partnerProfile) ->
                if (myProfile?.isAccountBanned() == true) return@collect
                if (couple == null || couple.status != "paired") return@collect
                val partnerUid = couple.otherUid(currentUserId)?.takeIf { it.isNotBlank() } ?: return@collect
                if (partnerProfile == null || partnerProfile.userId != partnerUid) return@collect
                if (!partnerProfile.isAccountBanned()) return@collect

                val reason = partnerProfile.resolvedBanReason()
                val bannedAtMillis = partnerProfile.bannedAt?.toDate()?.time ?: 0L
                val notifyKey = "partner_banned_${couple.id}_${partnerUid}_${bannedAtMillis}_${reason.orEmpty()}"
                if (!notificationStore.hasNotified(notifyKey)) {
                    notificationStore.markNotified(notifyKey)
                    val partnerName = couple.resolvedPartnerName(currentUserId, myProfile?.partnerNickname)
                    DuetNotificationHelper.showPartnerBannedNotification(
                        context = appContext,
                        partnerName = partnerName,
                        reason = reason
                    )
                }
            }
        }
    }

    private fun bootstrapProfile() {
        viewModelScope.launch {
            syncMyIdentityFromStore()
            val result = repository.ensureUserProfile(initialDisplayName, _myPublicKey.value)
            result.onSuccess { profile ->
                if (profile.isAccountBanned()) {
                    handleBannedProfile(profile)
                } else if (wasAccountBanned || _bannedAccount.value != null) {
                    handleUnbannedProfile(profile)
                }
            }.onFailure { err ->
                _streamError.value = err.localizedMessage ?: "Unable to load profile."
            }
            _profileBootstrapped.value = true
        }
    }

    private fun observeCoupleSideEffects() {
        viewModelScope.launch {
            combine(profileFlow, coupleLoadFlow) { profile, loadState ->
                profile to loadState
            }.collect { (profile, loadState) ->
                if (profile?.isAccountBanned() == true) {
                    handleBannedProfile(profile)
                    return@collect
                }
                if (profile != null && (wasAccountBanned || _bannedAccount.value != null)) {
                    handleUnbannedProfile(profile)
                }
                if (profile != null && profile.coupleId.isNullOrBlank()) {
                    handlingDissolutionForCoupleId = null
                    handlingMissingCoupleId = null
                    consumePendingRemovalNoticeIfAny()
                    return@collect
                }
                val expectedCoupleId = profile?.coupleId?.takeIf { it.isNotBlank() } ?: return@collect

                when (loadState) {
                    CoupleLoadState.Unloaded -> return@collect
                    CoupleLoadState.Missing -> {
                        if (handlingMissingCoupleId != expectedCoupleId && handlingDissolutionForCoupleId != expectedCoupleId) {
                            viewModelScope.launch {
                                delay(650L)
                                val currentProfile = profileFlow.value
                                if (currentProfile?.isAccountBanned() != true &&
                                    currentProfile?.coupleId == expectedCoupleId &&
                                    coupleLoadFlow.value is CoupleLoadState.Missing &&
                                    repository.fetchCoupleOnce(expectedCoupleId) == null
                                ) {
                                    handlingMissingCoupleId = expectedCoupleId
                                    finalizeMissingCoupleCleanup()
                                }
                            }
                        }
                        return@collect
                    }
                    is CoupleLoadState.Loaded -> {
                        val couple = loadState.couple
                        if (couple.status == "dissolved") {
                            if (handlingDissolutionForCoupleId != couple.id) {
                                handlingDissolutionForCoupleId = couple.id
                                finalizeRemovedPartnerSession(couple)
                            }
                            return@collect
                        }

                        handlingDissolutionForCoupleId = null
                        handlingMissingCoupleId = null
                        repository.syncMyPublicKeyOnCouple(couple, _myPublicKey.value)
                        val syncTag = "${couple.id}:${couple.status}:${couple.memberUids.size}"
                        if (couple.status == "paired" && couple.creatorUid == currentUserId && syncedCoupleMembersFor != syncTag) {
                            syncedCoupleMembersFor = syncTag
                            repository.syncPrePairingSubcollections(couple)
                        }
                        if (_backgroundSyncEnabled.value) {
                            DuetSyncService.start(appContext)
                        }
                    }
                }
            }
        }
    }

    private suspend fun finalizeRemovedPartnerSession(couple: CoupleSpace) {
        typingStopJob?.cancel()
        lastSentTypingState = false
        DuetSyncService.stop(appContext)
        notificationStore.setLastExcludedInviteCode(couple.inviteCode)
        val freshPubKey = rotateMyIdentityKeyPair()
        repository.completePartnerRemovalForRemovedUser(couple, freshPubKey)
        clearLocalCoupleCaches()
        _preferEnterCodeTabOnPairing.value = true
        if (couple.dissolvedByUid != currentUserId) {
            val removerName = couple.dissolvedByName?.takeIf { it.isNotBlank() } ?: "Your partner"
            showBanner("$removerName removed the partner link. All shared data was reset — enter or create a new 6-digit code to start fresh.")
        }
    }

    private suspend fun finalizeMissingCoupleCleanup() {
        typingStopJob?.cancel()
        lastSentTypingState = false
        DuetSyncService.stop(appContext)
        val freshPubKey = rotateMyIdentityKeyPair()
        repository.clearDanglingCoupleReference(freshPubKey)
        clearLocalCoupleCaches()
        _preferEnterCodeTabOnPairing.value = true
        val pending = notificationStore.consumePendingRemovalNotice()
        showBanner(pending ?: "Your previous couple space was reset. Enter or create a new 6-digit code to link freshly.")
    }

    private fun clearLocalCoupleCaches() {
        DuetAppearanceStore.clearCustomChatWallpaper(appContext)
        widgetStore.resetToUnpaired()
        WidgetStateStore.updateAllDuetWidgets(appContext)
        notificationStore.clearCoupleNotifications()
        markedSeenMessageIds.clear()
        lastTelemetrySyncKey = null
        lastTelemetrySyncedAtMs = 0L
        syncedCoupleMembersFor = null
        _streamError.value = null
    }

    private fun observeTelemetrySync() {
        viewModelScope.launch {
            val heartbeat = flow {
                var tick = 0L
                while (currentCoroutineContext().isActive) {
                    emit(tick++)
                    delay(20_000L)
                }
            }
            combine(
                profileFlow,
                coupleFlow,
                localTelemetryFlow,
                presencesFlow,
                combine(DuetNotificationStateStore.appForegroundFlow, heartbeat) { fg, t -> fg to t }
            ) { profile, couple, telemetry, presences, (inFg, _) ->
                val corrected = if (inFg && telemetry.deviceStatus != "LOCKED") {
                    telemetry.copy(deviceStatus = "ACTIVE_IN_DUET", activeAppName = "Duet")
                } else {
                    telemetry
                }
                TelemetrySyncPacket(profile, couple, corrected, presences.firstOrNull { it.userId == currentUserId })
            }.collect { pkt ->
                val couple = pkt.couple ?: return@collect
                if (couple.status == "dissolved") return@collect
                val profile = pkt.profile ?: return@collect
                if (profile.isAccountBanned()) return@collect
                val shareStatus = profile.shareDeviceStatus
                val moodKey = pkt.myPresence?.moodKey ?: "IN_LOVE"
                val moodNote = pkt.myPresence?.moodNote ?: ""
                val displayName = couple.myDisplayName(currentUserId)
                val syncKey = "${couple.id}|${couple.memberUids.joinToString(",")}|$shareStatus|${pkt.telemetry}|$moodKey|$moodNote|$displayName"
                val now = System.currentTimeMillis()
                val heartbeatDue = (now - lastTelemetrySyncedAtMs) >= 20_000L
                if (syncKey == lastTelemetrySyncKey && !heartbeatDue) return@collect
                val result = repository.upsertMyPresence(
                    couple = couple,
                    displayName = displayName,
                    telemetry = pkt.telemetry,
                    moodKey = moodKey,
                    moodNote = moodNote,
                    shareStatus = shareStatus,
                    markLastSeenNow = pkt.telemetry.deviceStatus == "ACTIVE_IN_DUET"
                )
                if (result.isSuccess) {
                    lastTelemetrySyncKey = syncKey
                    lastTelemetrySyncedAtMs = now
                }
            }
        }
    }

    private fun observeWidgetUpdates() {
        viewModelScope.launch {
            uiState.collect { state ->
                val couple = state.couple ?: return@collect
                if (couple.status == "dissolved") return@collect
                val partner = state.partnerPresence
                val displayPresence = partner ?: state.myPresence
                val moodOption = MoodCatalog.byKey(displayPresence?.moodKey)
                val partnerName = state.resolvedPartnerName()

                val coupleTitle = if (couple.status == "paired") {
                    "${couple.myDisplayName(currentUserId)} & $partnerName"
                } else {
                    "Duet • Code ${couple.inviteCode}"
                }

                val batteryBadge = when {
                    displayPresence == null -> "Live"
                    !displayPresence.statusShared || displayPresence.batteryPercent < 0 -> "Private"
                    displayPresence.charging -> "${displayPresence.batteryPercent}% ⚡"
                    else -> "${displayPresence.batteryPercent}%"
                }

                val whoLabel = if (partner != null) partnerName else "You"
                val moodLine = "${moodOption.emoji} $whoLabel: ${moodOption.label}"
                val countdownLine = buildWidgetCountdownLine(couple)
                val statusFooter = if (displayPresence != null) {
                    "$whoLabel: ${displayPresence.liveActivityLabel()}"
                } else {
                    "End-to-end encrypted for two"
                }

                val partnerStatus = when {
                    state.partnerBanned != null -> "⛔ Account Banned"
                    couple.status != "paired" -> "Invite code ${couple.inviteCode}"
                    partner == null -> "End-to-end encrypted"
                    partner.isPartnerActivelyTyping() -> "Typing…"
                    partner.deviceStatus == "ACTIVE_IN_DUET" -> "Online in Duet"
                    else -> partner.liveActivityLabel()
                }
                val lastTwo = state.messages.takeLast(2)
                val msg1 = if (lastTwo.size >= 2) {
                    formatWidgetMessageLine(lastTwo[0], currentUserId, partnerName)
                } else {
                    "🔒 End-to-end encrypted"
                }
                val latestMsg = lastTwo.lastOrNull()
                val msg2 = if (latestMsg != null) {
                    formatWidgetMessageLine(latestMsg, currentUserId, partnerName)
                } else {
                    "Tap below to chat privately"
                }
                val receipt = when {
                    latestMsg == null -> ""
                    latestMsg.raw.senderUid != currentUserId -> ""
                    latestMsg.raw.isSeenByOther(currentUserId) -> "✓✓ Seen"
                    else -> "✓ Sent"
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
                val bdayCd = couple.partnerBirthday(currentUserId)?.let { (m, d) ->
                    val b = CountdownCalculator.computeYearlyCountdown(m, d)
                    if (b.isToday) "Today! 🎂" else "${b.days}d ${b.hours}h"
                } ?: "Tap to set"

                val prompt = state.todayPrompt
                val isCreator = couple.creatorUid == currentUserId
                val myAnswer = if (isCreator) prompt?.creatorAnswer else prompt?.partnerAnswer
                val theirAnswer = if (isCreator) prompt?.partnerAnswer else prompt?.creatorAnswer
                val dailyQuestionText = DailyPromptDeck.questionForIndex(state.todayPromptIndex)
                val dailyQuestionStatus = when {
                    !myAnswer.isNullOrBlank() && !theirAnswer.isNullOrBlank() -> "✨ Both answered — tap to read!"
                    !myAnswer.isNullOrBlank() -> "✓ Answered — waiting for $partnerName"
                    !theirAnswer.isNullOrBlank() -> "✨ $partnerName answered — reply to unlock!"
                    else -> "Tap to answer Today's Question"
                }
                val dailyQuestionExpiry = "Expires in ${DailyPromptDeck.computeExpiryCountdown().formattedCompact} (IST)"
                val partnerNet = when {
                    displayPresence == null || !displayPresence.statusShared -> "Private"
                    else -> displayPresence.networkType.ifBlank { "Live" }
                }
                val partnerApp = displayPresence?.liveActivityLabel() ?: "Duet"
                val partnerDevice = displayPresence?.loggedInDeviceLabel()?.ifBlank { "Partner Phone" } ?: "Partner Phone"
                val jarHighlight = state.loveJarNotes.firstOrNull()?.let { "🫙 ${it.text}" }
                    ?: "Drop a sweet note in your Love Jar"
                val songHighlight = state.vibeDedications.firstOrNull()?.let { "${it.vibeEmoji} ${it.songTitle} • ${it.artistName}" }
                    ?: "Dedicate a song to each other"
                val doneBucket = state.bucketItems.count { it.completed }
                val bucketSummary = "$doneBucket/${state.bucketItems.size} Dreams Done"

                widgetStore.saveSnapshot(
                    DuetWidgetSnapshot(
                        coupleId = couple.id,
                        coupleTitle = coupleTitle,
                        partnerName = partnerName,
                        partnerStatus = partnerStatus,
                        partnerTyping = partner?.isPartnerActivelyTyping() == true,
                        batteryBadge = batteryBadge,
                        moodLine = moodLine,
                        countdownLine = countdownLine,
                        statusFooter = statusFooter,
                        lastMsg1 = msg1,
                        lastMsg2 = msg2,
                        lastMsgReceipt = receipt,
                        daysTogetherLabel = daysTogetherLabel,
                        anniversaryCountdown = annCd,
                        birthdayCountdown = bdayCd,
                        dailyQuestionText = dailyQuestionText,
                        dailyQuestionStatus = dailyQuestionStatus,
                        dailyQuestionExpiry = dailyQuestionExpiry,
                        partnerNetwork = partnerNet,
                        partnerActiveApp = partnerApp,
                        partnerDeviceName = partnerDevice,
                        loveJarHighlight = jarHighlight,
                        songVibeHighlight = songHighlight,
                        bucketSummary = bucketSummary,
                        themeStyle = state.widgetTheme
                    )
                )
                WidgetStateStore.updateAllDuetWidgets(appContext)
            }
        }
    }

    private fun formatWidgetMessageLine(
        item: DecryptedMessageItem,
        myUid: String,
        partnerName: String
    ): String {
        val prefix = if (item.raw.senderUid == myUid) "You" else partnerName.substringBefore(" ")
        val body = when {
            item.raw.messageType == "IMAGE" && item.text.isBlank() -> "📷 Encrypted photo"
            item.raw.messageType == "LOVE_PULSE" -> "❤️ ${item.text.ifBlank { "Heartbeat" }}"
            else -> item.text.ifBlank { "New message" }
        }
        return "$prefix: $body"
    }

    private fun buildWidgetCountdownLine(couple: CoupleSpace): String {
        val annM = couple.anniversaryMonth
        val annD = couple.anniversaryDay
        if (annM != null && annD != null) {
            val ann = CountdownCalculator.computeYearlyCountdown(annM, annD)
            return if (ann.isToday) "Happy Anniversary Today!" else "${ann.days}d until Anniversary"
        }
        val partnerBday = couple.partnerBirthday(currentUserId)
        if (partnerBday != null) {
            val bday = CountdownCalculator.computeYearlyCountdown(partnerBday.first, partnerBday.second)
            return if (bday.isToday) "Partner's Birthday Today!" else "${bday.days}d until Partner's Birthday"
        }
        return "Tap to set anniversary & birthday"
    }

    // --- LIFECYCLE & PERMISSIONS ---

    fun refreshPermissionsState() {
        _hasNotificationPermission.value = DuetNotificationHelper.hasNotificationPermission(appContext)
        _hasUsageAccessPermission.value = AppUsageMonitor.hasUsageStatsPermission(appContext)
    }

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        notificationStore.setBackgroundSyncEnabled(enabled)
        _backgroundSyncEnabled.value = enabled
        if (enabled) {
            DuetSyncService.start(appContext)
            showBanner("Always-on background notifications enabled")
        } else {
            DuetSyncService.stop(appContext)
            showBanner("Background sync paused")
        }
    }

    fun sendTestNotification() {
        val state = uiState.value
        val couple = state.couple
        if (!DuetNotificationHelper.hasNotificationPermission(appContext)) {
            showBanner("Enable notification permission first")
            return
        }
        DuetNotificationHelper.showTestNotificationWithReply(
            context = appContext,
            coupleId = couple?.id ?: "",
            partnerName = state.resolvedPartnerName()
        )
        showBanner("Test notification sent — try replying from the shade!")
    }

    fun setAppForeground(inForeground: Boolean) {
        val currentProfile = uiState.value.profile
        if (currentProfile?.isAccountBanned() == true || _bannedAccount.value != null) {
            DuetNotificationStateStore.isAppInForeground = false
            telemetryCollector.setAppForeground(false)
            return
        }
        DuetNotificationStateStore.isAppInForeground = inForeground
        telemetryCollector.setAppForeground(inForeground)
        if (inForeground) {
            refreshPermissionsState()
            consumePendingRemovalNoticeIfAny()
            val activeCouple = uiState.value.couple
            if (_backgroundSyncEnabled.value && activeCouple != null && activeCouple.status != "dissolved") {
                DuetSyncService.start(appContext)
            }
        }
        val couple = uiState.value.couple ?: return
        if (couple.status == "dissolved") return
        val profile = currentProfile ?: return
        val myPresence = uiState.value.myPresence
        val freshTelemetry = telemetryCollector.readSnapshot(forceRefreshSpecs = inForeground)
        if (!inForeground) {
            onChatTypingChanged(false)
        }
        viewModelScope.launch {
            repository.upsertMyPresence(
                couple = couple,
                displayName = couple.myDisplayName(currentUserId),
                telemetry = freshTelemetry,
                moodKey = myPresence?.moodKey ?: "IN_LOVE",
                moodNote = myPresence?.moodNote ?: "",
                shareStatus = profile.shareDeviceStatus,
                // Always stamp exact WhatsApp-style lastSeenAt when active in Duet OR the instant user leaves Duet
                markLastSeenNow = true
            )
        }
    }

    fun refreshMyTelemetrySilently() {
        refreshPermissionsState()
        val couple = uiState.value.couple ?: return
        if (couple.status == "dissolved") return
        val profile = uiState.value.profile ?: return
        if (profile.isAccountBanned() || _bannedAccount.value != null) return
        val myPresence = uiState.value.myPresence
        val fresh = telemetryCollector.readSnapshot(forceRefreshSpecs = false)
        viewModelScope.launch {
            val res = repository.upsertMyPresence(
                couple = couple,
                displayName = couple.myDisplayName(currentUserId),
                telemetry = fresh,
                moodKey = myPresence?.moodKey ?: "IN_LOVE",
                moodNote = myPresence?.moodNote ?: "",
                shareStatus = profile.shareDeviceStatus,
                markLastSeenNow = fresh.deviceStatus == "ACTIVE_IN_DUET"
            )
            if (res.isSuccess) {
                lastTelemetrySyncedAtMs = System.currentTimeMillis()
            }
        }
    }

    fun refreshMyTelemetryNow() {
        refreshMyTelemetrySilently()
    }

    // --- USER ACTIONS ---

    fun createCoupleSpace(nickname: String) {
        runAction {
            syncMyIdentityFromStore()
            repository.createCoupleSpace(
                nickname = nickname,
                myPublicKey = _myPublicKey.value,
                excludeInviteCode = notificationStore.getLastExcludedInviteCode(),
                vaultKeyCreator = { code, cId -> crypto.createWrappedVaultKey(code, cId) }
            ).onSuccess { created ->
                _preferEnterCodeTabOnPairing.value = false
                showBanner("New code ${created.inviteCode} ready — share it with your partner!")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to create space")
            }
        }
    }

    fun joinCoupleByCode(code: String, nickname: String) {
        runAction {
            syncMyIdentityFromStore()
            repository.joinCoupleByCode(
                rawCode = code,
                nickname = nickname,
                myPublicKey = _myPublicKey.value
            ).onSuccess {
                _preferEnterCodeTabOnPairing.value = false
                showBanner("Connected! End-to-end encryption is active.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Unable to link with that code")
            }
        }
    }

    /**
     * Permanently removes the partner link for this couple space ONLY after verifying this couple's
     * unique 24-digit Couple Safety Number. Wipes all shared moments, sent chats, and couple data,
     * invalidates & rotates the 6-digit linking code, resets local state, and transfers BOTH users
     * to the Enter/Create 6-Digit Code screen to start freshly.
     */
    fun removePartnerWithSafetyNumber(
        enteredSafetyNumber: String,
        onCompleted: () -> Unit = {}
    ) {
        val state = uiState.value
        val couple = state.couple ?: return
        val expectedDigits = DuetCryptoManager.normalizeSafetyNumberDigits(state.safetyNumber)
        val enteredDigits = DuetCryptoManager.normalizeSafetyNumberDigits(enteredSafetyNumber)
        if (expectedDigits.isBlank() || enteredDigits != expectedDigits) {
            showBanner("Couple Safety Number does not match. Enter the exact 24 digits for this couple.")
            return
        }

        runAction {
            typingStopJob?.cancel()
            lastSentTypingState = false
            notificationStore.setLastExcludedInviteCode(couple.inviteCode)
            handlingDissolutionForCoupleId = couple.id
            DuetSyncService.stop(appContext)

            val freshPubKey = rotateMyIdentityKeyPair()
            val result = repository.removePartnerAndWipeCouple(
                couple = couple,
                myDisplayName = couple.myDisplayName(currentUserId),
                newPublicKey = freshPubKey
            )
            result.onSuccess {
                clearLocalCoupleCaches()
                _preferEnterCodeTabOnPairing.value = true
                showBanner("Partner removed. All shared chats & moments were deleted and your 6-digit code was reset.")
                onCompleted()
            }.onFailure { err ->
                handlingDissolutionForCoupleId = null
                if (_backgroundSyncEnabled.value) {
                    DuetSyncService.start(appContext)
                }
                showBanner(err.localizedMessage ?: "Could not complete partner removal. Check your connection and try again.")
            }
        }
    }

    fun shareMood(mood: MoodOption, note: String) {
        val couple = uiState.value.couple ?: return
        val profile = uiState.value.profile ?: return
        runAction {
            val myName = couple.myDisplayName(currentUserId)
            val logResult = repository.logMood(couple, myName, mood, note)
            repository.upsertMyPresence(
                couple = couple,
                displayName = myName,
                telemetry = telemetryCollector.readSnapshot(),
                moodKey = mood.key,
                moodNote = note,
                shareStatus = profile.shareDeviceStatus,
                markLastSeenNow = true
            )
            logResult.onSuccess {
                showBanner("Mood updated to ${mood.emoji} ${mood.label}")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Failed to update mood")
            }
        }
    }

    fun sendComfortHug(moodId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.sendComfortHug(couple.id, moodId)
                .onSuccess { showBanner("Warm hug sent to your partner") }
                .onFailure { showBanner(it.localizedMessage ?: "Could not send hug") }
        }
    }

    fun sendLovePulse() {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.sendLovePulse(couple)
                .onSuccess { showBanner("Thinking-of-you pulse sent") }
                .onFailure { showBanner(it.localizedMessage ?: "Could not send pulse") }
        }
    }

    fun sendTextMessage(text: String) {
        val couple = uiState.value.couple ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        onChatTypingChanged(false)
        viewModelScope.launch {
            val payloadJson = JSONObject().apply { put("text", trimmed) }.toString()
            val envelope = crypto.encrypt(
                plaintext = payloadJson,
                coupleId = couple.id,
                inviteCode = couple.inviteCode,
                wrappedVaultKey = couple.wrappedVaultKey,
                partnerPublicKey = couple.otherPublicKey(currentUserId)
            )
            repository.sendEncryptedMessage(
                couple = couple,
                senderName = couple.myDisplayName(currentUserId),
                messageType = "TEXT",
                envelope = envelope
            ).onFailure {
                showBanner(it.localizedMessage ?: "Could not send message")
            }
        }
    }

    fun sendLovePulseMessage(note: String = "Sending you a warm heartbeat") {
        val couple = uiState.value.couple ?: return
        onChatTypingChanged(false)
        viewModelScope.launch {
            val payloadJson = JSONObject().apply { put("text", note) }.toString()
            val envelope = crypto.encrypt(
                plaintext = payloadJson,
                coupleId = couple.id,
                inviteCode = couple.inviteCode,
                wrappedVaultKey = couple.wrappedVaultKey,
                partnerPublicKey = couple.otherPublicKey(currentUserId)
            )
            repository.sendEncryptedMessage(
                couple = couple,
                senderName = couple.myDisplayName(currentUserId),
                messageType = "LOVE_PULSE",
                envelope = envelope
            ).onSuccess {
                repository.sendLovePulse(couple)
            }.onFailure {
                showBanner(it.localizedMessage ?: "Could not send pulse")
            }
        }
    }

    fun sendPhotoMessage(uri: Uri?, stagedFile: File?, caption: String) {
        val couple = uiState.value.couple ?: return
        onChatTypingChanged(false)
        runAction {
            try {
                val base64 = PhotoCompressor.compressToBase64(appContext, uri, stagedFile)
                if (base64 == null) {
                    showBanner("Could not decode that image. Please pick a JPEG, PNG, HEIC, or WebP photo.")
                    return@runAction
                }
                val payloadJson = JSONObject().apply {
                    put("text", caption.trim().take(200))
                    put("imageBase64", base64)
                }.toString()
                val envelope = crypto.encrypt(
                    plaintext = payloadJson,
                    coupleId = couple.id,
                    inviteCode = couple.inviteCode,
                    wrappedVaultKey = couple.wrappedVaultKey,
                    partnerPublicKey = couple.otherPublicKey(currentUserId)
                )
                repository.sendEncryptedMessage(
                    couple = couple,
                    senderName = couple.myDisplayName(currentUserId),
                    messageType = "IMAGE",
                    envelope = envelope
                ).onSuccess {
                    showBanner("HD encrypted photo sent")
                }.onFailure {
                    showBanner(it.localizedMessage ?: "Could not send photo")
                }
            } finally {
                stagedFile?.delete()
            }
        }
    }

    fun onChatTypingChanged(isTyping: Boolean) {
        val coupleId = uiState.value.couple?.id ?: return
        typingStopJob?.cancel()
        val now = System.currentTimeMillis()
        if (isTyping) {
            if (!lastSentTypingState || (now - lastTypingHeartbeatMs) >= 3_500L) {
                lastSentTypingState = true
                lastTypingHeartbeatMs = now
                viewModelScope.launch {
                    repository.setTypingStatus(coupleId, true)
                }
            }
            typingStopJob = viewModelScope.launch {
                delay(4_500L)
                if (lastSentTypingState) {
                    lastSentTypingState = false
                    repository.setTypingStatus(coupleId, false)
                }
            }
        } else {
            if (lastSentTypingState) {
                lastSentTypingState = false
                viewModelScope.launch {
                    repository.setTypingStatus(coupleId, false)
                }
            }
        }
    }

    fun markIncomingMessagesSeen(messages: List<DecryptedMessageItem>) {
        val coupleId = uiState.value.couple?.id ?: return
        val unseenIds = messages
            .asSequence()
            .map { it.raw }
            .filter { it.id.isNotBlank() && it.senderUid != currentUserId && it.seenByUid.isNullOrBlank() }
            .map { it.id }
            .filter { it !in markedSeenMessageIds }
            .toList()
        if (unseenIds.isEmpty()) return
        markedSeenMessageIds.addAll(unseenIds)
        viewModelScope.launch {
            val res = repository.markMessagesSeen(coupleId, unseenIds)
            if (res.isFailure) {
                markedSeenMessageIds.removeAll(unseenIds.toSet())
            }
        }
    }

    fun reactToMessage(
        messageId: String,
        currentReactions: Map<String, String>,
        reactionKey: String
    ) {
        val couple = uiState.value.couple ?: return
        val nextKey = if (currentReactions[currentUserId] == reactionKey) null else reactionKey
        viewModelScope.launch {
            repository.reactToMessage(couple.id, messageId, nextKey)
        }
    }

    fun deleteMessage(messageId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.deleteMessage(couple.id, messageId)
                .onSuccess { showBanner("Message deleted") }
                .onFailure { showBanner(it.localizedMessage ?: "Could not delete message") }
        }
    }

    fun uploadMoment(uri: Uri?, stagedFile: File?, caption: String, locationTag: String) {
        val couple = uiState.value.couple ?: return
        runAction {
            try {
                val base64 = PhotoCompressor.compressToBase64(appContext, uri, stagedFile)
                if (base64 == null) {
                    showBanner("Could not decode that image. Please pick a JPEG, PNG, HEIC, or WebP photo.")
                    return@runAction
                }
                val payloadJson = JSONObject().apply {
                    put("caption", caption.trim().take(220))
                    put("locationTag", locationTag.trim().take(60))
                    put("imageBase64", base64)
                }.toString()
                val envelope = crypto.encrypt(
                    plaintext = payloadJson,
                    coupleId = couple.id,
                    inviteCode = couple.inviteCode,
                    wrappedVaultKey = couple.wrappedVaultKey,
                    partnerPublicKey = couple.otherPublicKey(currentUserId)
                )
                repository.uploadEncryptedMoment(
                    couple = couple,
                    uploaderName = couple.myDisplayName(currentUserId),
                    envelope = envelope
                ).onSuccess {
                    showBanner("HD encrypted keepsake shared with your partner")
                }.onFailure {
                    showBanner(it.localizedMessage ?: "Could not upload photo")
                }
            } finally {
                stagedFile?.delete()
            }
        }
    }

    fun toggleMomentLove(moment: SharedMoment) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.toggleMomentLove(couple.id, moment)
        }
    }

    fun deleteMoment(momentId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.deleteMoment(couple.id, momentId)
                .onSuccess { showBanner("Moment removed") }
        }
    }

    fun updateAnniversary(year: Int, month: Int, day: Int) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.updateAnniversary(couple.id, year, month, day)
                .onSuccess { showBanner("Anniversary date saved") }
                .onFailure { showBanner(it.localizedMessage ?: "Failed to save anniversary") }
        }
    }

    fun updateBirthday(forPartner: Boolean, month: Int, day: Int) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.updateBirthday(couple, forPartner, month, day)
                .onSuccess { showBanner(if (forPartner) "Partner's birthday saved" else "Your birthday saved") }
                .onFailure { showBanner(it.localizedMessage ?: "Failed to save birthday") }
        }
    }

    fun addSpecialEvent(
        title: String,
        category: String,
        year: Int,
        month: Int,
        day: Int,
        repeatsYearly: Boolean
    ) {
        val couple = uiState.value.couple ?: return
        runAction {
            repository.addSpecialEvent(couple, title, category, year, month, day, repeatsYearly)
                .onSuccess { showBanner("Countdown added") }
                .onFailure { showBanner(it.localizedMessage ?: "Could not add countdown") }
        }
    }

    fun deleteSpecialEvent(eventId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.deleteSpecialEvent(couple.id, eventId)
                .onSuccess { showBanner("Countdown removed") }
        }
    }

    fun submitDailyPromptAnswer(answer: String) {
        val couple = uiState.value.couple ?: return
        val todayKey = DailyPromptDeck.todayDateKey()
        runAction {
            repository.submitDailyPromptAnswer(
                couple = couple,
                dateKey = todayKey,
                promptIndex = uiState.value.todayPromptIndex,
                answer = answer
            ).onSuccess {
                // Suppress 1-hour expiry alert immediately once answered
                notificationStore.markNotified("prompt_expiry_${couple.id}_$todayKey")
                showBanner("Answer sealed! Unlocks when both reply.")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Could not save answer")
            }
        }
    }

    fun addBucketItem(title: String, category: String) {
        val couple = uiState.value.couple ?: return
        runAction {
            repository.addBucketItem(
                couple = couple,
                creatorName = couple.myDisplayName(currentUserId),
                title = title,
                category = category
            ).onSuccess {
                showBanner("Added to your shared Bucket List ✨")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Could not add dream")
            }
        }
    }

    fun toggleBucketItem(item: BucketListItem) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.toggleBucketItemCompleted(couple.id, item)
                .onSuccess {
                    if (!item.completed) {
                        showBanner("Dream completed together! 🎉")
                    }
                }
                .onFailure {
                    showBanner(it.localizedMessage ?: "Could not update bucket list")
                }
        }
    }

    fun deleteBucketItem(itemId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.deleteBucketItem(couple.id, itemId)
                .onSuccess { showBanner("Removed from Bucket List") }
                .onFailure { showBanner(it.localizedMessage ?: "Could not delete item") }
        }
    }

    // --- 6 NEW COUPLE FACILITIES ---

    fun sendLoveLetter(title: String, body: String, sealStyle: String, unlockAtMillis: Long) {
        val couple = uiState.value.couple ?: return
        runAction {
            repository.sendLoveLetter(
                couple = couple,
                senderName = couple.myDisplayName(currentUserId),
                title = title,
                body = body,
                sealStyle = sealStyle,
                unlockAtMillis = unlockAtMillis
            ).onSuccess {
                val msg = if (unlockAtMillis > System.currentTimeMillis()) {
                    "Time Capsule Letter sealed ⏳💌"
                } else {
                    "Love Letter sealed & delivered 💌"
                }
                showBanner(msg)
            }.onFailure {
                showBanner(it.localizedMessage ?: "Could not seal letter")
            }
        }
    }

    fun openLoveLetter(letter: LoveLetter) {
        val couple = uiState.value.couple ?: return
        if (letter.senderUid == currentUserId || letter.openedByPartner) return
        viewModelScope.launch {
            repository.markLoveLetterOpened(couple.id, letter.id)
        }
    }

    fun deleteLoveLetter(letterId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.deleteLoveLetter(couple.id, letterId)
                .onSuccess { showBanner("Love letter removed") }
                .onFailure { showBanner(it.localizedMessage ?: "Could not delete letter") }
        }
    }

    fun addLoveJarNote(kind: String, text: String) {
        val couple = uiState.value.couple ?: return
        runAction {
            repository.addLoveJarNote(
                couple = couple,
                authorName = couple.myDisplayName(currentUserId),
                kind = kind,
                text = text
            ).onSuccess {
                showBanner("Dropped into your Love Jar 🫙💖")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Could not add note")
            }
        }
    }

    fun toggleLoveJarGranted(note: LoveJarNote) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.toggleLoveJarGranted(couple.id, note)
                .onSuccess {
                    if (!note.granted) showBanner("Wish granted! 🌠")
                }
                .onFailure { showBanner(it.localizedMessage ?: "Could not update wish") }
        }
    }

    fun deleteLoveJarNote(noteId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.deleteLoveJarNote(couple.id, noteId)
                .onSuccess { showBanner("Note removed from jar") }
        }
    }

    fun sendVibeDedication(songTitle: String, artistName: String, vibeEmoji: String, note: String) {
        val couple = uiState.value.couple ?: return
        runAction {
            repository.sendVibeDedication(
                couple = couple,
                senderName = couple.myDisplayName(currentUserId),
                songTitle = songTitle,
                artistName = artistName,
                vibeEmoji = vibeEmoji,
                dedicationNote = note
            ).onSuccess {
                showBanner("Song dedicated to your partner $vibeEmoji")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Could not dedicate song")
            }
        }
    }

    fun toggleVibeLoved(vibe: VibeDedication) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.toggleVibeLoved(couple.id, vibe)
        }
    }

    fun deleteVibeDedication(vibeId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.deleteVibeDedication(couple.id, vibeId)
                .onSuccess { showBanner("Dedication removed") }
        }
    }

    fun sendCareCheckIn(careType: String, customMessage: String) {
        val couple = uiState.value.couple ?: return
        val preset = CareCheckInCatalog.byKey(careType)
        runAction {
            repository.sendCareCheckIn(
                couple = couple,
                senderName = couple.myDisplayName(currentUserId),
                careType = preset.key,
                message = customMessage
            ).onSuccess {
                showBanner("${preset.emoji} Care check-in sent to your partner!")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Could not send care check-in")
            }
        }
    }

    fun acknowledgeCareCheckIn(checkInId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.acknowledgeCareCheckIn(couple.id, checkInId)
                .onSuccess { showBanner("Marked done! Partner notified ❤️") }
                .onFailure { showBanner(it.localizedMessage ?: "Could not update check-in") }
        }
    }

    fun sendLoveDoodle(title: String, imageBase64: String) {
        val couple = uiState.value.couple ?: return
        runAction {
            repository.sendLoveDoodle(
                couple = couple,
                artistName = couple.myDisplayName(currentUserId),
                title = title,
                imageBase64 = imageBase64
            ).onSuccess {
                showBanner("HD Love Doodle sent to your gallery 🎨")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Could not save doodle")
            }
        }
    }

    fun toggleDoodleLoved(doodle: LoveDoodle) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.toggleDoodleLoved(couple.id, doodle)
        }
    }

    fun deleteLoveDoodle(doodleId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.deleteLoveDoodle(couple.id, doodleId)
                .onSuccess { showBanner("Doodle removed") }
        }
    }

    fun addCouplePromise(title: String, category: String) {
        val couple = uiState.value.couple ?: return
        runAction {
            repository.addCouplePromise(
                couple = couple,
                creatorName = couple.myDisplayName(currentUserId),
                title = title,
                category = category
            ).onSuccess {
                showBanner("Promise added — waiting for partner's pinky swear 🤙")
            }.onFailure {
                showBanner(it.localizedMessage ?: "Could not add promise")
            }
        }
    }

    fun coSignPromise(promise: CouplePromise) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.coSignPromise(couple.id, promise)
                .onSuccess { showBanner("Pinky-swore together! 🤙💍") }
                .onFailure { showBanner(it.localizedMessage ?: "Could not co-sign promise") }
        }
    }

    fun deleteCouplePromise(promiseId: String) {
        val couple = uiState.value.couple ?: return
        viewModelScope.launch {
            repository.deleteCouplePromise(couple.id, promiseId)
                .onSuccess { showBanner("Promise removed") }
        }
    }

    fun setShareDeviceStatus(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShareDeviceStatus(enabled)
                .onSuccess {
                    showBanner(if (enabled) "Sharing live battery, app & status" else "Device telemetry paused")
                }
        }
    }

    fun updateMyNickname(newName: String) {
        runAction {
            repository.updateMyDisplayName(uiState.value.couple, newName)
                .onSuccess { showBanner("Your name updated") }
                .onFailure { showBanner(it.localizedMessage ?: "Could not update name") }
        }
    }

    fun updatePartnerNickname(nickname: String) {
        runAction {
            val clean = nickname.trim().take(40)
            repository.updatePartnerNickname(clean)
                .onSuccess {
                    showBanner(
                        if (clean.isBlank()) "Partner nickname cleared"
                        else "Partner nickname set to \"$clean\""
                    )
                }
                .onFailure { showBanner(it.localizedMessage ?: "Could not save partner nickname") }
        }
    }

    fun selectWidgetTheme(style: WidgetThemeStyle) {
        _widgetTheme.value = style
        DuetAppearanceStore.selectTheme(appContext, style.toAppThemePreset())
    }

    fun dismissBanner() {
        _bannerMessage.value = null
        _streamError.value = null
    }

    fun showBanner(message: String) {
        _bannerMessage.value = message
    }

    private fun decryptMessage(raw: EncryptedMessage, couple: CoupleSpace): DecryptedMessageItem {
        val plain = crypto.decrypt(
            ciphertext = raw.ciphertext,
            ivBase64 = raw.iv,
            keyMode = raw.keyMode,
            coupleId = couple.id,
            inviteCode = couple.inviteCode,
            wrappedVaultKey = couple.wrappedVaultKey,
            partnerPublicKey = couple.otherPublicKey(currentUserId)
        ) ?: return DecryptedMessageItem(raw = raw, text = "Encrypted on another device", decryptedOk = false)

        return try {
            val json = JSONObject(plain)
            DecryptedMessageItem(
                raw = raw,
                text = json.optString("text", ""),
                imageBase64 = json.optString("imageBase64", "").takeIf { it.isNotBlank() },
                decryptedOk = true
            )
        } catch (_: Exception) {
            DecryptedMessageItem(raw = raw, text = plain, decryptedOk = true)
        }
    }

    private fun decryptMoment(raw: SharedMoment, couple: CoupleSpace): DecryptedMomentItem {
        val plain = crypto.decrypt(
            ciphertext = raw.ciphertext,
            ivBase64 = raw.iv,
            keyMode = raw.keyMode,
            coupleId = couple.id,
            inviteCode = couple.inviteCode,
            wrappedVaultKey = couple.wrappedVaultKey,
            partnerPublicKey = couple.otherPublicKey(currentUserId)
        ) ?: return DecryptedMomentItem(raw = raw, caption = "Encrypted keepsake", decryptedOk = false)

        return try {
            val json = JSONObject(plain)
            DecryptedMomentItem(
                raw = raw,
                caption = json.optString("caption", ""),
                locationTag = json.optString("locationTag", ""),
                imageBase64 = json.optString("imageBase64", "").takeIf { it.isNotBlank() },
                decryptedOk = true
            )
        } catch (_: Exception) {
            DecryptedMomentItem(raw = raw, caption = plain, decryptedOk = true)
        }
    }

    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }

    private fun <T> Flow<T>.safeCatch(fallbackMessage: String): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallbackMessage, rootError)
        _streamError.update { rootError.message ?: fallbackMessage }
    }

    private data class CoreBundle(
        val profile: UserProfile?,
        val couple: CoupleSpace?,
        val partnerProfile: UserProfile?,
        val presences: List<PartnerPresence>,
        val localTelemetry: LocalDeviceTelemetry,
        val bootstrapped: Boolean
    )

    private data class ContentBundle(
        val messages: List<EncryptedMessage>,
        val moods: List<MoodEntry>,
        val moments: List<SharedMoment>,
        val events: List<SpecialDateEvent>,
        val bucketItems: List<BucketListItem>,
        val prompt: DailyPromptEntry?
    )

    private data class FacilitiesBundle(
        val loveLetters: List<LoveLetter>,
        val loveJarNotes: List<LoveJarNote>,
        val vibeDedications: List<VibeDedication>,
        val careCheckIns: List<CareCheckIn>,
        val loveDoodles: List<LoveDoodle>,
        val couplePromises: List<CouplePromise>
    )

    private data class PermissionsBundle(
        val backgroundSyncEnabled: Boolean,
        val hasNotificationPermission: Boolean,
        val hasUsageAccessPermission: Boolean,
        val isOnline: Boolean
    )

    private data class IdentityBundle(
        val myPublicKey: String,
        val myKeyFingerprint: String,
        val preferEnterCodeTabOnPairing: Boolean
    )

    private data class MetaBundle(
        val busy: Boolean,
        val banner: String?,
        val streamError: String?,
        val widgetTheme: WidgetThemeStyle,
        val permissions: PermissionsBundle,
        val identity: IdentityBundle
    )

    private data class TelemetrySyncPacket(
        val profile: UserProfile?,
        val couple: CoupleSpace?,
        val telemetry: LocalDeviceTelemetry,
        val myPresence: PartnerPresence?
    )

    companion object {
        private const val TAG = "DuetViewModel"
    }
}
