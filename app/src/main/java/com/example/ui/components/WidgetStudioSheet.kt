package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.widget.DuetCountdownWidgetProvider
import com.example.widget.DuetCoupleHubWidgetProvider
import com.example.widget.DuetLoveWidgetProvider
import com.example.widget.DuetMoodWidgetProvider
import com.example.widget.DuetQuestionWidgetProvider
import com.example.widget.DuetQuickChatWidgetProvider
import com.example.widget.DuetRadarWidgetProvider
import com.example.widget.DuetThemeWidgetProvider
import com.example.widget.WidgetStateStore
import com.example.widget.WidgetThemeStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetStudioSheet(
    coupleTitle: String,
    partnerName: String,
    batteryBadge: String,
    moodLine: String,
    countdownLine: String,
    statusFooter: String,
    lastMsgPreview: String,
    selectedTheme: WidgetThemeStyle,
    onSelectTheme: (WidgetThemeStyle) -> Unit,
    onPinResult: (widgetName: String, pinned: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snapshot = remember(selectedTheme, coupleTitle, partnerName, batteryBadge, moodLine) {
        WidgetStateStore(context).readSnapshot()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .testTag("widget_studio_sheet"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "8 Interactive Home Widgets",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pin private chat, themes, countdowns, mood check-in, daily question, live radar & couple hub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Shared Theme Selector synced across App + all 8 Widgets
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Synced App & Widget Theme",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WidgetThemeStyle.entries.forEach { style ->
                            val active = style == selectedTheme
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (active) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .glossyPressEffect(cornerRadius = 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (active) 2.dp else 1.dp,
                                        color = if (active) MaterialTheme.colorScheme.primary
                                        else if (style.isExclusive) Color(0xFFE5BD5F).copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onSelectTheme(style) }
                                    .testTag("widget_theme_${style.key.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = style.emoji)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = style.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (style.isExclusive) {
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = "✦",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE5BD5F)
                                        )
                                    }
                                    if (active) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // WIDGET 1: Private Quick-Chat Widget
            item {
                WidgetStudioItemBlock(
                    icon = Icons.Rounded.Forum,
                    title = "1. Private Quick-Chat Widget",
                    description = "See latest encrypted messages, send 1-tap Miss You / Kiss / Hug notes, or type a private reply straight from your home screen.",
                    buttonLabel = "Add Private Chat Widget",
                    buttonTestTag = "pin_quick_chat_widget_button",
                    isPrimary = true,
                    onPinClick = {
                        val pinned = DuetQuickChatWidgetProvider.requestPinWidget(context)
                        onPinResult("Private Quick-Chat Widget", pinned)
                    }
                ) {
                    QuickChatWidgetPreviewCard(
                        partnerName = partnerName,
                        lastMsg = lastMsgPreview,
                        themeStyle = selectedTheme
                    )
                }
            }

            // WIDGET 2: Theme & Pulse Switcher Widget
            item {
                WidgetStudioItemBlock(
                    icon = Icons.Rounded.Palette,
                    title = "2. Theme & Pulse Switcher Widget",
                    description = "Change Duet's application theme live from your home screen with 1 tap and send an instant Love Pulse.",
                    buttonLabel = "Add Theme Switcher Widget",
                    buttonTestTag = "pin_theme_widget_button",
                    onPinClick = {
                        val pinned = DuetThemeWidgetProvider.requestPinWidget(context)
                        onPinResult("Theme Switcher Widget", pinned)
                    }
                ) {
                    ThemeSwitcherWidgetPreviewCard(themeStyle = selectedTheme)
                }
            }

            // WIDGET 3: Live Couple Status & Countdown Widget
            item {
                WidgetStudioItemBlock(
                    icon = Icons.Rounded.Widgets,
                    title = "3. Live Couple Status Widget",
                    description = "Shows partner battery %, live mood, active app status, and upcoming countdown together.",
                    buttonLabel = "Add Love Status Widget",
                    buttonTestTag = "pin_widget_home_button",
                    onPinClick = {
                        val pinned = DuetLoveWidgetProvider.requestPinWidget(context)
                        onPinResult("Duet Love Status Widget", pinned)
                    }
                ) {
                    CuteWidgetPreviewCard(
                        coupleTitle = coupleTitle,
                        batteryBadge = batteryBadge,
                        moodLine = moodLine,
                        countdownLine = countdownLine,
                        statusFooter = statusFooter,
                        themeStyle = selectedTheme
                    )
                }
            }

            // WIDGET 4: Days Together & Anniversary/Birthday Countdown Widget
            item {
                WidgetStudioItemBlock(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "4. Days Together & Countdown Widget",
                    description = "Celebrate your exact Days Together alongside live Anniversary and Partner Birthday countdowns.",
                    buttonLabel = "Add Countdown Widget",
                    buttonTestTag = "pin_countdown_widget_button",
                    onPinClick = {
                        val pinned = DuetCountdownWidgetProvider.requestPinWidget(context)
                        onPinResult("Days Together Countdown Widget", pinned)
                    }
                ) {
                    CountdownWidgetPreviewCard(
                        coupleTitle = coupleTitle,
                        daysTogether = snapshot.daysTogetherLabel,
                        anniversaryCd = snapshot.anniversaryCountdown,
                        birthdayCd = snapshot.birthdayCountdown,
                        themeStyle = selectedTheme
                    )
                }
            }

            // WIDGET 5: Quick Mood Check-In Widget
            item {
                WidgetStudioItemBlock(
                    icon = Icons.Rounded.Mood,
                    title = "5. Quick Mood Check-In Widget",
                    description = "See live mood and broadcast 🥰 In Love, 🥺 Miss U, ☕ Cozy, or 🫂 Hugs in 1 tap right from your home screen.",
                    buttonLabel = "Add Quick Mood Widget",
                    buttonTestTag = "pin_mood_widget_button",
                    onPinClick = {
                        val pinned = DuetMoodWidgetProvider.requestPinWidget(context)
                        onPinResult("Quick Mood Check-In Widget", pinned)
                    }
                ) {
                    MoodWidgetPreviewCard(
                        moodLine = moodLine,
                        themeStyle = selectedTheme
                    )
                }
            }

            // WIDGET 6: Today's Question & Expiry Widget
            item {
                WidgetStudioItemBlock(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "6. Today's Question Widget",
                    description = "Shows Today's Question for Two with live unlock status and 12:00 AM IST expiry countdown.",
                    buttonLabel = "Add Daily Question Widget",
                    buttonTestTag = "pin_question_widget_button",
                    onPinClick = {
                        val pinned = DuetQuestionWidgetProvider.requestPinWidget(context)
                        onPinResult("Today's Question Widget", pinned)
                    }
                ) {
                    QuestionWidgetPreviewCard(
                        question = snapshot.dailyQuestionText,
                        status = snapshot.dailyQuestionStatus,
                        expiry = snapshot.dailyQuestionExpiry,
                        themeStyle = selectedTheme
                    )
                }
            }

            // WIDGET 7: Partner Battery & Active App Radar Widget
            item {
                WidgetStudioItemBlock(
                    icon = Icons.Rounded.Sensors,
                    title = "7. Partner Live Radar Widget",
                    description = "Track partner battery, network, logged-in phone & active app with 1-tap Care Nudge and Love Pulse buttons.",
                    buttonLabel = "Add Partner Radar Widget",
                    buttonTestTag = "pin_radar_widget_button",
                    onPinClick = {
                        val pinned = DuetRadarWidgetProvider.requestPinWidget(context)
                        onPinResult("Partner Live Radar Widget", pinned)
                    }
                ) {
                    RadarWidgetPreviewCard(
                        partnerName = partnerName,
                        batteryBadge = batteryBadge,
                        activeApp = snapshot.partnerActiveApp,
                        network = snapshot.partnerNetwork,
                        deviceName = snapshot.partnerDeviceName,
                        themeStyle = selectedTheme
                    )
                }
            }

            // WIDGET 8: Couple Care, Love Jar & Song Vibe Hub Widget
            item {
                WidgetStudioItemBlock(
                    icon = Icons.Rounded.Favorite,
                    title = "8. Couple Care & Love Hub Widget",
                    description = "Displays your latest Love Jar wish, Song Dedication, Bucket progress & 1-tap Hydrate / Pulse actions.",
                    buttonLabel = "Add Couple Hub Widget",
                    buttonTestTag = "pin_hub_widget_button",
                    onPinClick = {
                        val pinned = DuetCoupleHubWidgetProvider.requestPinWidget(context)
                        onPinResult("Couple Care & Love Hub Widget", pinned)
                    }
                ) {
                    CoupleHubWidgetPreviewCard(
                        coupleTitle = coupleTitle,
                        bucketSummary = snapshot.bucketSummary,
                        jarHighlight = snapshot.loveJarHighlight,
                        songHighlight = snapshot.songVibeHighlight,
                        themeStyle = selectedTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetStudioItemBlock(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    buttonTestTag: String,
    isPrimary: Boolean = false,
    onPinClick: () -> Unit,
    preview: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        preview()
        if (isPrimary) {
            Button(
                onClick = onPinClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .glossyPressEffect(cornerRadius = 16.dp)
                    .testTag(buttonTestTag)
            ) {
                Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(buttonLabel)
            }
        } else {
            FilledTonalButton(
                onClick = onPinClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .glossyPressEffect(cornerRadius = 16.dp)
                    .testTag(buttonTestTag)
            ) {
                Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(buttonLabel)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    }
}

private data class WidgetPreviewColors(
    val gradient: List<Color>,
    val border: Color,
    val title: Color,
    val body: Color,
    val sub: Color,
    val chipBg: Color,
    val chipText: Color
)

private fun previewColorsFor(style: WidgetThemeStyle): WidgetPreviewColors = when (style) {
    WidgetThemeStyle.ROSE -> WidgetPreviewColors(
        gradient = listOf(Color(0xFFFFF4EE), Color(0xFFFCE4DC), Color(0xFFF7D5D2)),
        border = Color(0xFFE7B7B5),
        title = Color(0xFF3A1624),
        body = Color(0xFF622B3E),
        sub = Color(0xFF855262),
        chipBg = Color(0x33A83E53),
        chipText = Color(0xFF8E2D45)
    )
    WidgetThemeStyle.VELVET -> WidgetPreviewColors(
        gradient = listOf(Color(0xFF2A121D), Color(0xFF3C192A), Color(0xFF4E2034)),
        border = Color(0xFF6E354E),
        title = Color(0xFFFFF2F4),
        body = Color(0xFFF3C4D0),
        sub = Color(0xFFD5A4B3),
        chipBg = Color(0x33FFFFFF),
        chipText = Color(0xFFFFD9DF)
    )
    WidgetThemeStyle.MIDNIGHT -> WidgetPreviewColors(
        gradient = listOf(Color(0xFF0C182D), Color(0xFF152A4A), Color(0xFF1F3E6B)),
        border = Color(0xFF426A9C),
        title = Color(0xFFECF7FF),
        body = Color(0xFFBBE4FF),
        sub = Color(0xFF95BFE0),
        chipBg = Color(0x2E76D1FF),
        chipText = Color(0xFFE4F5FF)
    )
    WidgetThemeStyle.LAVENDER -> WidgetPreviewColors(
        gradient = listOf(Color(0xFFF8F2FF), Color(0xFFEDE1FF), Color(0xFFE3D0FF)),
        border = Color(0xFFCBB1F2),
        title = Color(0xFF2A164B),
        body = Color(0xFF4E2E7E),
        sub = Color(0xFF6D5196),
        chipBg = Color(0x296D49B5),
        chipText = Color(0xFF5A3299)
    )
    WidgetThemeStyle.PEACH -> WidgetPreviewColors(
        gradient = listOf(Color(0xFFFFF8EB), Color(0xFFFCE9D4), Color(0xFFF8D8C2)),
        border = Color(0xFFE5BE9E),
        title = Color(0xFF3E2114),
        body = Color(0xFF6B3A24),
        sub = Color(0xFF8A5B44),
        chipBg = Color(0x2EC4552B),
        chipText = Color(0xFF7D3F1E)
    )
    WidgetThemeStyle.EMERALD -> WidgetPreviewColors(
        gradient = listOf(Color(0xFF0B1F1B), Color(0xFF12342C), Color(0xFF1B4B3F)),
        border = Color(0xFF3B7566),
        title = Color(0xFFEAFFF7),
        body = Color(0xFFB7EEDB),
        sub = Color(0xFF8EC8B5),
        chipBg = Color(0x2E6CD9B4),
        chipText = Color(0xFFDCFFF3)
    )
    WidgetThemeStyle.SAKURA -> WidgetPreviewColors(
        gradient = listOf(Color(0xFFFFF7FA), Color(0xFFFCE4EE), Color(0xFFF7CEE0)),
        border = Color(0xFFE59AB8),
        title = Color(0xFF421528),
        body = Color(0xFF702847),
        sub = Color(0xFF8F4E69),
        chipBg = Color(0x2ECC4478),
        chipText = Color(0xFF9C2252)
    )
    WidgetThemeStyle.ROYAL_GOLD -> WidgetPreviewColors(
        gradient = listOf(Color(0xFF0A090D), Color(0xFF1C1509), Color(0xFF38280E)),
        border = Color(0xFFD4A647),
        title = Color(0xFFFFF2CF),
        body = Color(0xFFF4D07A),
        sub = Color(0xFFCBB17A),
        chipBg = Color(0x33F5CA6E),
        chipText = Color(0xFFFFE59D)
    )
    WidgetThemeStyle.AURORA -> WidgetPreviewColors(
        gradient = listOf(Color(0xFF070B1E), Color(0xFF13264F), Color(0xFF34144F)),
        border = Color(0xFF4DF3FF),
        title = Color(0xFFECFBFF),
        body = Color(0xFFB6F6FF),
        sub = Color(0xFFC9B4F7),
        chipBg = Color(0x2E4DF3FF),
        chipText = Color(0xFF8DF8FF)
    )
    WidgetThemeStyle.AMETHYST -> WidgetPreviewColors(
        gradient = listOf(Color(0xFF10081F), Color(0xFF281245), Color(0xFF4E1A4D)),
        border = Color(0xFFC98BFF),
        title = Color(0xFFFBF1FF),
        body = Color(0xFFE5C4FF),
        sub = Color(0xFFD8A8D8),
        chipBg = Color(0x33D08CFF),
        chipText = Color(0xFFF0D2FF)
    )
    WidgetThemeStyle.CHAMPAGNE -> WidgetPreviewColors(
        gradient = listOf(Color(0xFFFDF9F0), Color(0xFFF7ECD8), Color(0xFFEFE0C2)),
        border = Color(0xFFC8A252),
        title = Color(0xFF2D200E),
        body = Color(0xFF5A411E),
        sub = Color(0xFF7D633F),
        chipBg = Color(0x2E9E742A),
        chipText = Color(0xFF6B4B12)
    )
    WidgetThemeStyle.SOLARIS -> WidgetPreviewColors(
        gradient = listOf(Color(0xFFFFF2EE), Color(0xFFFFDCD3), Color(0xFFFEC7B4)),
        border = Color(0xFFEB8A72),
        title = Color(0xFF3E1524),
        body = Color(0xFF74293F),
        sub = Color(0xFF994D5C),
        chipBg = Color(0x2EC84664),
        chipText = Color(0xFFA32A4B)
    )
    WidgetThemeStyle.SAPPHIRE -> WidgetPreviewColors(
        gradient = listOf(Color(0xFF061129), Color(0xFF0F2D63), Color(0xFF1F639E)),
        border = Color(0xFF7CE7FF),
        title = Color(0xFFEDF7FF),
        body = Color(0xFFBDE8FF),
        sub = Color(0xFF96C4E8),
        chipBg = Color(0x337CE7FF),
        chipText = Color(0xFF8CEAFF)
    )
    WidgetThemeStyle.RUBY -> WidgetPreviewColors(
        gradient = listOf(Color(0xFF1A040C), Color(0xFF520C22), Color(0xFF94203F)),
        border = Color(0xFFFF7A9A),
        title = Color(0xFFFFF0F4),
        body = Color(0xFFFFC2D2),
        sub = Color(0xFFE69DB2),
        chipBg = Color(0x33FF7A9A),
        chipText = Color(0xFFFF99B5)
    )
    WidgetThemeStyle.LOTUS -> WidgetPreviewColors(
        gradient = listOf(Color(0xFF120621), Color(0xFF471259), Color(0xFF962A74)),
        border = Color(0xFFFF8CE0),
        title = Color(0xFFFFF0FC),
        body = Color(0xFFFBC2F0),
        sub = Color(0xFFE4A6DB),
        chipBg = Color(0x33FF8CE0),
        chipText = Color(0xFFFFA8EA)
    )
}

@Composable
fun CuteWidgetPreviewCard(
    coupleTitle: String,
    batteryBadge: String,
    moodLine: String,
    countdownLine: String,
    statusFooter: String,
    themeStyle: WidgetThemeStyle,
    modifier: Modifier = Modifier
) {
    val palette = previewColorsFor(themeStyle)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(palette.gradient))
            .exclusiveAnimatedBorder(
                cornerRadius = 24.dp,
                strokeWidth = 1.6.dp,
                fallbackColor = palette.border,
                fallbackWidth = 1.5.dp
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = coupleTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.chipBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = batteryBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.chipText
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = moodLine,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.body
            )
            Text(
                text = countdownLine,
                style = MaterialTheme.typography.bodySmall,
                color = palette.sub
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = statusFooter,
                style = MaterialTheme.typography.labelSmall,
                color = palette.sub
            )
        }
    }
}

@Composable
private fun QuickChatWidgetPreviewCard(
    partnerName: String,
    lastMsg: String,
    themeStyle: WidgetThemeStyle
) {
    val palette = previewColorsFor(themeStyle)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(palette.gradient))
            .exclusiveAnimatedBorder(
                cornerRadius = 24.dp,
                strokeWidth = 1.6.dp,
                fallbackColor = palette.border,
                fallbackWidth = 1.5.dp
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = partnerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.title
                    )
                    Text(
                        text = "🔒 End-to-end encrypted",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.sub
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.chipBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Open",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.chipText
                    )
                }
            }

            Text(
                text = lastMsg,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("❤️ Miss you", "😘 Kiss", "🫂 Hug").forEach { pill ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(palette.chipBg)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pill,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.chipText,
                            maxLines = 1
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .border(1.dp, palette.border.copy(alpha = 0.6f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "✉️ Reply privately from widget…",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.title
                )
            }
        }
    }
}

