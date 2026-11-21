package com.example.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MoodCatalog
import com.example.data.PartnerBannedNotice
import com.example.data.PartnerPresence
import com.example.telemetry.DeviceSpecsSnapshot
import com.example.ui.theme.BatteryAmberWarn
import com.example.ui.theme.BatteryHealthyGreen
import com.example.ui.theme.BatteryLowCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerStatusSheet(
    accountName: String,
    isSelf: Boolean,
    isPaired: Boolean,
    inviteCode: String,
    presence: PartnerPresence?,
    partnerBanNotice: PartnerBannedNotice? = null,
    localDeviceSpecs: DeviceSpecsSnapshot? = null,
    hasUsageAccessPermission: Boolean = true,
    onDismiss: () -> Unit,
    onSendLovePulse: () -> Unit,
    onRemindCharge: () -> Unit,
    onAutoRefresh: () -> Unit = {},
    onShareCode: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit = {},
    onOpenDeviceDetails: (showSelf: Boolean) -> Unit = {},
    onOpenRemovePartner: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var clockTickMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Silently keep phone status and timestamps fresh automatically while open
    LaunchedEffect(isSelf) {
        onAutoRefresh()
        while (true) {
            delay(10_000L)
            clockTickMs = System.currentTimeMillis()
            onAutoRefresh()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isSelf && !isPaired) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Waiting for Partner to Link",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Share your 6-digit invite code below. Once your partner links, tapping their account reveals their live battery %, network, and whether their phone is in use or locked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = inviteCode.chunked(3).joinToString(" "),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onShareCode,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .glossyPressEffect(cornerRadius = 16.dp)
                        .testTag("sheet_share_invite_button")
                ) {
                    Text("Share 6-Digit Code")
                }
                return@ModalBottomSheet
            }

            val mood = MoodCatalog.byKey(presence?.moodKey)
            val statusShared = presence?.statusShared == true
            val batteryPct = (presence?.batteryPercent ?: -1L).toInt()
            val charging = presence?.charging == true
            val networkType = presence?.networkType ?: "OFFLINE"
            val deviceStatus = presence?.deviceStatus ?: "IN_USE"
            val updatedMillis = presence?.updatedAt?.toDate()?.time
            val updatedLabel = if (updatedMillis != null) {
                DateUtils.getRelativeTimeSpanString(
                    updatedMillis,
                    clockTickMs,
                    DateUtils.SECOND_IN_MILLIS
                ).toString()
            } else {
                "Just now"
            }

            // Header with Mood, Name & WhatsApp-style IST Last Seen
            Text(
                text = if (isSelf) "$accountName (Your Device)" else accountName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isSelf && partnerBanNotice != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.testTag("partner_sheet_banned_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.partner_banned_admin_mark).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("partner_sheet_banned_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.banned_reason_label)}: ${
                                partnerBanNotice.reason?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.banned_default_reason)
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = stringResource(R.string.partner_banned_wipe_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${mood.emoji} ${mood.label} • Updated $updatedLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (presence != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = presence.lastSeenIstLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (presence.deviceStatus == "ACTIVE_IN_DUET") BatteryHealthyGreen
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("partner_sheet_last_seen")
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Animated Battery Percentage Gauge
            val batteryColor = when {
                !statusShared || batteryPct < 0 -> MaterialTheme.colorScheme.outline
                batteryPct <= 20 -> BatteryLowCrimson
                batteryPct <= 45 -> BatteryAmberWarn
                else -> BatteryHealthyGreen
            }
            val sweepFraction by animateFloatAsState(
                targetValue = if (statusShared && batteryPct >= 0) batteryPct / 100f else 0f,
                animationSpec = tween(durationMillis = 700),
                label = "battery_sweep"
            )
            val trackColor = MaterialTheme.colorScheme.surfaceVariant

            Box(
                modifier = Modifier
                    .size(144.dp)
                    .testTag("partner_battery_gauge"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(144.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = batteryColor,
                        startAngle = 135f,
                        sweepAngle = 270f * sweepFraction,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when {
                            charging -> Icons.Rounded.BatteryChargingFull
                            batteryPct in 0..20 -> Icons.Rounded.BatteryAlert
                            else -> Icons.Rounded.BatteryStd
                        },
                        contentDescription = "Battery icon",
                        tint = batteryColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (statusShared && batteryPct >= 0) "$batteryPct%" else "--",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            !statusShared || batteryPct < 0 -> "Hidden"
                            charging -> "Charging"
                            else -> "On Battery"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = batteryColor
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Two live telemetry pills: Network & Mobile In-Use / Locked Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (netIcon, netTitle) = when (networkType) {
                    "WIFI" -> Icons.Rounded.Wifi to "Wi-Fi"
                    "CELLULAR" -> Icons.Rounded.CellTower to "Mobile Data"
                    "ETHERNET" -> Icons.Rounded.Wifi to "Ethernet"
                    "HIDDEN" -> Icons.Rounded.WifiOff to "Hidden"
                    else -> Icons.Rounded.WifiOff to "Offline"
                }
                TelemetryMetricCard(
                    icon = netIcon,
                    label = "Network",
                    value = netTitle,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("partner_network_card")
                )

                val devTitle = presence?.liveActivityLabel() ?: "Mobile In Use"
                val (devIcon, devColor) = when {
                    !statusShared || deviceStatus == "HIDDEN" -> Icons.Rounded.Lock to MaterialTheme.colorScheme.outline
                    deviceStatus == "LOCKED" -> Icons.Rounded.Lock to MaterialTheme.colorScheme.tertiary
                    deviceStatus == "ACTIVE_IN_DUET" -> Icons.Rounded.PhoneAndroid to BatteryHealthyGreen
                    !presence?.activeAppName.isNullOrBlank() -> Icons.Rounded.Apps to BatteryHealthyGreen
                    else -> Icons.Rounded.Smartphone to BatteryHealthyGreen
                }
                TelemetryMetricCard(
                    icon = devIcon,
                    label = if (!presence?.activeAppName.isNullOrBlank() && deviceStatus != "LOCKED" && statusShared) {
                        "Live Activity"
                    } else {
                        "Phone Status"
                    },
                    value = devTitle,
                    accent = devColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("partner_device_status_card")
                )
            }

            // Logged-In Device Summary Pill -> Tap to inspect full A-to-Z Device Details
            val resolvedSpecs = if (isSelf) localDeviceSpecs ?: presence?.parsedDeviceSpecs() else presence?.parsedDeviceSpecs()
            val deviceHeadline = when {
                resolvedSpecs != null -> resolvedSpecs.deviceName.ifBlank { "Android Device" }
                !presence?.loggedInDeviceLabel().isNullOrBlank() -> presence!!.loggedInDeviceLabel()
                !statusShared && !isSelf -> "Device Info Hidden"
                else -> "Waiting for device sync"
            }
            val deviceSubline = when {
                resolvedSpecs != null -> {
                    val patch = resolvedSpecs.securityPatch.takeIf { it.isNotBlank() && it != "Unavailable" }
                        ?.let { " • Patch $it" } ?: ""
                    "${resolvedSpecs.androidVersion}$patch • Tap for A–Z specs"
                }
                !statusShared && !isSelf -> "$accountName paused device sharing"
                else -> "Tap to inspect full A–Z device details"
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .glossyPressEffect(cornerRadius = 18.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        onDismiss()
                        onOpenDeviceDetails(isSelf)
                    }
                    .testTag("partner_sheet_device_details_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isSelf) "Logged In On • $deviceHeadline" else "Partner's Device • $deviceHeadline",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = deviceSubline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "View all A-to-Z device details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!presence?.moodNote.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${presence?.moodNote}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            if (isSelf) {
                if (!hasUsageAccessPermission) {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onOpenUsageAccessSettings,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .glossyPressEffect(cornerRadius = 16.dp)
                            .testTag("sheet_enable_usage_access_button")
                    ) {
                        Icon(Icons.Rounded.Apps, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share Open App (Instagram, WhatsApp…)")
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (statusShared && batteryPct in 0..20 && !charging) {
                        FilledTonalButton(
                            onClick = {
                                onRemindCharge()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .glossyPressEffect(cornerRadius = 16.dp)
                                .testTag("remind_charge_button")
                        ) {
                            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Plug In!")
                        }
                    }
                    Button(
                        onClick = {
                            onSendLovePulse()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .glossyPressEffect(cornerRadius = 16.dp)
                            .testTag("status_sheet_send_pulse_button")
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Send Love Pulse")
                    }
                }
                if (isPaired) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenRemovePartner()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .glossyPressEffect(cornerRadius = 16.dp)
                            .testTag("partner_sheet_remove_partner_button")
                    ) {
                        Icon(Icons.Rounded.LinkOff, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Remove as Partner",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
