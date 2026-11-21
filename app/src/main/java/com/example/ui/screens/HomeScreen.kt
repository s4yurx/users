package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CountdownCalculator
import com.example.data.DailyPromptDeck
import com.example.data.LiveAppConfig
import com.example.data.MoodCatalog
import com.example.data.MoodEntry
import com.example.data.MoodOption
import com.example.data.PartnerPresence
import com.example.ui.DuetUiState
import com.example.ui.components.CuteWidgetPreviewCard
import com.example.ui.components.DuetBrandLogoBadge
import com.example.ui.components.FeatureDisabledCard
import com.example.ui.components.exclusiveAnimatedBorder
import com.example.ui.components.glossyPressEffect
import com.example.ui.theme.BatteryHealthyGreen
import com.example.ui.theme.BatteryLowCrimson
import com.example.ui.theme.DuetAppearanceState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDuetThemePreset
import com.example.ui.theme.StatusLiveCoral
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    state: DuetUiState,
    appearance: DuetAppearanceState = DuetAppearanceState(),
    liveAppConfig: LiveAppConfig = LiveAppConfig(),
    onTapPartnerAccount: () -> Unit,
    onTapMyAccount: () -> Unit,
    onShareMood: (MoodOption, String) -> Unit,
    onSendComfortHug: (String) -> Unit,
    onSendLovePulse: () -> Unit,
    onSubmitDailyAnswer: (String) -> Unit,
    onOpenWidgetStudio: () -> Unit,
    onOpenAppearanceStudio: () -> Unit,
    onOpenDatesTab: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyInviteCode: () -> Unit,
    onShareInviteCode: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenMoodHistory: () -> Unit = {},
    onOpenBucketList: () -> Unit = {},
    onOpenDateSpinner: () -> Unit = {},
    onOpenLoveGames: () -> Unit = {},
    onOpenLoveLetters: () -> Unit = {},
    onOpenLoveJar: () -> Unit = {},
    onOpenSongVibe: () -> Unit = {},
    onOpenCareCheckIn: () -> Unit = {},
    onOpenLoveDoodle: () -> Unit = {},
    onOpenCouplePromises: () -> Unit = {},
    onOpenRemovePartner: () -> Unit = {}
) {
    val couple = state.couple ?: return
    val myUid = state.currentUserId
    val isPaired = couple.status == "paired"
    val myName = couple.myDisplayName(myUid)
    val partnerName = state.resolvedPartnerName()
    val haptics = LocalHapticFeedback.current

    var selectedMoodKey by rememberSaveable { mutableStateOf(state.myPresence?.moodKey ?: "IN_LOVE") }
    var moodNoteDraft by rememberSaveable { mutableStateOf("") }
    var promptDraft by rememberSaveable { mutableStateOf("") }
    var pulseAnimating by remember { mutableStateOf(false) }
    var dismissedSetupCard by rememberSaveable { mutableStateOf(false) }

    val pulseScale by animateFloatAsState(
        targetValue = if (pulseAnimating) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pulse_scale"
    )
    LaunchedEffect(pulseAnimating) {
        if (pulseAnimating) {
            delay(260L)
            pulseAnimating = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val activeTheme = LocalDuetThemePreset.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Editorial Header with Custom App Logo Badge, Theme Studio & Settings
            item {
                val customTitle = appearance.customBrandTitle.trim()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DuetBrandLogoBadge(
                            appearance = appearance,
                            size = 44.dp,
                            onClick = onOpenAppearanceStudio,
                            modifier = Modifier.testTag("home_brand_logo_badge")
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    customTitle.isNotBlank() -> customTitle
                                    isPaired -> "$myName & $partnerName"
                                    else -> "Your Duet Space"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isPaired) "End-to-end encrypted • ${activeTheme.label}"
                                else "Share your 6-digit code to pair",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val compactPillBg = if (activeTheme.isDark) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
                        } else {
                            Color.White.copy(alpha = 0.78f)
                        }
                        Surface(
                            shape = CircleShape,
                            color = compactPillBg,
                            modifier = Modifier
                                .size(38.dp)
                                .glossyPressEffect(cornerRadius = 19.dp)
                                .clip(CircleShape)
                                .exclusiveAnimatedBorder(
                                    cornerRadius = 19.dp,
                                    strokeWidth = 1.3.dp,
                                    fallbackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                    fallbackWidth = 0.8.dp
                                )
                                .clickable(onClick = onOpenAppearanceStudio)
                                .testTag("home_appearance_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = "Themes, app logo and icon studio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = compactPillBg,
                            modifier = Modifier
                                .size(38.dp)
                                .glossyPressEffect(cornerRadius = 19.dp)
                                .clip(CircleShape)
                                .exclusiveAnimatedBorder(
                                    cornerRadius = 19.dp,
                                    strokeWidth = 1.3.dp,
                                    fallbackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                    fallbackWidth = 0.8.dp
                                )
                                .clickable(onClick = onOpenSettings)
                                .testTag("home_settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Open Duet settings",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1b. Dynamic Key-Value Remote Notice (from Bondly Admin Dynamic Config)
            val dynamicHomeNotice = liveAppConfig.dynamicHomeNotice()
            if (dynamicHomeNotice.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .testTag("home_dynamic_notice_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Admin note",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = dynamicHomeNotice,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // 2. Waiting-for-Partner 6-Digit Code Banner
            if (!isPaired) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .exclusiveAnimatedBorder(cornerRadius = 24.dp, strokeWidth = 1.5.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Partner 6-Digit Invite Code",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = couple.inviteCode.chunked(3).joinToString(" "),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("invite_code_display")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onCopyInviteCode,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .glossyPressEffect(cornerRadius = 14.dp)
                                        .testTag("home_copy_code_button")
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy")
                                }
                                FilledTonalButton(
                                    onClick = onShareInviteCode,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .glossyPressEffect(cornerRadius = 14.dp)
                                        .testTag("home_share_code_button")
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }

            // 2b. Compact Setup Banner for Background Notifications & Active App Sharing (Instagram, WhatsApp, etc.)
            if (!dismissedSetupCard && (!state.hasNotificationPermission || !state.hasUsageAccessPermission)) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Complete Live Partner Setup",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                IconButton(
                                    onClick = { dismissedSetupCard = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss setup card",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (!state.hasNotificationPermission) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsActive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Alerts & background inline reply",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = onRequestNotificationPermission,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .glossyPressEffect(cornerRadius = 12.dp)
                                            .testTag("home_enable_notifications_button")
                                    ) {
                                        Text("Allow")
                                    }
                                }
                            }
                            if (!state.hasUsageAccessPermission) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Apps,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Show live open app (Instagram, WhatsApp…)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = onOpenUsageAccessSettings,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .glossyPressEffect(cornerRadius = 12.dp)
                                            .testTag("home_enable_usage_access_button")
                                    ) {
                                        Text("Enable")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Signature Partner & You Account Presence Cards (Tap Partner for Battery, Network & Lock Status!)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Live Presence",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccountPresenceCard(
                            name = partnerName,
                            subtitle = if (isPaired) "Tap for battery & status" else "Tap to invite",
                            presence = state.partnerPresence,
                            isPairedSlot = isPaired,
                            isPartnerCard = true,
                            isBannedAccount = state.partnerBanned != null,
                            onClick = onTapPartnerAccount,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("partner_account_card")
                        )
                        AccountPresenceCard(
                            name = "$myName (You)",
                            subtitle = "Your live telemetry",
                            presence = state.myPresence,
                            isPairedSlot = true,
                            isPartnerCard = false,
                            isBannedAccount = false,
                            onClick = onTapMyAccount,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("my_account_card")
                        )
                    }
                }
            }

            // 3b. Partner Account Banned by Admin Notice Banner (with isolated wipe option)
            val partnerBan = state.partnerBanned
            if (isPaired && partnerBan != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        border = BorderStroke(
                            width = 1.2.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("partner_banned_notice_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Block,
                                        contentDescription = stringResource(R.string.partner_banned_admin_mark),
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$partnerName${stringResource(R.string.partner_banned_title_suffix)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.partner_banned_admin_mark),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${stringResource(R.string.banned_reason_label)}: ${
                                        partnerBan.reason?.takeIf { it.isNotBlank() }
                                            ?: stringResource(R.string.banned_default_reason)
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .testTag("partner_banned_reason_text")
                                )
                            }
                            Text(
                                text = stringResource(R.string.partner_banned_wipe_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.88f)
                            )
                            Button(
                                onClick = onOpenRemovePartner,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .glossyPressEffect(cornerRadius = 14.dp)
                                    .testTag("partner_banned_remove_data_button")
                            ) {
                                Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.partner_banned_wipe_button),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Love Pulse Card (Velvet Dusk Gradient + Haptics)
            item {
                val lastPulseText = when {
                    couple.lovePulseCount == 0L -> "Tap to let your partner feel your heartbeat"
                    couple.lastPulseByUid == myUid -> "You sent the last pulse • ${couple.lovePulseCount} total"
                    else -> "$partnerName is thinking of you • ${couple.lovePulseCount} total"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                colors = activeTheme.heroGradientColors()
                            )
                        )
                        .exclusiveAnimatedBorder(
                            cornerRadius = 26.dp,
                            strokeWidth = 1.8.dp,
                            fallbackColor = Color.White.copy(alpha = 0.24f),
                            fallbackWidth = 1.dp
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Thinking of You",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = lastPulseText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                pulseAnimating = true
                                onSendLovePulse()
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusLiveCoral,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                            modifier = Modifier
                                .scale(pulseScale)
                                .height(48.dp)
                                .glossyPressEffect(cornerRadius = 24.dp)
                                .testTag("send_love_pulse_button")
                        ) {
                            Icon(Icons.Rounded.Favorite, contentDescription = "Send love pulse")
                            Spacer(Modifier.width(6.dp))
                            Text("Pulse")
                        }
                    }
                }
            }

            // 5. Quick Anniversary & Birthday Countdown Strip (gated by Date Planner feature flag)
            if (liveAppConfig.datePlannerEnabled) {
                item {
                    QuickCountdownsStrip(
                        anniversaryMonth = couple.anniversaryMonth,
                        anniversaryDay = couple.anniversaryDay,
                        partnerBirthday = couple.partnerBirthday(myUid),
                        onClick = onOpenDatesTab
                    )
                }
            }

            // 6. Shared Mood Tracker Card (gated by Mood Sync feature flag)
            item {
                if (!liveAppConfig.moodSyncEnabled) {
                    FeatureDisabledCard(
                        title = "Mood Sync Paused",
                        subtitle = "Mood check-ins and live mood sync are currently turned off by Bondly Admin.",
                        testTag = "feature_disabled_mood_sync"
                    )
                } else {
                val activeMood = MoodCatalog.byKey(selectedMoodKey)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .exclusiveAnimatedBorder(cornerRadius = 24.dp, strokeWidth = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "How are you feeling right now?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = onOpenMoodHistory,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = CircleShape,
                                modifier = Modifier
                                    .glossyPressEffect(cornerRadius = 20.dp)
                                    .testTag("open_mood_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = "View mood history",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (state.moods.isEmpty()) "History" else "History (${state.moods.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MoodCatalog.all.forEach { mood ->
                                val selected = mood.key == selectedMoodKey
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (selected) mood.accentColor.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) mood.accentColor else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedMoodKey = mood.key }
                                        .testTag("mood_chip_${mood.key.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = mood.emoji)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = mood.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = moodNoteDraft,
                                onValueChange = { if (it.length <= 120) moodNoteDraft = it },
                                placeholder = { Text("Add a sweet note for your partner…") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mood_note_input")
                            )
                            Button(
                                onClick = {
                                    onShareMood(activeMood, moodNoteDraft)
                                    moodNoteDraft = ""
                                },
                                enabled = !state.busyAction,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .glossyPressEffect(cornerRadius = 16.dp)
                                    .testTag("share_mood_button")
                            ) {
                                Text("Share")
                            }
                        }

                        if (state.moods.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.moods.take(3).forEach { entry ->
                                    MoodHistoryRow(
                                        entry = entry,
                                        isMine = entry.userId == myUid,
                                        onSendHug = { onSendComfortHug(entry.id) }
                                    )
                                }
                                FilledTonalButton(
                                    onClick = onOpenMoodHistory,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glossyPressEffect(cornerRadius = 14.dp)
                                        .testTag("open_full_mood_history_footer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("View Full Mood History & Insights")
                                }
                            }
                        }
                    }
                }
                }
            }

            // 7. Daily Couple Question (Unlocks Together + Live Midnight IST Expiry Countdown!)
            item {
                if (!liveAppConfig.dailyPromptEnabled) {
                    FeatureDisabledCard(
                        title = "Daily Prompt Check Paused",
                        subtitle = "Today's Question for Two is currently turned off by Bondly Admin.",
                        testTag = "feature_disabled_daily_prompt"
                    )
                } else {
                val prompt = state.todayPrompt
                val question = DailyPromptDeck.questionForIndex(state.todayPromptIndex)
                val isCreator = couple.creatorUid == myUid
                val myAnswer = if (isCreator) prompt?.creatorAnswer else prompt?.partnerAnswer
                val theirAnswer = if (isCreator) prompt?.partnerAnswer else prompt?.creatorAnswer

                var promptClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1_000L)
                        promptClockMs = System.currentTimeMillis()
                    }
                }
                val expiry = DailyPromptDeck.computeExpiryCountdown(promptClockMs)
                val urgentUnanswered = expiry.isExpiringSoon && myAnswer.isNullOrBlank()

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .exclusiveAnimatedBorder(cornerRadius = 24.dp, strokeWidth = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Today's Question for Two",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (urgentUnanswered) StatusLiveCoral
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                contentColor = if (urgentUnanswered) Color.White
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (urgentUnanswered) Color.White.copy(alpha = 0.65f)
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .testTag("daily_prompt_expiry_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = "Question expiry timer",
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (urgentUnanswered) "Expires soon • ${expiry.formattedCompact}"
                                        else "Expires in ${expiry.formattedCompact}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = question,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = when {
                                myAnswer.isNullOrBlank() && !theirAnswer.isNullOrBlank() ->
                                    "✨ $partnerName already answered! Reply before 12:00 AM IST to reveal it."
                                myAnswer.isNullOrBlank() ->
                                    "Resets at 12:00 AM IST • 1-hour expiry alert active until you answer"
                                else ->
                                    "Answered for today ✓ • 1-hour expiry reminder automatically muted"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (myAnswer.isNullOrBlank() && !theirAnswer.isNullOrBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        if (myAnswer.isNullOrBlank()) {
                            OutlinedTextField(
                                value = promptDraft,
                                onValueChange = { if (it.length <= 350) promptDraft = it },
                                placeholder = { Text("Write your answer to unlock your partner's…") },
                                minLines = 2,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("daily_prompt_input")
                            )
                            Button(
                                onClick = {
                                    onSubmitDailyAnswer(promptDraft)
                                    promptDraft = ""
                                },
                                enabled = promptDraft.trim().isNotEmpty() && !state.busyAction,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .glossyPressEffect(cornerRadius = 16.dp)
                                    .testTag("submit_daily_prompt_button")
                            ) {
                                Text("Seal My Answer")
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Your Answer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = myAnswer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "$partnerName's Answer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = theirAnswer ?: "Waiting for $partnerName to reply…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (theirAnswer != null) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }

            // 7b. Play, Care & Dream Together — 9 Interactive Couple Facilities!
            item {
                val doneBucket = state.bucketItems.count { it.completed }
                val bucketSub = if (state.bucketItems.isEmpty()) "Shared dreams"
                else "$doneBucket/${state.bucketItems.size} done"
                val lettersSub = if (state.loveLetters.isEmpty()) "Time capsules"
                else "${state.loveLetters.size} sealed"
                val jarSub = if (state.loveJarNotes.isEmpty()) "Wishes & notes"
                else "${state.loveJarNotes.size} notes"
                val vibeSub = if (state.vibeDedications.isEmpty()) "Dedicate song"
                else "${state.vibeDedications.size} tracks"
                val pendingCare = state.careCheckIns.count { !it.acknowledged && it.senderUid != myUid }
                val careSub = if (pendingCare > 0) "$pendingCare for you" else "Hydrate & care"
                val doodleSub = if (state.loveDoodles.isEmpty()) "HD sketch pad"
                else "${state.loveDoodles.size} sketches"
                val swornCount = state.couplePromises.count { it.isFullyCoSigned() }
                val promiseSub = if (state.couplePromises.isEmpty()) "Pacts & quiz"
                else "$swornCount sworn"

                data class FacilitySpec(
                    val emoji: String,
                    val title: String,
                    val subtitle: String,
                    val gradient: List<Color>,
                    val icon: androidx.compose.ui.graphics.vector.ImageVector,
                    val tag: String,
                    val onClick: () -> Unit
                )

                val visibleFacilities = buildList {
                    if (liveAppConfig.sharedJournalEnabled) {
                        add(
                            FacilitySpec(
                                emoji = "💌",
                                title = "Letters",
                                subtitle = lettersSub,
                                gradient = listOf(Color(0xFF9F1239), Color(0xFFE11D48)),
                                icon = Icons.Rounded.Mail,
                                tag = "open_love_letters_tile",
                                onClick = onOpenLoveLetters
                            )
                        )
                        add(
                            FacilitySpec(
                                emoji = "🫙",
                                title = "Love Jar",
                                subtitle = jarSub,
                                gradient = listOf(Color(0xFF5B21B6), Color(0xFFDB2777)),
                                icon = Icons.Rounded.AutoAwesome,
                                tag = "open_love_jar_tile",
                                onClick = onOpenLoveJar
                            )
                        )
                    }
                    add(
                        FacilitySpec(
                            emoji = "🎶",
                            title = "Song Vibe",
                            subtitle = vibeSub,
                            gradient = listOf(Color(0xFF1E3A8A), Color(0xFF7C3AED)),
                            icon = Icons.Rounded.MusicNote,
                            tag = "open_song_vibe_tile",
                            onClick = onOpenSongVibe
                        )
                    )
                    add(
                        FacilitySpec(
                            emoji = "💧",
                            title = "Care Nudge",
                            subtitle = careSub,
                            gradient = listOf(Color(0xFF0369A1), Color(0xFF0D9488)),
                            icon = Icons.Rounded.HealthAndSafety,
                            tag = "open_care_checkin_tile",
                            onClick = onOpenCareCheckIn
                        )
                    )
                    add(
                        FacilitySpec(
                            emoji = "🎨",
                            title = "Doodle",
                            subtitle = doodleSub,
                            gradient = listOf(Color(0xFFB45309), Color(0xFFEA580C)),
                            icon = Icons.Rounded.Brush,
                            tag = "open_love_doodle_tile",
                            onClick = onOpenLoveDoodle
                        )
                    )
                    add(
                        FacilitySpec(
                            emoji = "🤙",
                            title = "Promises",
                            subtitle = promiseSub,
                            gradient = listOf(Color(0xFF065F46), Color(0xFF059669)),
                            icon = Icons.Rounded.Handshake,
                            tag = "open_couple_promises_tile",
                            onClick = onOpenCouplePromises
                        )
                    )
                    add(
                        FacilitySpec(
                            emoji = "🌟",
                            title = "Bucket List",
                            subtitle = bucketSub,
                            gradient = listOf(Color(0xFF7B2654), Color(0xFFD65678)),
                            icon = Icons.Rounded.Checklist,
                            tag = "open_bucket_list_tile",
                            onClick = onOpenBucketList
                        )
                    )
                    if (liveAppConfig.datePlannerEnabled) {
                        add(
                            FacilitySpec(
                                emoji = "🎡",
                                title = "Date Wheel",
                                subtitle = "Spin a plan",
                                gradient = listOf(Color(0xFF283593), Color(0xFF00ACC1)),
                                icon = Icons.Rounded.Casino,
                                tag = "open_date_spinner_tile",
                                onClick = onOpenDateSpinner
                            )
                        )
                    }
                    add(
                        FacilitySpec(
                            emoji = "🎟️",
                            title = "Coupons",
                            subtitle = "& Love Deck",
                            gradient = listOf(Color(0xFF6A1B9A), Color(0xFFE06D3B)),
                            icon = Icons.Rounded.CardGiftcard,
                            tag = "open_love_games_tile",
                            onClick = onOpenLoveGames
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("couple_playground_section"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Couple Sanctuary & Facilities",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Love letters, wish jar, song vibes, care nudges, HD doodles, promises & games",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!liveAppConfig.sharedJournalEnabled) {
                        FeatureDisabledCard(
                            title = "Shared Journal Paused",
                            subtitle = "Letters, Love Jar & Shared Moments are turned off by Bondly Admin.",
                            testTag = "feature_disabled_shared_journal_banner"
                        )
                    }

                    visibleFacilities.chunked(3).forEach { rowSpecs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowSpecs.forEach { spec ->
                                CouplePlaygroundTile(
                                    emoji = spec.emoji,
                                    title = spec.title,
                                    subtitle = spec.subtitle,
                                    gradient = spec.gradient,
                                    icon = spec.icon,
                                    onClick = spec.onClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(spec.tag)
                                )
                            }
                            repeat(3 - rowSpecs.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 8. Cute Home Screen Widget Studio Card (8 Widgets!)
            item {
                val previewPresence = state.partnerPresence ?: state.myPresence
                val previewMood = MoodCatalog.byKey(previewPresence?.moodKey)
                val who = if (state.partnerPresence != null) partnerName else "You"
                val batteryText = when {
                    previewPresence == null -> "Live"
                    !previewPresence.statusShared || previewPresence.batteryPercent < 0 -> "Private"
                    previewPresence.charging -> "${previewPresence.batteryPercent}% ⚡"
                    else -> "${previewPresence.batteryPercent}%"
                }
                val countdownSummary = couple.anniversaryMonth?.let { m ->
                    couple.anniversaryDay?.let { d ->
                        val c = CountdownCalculator.computeYearlyCountdown(m, d)
                        if (c.isToday) "Happy Anniversary Today!" else "${c.days}d until Anniversary"
                    }
                } ?: "Set your anniversary & birthday"

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Widgets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Interactive Widgets",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Chat, Mood, Radar, Countdown, Hub & More",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = onOpenWidgetStudio,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .glossyPressEffect(cornerRadius = 14.dp)
                                .testTag("open_widget_studio_button")
                        ) {
                            Text("8 Widgets")
                        }
                    }

                    CuteWidgetPreviewCard(
                        coupleTitle = if (isPaired) "$myName & $partnerName" else "Duet • ${couple.inviteCode}",
                        batteryBadge = batteryText,
                        moodLine = "${previewMood.emoji} $who: ${previewMood.label}",
                        countdownLine = countdownSummary,
                        statusFooter = if (previewPresence != null) {
                            "$who: ${previewPresence.liveActivityLabel()}"
                        } else {
                            "End-to-end encrypted for two"
                        },
                        themeStyle = state.widgetTheme,
                        modifier = Modifier
                            .glossyPressEffect(cornerRadius = 24.dp, pressScale = 0.98f)
                            .exclusiveAnimatedBorder(cornerRadius = 24.dp, strokeWidth = 1.6.dp)
                            .clickable { onOpenWidgetStudio() }
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun AccountPresenceCard(
    name: String,
    subtitle: String,
    presence: PartnerPresence?,
    isPairedSlot: Boolean,
    isPartnerCard: Boolean,
    isBannedAccount: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDuetThemePreset.current.isDark
    val mood = MoodCatalog.byKey(presence?.moodKey)
    val batteryPct = (presence?.batteryPercent ?: -1L).toInt()
    val isBannedPartner = isPartnerCard && isPairedSlot && isBannedAccount
    val isLive = !isBannedPartner && (presence?.deviceStatus == "ACTIVE_IN_DUET" || presence?.deviceStatus == "IN_USE")

    val cardBg = when {
        isBannedPartner -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
        isDark && isPartnerCard -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f)
        isDark -> MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        isPartnerCard -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val cardBorderColor = when {
        isBannedPartner -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        isDark && isPartnerCard -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        isDark -> MaterialTheme.colorScheme.outlineVariant
        else -> Color.Transparent
    }
    val cardTitleColor = MaterialTheme.colorScheme.onSurface
    val cardSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val batteryPillBg = if (isDark) {
        MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val batteryPillBorder = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    }
    val batteryTextColor = if (isDark) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val batteryIconTint = when {
        batteryPct in 0..20 && presence?.charging != true -> BatteryLowCrimson
        presence?.charging == true -> BatteryHealthyGreen
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        contentColor = cardTitleColor,
        modifier = modifier
            .glossyPressEffect(cornerRadius = 22.dp, pressScale = 0.97f)
            .clip(RoundedCornerShape(22.dp))
            .exclusiveAnimatedBorder(
                cornerRadius = 22.dp,
                strokeWidth = 1.6.dp,
                fallbackColor = cardBorderColor,
                fallbackWidth = if (cardBorderColor != Color.Transparent) 1.dp else 0.dp
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface)
                            .border(
                                width = 2.dp,
                                color = if (isBannedPartner) MaterialTheme.colorScheme.error else mood.accentColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBannedPartner) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = stringResource(R.string.partner_banned_admin_mark),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Text(
                                text = if (isPairedSlot) mood.emoji else "💌",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                    if (!isBannedPartner && isPairedSlot && presence != null) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isLive) BatteryHealthyGreen else MaterialTheme.colorScheme.outline)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                }

                if (isBannedPartner) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.testTag("partner_banned_badge")
                    ) {
                        Text(
                            text = stringResource(R.string.partner_banned_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (isPairedSlot && presence?.statusShared == true && batteryPct >= 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = batteryPillBg,
                        contentColor = batteryTextColor,
                        modifier = Modifier
                            .border(1.dp, batteryPillBorder, RoundedCornerShape(10.dp))
                            .testTag(if (isPartnerCard) "partner_battery_pill" else "my_battery_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (presence.charging) Icons.Rounded.BatteryChargingFull
                                else Icons.Rounded.BatteryStd,
                                contentDescription = "Battery",
                                tint = batteryIconTint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "$batteryPct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = batteryTextColor
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = cardTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        isBannedPartner -> stringResource(R.string.partner_banned_admin_mark)
                        isPairedSlot && presence != null -> presence.liveActivityLabel()
                        else -> subtitle
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isBannedPartner -> MaterialTheme.colorScheme.error
                        isLive -> MaterialTheme.colorScheme.primary
                        else -> cardSecondaryColor
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isPartnerCard) {
                    Text(
                        text = when {
                            isBannedPartner -> "Tap for reason & options"
                            isPairedSlot && presence != null -> {
                                if (presence.deviceStatus == "ACTIVE_IN_DUET") "Online • Tap for details"
                                else presence.lastSeenIstLabel()
                            }
                            isPairedSlot -> "Tap for details"
                            else -> "Invite partner"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = cardSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val loggedInDevice = presence?.loggedInDeviceLabel().orEmpty()
                if (!isBannedPartner && isPairedSlot && loggedInDevice.isNotBlank()) {
                    Text(
                        text = "📱 $loggedInDevice",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCountdownsStrip(
    anniversaryMonth: Int?,
    anniversaryDay: Int?,
    partnerBirthday: Pair<Int, Int>?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("home_countdowns_strip"),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val annLabel = if (anniversaryMonth != null && anniversaryDay != null) {
            val c = CountdownCalculator.computeYearlyCountdown(anniversaryMonth, anniversaryDay)
            if (c.isToday) "Today!" else "${c.days}d ${c.hours}h"
        } else {
            "Set date"
        }
        val bdayLabel = if (partnerBirthday != null) {
            val c = CountdownCalculator.computeYearlyCountdown(partnerBirthday.first, partnerBirthday.second)
            if (c.isToday) "Today!" else "${c.days}d ${c.hours}h"
        } else {
            "Set date"
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
            modifier = Modifier
                .weight(1f)
                .exclusiveAnimatedBorder(cornerRadius = 20.dp, strokeWidth = 1.4.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = "Anniversary countdown",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Anniversary",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = annLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
            modifier = Modifier
                .weight(1f)
                .exclusiveAnimatedBorder(cornerRadius = 20.dp, strokeWidth = 1.4.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cake,
                    contentDescription = "Partner birthday countdown",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Partner B-Day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = bdayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodHistoryRow(
    entry: MoodEntry,
    isMine: Boolean,
    onSendHug: () -> Unit
) {
    val mood = MoodCatalog.byKey(entry.moodKey)
    val timeLabel = entry.createdAt?.toDate()?.time?.let {
        DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    } ?: "Just now"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = mood.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.authorName} • ${mood.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (!entry.comfortSentByUid.isNullOrBlank()) "$timeLabel • Warm hug received" else timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (!isMine && entry.comfortSentByUid.isNullOrBlank()) {
                FilledTonalButton(
                    onClick = onSendHug,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.glossyPressEffect(cornerRadius = 12.dp)
                ) {
                    Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hug", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun CouplePlaygroundTile(
    emoji: String,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glossyPressEffect(cornerRadius = 22.dp, pressScale = 0.95f)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(gradient))
            .exclusiveAnimatedBorder(
                cornerRadius = 22.dp,
                strokeWidth = 1.5.dp,
                fallbackColor = Color.White.copy(alpha = 0.45f),
                fallbackWidth = 1.2.dp
            )
            .clickable(onClick = onClick)
            .padding(13.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 22.sp)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.80f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.84f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