@Composable
private fun ThemeSwitcherWidgetPreviewCard(themeStyle: WidgetThemeStyle) {
    val palette = previewColorsFor(themeStyle)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(palette.gradient))
            .exclusiveAnimatedBorder(
                cornerRadius = 24.dp,
                strokeWidth = 1.6.dp,
                fallbackColor = palette.border,
                fallbackWidth = 1.5.dp
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Duet Theme Studio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.title
                    )
                    Text(
                        text = "Active: ${themeStyle.emoji} ${themeStyle.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.sub
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.chipBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "❤️ Pulse",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.chipText
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("🌸 Rose", "🍷 Velvet", "🌊 Ocean").forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(palette.chipBg)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.chipText
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("💜 Lilac", "🌿 Forest", "✨ Next").forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(palette.chipBg)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.chipText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownWidgetPreviewCard(
    coupleTitle: String,
    daysTogether: String,
    anniversaryCd: String,
    birthdayCd: String,
    themeStyle: WidgetThemeStyle
) {
    val palette = previewColorsFor(themeStyle)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(palette.gradient))
            .exclusiveAnimatedBorder(24.dp, 1.6.dp, palette.border, 1.5.dp)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = coupleTitle, style = MaterialTheme.typography.labelMedium, color = palette.sub)
            Text(
                text = daysTogether,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = palette.title
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("💍 Anniversary" to anniversaryCd, "🎂 Partner B-Day" to birthdayCd).forEach { (lbl, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.chipBg)
                            .padding(10.dp)
                    ) {
                        Text(text = lbl, style = MaterialTheme.typography.labelSmall, color = palette.sub)
                        Text(text = value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.chipText)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodWidgetPreviewCard(
    moodLine: String,
    themeStyle: WidgetThemeStyle
) {
    val palette = previewColorsFor(themeStyle)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(palette.gradient))
            .exclusiveAnimatedBorder(24.dp, 1.6.dp, palette.border, 1.5.dp)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Quick Mood Check-In", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.title)
            Text(text = moodLine, style = MaterialTheme.typography.labelSmall, color = palette.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("🥰 Love", "🥺 Miss U", "☕ Cozy", "🫂 Hugs").forEach { chip ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(palette.chipBg)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = chip, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.chipText, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionWidgetPreviewCard(
    question: String,
    status: String,
    expiry: String,
    themeStyle: WidgetThemeStyle
) {
    val palette = previewColorsFor(themeStyle)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(palette.gradient))
            .exclusiveAnimatedBorder(24.dp, 1.6.dp, palette.border, 1.5.dp)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "✨ Today's Question", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.sub)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.chipBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = expiry, style = MaterialTheme.typography.labelSmall, color = palette.chipText)
                }
            }
            Text(
                text = question,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = palette.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = status, style = MaterialTheme.typography.labelSmall, color = palette.body)
        }
    }
}

