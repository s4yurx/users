package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.LiveAppConfig
import com.example.data.PartnerPresence
import com.example.telemetry.DeviceSpecsSnapshot
import com.example.ui.theme.EncryptedEmerald
import com.example.ui.theme.EncryptedEmeraldContainer
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptionInfoSheet(
    isPaired: Boolean,
    safetyNumber: String,
    myFingerprint: String,
    partnerFingerprint: String?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = EncryptedEmerald
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "End-to-End Encryption",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EncryptedEmeraldContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = EncryptedEmerald
                    )
                    Column {
                        Text(
                            text = if (isPaired) "Active: ECDH P-256 + AES-256-GCM" else "Active: AES-256-GCM Invite Vault",
                            style = MaterialTheme.typography.titleSmall,
                            color = EncryptedEmerald
                        )
                        Text(
                            text = "Every message and photo is encrypted on your phone before reaching the cloud.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EncryptedEmerald.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Text(
                text = "Couple Safety Number",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = safetyNumber,
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Your Device Public Key",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = myFingerprint,
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.bodySmall
                )
                if (!partnerFingerprint.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Partner's Public Key",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = partnerFingerprint,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .glossyPressEffect(cornerRadius = 16.dp)
            ) {
                Text("Done")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuetSettingsSheet(
    myNickname: String,
    partnerName: String = "Partner",
    inviteCode: String,
    isPaired: Boolean,
    shareDeviceStatus: Boolean,
    backgroundSyncEnabled: Boolean,
    hasNotificationPermission: Boolean,
    hasUsageAccessPermission: Boolean,
    myDeviceSpecs: DeviceSpecsSnapshot? = null,
    partnerPresence: PartnerPresence? = null,
    liveAppConfig: LiveAppConfig = LiveAppConfig(),
    isAdminAppInstalled: Boolean = false,
    onUpdateNickname: (String) -> Unit,
    onToggleShareStatus: (Boolean) -> Unit,
    onToggleBackgroundSync: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit,
    onOpenAppearanceStudio: () -> Unit = {},
    onOpenWidgetStudio: () -> Unit = {},
    onOpenDeviceDetails: (showSelf: Boolean) -> Unit = {},
    onOpenUpdateDetails: () -> Unit = {},
    onOpenAdminCompanion: () -> Unit = {},
    onOpenEncryptionInfo: () -> Unit,
    onOpenRemovePartner: () -> Unit = {},
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nameDraft by remember(myNickname) { mutableStateOf(myNickname) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Duet Settings",
                style = MaterialTheme.typography.headlineSmall
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isPaired) "Your Couple Invite Code (Paired)" else "6-Digit Partner Invite Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = inviteCode.chunked(3).joinToString(" "),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = onCopyCode,
                            modifier = Modifier
                                .weight(1f)
                                .glossyPressEffect(cornerRadius = 20.dp)
                                .testTag("settings_copy_code_button")
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }
                        FilledTonalButton(
                            onClick = onShareCode,
                            modifier = Modifier
                                .weight(1f)
                                .glossyPressEffect(cornerRadius = 20.dp)
                                .testTag("settings_share_code_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        onDismiss()
                        onOpenAppearanceStudio()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .glossyPressEffect(cornerRadius = 16.dp)
                        .testTag("settings_open_appearance_button")
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Theme & Logo")
                }
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onOpenWidgetStudio()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .glossyPressEffect(cornerRadius = 16.dp)
                        .testTag("settings_open_widgets_button")
                ) {
                    Icon(Icons.Rounded.Widgets, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("8 Widgets")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { if (it.length <= 40) nameDraft = it },
                    label = { Text("Your Nickname") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = { onUpdateNickname(nameDraft) },
                    enabled = nameDraft.trim().isNotEmpty() && nameDraft.trim() != myNickname,
                    modifier = Modifier.glossyPressEffect(cornerRadius = 20.dp)
                ) {
                    Text("Save")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Always-On Background Sync",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Keeps Duet running in background for instant messages, pulses, mood alerts & live app status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = backgroundSyncEnabled,
                    onCheckedChange = onToggleBackgroundSync,
                    modifier = Modifier.testTag("toggle_background_sync_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Share Live Battery, App & Device Specs",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Lets your partner see battery %, network, IST last seen, open app & logged-in device details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = shareDeviceStatus,
                    onCheckedChange = onToggleShareStatus,
                    modifier = Modifier.testTag("toggle_share_status_switch")
                )
            }

            // Logged-In Device Information (A to Z) for Both You & Your Partner
            val partnerSpecs = remember(partnerPresence?.deviceSpecsJson, partnerPresence?.statusShared) {
                partnerPresence?.parsedDeviceSpecs()
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Logged-In Devices (A–Z Details)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .glossyPressEffect(cornerRadius = 14.dp)
                            .clickable {
                                onDismiss()
                                onOpenDeviceDetails(true)
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                            .testTag("settings_my_device_details_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "This Device (You)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = myDeviceSpecs?.shortSummaryLine() ?: "View Android version, security patch & hardware",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Open my device details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .glossyPressEffect(cornerRadius = 14.dp)
                            .clickable {
                                onDismiss()
                                onOpenDeviceDetails(false)
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                            .testTag("settings_partner_device_details_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Smartphone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isPaired) "$partnerName's Logged-In Device" else "Partner's Device",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when {
                                        !isPaired -> "Pair with your partner to see their device"
                                        partnerSpecs != null -> partnerSpecs.shortSummaryLine()
                                        !partnerPresence?.loggedInDeviceLabel().isNullOrBlank() -> partnerPresence!!.loggedInDeviceLabel()
                                        partnerPresence?.statusShared == false -> "Hidden by $partnerName"
                                        else -> "Waiting for partner device sync"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Open partner device details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Notifications & Background Inline Reply Setup Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Notifications & Inline Reply",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (hasNotificationPermission) "Enabled for messages, pulses & moods"
                                    else "Permission needed for partner alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!hasNotificationPermission) {
                            FilledTonalButton(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier
                                    .glossyPressEffect(cornerRadius = 20.dp)
                                    .testTag("settings_allow_notifications_button")
                            ) {
                                Text("Allow")
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onSendTestNotification,
                                modifier = Modifier
                                    .glossyPressEffect(cornerRadius = 20.dp)
                                    .testTag("settings_test_notification_button")
                            ) {
                                Text("Test")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Live Current App Detection",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (hasUsageAccessPermission) "Active • Shows Instagram, WhatsApp & all apps"
                                    else "Grant Usage Access to show currently open app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = onOpenUsageAccessSettings,
                            modifier = Modifier
                                .glossyPressEffect(cornerRadius = 20.dp)
                                .testTag("settings_usage_access_button")
                        ) {
                            Text(if (hasUsageAccessPermission) "Manage" else "Enable")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                                imageVector = Icons.Rounded.BatteryChargingFull,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Keep Running Uninterrupted",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Set battery to Unrestricted so OS never pauses sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = onOpenBatterySettings,
                            modifier = Modifier
                                .glossyPressEffect(cornerRadius = 20.dp)
                                .testTag("settings_battery_settings_button")
                        ) {
                            Text("Battery")
                        }
                    }
                }
            }

            // App Updates & Bondly Control Card
            val hasUpdateAvailable = liveAppConfig.hasAnyUpdateForNotification(
                currentVersionName = BuildConfig.VERSION_NAME,
                currentVersionCode = BuildConfig.VERSION_CODE
            )
            val availableVer = liveAppConfig.notificationUpdateVersion(BuildConfig.VERSION_NAME)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_updates_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "App Updates & Bondly Sync",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (hasUpdateAvailable) {
                                        "New update v$availableVer available • Current v${BuildConfig.VERSION_NAME}"
                                    } else {
                                        "Up to date (v${BuildConfig.VERSION_NAME}) • Synced with Bondly"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (hasUpdateAvailable) {
                            FilledTonalButton(
                                onClick = {
                                    onDismiss()
                                    onOpenUpdateDetails()
                                },
                                modifier = Modifier
                                    .glossyPressEffect(cornerRadius = 20.dp)
                                    .testTag("settings_view_update_button")
                            ) {
                                Text("View Update")
                            }
                        }
                    }

                    if (isAdminAppInstalled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Bondly Admin Connected",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = liveAppConfig.resolvedAdminPackageId(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = {
                                    onDismiss()
                                    onOpenAdminCompanion()
                                },
                                modifier = Modifier
                                    .glossyPressEffect(cornerRadius = 20.dp)
                                    .testTag("settings_open_admin_button")
                            ) {
                                Text("Open")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_feature_flags_section"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Live Feature Flags",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val flagSummary = buildString {
                            append("Shared Journal: ${if (liveAppConfig.sharedJournalEnabled) "ON" else "PAUSED"}")
                            append(" • Date Planner: ${if (liveAppConfig.datePlannerEnabled) "ON" else "PAUSED"}")
                            append("\nMood Sync: ${if (liveAppConfig.moodSyncEnabled) "ON" else "PAUSED"}")
                            append(" • Daily Prompt: ${if (liveAppConfig.dailyPromptEnabled) "ON" else "PAUSED"}")
                        }
                        Text(
                            text = flagSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("settings_feature_flags_summary")
                        )

                        val versionRules = buildList {
                            if (liveAppConfig.minVersion.isNotBlank()) add("Min v${liveAppConfig.minVersion}")
                            liveAppConfig.minVersionCode?.takeIf { it > 0 }?.let { add("Min Code $it") }
                            if (liveAppConfig.latestVersion.isNotBlank()) add("Latest v${liveAppConfig.latestVersion}")
                            if (liveAppConfig.blockedVersions.isNotEmpty()) {
                                add("Blocked: ${liveAppConfig.blockedVersions.joinToString(", ")}")
                            }
                        }
                        if (versionRules.isNotEmpty()) {
                            Text(
                                text = "Version Rules: ${versionRules.joinToString(" • ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.testTag("settings_version_control_summary")
                            )
                        }
                    }

                    if (liveAppConfig.dynamicConfig.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_dynamic_config_section"),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Dynamic Key-Value Config (${liveAppConfig.dynamicConfig.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            liveAppConfig.dynamicConfig.entries.take(8).forEach { (k, v) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = k,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = v,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenEncryptionInfo()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .glossyPressEffect(cornerRadius = 16.dp)
            ) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Inspect E2EE Safety Number")
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenRemovePartner()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .glossyPressEffect(cornerRadius = 16.dp)
                    .testTag("settings_remove_partner_button")
            ) {
                Icon(Icons.Rounded.LinkOff, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Remove as Partner",
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            Button(
                onClick = {
                    onDismiss()
                    onSignOut()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .glossyPressEffect(cornerRadius = 16.dp)
                    .testTag("settings_sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }

            Text(
                text = "Duet • Version ${BuildConfig.VERSION_NAME} • Timezone IST (Asia/Kolkata)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("settings_version_label")
            )
        }
    }
}
