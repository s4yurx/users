package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.example.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOutGoogleAccount
import com.example.crypto.DuetCryptoManager
import com.example.data.AppConfigUrlValidator
import com.example.data.BONDLY_ADMIN_PACKAGE_ID
import com.example.data.BannedAccountNotice
import com.example.data.CountdownCalculator
import com.example.data.DuetRepository
import com.example.data.LiveAppConfig
import com.example.data.MoodCatalog
import com.example.notifications.DuetNotificationHelper
import com.example.notifications.DuetNotificationStateStore
import com.example.service.DuetSyncService
import com.example.telemetry.AppUsageMonitor
import com.example.telemetry.DeviceTelemetryCollector
import com.example.telemetry.NetworkConnectivityMonitor
import com.example.ui.components.AccountSuspendedScreen
import com.example.ui.components.AppearanceStudioSheet
import com.example.ui.components.AppleStyleFloatingNavBar
import com.example.ui.components.BucketListSheet
import com.example.ui.components.CareCheckInSheet
import com.example.ui.components.ChatSettingsSheet
import com.example.ui.components.CouplePromisesSheet
import com.example.ui.components.DateSpinnerSheet
import com.example.ui.components.DeviceDetailsSheet
import com.example.ui.components.DuetSettingsSheet
import com.example.ui.components.EncryptionInfoSheet
import com.example.ui.components.FloatingNavItem
import com.example.ui.components.ForceUpdateDialog
import com.example.ui.components.LoveDoodleSheet
import com.example.ui.components.LoveGamesSheet
import com.example.ui.components.LoveJarSheet
import com.example.ui.components.LoveLettersSheet
import com.example.ui.components.MaintenanceModeScreen
import com.example.ui.components.MoodHistorySheet
import com.example.ui.components.OfflineConnectivityPrompt
import com.example.ui.components.OptionalUpdateDialog
import com.example.ui.components.PartnerStatusSheet
import com.example.ui.components.RemovePartnerWarningSheet
import com.example.ui.components.RisingZoomBubblesContainer
import com.example.ui.components.SongVibeDedicationSheet
import com.example.ui.components.TopAnnouncementBanner
import com.example.ui.components.TopPromoBanner
import com.example.ui.components.TopUpdateAvailableBanner
import com.example.ui.components.UpcomingMaintenanceBanner
import com.example.ui.components.WidgetStudioSheet
import com.example.ui.components.glossyPressEffect
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DatesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MomentsScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.theme.DuetAppearanceState
import com.example.widget.WidgetStateStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class DuetTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    CHAT("chat", "Private", Icons.Rounded.Forum, Icons.Outlined.Forum),
    MOMENTS("moments", "Moments", Icons.Rounded.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    DATES("dates", "Dates", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth)
}

