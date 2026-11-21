package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppIconPreset
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.DuetAppearanceState
import com.example.ui.theme.DuetAppearanceStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceStudioSheet(
    appearance: DuetAppearanceState,
    onFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var brandDraft by remember(appearance.customBrandTitle) { mutableStateOf(appearance.customBrandTitle) }
    var savingLogo by remember { mutableStateOf(false) }

    val exclusiveThemes = remember { AppThemePreset.entries.filter { it.isExclusive } }
    val classicThemes = remember { AppThemePreset.entries.filterNot { it.isExclusive } }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                savingLogo = true
                scope.launch {
                    val ok = DuetAppearanceStore.saveCustomLogo(context, uri)
                    savingLogo = false
                    onFeedback(
                        if (ok) "Custom app logo updated!"
                        else "Could not load that image for the logo."
                    )
                }
            }
        }
    )

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Theme, Logo & Icon Studio",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Exclusive luxury skins, classic palettes, custom logo & zoom bubbles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1A. Flagship EXCLUSIVE Themes Showcase with Shining Animated EXCLUSIVE Text
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Signature Collection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ShiningExclusiveBadge(text = "✦ EXCLUSIVE")
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    exclusiveThemes.forEach { preset ->
                        ExclusiveThemeShowcaseCard(
                            preset = preset,
                            active = preset == appearance.themePreset,
                            onSelect = {
                                DuetAppearanceStore.selectTheme(context, preset)
                                onFeedback("Unlocked ${preset.emoji} ${preset.label} Exclusive theme")
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 1B. Classic Application Themes (synced across app + all 3 widgets)
            Text(
                text = "Classic Themes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            val classicRows = remember(classicThemes) { classicThemes.chunked(2) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                classicRows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { preset ->
                            val active = preset == appearance.themePreset
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .glossyPressEffect(cornerRadius = 18.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(
                                        width = if (active) 2.dp else 1.dp,
                                        color = if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        DuetAppearanceStore.selectTheme(context, preset)
                                        onFeedback("Switched theme to ${preset.emoji} ${preset.label}")
                                    }
                                    .testTag("app_theme_${preset.key.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(preset.swatchPrimary, preset.swatchSecondary)
                                                )
                                            )
                                            .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = preset.emoji)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (preset.isDark) "Dark Mode" else "Light Mode",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (active) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 2. Switchable Android Home Screen Launcher Icons (6 aliases)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Home Screen App Icon",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap any style below to change your phone's real launcher icon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val iconRows = remember { AppIconPreset.entries.chunked(3) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                iconRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { iconPreset ->
                            val selected = iconPreset == appearance.iconPreset
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .glossyPressEffect(cornerRadius = 18.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        val ok = DuetAppearanceStore.selectIconPreset(context, iconPreset)
                                        onFeedback(
                                            if (ok) "App icon changed to ${iconPreset.label}! Home screen updates shortly."
                                            else "Could not switch launcher icon on this device."
                                        )
                                    }
                                    .testTag("launcher_icon_${iconPreset.key.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        AppIconPresetArtwork(
                                            preset = iconPreset,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                        )
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Active icon",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surface)
                                            )
                                        }
                                    }
                                    Text(
                                        text = iconPreset.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 3. Custom User-Uploaded App Logo & Space Name
            Text(
                text = "Custom In-App Logo & Brand Badge",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DuetBrandLogoBadge(
                            appearance = appearance,
                            size = 64.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (appearance.customLogoPath != null) "Custom Couple Photo Logo"
                                else "Using ${appearance.iconPreset.label} Emblem",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Upload any couple photo or custom emblem to personalize your app badge.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !savingLogo,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .glossyPressEffect(cornerRadius = 14.dp)
                                .testTag("upload_custom_logo_button")
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (savingLogo) "Saving…" else "Upload Logo")
                        }
                        if (appearance.customLogoPath != null) {
                            OutlinedButton(
                                onClick = {
                                    DuetAppearanceStore.clearCustomLogo(context)
                                    onFeedback("Restored emblem logo")
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .glossyPressEffect(cornerRadius = 14.dp)
                                    .testTag("reset_custom_logo_button")
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reset")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = brandDraft,
                            onValueChange = { if (it.length <= 24) brandDraft = it },
                            label = { Text("Custom Logo Title (optional)") },
                            placeholder = { Text("e.g. Duet, Our World…") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = {
                                DuetAppearanceStore.setCustomBrandTitle(context, brandDraft)
                                onFeedback("Custom app title saved")
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.glossyPressEffect(cornerRadius = 14.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 4. Floating Optical Zoom Bubbles Effect Toggle
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth()
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
                            imageVector = Icons.Rounded.BubbleChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Rising Magnifying Bubbles",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "3D glass bubbles float bottom to top and zoom whatever passes inside",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = appearance.bubblesEnabled,
                        onCheckedChange = { enabled ->
                            DuetAppearanceStore.setBubblesEnabled(context, enabled)
                            onFeedback(if (enabled) "Rising zoom bubbles enabled" else "Rising zoom bubbles paused")
                        },
                        modifier = Modifier.testTag("toggle_zoom_bubbles_switch")
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Liquid-glass glossy press feedback & floating navigation are always active.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .glossyPressEffect(cornerRadius = 16.dp)
            ) {
                Text("Done")
            }
        }
    }
}

/**
 * Gleaming animated metallic shimmer pill displaying "EXCLUSIVE" in shining moving gradient text.
 */
@Composable
fun ShiningExclusiveBadge(
    text: String = "EXCLUSIVE",
    accent: Color = Color(0xFFFFD86B),
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "exclusive_shimmer")
    val phase by transition.animateFloat(
        initialValue = -140f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "exclusive_shimmer_phase"
    )

    val textBrush = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = 0.78f),
            Color(0xFFFFF8D6),
            Color.White,
            accent,
            accent.copy(alpha = 0.78f)
        ),
        start = Offset(phase - 90f, 0f),
        end = Offset(phase + 90f, 45f)
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = 0.55f),
            Color.White.copy(alpha = 0.92f),
            accent.copy(alpha = 0.65f)
        ),
        start = Offset(phase - 110f, 0f),
        end = Offset(phase + 110f, 40f)
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF141118), Color(0xFF251D13))
                )
            )
            .border(1.2.dp, borderBrush, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                brush = textBrush,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun ExclusiveThemeShowcaseCard(
    preset: AppThemePreset,
    active: Boolean,
    onSelect: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "exclusive_card_${preset.key}")
    val shimmerX by transition.animateFloat(
        initialValue = -220f,
        targetValue = 980f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "exclusive_card_border"
    )
    val glowDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "exclusive_card_glow"
    )

    val borderBrush = Brush.linearGradient(
        colors = if (active) {
            listOf(preset.exclusiveAccent, Color.White, preset.bubbleAccentB, preset.exclusiveAccent)
        } else {
            listOf(
                preset.exclusiveAccent.copy(alpha = 0.65f),
                Color.White.copy(alpha = 0.92f),
                preset.bubbleAccentB.copy(alpha = 0.72f),
                preset.exclusiveAccent.copy(alpha = 0.65f)
            )
        },
        start = Offset(shimmerX - 180f, 0f),
        end = Offset(shimmerX + 180f, 120f)
    )

    val cardBg = if (preset.isDark) {
        Brush.linearGradient(
            colors = preset.heroGradientColors(),
            start = Offset(glowDrift * 140f, 0f),
            end = Offset(480f + glowDrift * 180f, 220f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                preset.colorScheme.background,
                preset.colorScheme.primaryContainer.copy(alpha = 0.88f),
                preset.bubbleAccentB.copy(alpha = 0.58f)
            ),
            start = Offset(glowDrift * 120f, 0f),
            end = Offset(480f + glowDrift * 180f, 220f)
        )
    }

    val movingSheen = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = if (preset.isDark) 0.14f else 0.34f),
            preset.exclusiveAccent.copy(alpha = 0.12f),
            Color.Transparent
        ),
        start = Offset(shimmerX - 150f, 0f),
        end = Offset(shimmerX + 150f, 160f)
    )

    val primaryTextColor = if (preset.isDark) Color(0xFFFFF8EC) else Color(0xFF2B1418)
    val secondaryTextColor = if (preset.isDark) Color(0xFFE8DDCE) else Color(0xFF623F43)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glossyPressEffect(cornerRadius = 22.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .background(movingSheen)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (preset.isDark) 0.16f else 0.42f),
                        Color.White.copy(alpha = if (preset.isDark) 0.04f else 0.12f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = if (active) 2.3.dp else 1.5.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onSelect)
            .testTag("app_theme_${preset.key.lowercase()}")
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Radiant 3-stop luxury emblem orb with live shimmer rim
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(preset.swatchPrimary, preset.exclusiveAccent, preset.bubbleAccentB),
                            start = Offset(glowDrift * 40f, 0f),
                            end = Offset(110f, 110f - glowDrift * 40f)
                        )
                    )
                    .border(1.6.dp, borderBrush, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = preset.emoji, fontSize = 20.sp)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    ShiningExclusiveBadge(
                        text = "EXCLUSIVE",
                        accent = preset.exclusiveAccent
                    )
                }
                Text(
                    text = "${preset.tagline} • ${if (preset.isDark) "Dark" else "Light"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (active) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected Exclusive Theme",
                    tint = preset.exclusiveAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
