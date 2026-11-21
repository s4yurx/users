package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PartnerPresence
import com.example.telemetry.DeviceSpecSection
import com.example.telemetry.DeviceSpecsSnapshot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDuetThemePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsSheet(
    myName: String,
    partnerName: String,
    isPaired: Boolean,
    initialShowSelf: Boolean,
    myDeviceSpecs: DeviceSpecsSnapshot?,
    partnerPresence: PartnerPresence?,
    shareDeviceStatus: Boolean,
    onRefreshMine: () -> Unit,
    onFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeTheme = LocalDuetThemePreset.current
    var viewingSelf by remember(initialShowSelf) { mutableStateOf(initialShowSelf) }

    val partnerSpecs = remember(partnerPresence?.deviceSpecsJson, partnerPresence?.statusShared) {
        partnerPresence?.parsedDeviceSpecs()
    }
    val activeSpecs = if (viewingSelf) myDeviceSpecs else partnerSpecs
    val activeOwnerLabel = if (viewingSelf) "$myName (This Device)" else "$partnerName's Device"

    val copyText: (String, String) -> Unit = { label, value ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
        onFeedback("$label copied")
    }

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
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Details (A to Z)",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Complete hardware, OS, security patch & logged-in device info",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Switch between Your Device and Partner's Logged-In Device
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = viewingSelf,
                    onClick = { viewingSelf = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier
                        .glossyPressEffect(cornerRadius = 20.dp)
                        .testTag("device_details_tab_self")
                ) {
                    Text("My Device", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                SegmentedButton(
                    selected = !viewingSelf,
                    onClick = { viewingSelf = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier
                        .glossyPressEffect(cornerRadius = 20.dp)
                        .testTag("device_details_tab_partner")
                ) {
                    Text(
                        text = if (isPaired) "$partnerName's Phone" else "Partner",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (activeSpecs == null) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Smartphone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        val title = when {
                            viewingSelf -> "Reading Device Specifications…"
                            !isPaired -> "Partner Not Linked Yet"
                            partnerPresence?.statusShared == false -> "$partnerName Paused Device Sharing"
                            else -> "Waiting for $partnerName's Device Sync"
                        }
                        val body = when {
                            viewingSelf -> "Tap refresh to read all hardware and Android version details."
                            !isPaired -> "Share your 6-digit code so you can see the exact phone your partner is logged in with."
                            partnerPresence?.statusShared == false -> "$partnerName currently has device status sharing turned off in Settings."
                            else -> "As soon as $partnerName opens Duet or refreshes status, all of their logged-in device details will appear here automatically."
                        }
                        Text(text = title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Hero Logged-In Device Summary Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(activeTheme.heroStart, activeTheme.heroEnd)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (viewingSelf) {
                                    if (shareDeviceStatus) "Shared with $partnerName • ${activeSpecs.loggedInAccount}"
                                    else "Private (Sharing off) • ${activeSpecs.loggedInAccount}"
                                } else {
                                    "Partner Logged In • ${activeSpecs.loggedInAccount}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = activeSpecs.deviceName.ifBlank { "${activeSpecs.manufacturer} ${activeSpecs.model}".trim() },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroSpecChip(activeSpecs.androidVersion.ifBlank { "Android" })
                            HeroSpecChip("Patch ${activeSpecs.securityPatch.ifBlank { "N/A" }}")
                            HeroSpecChip(activeSpecs.apiLevel.substringBefore(" "))
                        }
                    }
                }

                // Quick Action Buttons (Copy All A-Z / Refresh Live Specs)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            copyText(
                                "$activeOwnerLabel Specs",
                                activeSpecs.toPlainTextReport(activeOwnerLabel)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .glossyPressEffect(cornerRadius = 14.dp)
                            .testTag("device_details_copy_all_button")
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy All A–Z")
                    }
                    if (viewingSelf) {
                        FilledTonalButton(
                            onClick = {
                                onRefreshMine()
                                onFeedback("Device details refreshed & synced")
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .glossyPressEffect(cornerRadius = 14.dp)
                                .testTag("device_details_refresh_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync Now")
                        }
                    }
                }

                Text(
                    text = "Tap any row below to copy its value.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val sections = remember(activeSpecs) { activeSpecs.toSections() }
                sections.forEachIndexed { idx, section ->
                    DeviceSpecSectionCard(
                        section = section,
                        icon = sectionIconFor(idx),
                        onCopyItem = { label, value -> copyText(label, value) }
                    )
                }
            }

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .glossyPressEffect(cornerRadius = 16.dp)
                    .testTag("device_details_done_button")
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun HeroSpecChip(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun DeviceSpecSectionCard(
    section: DeviceSpecSection,
    icon: ImageVector,
    onCopyItem: (String, String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))

            section.items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCopyItem(item.label, item.value) }
                        .padding(vertical = 3.dp, horizontal = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.value,
                        fontFamily = if (item.monospace) JetBrainsMonoFontFamily else null,
                        style = if (item.monospace) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun sectionIconFor(index: Int): ImageVector = when (index) {
    0 -> Icons.Rounded.Smartphone
    1 -> Icons.Rounded.Security
    2 -> Icons.Rounded.DeveloperMode
    3 -> Icons.Rounded.Memory
    else -> Icons.Rounded.SettingsInputAntenna
}