@Composable
fun DuetRootApp(
    appearance: DuetAppearanceState = DuetAppearanceState(),
    requestedTabRoute: String? = null,
    onRequestedTabConsumed: () -> Unit = {},
    openUpdatePromptRequested: Boolean = false,
    onUpdatePromptConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    val databaseId = remember(context) { context.getString(R.string.firestore_database_id) }
    val rootRepository = remember(databaseId) { DuetRepository(FirebaseFirestore.getInstance(databaseId)) }
    val liveConfig by remember(rootRepository) { rootRepository.observeLiveAppConfig() }
        .collectAsStateWithLifecycle(initialValue = LiveAppConfig())
    val notificationStateStore = remember(context) { DuetNotificationStateStore(context) }

    var bannedNotice by remember { mutableStateOf<BannedAccountNotice?>(null) }
    LaunchedEffect(user?.uid) {
        if (user == null) {
            bannedNotice = null
        }
    }
    var dismissedBannerSignature by rememberSaveable {
        mutableStateOf(notificationStateStore.getDismissedBannerSignature())
    }
    var dismissedPromoSignature by rememberSaveable {
        mutableStateOf(notificationStateStore.getDismissedPromoSignature())
    }
    var dismissedUpdateSignature by rememberSaveable {
        mutableStateOf(notificationStateStore.getDismissedUpdateSignature())
    }
    var dismissedUpcomingMaintSig by rememberSaveable { mutableStateOf<String?>(null) }
    var manualOpenUpdatePopup by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(openUpdatePromptRequested) {
        if (openUpdatePromptRequested) {
            manualOpenUpdatePopup = true
            onUpdatePromptConsumed()
        }
    }

    LaunchedEffect(liveConfig) {
        if (!liveConfig.shouldShowAnnouncementBanner()) {
            dismissedBannerSignature = null
            notificationStateStore.setDismissedBannerSignature(null)
        }
        if (!liveConfig.shouldShowPromoBanner()) {
            dismissedPromoSignature = null
            notificationStateStore.setDismissedPromoSignature(null)
        }
        if (!liveConfig.upcomingMaintenance) {
            dismissedUpcomingMaintSig = null
        }
        val hasUpdate = liveConfig.hasAnyUpdateForNotification(
            currentVersionName = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE
        )
        if (hasUpdate) {
            val updateNotifyKey = "ota_update_${liveConfig.updateSignature()}"
            if (!notificationStateStore.hasNotified(updateNotifyKey)) {
                notificationStateStore.markNotified(updateNotifyKey)
                val targetVer = liveConfig.notificationUpdateVersion(BuildConfig.VERSION_NAME)
                val isForced = liveConfig.isForceUpdateRequired(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    currentVersionCode = BuildConfig.VERSION_CODE
                )
                DuetNotificationHelper.showAppUpdateNotification(
                    context = context,
                    newVersion = targetVer,
                    title = liveConfig.updateTitle,
                    releaseNotes = liveConfig.updateMessage,
                    updateLink = liveConfig.effectiveUpdateLink(context.packageName),
                    isForced = isForced
                )
            }
        }
    }

    val openExternalWebUrl: (String) -> Unit = { rawUrl ->
        if (AppConfigUrlValidator.isSafeWebUrl(rawUrl)) {
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl.trim())).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(viewIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to open link on this device", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No valid web link available", Toast.LENGTH_SHORT).show()
        }
    }

    val adminPackageId = liveConfig.resolvedAdminPackageId().ifBlank { BONDLY_ADMIN_PACKAGE_ID }
    val isAdminAppInstalled = remember(context, adminPackageId) {
        context.packageManager.getLaunchIntentForPackage(adminPackageId) != null
    }
    val openAdminCompanion: () -> Unit = {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(adminPackageId)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to launch Bondly Admin app", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Bondly Admin ($adminPackageId) not installed on this phone", Toast.LENGTH_SHORT).show()
        }
    }

    val connectivityMonitor = remember(context) { NetworkConnectivityMonitor(context) }
    val rootIsOnline by connectivityMonitor.observeIsOnline()
        .collectAsStateWithLifecycle(initialValue = connectivityMonitor.isCurrentlyOnline())

    val currentBannerSignature = liveConfig.bannerSignature()
    val showAnnouncementBanner = bannedNotice == null &&
        !liveConfig.maintenanceMode &&
        liveConfig.shouldShowAnnouncementBanner() &&
        dismissedBannerSignature != currentBannerSignature

    val currentPromoSignature = liveConfig.promoSignature()
    val showPromoBanner = bannedNotice == null &&
        !liveConfig.maintenanceMode &&
        liveConfig.shouldShowPromoBanner() &&
        dismissedPromoSignature != currentPromoSignature

    val currentUpcomingSig = liveConfig.upcomingMaintenanceSignature()
    val showUpcomingMaintenanceBanner = bannedNotice == null &&
        !liveConfig.maintenanceMode &&
        liveConfig.upcomingMaintenance &&
        liveConfig.upcomingMaintenanceText.isNotBlank() &&
        dismissedUpcomingMaintSig != currentUpcomingSig

    val showForceUpdateDialog = bannedNotice == null &&
        !liveConfig.maintenanceMode &&
        liveConfig.isForceUpdateRequired(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

    val currentUpdateSignature = liveConfig.updateSignature()
    val showOptionalUpdateBanner = bannedNotice == null &&
        !liveConfig.maintenanceMode &&
        !showForceUpdateDialog &&
        liveConfig.shouldShowOptionalUpdateBanner(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) &&
        dismissedUpdateSignature != currentUpdateSignature

    val showOptionalUpdatePopup = bannedNotice == null &&
        !liveConfig.maintenanceMode &&
        !showForceUpdateDialog &&
        (
            manualOpenUpdatePopup ||
                (
                    liveConfig.shouldShowOptionalUpdatePopup(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) &&
                        dismissedUpdateSignature != currentUpdateSignature
                    )
            )

    Box(modifier = Modifier.fillMaxSize()) {
        RisingZoomBubblesContainer(
            enabled = appearance.bubblesEnabled,
            accentA = appearance.themePreset.bubbleAccentA,
            accentB = appearance.themePreset.bubbleAccentB
        ) {
            when {
                user == null && bannedNotice != null -> {
                    AccountSuspendedScreen(
                        reason = bannedNotice?.reason,
                        onTryDifferentAccount = { bannedNotice = null }
                    )
                }
                liveConfig.maintenanceMode && bannedNotice == null -> {
                    MaintenanceModeScreen(
                        message = liveConfig.maintenanceMessage,
                        title = liveConfig.maintenanceTitle,
                        eta = liveConfig.maintenanceEta,
                        adminPackageLabel = adminPackageId
                    )
                }
                showForceUpdateDialog -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .testTag("old_version_blocked_gate")
                    )
                }
                user == null -> {
                    AuthScreen(onAuthSuccess = { /* AuthStateFlow triggers transition */ })
                }
                else -> {
                    key(user.uid) {
                        AuthenticatedDuetHost(
                            currentUserId = user.uid,
                            initialDisplayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Sweetheart",
                            appearance = appearance,
                            liveAppConfig = liveConfig,
                            isAdminAppInstalled = isAdminAppInstalled,
                            onOpenUpdateDetails = { manualOpenUpdatePopup = true },
                            onOpenAdminCompanion = openAdminCompanion,
                            requestedTabRoute = requestedTabRoute,
                            onRequestedTabConsumed = onRequestedTabConsumed,
                            onAccountBanChanged = { notice -> bannedNotice = notice }
                        )
                    }
                }
            }
        }

        // Stacked Top Overlays: Connectivity Prompt + Scheduled Maintenance + Announcement + Update Banner
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OfflineConnectivityPrompt(isOnline = rootIsOnline)
            UpcomingMaintenanceBanner(
                visible = showUpcomingMaintenanceBanner,
                message = liveConfig.upcomingMaintenanceText,
                onDismiss = { dismissedUpcomingMaintSig = currentUpcomingSig }
            )
            TopAnnouncementBanner(
                visible = showAnnouncementBanner,
                bannerText = liveConfig.bannerText,
                bannerLink = liveConfig.bannerLink,
                onOpenLink = openExternalWebUrl,
                onDismiss = {
                    dismissedBannerSignature = currentBannerSignature
                    notificationStateStore.setDismissedBannerSignature(currentBannerSignature)
                },
                bannerTitle = liveConfig.bannerTitle,
                bannerType = liveConfig.bannerType
            )
            TopPromoBanner(
                visible = showPromoBanner,
                promoTitle = liveConfig.promoTitle,
                promoText = liveConfig.promoText,
                promoBadge = liveConfig.promoBadge,
                promoCtaText = liveConfig.promoCtaText,
                promoLink = liveConfig.promoLink,
                promoColor = liveConfig.promoColor,
                onOpenLink = openExternalWebUrl,
                onDismiss = {
                    dismissedPromoSignature = currentPromoSignature
                    notificationStateStore.setDismissedPromoSignature(currentPromoSignature)
                }
            )
            TopUpdateAvailableBanner(
                visible = showOptionalUpdateBanner,
                versionLabel = liveConfig.notificationUpdateVersion(BuildConfig.VERSION_NAME),
                title = liveConfig.updateTitle,
                summary = liveConfig.updateMessage,
                onOpenPopup = { manualOpenUpdatePopup = true },
                onDismiss = {
                    dismissedUpdateSignature = currentUpdateSignature
                    notificationStateStore.setDismissedUpdateSignature(currentUpdateSignature)
                }
            )
        }

        if (showForceUpdateDialog) {
            ForceUpdateDialog(
                currentVersion = BuildConfig.VERSION_NAME,
                minVersion = liveConfig.effectiveMinVersionLabel(),
                updateMessage = liveConfig.updateMessage,
                updateLink = liveConfig.effectiveUpdateLink(context.packageName),
                onOpenUpdateLink = openExternalWebUrl,
                releaseTitle = liveConfig.updateTitle
            )
        } else if (showOptionalUpdatePopup) {
            OptionalUpdateDialog(
                currentVersion = BuildConfig.VERSION_NAME,
                newVersion = liveConfig.notificationUpdateVersion(BuildConfig.VERSION_NAME),
                title = liveConfig.updateTitle,
                releaseNotes = liveConfig.updateMessage,
                updateLink = liveConfig.effectiveUpdateLink(context.packageName),
                onOpenUpdateLink = openExternalWebUrl,
                onDismissLater = {
                    manualOpenUpdatePopup = false
                    dismissedUpdateSignature = currentUpdateSignature
                    notificationStateStore.setDismissedUpdateSignature(currentUpdateSignature)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedDuetHost(
    currentUserId: String,
    initialDisplayName: String,
    appearance: DuetAppearanceState,
    liveAppConfig: LiveAppConfig,
    isAdminAppInstalled: Boolean,
    onOpenUpdateDetails: () -> Unit,
    onOpenAdminCompanion: () -> Unit,
    requestedTabRoute: String?,
    onRequestedTabConsumed: () -> Unit,
    onAccountBanChanged: (BannedAccountNotice?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DuetViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DuetViewModel(
                    repository = DuetRepository(db),
                    crypto = DuetCryptoManager(app, currentUserId),
                    telemetryCollector = DeviceTelemetryCollector(app),
                    widgetStore = WidgetStateStore(app),
                    appContext = app,
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName
                )
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setAppForeground(true)
                Lifecycle.Event.ON_STOP -> viewModel.setAppForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.refreshPermissionsState()
            if (granted) {
                viewModel.showBanner("Partner notifications & inline reply enabled")
                if (state.backgroundSyncEnabled) {
                    DuetSyncService.start(context)
                }
            } else {
                viewModel.showBanner("You can enable notifications anytime in Settings")
            }
        }
    )

    var promptedNotificationsOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.stage, state.hasNotificationPermission) {
        if (state.stage == SessionStage.Ready && !state.hasNotificationPermission && !promptedNotificationsOnce) {
            promptedNotificationsOnce = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(state.bannerMessage) {
        val msg = state.bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    val requestNotificationPerm: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    val openUsageAccess: () -> Unit = {
        AppUsageMonitor.openUsageAccessSettings(context)
    }

    val openBatterySettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            viewModel.showBanner("Open Android Settings -> Apps -> Duet -> Battery -> Unrestricted")
        }
    }

    val performSignOut: () -> Unit = {
        DuetSyncService.stop(context)
        signOutGoogleAccount(
            credentialManager = credentialManager,
            scope = scope,
            onBeforeSignOut = { viewModel.setAppForeground(false) }
        )
    }

    val activeBan = state.bannedAccount
    LaunchedEffect(activeBan) {
        onAccountBanChanged(activeBan)
    }
    DisposableEffect(Unit) {
        onDispose {
            onAccountBanChanged(null)
        }
    }

    if (activeBan != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            AccountSuspendedScreen(
                reason = activeBan.reason,
                isRefreshing = state.busyAction,
                onRefreshStatus = { viewModel.refreshSuspendedAccountStatus() },
                onTryDifferentAccount = {
                    onAccountBanChanged(null)
                    performSignOut()
                }
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
            )
        }
        return
    }

    when (val stage = state.stage) {
        SessionStage.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                    CircularProgressIndicator(strokeWidth = 2.5.dp)
                    Text(
                        text = "Opening your Duet…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is SessionStage.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Connection interrupted",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stage.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = performSignOut,
                        modifier = Modifier
                            .glossyPressEffect(cornerRadius = 20.dp)
                            .testTag("error_sign_out_button")
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }

        SessionStage.NeedsPairing -> {
            PairingScreen(
                defaultNickname = state.profile?.displayName ?: initialDisplayName,
                isBusy = state.busyAction,
                bannerMessage = state.bannerMessage,
                initialJoinTab = state.preferEnterCodeTabOnPairing,
                onCreateSpace = { nickname -> viewModel.createCoupleSpace(nickname) },
                onJoinSpace = { code, nickname -> viewModel.joinCoupleByCode(code, nickname) },
                onSignOut = performSignOut
            )
        }

        SessionStage.Ready -> {
            MainDuetScaffold(
                state = state,
                appearance = appearance,
                liveAppConfig = liveAppConfig,
                isAdminAppInstalled = isAdminAppInstalled,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                requestedTabRoute = requestedTabRoute,
                onRequestedTabConsumed = onRequestedTabConsumed,
                onRequestNotificationPermission = requestNotificationPerm,
                onOpenUsageAccessSettings = openUsageAccess,
                onOpenBatterySettings = openBatterySettings,
                onOpenUpdateDetails = onOpenUpdateDetails,
                onOpenAdminCompanion = onOpenAdminCompanion,
                onSignOut = performSignOut
            )
        }
    }
}

@Composable
private fun MainDuetScaffold(
    state: DuetUiState,
    appearance: DuetAppearanceState,
    liveAppConfig: LiveAppConfig,
    isAdminAppInstalled: Boolean,
    viewModel: DuetViewModel,
    snackbarHostState: SnackbarHostState,
    requestedTabRoute: String?,
    onRequestedTabConsumed: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenUpdateDetails: () -> Unit,
    onOpenAdminCompanion: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val couple = state.couple ?: return
    val myUid = state.currentUserId
    val isPaired = couple.status == "paired"

    var selectedTab by rememberSaveable { mutableStateOf(DuetTab.HOME) }
    var inspectAccountSelf by remember { mutableStateOf<Boolean?>(null) }
    var showWidgetStudio by remember { mutableStateOf(false) }
    var showAppearanceStudio by remember { mutableStateOf(false) }
    var showEncryptionSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showChatSettingsSheet by remember { mutableStateOf(false) }
    var showMoodHistory by remember { mutableStateOf(false) }
    var showBucketList by remember { mutableStateOf(false) }
    var showDateSpinner by remember { mutableStateOf(false) }
    var showLoveGames by remember { mutableStateOf(false) }
    var showLoveLetters by remember { mutableStateOf(false) }
    var showLoveJar by remember { mutableStateOf(false) }
    var showSongVibe by remember { mutableStateOf(false) }
    var showCareCheckIn by remember { mutableStateOf(false) }
    var showLoveDoodle by remember { mutableStateOf(false) }
    var showCouplePromises by remember { mutableStateOf(false) }
    var showRemovePartnerSheet by remember { mutableStateOf(false) }
    var deviceDetailsShowSelf by remember { mutableStateOf<Boolean?>(null) }
    val resolvedPartnerName = state.resolvedPartnerName()

    // Trigger counters so the Apple Photos floating action orb triggers native tab actions cleanly
    var uploadMomentTrigger by remember { mutableIntStateOf(0) }
    var addDateTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(requestedTabRoute) {
        if (!requestedTabRoute.isNullOrBlank()) {
            val matched = DuetTab.entries.firstOrNull { it.route == requestedTabRoute }
            if (matched != null) {
                selectedTab = matched
            }
            onRequestedTabConsumed()
        }
    }

    LaunchedEffect(
        liveAppConfig.moodSyncEnabled,
        liveAppConfig.sharedJournalEnabled,
        liveAppConfig.datePlannerEnabled
    ) {
        if (!liveAppConfig.moodSyncEnabled) showMoodHistory = false
        if (!liveAppConfig.sharedJournalEnabled) {
            showLoveLetters = false
            showLoveJar = false
        }
        if (!liveAppConfig.datePlannerEnabled) showDateSpinner = false
    }

    DisposableEffect(selectedTab) {
        DuetNotificationStateStore.isChatTabVisible = (selectedTab == DuetTab.CHAT)
        onDispose {
            DuetNotificationStateStore.isChatTabVisible = false
        }
    }

    BackHandler(enabled = selectedTab != DuetTab.HOME) {
        selectedTab = DuetTab.HOME
    }

    val copyCode: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Duet Invite Code", couple.inviteCode))
        viewModel.showBanner("Invite code ${couple.inviteCode} copied")
    }

    val shareCode: () -> Unit = {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Join me in our private Duet space! Enter my 6-digit invite code: ${couple.inviteCode}"
            )
        }
        context.startActivity(Intent.createChooser(intent, "Share Duet 6-Digit Code"))
    }

    // Active Modal Bottom Sheets
    inspectAccountSelf?.let { isSelf ->
        PartnerStatusSheet(
            accountName = if (isSelf) couple.myDisplayName(myUid) else resolvedPartnerName,
            isSelf = isSelf,
            isPaired = isPaired,
            inviteCode = couple.inviteCode,
            presence = if (isSelf) state.myPresence else state.partnerPresence,
            partnerBanNotice = if (isSelf) null else state.partnerBanned,
            localDeviceSpecs = if (isSelf) state.myDeviceSpecs else null,
            hasUsageAccessPermission = state.hasUsageAccessPermission,
            onDismiss = { inspectAccountSelf = null },
            onSendLovePulse = { viewModel.sendLovePulse() },
            onRemindCharge = {
                viewModel.sendLovePulseMessage("Your battery is getting low — plug in your phone, sweetheart!")
            },
            onAutoRefresh = { viewModel.refreshMyTelemetrySilently() },
            onShareCode = {
                inspectAccountSelf = null
                shareCode()
            },
            onOpenUsageAccessSettings = onOpenUsageAccessSettings,
            onOpenDeviceDetails = { showSelf -> deviceDetailsShowSelf = showSelf },
            onOpenRemovePartner = { showRemovePartnerSheet = true }
        )
    }

    deviceDetailsShowSelf?.let { initialSelf ->
        DeviceDetailsSheet(
            myName = couple.myDisplayName(myUid),
            partnerName = resolvedPartnerName,
            isPaired = isPaired,
            initialShowSelf = initialSelf,
            myDeviceSpecs = state.myDeviceSpecs,
            partnerPresence = state.partnerPresence,
            shareDeviceStatus = state.profile?.shareDeviceStatus ?: true,
            onRefreshMine = { viewModel.refreshMyTelemetryNow() },
            onFeedback = { msg -> viewModel.showBanner(msg) },
            onDismiss = { deviceDetailsShowSelf = null }
        )
    }

    if (showAppearanceStudio) {
        AppearanceStudioSheet(
            appearance = appearance,
            onFeedback = { msg -> viewModel.showBanner(msg) },
            onDismiss = { showAppearanceStudio = false }
        )
    }

    if (showWidgetStudio) {
        val previewPresence = state.partnerPresence ?: state.myPresence
        val mood = MoodCatalog.byKey(previewPresence?.moodKey)
        val who = if (state.partnerPresence != null) resolvedPartnerName else "You"
        val batteryBadge = when {
            previewPresence == null -> "Live"
            !previewPresence.statusShared || previewPresence.batteryPercent < 0 -> "Private"
            previewPresence.charging -> "${previewPresence.batteryPercent}% ⚡"
            else -> "${previewPresence.batteryPercent}%"
        }
        val countdownLine = couple.anniversaryMonth?.let { m ->
            couple.anniversaryDay?.let { d ->
                val c = CountdownCalculator.computeYearlyCountdown(m, d)
                if (c.isToday) "Happy Anniversary Today!" else "${c.days}d until Anniversary"
            }
        } ?: "Set your anniversary & birthday"
        val latestMsg = state.messages.lastOrNull()
        val latestMsgPreview = when {
            latestMsg == null -> "Tap below to send a private message"
            latestMsg.raw.messageType == "IMAGE" && latestMsg.text.isBlank() -> "📷 Encrypted photo"
            else -> "${if (latestMsg.raw.senderUid == myUid) "You" else resolvedPartnerName}: ${latestMsg.text}"
        }

        WidgetStudioSheet(
            coupleTitle = if (isPaired) "${couple.myDisplayName(myUid)} & $resolvedPartnerName" else "Duet • ${couple.inviteCode}",
            partnerName = resolvedPartnerName,
            batteryBadge = batteryBadge,
            moodLine = "${mood.emoji} $who: ${mood.label}",
            countdownLine = countdownLine,
            statusFooter = if (previewPresence != null) {
                "$who: ${previewPresence.liveActivityLabel()}"
            } else {
                "End-to-end encrypted for two"
            },
            lastMsgPreview = latestMsgPreview,
            selectedTheme = state.widgetTheme,
            onSelectTheme = { viewModel.selectWidgetTheme(it) },
            onPinResult = { name, pinned ->
                if (pinned) {
                    viewModel.showBanner("Confirm on your home screen to place $name")
                } else {
                    viewModel.showBanner("Long-press home screen -> Widgets -> Duet -> $name")
                }
            },
            onDismiss = { showWidgetStudio = false }
        )
    }

    if (showEncryptionSheet) {
        EncryptionInfoSheet(
            isPaired = isPaired,
            safetyNumber = state.safetyNumber,
            myFingerprint = state.myKeyFingerprint,
            partnerFingerprint = state.partnerKeyFingerprint,
            onDismiss = { showEncryptionSheet = false }
        )
    }

    if (showSettingsSheet) {
        DuetSettingsSheet(
            myNickname = couple.myDisplayName(myUid),
            partnerName = resolvedPartnerName,
            inviteCode = couple.inviteCode,
            isPaired = isPaired,
            shareDeviceStatus = state.profile?.shareDeviceStatus ?: true,
            backgroundSyncEnabled = state.backgroundSyncEnabled,
            hasNotificationPermission = state.hasNotificationPermission,
            hasUsageAccessPermission = state.hasUsageAccessPermission,
            myDeviceSpecs = state.myDeviceSpecs,
            partnerPresence = state.partnerPresence,
            liveAppConfig = liveAppConfig,
            isAdminAppInstalled = isAdminAppInstalled,
            onUpdateNickname = { viewModel.updateMyNickname(it) },
            onToggleShareStatus = { viewModel.setShareDeviceStatus(it) },
            onToggleBackgroundSync = { viewModel.setBackgroundSyncEnabled(it) },
            onRequestNotificationPermission = onRequestNotificationPermission,
            onSendTestNotification = { viewModel.sendTestNotification() },
            onOpenUsageAccessSettings = onOpenUsageAccessSettings,
            onOpenBatterySettings = onOpenBatterySettings,
            onCopyCode = copyCode,
            onShareCode = shareCode,
            onOpenAppearanceStudio = { showAppearanceStudio = true },
            onOpenWidgetStudio = { showWidgetStudio = true },
            onOpenDeviceDetails = { showSelf -> deviceDetailsShowSelf = showSelf },
            onOpenUpdateDetails = onOpenUpdateDetails,
            onOpenAdminCompanion = onOpenAdminCompanion,
            onOpenEncryptionInfo = { showEncryptionSheet = true },
            onOpenRemovePartner = { showRemovePartnerSheet = true },
            onSignOut = onSignOut,
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showChatSettingsSheet) {
        ChatSettingsSheet(
            partnerOriginalName = couple.partnerDisplayName(myUid),
            currentPartnerNickname = state.profile?.partnerNickname.orEmpty(),
            appearance = appearance,
            onSavePartnerNickname = { viewModel.updatePartnerNickname(it) },
            onOpenEncryptionInfo = { showEncryptionSheet = true },
            onOpenAppearanceStudio = { showAppearanceStudio = true },
            onOpenRemovePartner = { showRemovePartnerSheet = true },
            onFeedback = { viewModel.showBanner(it) },
            onDismiss = { showChatSettingsSheet = false }
        )
    }

    if (showRemovePartnerSheet) {
        RemovePartnerWarningSheet(
            partnerName = resolvedPartnerName,
            isPaired = isPaired,
            inviteCode = couple.inviteCode,
            safetyNumber = state.safetyNumber,
            isBusy = state.busyAction,
            onConfirmRemove = { enteredSafetyNumber ->
                viewModel.removePartnerWithSafetyNumber(enteredSafetyNumber) {
                    showRemovePartnerSheet = false
                }
            },
            onDismiss = {
                if (!state.busyAction) {
                    showRemovePartnerSheet = false
                }
            }
        )
    }

    if (showMoodHistory) {
        MoodHistorySheet(
            moods = state.moods,
            myUid = myUid,
            partnerName = resolvedPartnerName,
            onSendComfortHug = { moodId -> viewModel.sendComfortHug(moodId) },
            onDismiss = { showMoodHistory = false }
        )
    }

    if (showBucketList) {
        BucketListSheet(
            items = state.bucketItems,
            partnerName = resolvedPartnerName,
            isBusy = state.busyAction,
            onAddItem = { title, cat -> viewModel.addBucketItem(title, cat) },
            onToggleCompleted = { item -> viewModel.toggleBucketItem(item) },
            onDeleteItem = { id -> viewModel.deleteBucketItem(id) },
            onDismiss = { showBucketList = false }
        )
    }

    if (showDateSpinner) {
        DateSpinnerSheet(
            partnerName = resolvedPartnerName,
            onSendInviteToChat = { inviteMsg ->
                viewModel.sendTextMessage(inviteMsg)
                selectedTab = DuetTab.CHAT
                viewModel.showBanner("Date invite sent to Private Chat!")
            },
            onDismiss = { showDateSpinner = false }
        )
    }

    if (showLoveGames) {
        LoveGamesSheet(
            partnerName = resolvedPartnerName,
            onSendToPrivateChat = { msg ->
                viewModel.sendTextMessage(msg)
                selectedTab = DuetTab.CHAT
                viewModel.showBanner("Sent to Private Chat!")
            },
            onDismiss = { showLoveGames = false }
        )
    }

    if (showLoveLetters) {
        LoveLettersSheet(
            letters = state.loveLetters,
            myUid = myUid,
            partnerName = resolvedPartnerName,
            isBusy = state.busyAction,
            onSendLetter = { title, body, seal, unlockAt ->
                viewModel.sendLoveLetter(title, body, seal, unlockAt)
            },
            onOpenLetter = { letter -> viewModel.openLoveLetter(letter) },
            onDeleteLetter = { id -> viewModel.deleteLoveLetter(id) },
            onDismiss = { showLoveLetters = false }
        )
    }

    if (showLoveJar) {
        LoveJarSheet(
            notes = state.loveJarNotes,
            myUid = myUid,
            partnerName = resolvedPartnerName,
            isBusy = state.busyAction,
            onAddNote = { kind, text -> viewModel.addLoveJarNote(kind, text) },
            onToggleGranted = { note -> viewModel.toggleLoveJarGranted(note) },
            onDeleteNote = { id -> viewModel.deleteLoveJarNote(id) },
            onDismiss = { showLoveJar = false }
        )
    }

    if (showSongVibe) {
        SongVibeDedicationSheet(
            dedications = state.vibeDedications,
            myUid = myUid,
            partnerName = resolvedPartnerName,
            isBusy = state.busyAction,
            onDedicateSong = { song, artist, emoji, note ->
                viewModel.sendVibeDedication(song, artist, emoji, note)
            },
            onToggleLoved = { vibe -> viewModel.toggleVibeLoved(vibe) },
            onDeleteDedication = { id -> viewModel.deleteVibeDedication(id) },
            onDismiss = { showSongVibe = false }
        )
    }

    if (showCareCheckIn) {
        CareCheckInSheet(
            checkIns = state.careCheckIns,
            myUid = myUid,
            partnerName = resolvedPartnerName,
            isBusy = state.busyAction,
            onSendCheckIn = { type, msg -> viewModel.sendCareCheckIn(type, msg) },
            onAcknowledge = { id -> viewModel.acknowledgeCareCheckIn(id) },
            onDismiss = { showCareCheckIn = false }
        )
    }

    if (showLoveDoodle) {
        LoveDoodleSheet(
            doodles = state.loveDoodles,
            myUid = myUid,
            partnerName = resolvedPartnerName,
            isBusy = state.busyAction,
            onSendDoodle = { title, b64 -> viewModel.sendLoveDoodle(title, b64) },
            onToggleLoved = { d -> viewModel.toggleDoodleLoved(d) },
            onDeleteDoodle = { id -> viewModel.deleteLoveDoodle(id) },
            onFeedback = { viewModel.showBanner(it) },
            onDismiss = { showLoveDoodle = false }
        )
    }

    if (showCouplePromises) {
        val bondScore = (couple.lovePulseCount.toInt() * 3) +
            (state.messages.size * 2) +
            (state.moods.size * 4) +
            (state.loveLetters.size * 10) +
            (state.loveJarNotes.size * 6) +
            (state.couplePromises.count { it.isFullyCoSigned() } * 15) +
            (state.moments.size * 8)

        CouplePromisesSheet(
            promises = state.couplePromises,
            myUid = myUid,
            partnerName = resolvedPartnerName,
            bondScorePoints = bondScore,
            isBusy = state.busyAction,
            onAddPromise = { title, cat -> viewModel.addCouplePromise(title, cat) },
            onCoSignPromise = { p -> viewModel.coSignPromise(p) },
            onDeletePromise = { id -> viewModel.deleteCouplePromise(id) },
            onShareQuizResultToChat = { summary ->
                viewModel.sendTextMessage(summary)
                showCouplePromises = false
                selectedTab = DuetTab.CHAT
                viewModel.showBanner("Quiz answers shared in Private Chat! 💘")
            },
            onDismiss = { showCouplePromises = false }
        )
    }

    val navItems = remember {
        DuetTab.entries.map { tab ->
            FloatingNavItem(
                route = tab.route,
                label = tab.label,
                selectedIcon = tab.selectedIcon,
                unselectedIcon = tab.unselectedIcon
            )
        }
    }

    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavRail = maxWidth >= 680.dp
        val showFloatingNav = !useNavRail && !(selectedTab == DuetTab.CHAT && isImeVisible)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            if (useNavRail) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = if (appearance.themePreset.isDark) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    } else {
                        Color.White.copy(alpha = 0.82f)
                    }
                ) {
                    Spacer(Modifier.height(16.dp))
                    DuetTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier
                                .glossyPressEffect(cornerRadius = 18.dp)
                                .testTag("rail_tab_${tab.route}")
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val movingForward = targetState.ordinal >= initialState.ordinal
                        val enter = slideInHorizontally(
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> if (movingForward) fullWidth / 4 else -fullWidth / 4 }
                        ) + fadeIn(tween(240)) + scaleIn(
                            initialScale = 0.965f,
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        )
                        val exit = slideOutHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> if (movingForward) -fullWidth / 5 else fullWidth / 5 }
                        ) + fadeOut(tween(180)) + scaleOut(
                            targetScale = 0.98f,
                            animationSpec = tween(220)
                        )
                        enter togetherWith exit
                    },
                    label = "duet_tab_transition"
                ) { tab ->
                    when (tab) {
                        DuetTab.HOME -> HomeScreen(
                            state = state,
                            appearance = appearance,
                            liveAppConfig = liveAppConfig,
                            onTapPartnerAccount = { inspectAccountSelf = false },
                            onTapMyAccount = { inspectAccountSelf = true },
                            onShareMood = { mood, note -> viewModel.shareMood(mood, note) },
                            onSendComfortHug = { moodId -> viewModel.sendComfortHug(moodId) },
                            onSendLovePulse = { viewModel.sendLovePulse() },
                            onSubmitDailyAnswer = { answer -> viewModel.submitDailyPromptAnswer(answer) },
                            onOpenWidgetStudio = { showWidgetStudio = true },
                            onOpenAppearanceStudio = { showAppearanceStudio = true },
                            onOpenDatesTab = { selectedTab = DuetTab.DATES },
                            onOpenSettings = { showSettingsSheet = true },
                            onCopyInviteCode = copyCode,
                            onShareInviteCode = shareCode,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                            onOpenMoodHistory = { showMoodHistory = true },
                            onOpenBucketList = { showBucketList = true },
                            onOpenDateSpinner = { showDateSpinner = true },
                            onOpenLoveGames = { showLoveGames = true },
                            onOpenLoveLetters = { showLoveLetters = true },
                            onOpenLoveJar = { showLoveJar = true },
                            onOpenSongVibe = { showSongVibe = true },
                            onOpenCareCheckIn = { showCareCheckIn = true },
                            onOpenLoveDoodle = { showLoveDoodle = true },
                            onOpenCouplePromises = { showCouplePromises = true },
                            onOpenRemovePartner = { showRemovePartnerSheet = true }
                        )

                        DuetTab.CHAT -> ChatScreen(
                            state = state,
                            appearance = appearance,
                            extraBottomNavPadding = if (showFloatingNav) 96.dp else 0.dp,
                            onSendText = { viewModel.sendTextMessage(it) },
                            onSendPhoto = { uri, staged, cap -> viewModel.sendPhotoMessage(uri, staged, cap) },
                            onSendHeartPulse = { viewModel.sendLovePulseMessage() },
                            onReactMessage = { msgId, current, react ->
                                viewModel.reactToMessage(msgId, current, react)
                            },
                            onDeleteMessage = { msgId -> viewModel.deleteMessage(msgId) },
                            onTypingChanged = { typing -> viewModel.onChatTypingChanged(typing) },
                            onMarkMessagesSeen = { msgs -> viewModel.markIncomingMessagesSeen(msgs) },
                            onOpenEncryptionInfo = { showEncryptionSheet = true },
                            onOpenChatSettings = { showChatSettingsSheet = true },
                            onTapPartnerHeader = { inspectAccountSelf = false },
                            onFeedback = { viewModel.showBanner(it) }
                        )

                        DuetTab.MOMENTS -> MomentsScreen(
                            state = state,
                            externalUploadTrigger = uploadMomentTrigger,
                            showInternalFab = useNavRail,
                            featureEnabled = liveAppConfig.sharedJournalEnabled,
                            onUploadMoment = { uri, staged, cap, loc -> viewModel.uploadMoment(uri, staged, cap, loc) },
                            onToggleLove = { viewModel.toggleMomentLove(it) },
                            onDeleteMoment = { viewModel.deleteMoment(it) }
                        )

                        DuetTab.DATES -> DatesScreen(
                            state = state,
                            externalAddEventTrigger = addDateTrigger,
                            showInternalFab = useNavRail,
                            featureEnabled = liveAppConfig.datePlannerEnabled,
                            onUpdateAnniversary = { y, m, d -> viewModel.updateAnniversary(y, m, d) },
                            onUpdateBirthday = { forPartner, m, d -> viewModel.updateBirthday(forPartner, m, d) },
                            onAddSpecialEvent = { title, cat, y, m, d, rep ->
                                viewModel.addSpecialEvent(title, cat, y, m, d, rep)
                            },
                            onDeleteSpecialEvent = { viewModel.deleteSpecialEvent(it) }
                        )
                    }
                }
            }
        }

        // Snackbar positioned gracefully above the floating Apple Photos nav bar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (showFloatingNav) 86.dp else 16.dp, start = 16.dp, end = 16.dp)
        )

        // Apple Photos-Style Floating Glass Navigation Pill + Contextual Action Orb
        if (!useNavRail) {
            val (orbIcon, orbDesc, orbTag, orbAction) = when (selectedTab) {
                DuetTab.HOME -> QuadAction(
                    Icons.Rounded.Favorite,
                    "Send quick love pulse",
                    "floating_orb_pulse"
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.sendLovePulse()
                }
                DuetTab.CHAT -> QuadAction(
                    Icons.Rounded.Palette,
                    "Open theme and icon studio",
                    "floating_orb_theme"
                ) {
                    showAppearanceStudio = true
                }
                DuetTab.MOMENTS -> QuadAction(
                    Icons.Rounded.AddPhotoAlternate,
                    "Upload encrypted photo",
                    "moments_upload_fab"
                ) {
                    if (liveAppConfig.sharedJournalEnabled) {
                        uploadMomentTrigger++
                    } else {
                        viewModel.showBanner("Shared Journal is currently paused by Bondly Admin")
                    }
                }
                DuetTab.DATES -> QuadAction(
                    Icons.Rounded.Add,
                    "Add special countdown",
                    "add_special_date_fab"
                ) {
                    if (liveAppConfig.datePlannerEnabled) {
                        addDateTrigger++
                    } else {
                        viewModel.showBanner("Date Planner is currently paused by Bondly Admin")
                    }
                }
            }

            AnimatedVisibility(
                visible = showFloatingNav,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AppleStyleFloatingNavBar(
                    items = navItems,
                    selectedIndex = DuetTab.entries.indexOf(selectedTab),
                    onSelect = { idx ->
                        selectedTab = DuetTab.entries[idx]
                    },
                    actionIcon = orbIcon,
                    actionContentDescription = orbDesc,
                    actionTestTag = orbTag,
                    onActionClick = orbAction
                )
            }
        }
    }
}

private data class QuadAction(
    val icon: ImageVector,
    val description: String,
    val tag: String,
    val action: () -> Unit
)