@Composable
private fun RadarWidgetPreviewCard(
    partnerName: String,
    batteryBadge: String,
    activeApp: String,
    network: String,
    deviceName: String,
    themeStyle: WidgetThemeStyle
) {
    val palette = previewColorsFor(themeStyle)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(palette.gradient))
            .exclusiveAnimatedBorder(24.dp, 1.6.dp, palette.border, 1.5.dp)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "$partnerName • Radar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.title)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.chipBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = "🔋 $batteryBadge", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.chipText)
                }
            }
            Text(text = activeApp, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = palette.body)
            Text(text = "📶 $network • 📱 $deviceName", style = MaterialTheme.typography.labelSmall, color = palette.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("💧 Care Nudge", "❤️ Send Pulse").forEach { btn ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(palette.chipBg)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = btn, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.chipText)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoupleHubWidgetPreviewCard(
    coupleTitle: String,
    bucketSummary: String,
    jarHighlight: String,
    songHighlight: String,
    themeStyle: WidgetThemeStyle
) {
    val palette = previewColorsFor(themeStyle)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(palette.gradient))
            .exclusiveAnimatedBorder(24.dp, 1.6.dp, palette.border, 1.5.dp)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = coupleTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.title)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.chipBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = "🌟 $bucketSummary", style = MaterialTheme.typography.labelSmall, color = palette.chipText)
                }
            }
            Text(text = jarHighlight, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = palette.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = songHighlight, style = MaterialTheme.typography.labelSmall, color = palette.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("💧 Hydrate", "❤️ Pulse", "💌 Open").forEach { btn ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(palette.chipBg)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = btn, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.chipText)
                    }
                }
            }
        }
    }
}
