package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.LetterSealCatalog
import com.example.data.LoveJarKindCatalog
import com.example.data.LoveJarNote
import com.example.data.LoveLetter
import com.example.data.VibeDedication
import com.example.data.VibeEmojiCatalog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun formatIstDateTime(millis: Long?): String {
    if (millis == null || millis <= 0L) return "Just now"
    val fmt = SimpleDateFormat("MMM d • h:mm a", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }
    return "${fmt.format(Date(millis))} IST"
}

private fun formatRemainingUntil(unlockMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diff = (unlockMillis - nowMillis).coerceAtLeast(0L)
    val totalMins = (diff / 60_000L).coerceAtLeast(1L)
    val days = totalMins / (60L * 24L)
    val hours = (totalMins % (60L * 24L)) / 60L
    val mins = totalMins % 60L
    return when {
        days > 0L -> "Unlocks in ${days}d ${hours}h"
        hours > 0L -> "Unlocks in ${hours}h ${mins}m"
        else -> "Unlocks in ${mins}m"
    }
}

// ============================================================================
// 1. SECRET LOVE LETTERS & TIME CAPSULES SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoveLettersSheet(
    letters: List<LoveLetter>,
    myUid: String,
    partnerName: String,
    isBusy: Boolean,
    onSendLetter: (title: String, body: String, sealStyle: String, unlockAtMillis: Long) -> Unit,
    onOpenLetter: (LoveLetter) -> Unit,
    onDeleteLetter: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var titleDraft by rememberSaveable { mutableStateOf("") }
    var bodyDraft by rememberSaveable { mutableStateOf("") }
    var selectedSeal by rememberSaveable { mutableStateOf(LetterSealCatalog.all.first().key) }
    var unlockModeIndex by rememberSaveable { mutableIntStateOf(0) } // 0=Now, 1=1h, 2=Tomorrow 9AM, 3=Next Week
    var readingLetter by remember { mutableStateOf<LoveLetter?>(null) }

    val unlockModes = remember {
        listOf(
            "Open Now" to "💌",
            "In 1 Hour" to "⏳",
            "Tomorrow 9 AM" to "🌅",
            "Next Week" to "🗓️"
        )
    }

    readingLetter?.let { letter ->
        val seal = LetterSealCatalog.byKey(letter.sealStyle)
        Dialog(onDismissRequest = { readingLetter = null }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .exclusiveAnimatedBorder(cornerRadius = 28.dp, strokeWidth = 1.8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(seal.accentStart, seal.accentEnd)))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${seal.emoji} ${seal.label} Seal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = letter.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { readingLetter = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close letter", tint = Color.White)
                            }
                        }
                    }

                    Text(
                        text = letter.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "From ${if (letter.senderUid == myUid) "You" else letter.senderName.ifBlank { partnerName }}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatIstDateTime(letter.createdAt?.toDate()?.time),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
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
                .testTag("love_letters_sheet"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Secret Love Letters & Capsules",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Seal heartfelt notes with wax seals or future unlock timers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .exclusiveAnimatedBorder(cornerRadius = 22.dp, strokeWidth = 1.4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Write a New Sealed Letter",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = titleDraft,
                            onValueChange = { if (it.length <= 80) titleDraft = it },
                            placeholder = { Text("Envelope title (e.g. Open when you miss me)…") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("love_letter_title_input")
                        )

                        OutlinedTextField(
                            value = bodyDraft,
                            onValueChange = { if (it.length <= 1500) bodyDraft = it },
                            placeholder = { Text("Pour your heart out to $partnerName…") },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("love_letter_body_input")
                        )

                        Text(
                            text = "Choose Wax Seal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LetterSealCatalog.all.forEach { seal ->
                                val active = seal.key == selectedSeal
                                Surface(
                                    shape = CircleShape,
                                    color = if (active) seal.accentStart else MaterialTheme.colorScheme.surface,
                                    contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .border(
                                            width = if (active) 1.5.dp else 1.dp,
                                            color = if (active) Color.White else MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedSeal = seal.key }
                                ) {
                                    Text(
                                        text = "${seal.emoji} ${seal.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Delivery Timing",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            unlockModes.forEachIndexed { idx, (label, emoji) ->
                                val active = unlockModeIndex == idx
                                Surface(
                                    shape = CircleShape,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .clickable { unlockModeIndex = idx }
                                ) {
                                    Text(
                                        text = "$emoji $label",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val unlockAt = when (unlockModeIndex) {
                                    1 -> System.currentTimeMillis() + 3_600_000L
                                    2 -> {
                                        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
                                            add(Calendar.DAY_OF_YEAR, 1)
                                            set(Calendar.HOUR_OF_DAY, 9)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                        }
                                        cal.timeInMillis
                                    }
                                    3 -> System.currentTimeMillis() + 7L * 86_400_000L
                                    else -> 0L
                                }
                                onSendLetter(titleDraft.trim(), bodyDraft.trim(), selectedSeal, unlockAt)
                                titleDraft = ""
                                bodyDraft = ""
                                unlockModeIndex = 0
                            },
                            enabled = titleDraft.trim().isNotEmpty() && bodyDraft.trim().isNotEmpty() && !isBusy,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .glossyPressEffect(cornerRadius = 16.dp)
                                .testTag("love_letter_send_button")
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (unlockModeIndex == 0) "Seal & Send Love Letter" else "Seal Time Capsule")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Your Envelopes (${letters.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (letters.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No sealed letters yet — write a sweet letter or schedule a future time capsule for $partnerName!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(letters, key = { it.id }) { letter ->
                    val seal = LetterSealCatalog.byKey(letter.sealStyle)
                    val isMine = letter.senderUid == myUid
                    val unlocked = letter.isUnlocked()
                    val canRead = isMine || unlocked

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glossyPressEffect(cornerRadius = 22.dp, pressScale = 0.98f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(seal.accentStart, seal.accentEnd)))
                            .exclusiveAnimatedBorder(
                                cornerRadius = 22.dp,
                                strokeWidth = 1.4.dp,
                                fallbackColor = Color.White.copy(alpha = 0.35f),
                                fallbackWidth = 1.dp
                            )
                            .clickable(enabled = canRead) {
                                readingLetter = letter
                                if (!isMine && !letter.openedByPartner) {
                                    onOpenLetter(letter)
                                }
                            }
                            .padding(16.dp)
                            .testTag("love_letter_card_${letter.id}")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = seal.emoji, fontSize = 24.sp)
                                    Column {
                                        Text(
                                            text = letter.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${if (isMine) "Sent by You" else "From ${letter.senderName.ifBlank { partnerName }}"} • ${formatIstDateTime(letter.createdAt?.toDate()?.time)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteLetter(letter.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete letter",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.25f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                !unlocked -> Icons.Rounded.HourglassTop
                                                letter.openedByPartner -> Icons.Rounded.MarkEmailRead
                                                else -> Icons.Rounded.Lock
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = when {
                                                !unlocked -> formatRemainingUntil(letter.unlockAtMillis)
                                                letter.openedByPartner -> "Opened by partner ✓"
                                                isMine -> "Delivered • Sealed"
                                                else -> "Tap to break wax seal"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. SHARED WISH & LOVE JAR SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoveJarSheet(
    notes: List<LoveJarNote>,
    myUid: String,
    partnerName: String,
    isBusy: Boolean,
    onAddNote: (kind: String, text: String) -> Unit,
    onToggleGranted: (LoveJarNote) -> Unit,
    onDeleteNote: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var selectedKind by rememberSaveable { mutableStateOf(LoveJarKindCatalog.all.first().key) }
    var filterKind by rememberSaveable { mutableStateOf("ALL") }
    var pulledNote by remember { mutableStateOf<LoveJarNote?>(null) }

    val filteredNotes = remember(notes, filterKind) {
        if (filterKind == "ALL") notes else notes.filter { it.kind == filterKind }
    }
    val grantedCount = remember(notes) { notes.count { it.granted } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .testTag("love_jar_sheet"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Glowing Jar + Shake-to-Pull Random Note
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF4C1D95), Color(0xFF9D174D), Color(0xFFD97706))
                            )
                        )
                        .exclusiveAnimatedBorder(cornerRadius = 24.dp, strokeWidth = 1.6.dp)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🫙 Shared Wish & Love Jar",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${notes.size} notes inside • $grantedCount wishes granted",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Button(
                                onClick = {
                                    if (notes.isNotEmpty()) {
                                        val pool = if (notes.size > 1) notes.filterNot { it.id == pulledNote?.id } else notes
                                        pulledNote = pool.random()
                                    }
                                },
                                enabled = notes.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.22f),
                                    contentColor = Color.White
                                ),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .glossyPressEffect(cornerRadius = 20.dp)
                                    .testTag("love_jar_shake_button")
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Pull Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        AnimatedVisibility(visible = pulledNote != null, enter = fadeIn() + scaleIn()) {
                            pulledNote?.let { picked ->
                                val kind = LoveJarKindCatalog.byKey(picked.kind)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.18f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${kind.emoji} Pulled from Jar • ${picked.authorName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFF3C4)
                                        )
                                        Text(
                                            text = "“${picked.text}”",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontStyle = FontStyle.Italic,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add Note Composer
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .exclusiveAnimatedBorder(cornerRadius = 20.dp, strokeWidth = 1.3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LoveJarKindCatalog.all.forEach { kind ->
                                val active = selectedKind == kind.key
                                Surface(
                                    shape = CircleShape,
                                    color = if (active) kind.tint else MaterialTheme.colorScheme.surface,
                                    contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .clickable { selectedKind = kind.key }
                                ) {
                                    Text(
                                        text = "${kind.emoji} ${kind.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = noteDraft,
                                onValueChange = { if (it.length <= 240) noteDraft = it },
                                placeholder = { Text("Drop a wish, compliment, or memory…") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("love_jar_input")
                            )
                            Button(
                                onClick = {
                                    onAddNote(selectedKind, noteDraft.trim())
                                    noteDraft = ""
                                },
                                enabled = noteDraft.trim().isNotEmpty() && !isBusy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .glossyPressEffect(cornerRadius = 14.dp)
                                    .testTag("love_jar_add_button")
                            ) {
                                Text("Drop")
                            }
                        }
                    }
                }
            }

            // Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("ALL" to "✨ All (${notes.size})") +
                        LoveJarKindCatalog.all.map { it.key to "${it.emoji} ${it.label}" }
                    filters.forEach { (key, label) ->
                        val active = filterKind == key
                        Surface(
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { filterKind = key }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            if (filteredNotes.isEmpty()) {
                item {
                    Text(
                        text = "Your Love Jar is waiting for its first sweet note for $partnerName!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(filteredNotes, key = { it.id }) { note ->
                    val kind = LoveJarKindCatalog.byKey(note.kind)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .exclusiveAnimatedBorder(cornerRadius = 18.dp, strokeWidth = 1.2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${kind.emoji} ${kind.label} • ${if (note.authorUid == myUid) "You" else note.authorName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = kind.tint
                                )
                                IconButton(
                                    onClick = { onDeleteNote(note.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete jar note",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = note.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatIstDateTime(note.createdAt?.toDate()?.time),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FilledTonalButton(
                                    onClick = { onToggleGranted(note) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = CircleShape,
                                    modifier = Modifier.glossyPressEffect(cornerRadius = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = if (note.granted) Icons.Rounded.CheckCircle else Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = if (note.granted) "Granted 🌠" else "Grant Wish",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. SHARED MUSIC & VIBE DEDICATION SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongVibeDedicationSheet(
    dedications: List<VibeDedication>,
    myUid: String,
    partnerName: String,
    isBusy: Boolean,
    onDedicateSong: (songTitle: String, artistName: String, vibeEmoji: String, note: String) -> Unit,
    onToggleLoved: (VibeDedication) -> Unit,
    onDeleteDedication: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var songDraft by rememberSaveable { mutableStateOf("") }
    var artistDraft by rememberSaveable { mutableStateOf("") }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var selectedEmoji by rememberSaveable { mutableStateOf(VibeEmojiCatalog.emojis.first()) }

    val featured = dedications.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .testTag("song_vibe_sheet"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Song & Vibe Dedications",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dedicate songs that remind you of $partnerName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Now Dedicated Vinyl Spotlight
            if (featured != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF312E81), Color(0xFF831843))
                                )
                            )
                            .exclusiveAnimatedBorder(cornerRadius = 24.dp, strokeWidth = 1.6.dp)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .border(2.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = featured.vibeEmoji, fontSize = 26.sp)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "NOW DEDICATED • ${if (featured.senderUid == myUid) "BY YOU" else "BY ${featured.senderName.uppercase()}"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = featured.songTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = featured.artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                if (featured.dedicationNote.isNotBlank()) {
                                    Text(
                                        text = "“${featured.dedicationNote}”",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontStyle = FontStyle.Italic,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dedicate Song Composer
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .exclusiveAnimatedBorder(cornerRadius = 20.dp, strokeWidth = 1.3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = songDraft,
                            onValueChange = { if (it.length <= 100) songDraft = it },
                            placeholder = { Text("Song title (e.g. Kesariya, Perfect)…") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vibe_song_input")
                        )
                        OutlinedTextField(
                            value = artistDraft,
                            onValueChange = { if (it.length <= 80) artistDraft = it },
                            placeholder = { Text("Artist / Singer") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vibe_artist_input")
                        )
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { if (it.length <= 220) noteDraft = it },
                            placeholder = { Text("Why this song reminds you of $partnerName…") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VibeEmojiCatalog.emojis.forEach { emoji ->
                                val active = selectedEmoji == emoji
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = if (active) 2.dp else 1.dp,
                                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedEmoji = emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                        Button(
                            onClick = {
                                onDedicateSong(songDraft.trim(), artistDraft.trim(), selectedEmoji, noteDraft.trim())
                                songDraft = ""
                                artistDraft = ""
                                noteDraft = ""
                            },
                            enabled = songDraft.trim().isNotEmpty() && !isBusy,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .glossyPressEffect(cornerRadius = 16.dp)
                                .testTag("vibe_dedicate_button")
                        ) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Dedicate Song to $partnerName")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Our Soundtrack (${dedications.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(dedications, key = { it.id }) { vibe ->
                val isMine = vibe.senderUid == myUid
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .exclusiveAnimatedBorder(cornerRadius = 18.dp, strokeWidth = 1.2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = vibe.vibeEmoji, fontSize = 26.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vibe.songTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${vibe.artistName} • ${if (isMine) "You" else vibe.senderName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (vibe.dedicationNote.isNotBlank()) {
                                Text(
                                    text = vibe.dedicationNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = formatIstDateTime(vibe.createdAt?.toDate()?.time),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        if (!isMine) {
                            IconButton(onClick = { onToggleLoved(vibe) }) {
                                Icon(
                                    imageVector = if (vibe.lovedByPartner) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Love song dedication",
                                    tint = if (vibe.lovedByPartner) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (vibe.lovedByPartner) {
                            Text(text = "❤️", fontSize = 18.sp)
                        }
                        IconButton(
                            onClick = { onDeleteDedication(vibe.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete song dedication",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
