package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IndiaTimeFormatter
import com.example.data.MoodCatalog
import com.example.data.MoodEntry
import com.example.ui.theme.LocalDuetThemePreset
import java.util.Locale

private enum class MoodHistoryFilter(val label: String, val tag: String) {
    ALL("All", "mood_filter_all"),
    PARTNER("Partner", "mood_filter_partner"),
    MINE("You", "mood_filter_mine")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodHistorySheet(
    moods: List<MoodEntry>,
    myUid: String,
    partnerName: String,
    onSendComfortHug: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeTheme = LocalDuetThemePreset.current
    var activeFilter by remember { mutableStateOf(MoodHistoryFilter.ALL) }

    val filteredMoods = remember(moods, activeFilter, myUid) {
        when (activeFilter) {
            MoodHistoryFilter.ALL -> moods
            MoodHistoryFilter.PARTNER -> moods.filter { it.userId != myUid }
            MoodHistoryFilter.MINE -> moods.filter { it.userId == myUid }
        }
    }

    val groupedByDay = remember(filteredMoods) {
        val buckets = LinkedHashMap<String, MutableList<MoodEntry>>()
        filteredMoods.forEach { entry ->
            val label = IndiaTimeFormatter.formatDayHeader(entry.createdAt?.toDate())
            buckets.getOrPut(label) { mutableListOf() }.add(entry)
        }
        buckets.entries.toList()
    }

    val topMood = remember(filteredMoods) {
        filteredMoods.groupingBy { it.moodKey }.eachCount().maxByOrNull { it.value }?.key
            ?.let { MoodCatalog.byKey(it) }
    }
    val hugsCount = remember(filteredMoods) {
        filteredMoods.count { !it.comfortSentByUid.isNullOrBlank() }
    }
    val avgEnergy = remember(filteredMoods) {
        if (filteredMoods.isEmpty()) 0.0
        else filteredMoods.map { it.energyLevel.coerceIn(1L, 5L) }.average()
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
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .testTag("mood_history_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mood History & Insights",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Every emotional check-in shared between you & $partnerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Radiant Mood Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(activeTheme.heroGradientColors()))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoodStatColumn(
                        value = "${filteredMoods.size}",
                        label = "Check-ins"
                    )
                    MoodStatColumn(
                        value = topMood?.let { "${it.emoji} ${it.label}" } ?: "—",
                        label = "Top Mood"
                    )
                    MoodStatColumn(
                        value = if (filteredMoods.isEmpty()) "—" else String.format(Locale.US, "%.1f/5", avgEnergy),
                        label = "Avg Energy"
                    )
                    MoodStatColumn(
                        value = "🫂 $hugsCount",
                        label = "Hugs"
                    )
                }
            }

            // Filter Chips (All / Partner / You)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MoodHistoryFilter.entries.forEach { filter ->
                    val selected = filter == activeFilter
                    val displayLabel = if (filter == MoodHistoryFilter.PARTNER) {
                        partnerName.substringBefore(" ")
                    } else {
                        filter.label
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .glossyPressEffect(cornerRadius = 21.dp)
                            .clip(CircleShape)
                            .clickable { activeFilter = filter }
                            .testTag(filter.tag)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = displayLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Timeline List
            if (filteredMoods.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoGraph,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No mood check-ins here yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Share how you feel on Home to build your couple mood journal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedByDay.forEach { (dayLabel, entries) ->
                        item(key = "header_$dayLabel") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                                ) {
                                    Text(
                                        text = dayLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "${entries.size} ${if (entries.size == 1) "entry" else "entries"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(entries, key = { it.id }) { entry ->
                            MoodTimelineCard(
                                entry = entry,
                                isMine = entry.userId == myUid,
                                partnerName = partnerName,
                                onSendHug = { onSendComfortHug(entry.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodStatColumn(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun MoodTimelineCard(
    entry: MoodEntry,
    isMine: Boolean,
    partnerName: String,
    onSendHug: () -> Unit
) {
    val mood = MoodCatalog.byKey(entry.moodKey)
    val timeText = IndiaTimeFormatter.formatTime(entry.createdAt?.toDate()).ifBlank { "Just now" }
    val authorLabel = if (isMine) "You" else entry.authorName.ifBlank { partnerName }
    val energy = entry.energyLevel.coerceIn(1L, 5L).toInt()
    val hugged = !entry.comfortSentByUid.isNullOrBlank()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = mood.accentColor.copy(alpha = 0.32f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(mood.accentColor.copy(alpha = 0.18f))
                    .border(1.5.dp, mood.accentColor.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = mood.emoji, fontSize = 22.sp)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = authorLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "• ${mood.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = mood.accentColor
                    )
                }

                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Energy level indicator dots
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(5) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (idx < energy) mood.accentColor
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                    if (hugged) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = mood.accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Hugged",
                                style = MaterialTheme.typography.labelSmall,
                                color = mood.accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (!isMine && !hugged) {
                FilledTonalButton(
                    onClick = onSendHug,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .glossyPressEffect(cornerRadius = 14.dp)
                        .testTag("mood_history_hug_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolunteerActivism,
                        contentDescription = "Send warm hug",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Hug", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
