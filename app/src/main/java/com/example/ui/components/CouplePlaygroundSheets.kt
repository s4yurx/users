package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Style
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BucketListCatalog
import com.example.data.BucketListItem
import com.example.data.CoupleCardKind
import com.example.data.DateIdeaOption
import com.example.data.DateSpinnerCatalog
import com.example.data.LoveCouponCatalog
import com.example.data.TruthOrLoveCatalog
import com.example.ui.theme.LocalDuetThemePreset
import kotlin.random.Random
import kotlinx.coroutines.launch

/**
 * 1. Shared Couple Bucket List Sheet — synced in real time via Firestore `/couples/{coupleId}/bucket_items`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketListSheet(
    items: List<BucketListItem>,
    partnerName: String,
    isBusy: Boolean,
    onAddItem: (title: String, category: String) -> Unit,
    onToggleCompleted: (BucketListItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeTheme = LocalDuetThemePreset.current
    var draftTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("DATE_NIGHT") }
    var filterCategory by remember { mutableStateOf<String?>(null) }

    val completedCount = remember(items) { items.count { it.completed } }
    val filteredItems = remember(items, filterCategory) {
        if (filterCategory == null) items else items.filter { it.category == filterCategory }
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
                .testTag("bucket_list_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
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
                        imageVector = Icons.Rounded.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Couple Bucket List",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dreams & adventures synced live with $partnerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Unified smooth-scrolling LazyColumn so small screens & open keyboards never squeeze list items
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("bucket_list_lazy_column"),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Progress Hero Pill
                item(key = "bucket_hero") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(activeTheme.heroGradientColors()))
                            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (items.isEmpty()) "Start your shared wish list"
                                    else "$completedCount of ${items.size} Dreams Completed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap any dream to mark it done together ✨",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                            Text(text = "🌟", fontSize = 24.sp)
                        }
                    }
                }

                // Quick-Add Composer + Category Picker
                item(key = "bucket_composer") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draftTitle,
                                onValueChange = { if (it.length <= 120) draftTitle = it },
                                placeholder = { Text("Add a dream or date idea…") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bucket_list_input")
                            )
                            Button(
                                onClick = {
                                    if (draftTitle.trim().isNotEmpty()) {
                                        onAddItem(draftTitle.trim(), selectedCategory)
                                        draftTitle = ""
                                    }
                                },
                                enabled = draftTitle.trim().isNotEmpty() && !isBusy,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .glossyPressEffect(cornerRadius = 16.dp)
                                    .testTag("bucket_list_add_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add dream")
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BucketListCatalog.categories.forEach { cat ->
                                val active = cat.key == selectedCategory
                                Surface(
                                    shape = CircleShape,
                                    color = if (active) cat.accentColor.copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .glossyPressEffect(cornerRadius = 16.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (active) 1.5.dp else 1.dp,
                                            color = if (active) cat.accentColor else MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedCategory = cat.key }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = cat.emoji, fontSize = 13.sp)
                                        Text(
                                            text = cat.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Category Filter Bar when items exist
                if (items.isNotEmpty()) {
                    item(key = "bucket_filters") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (filterCategory == null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                contentColor = if (filterCategory == null) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { filterCategory = null }
                            ) {
                                Text(
                                    text = "All (${items.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            BucketListCatalog.categories.forEach { cat ->
                                val count = items.count { it.category == cat.key }
                                if (count > 0) {
                                    val active = filterCategory == cat.key
                                    Surface(
                                        shape = CircleShape,
                                        color = if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { filterCategory = if (active) null else cat.key }
                                    ) {
                                        Text(
                                            text = "${cat.emoji} ${cat.label} ($count)",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredItems.isEmpty()) {
                    item(key = "bucket_starters") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Quick Romantic Ideas — Tap to Add:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            BucketListCatalog.starterSuggestions.forEach { (idea, catKey) ->
                                val cat = BucketListCatalog.byKey(catKey)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glossyPressEffect(cornerRadius = 16.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onAddItem(idea, catKey) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(text = cat.emoji, fontSize = 18.sp)
                                        Text(
                                            text = idea,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Add starter dream",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        val cat = BucketListCatalog.byKey(item.category)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (item.completed) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .glossyPressEffect(cornerRadius = 18.dp, pressScale = 0.98f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onToggleCompleted(item) }
                                .testTag("bucket_item_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.completed) Icons.Rounded.CheckCircle
                                    else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = if (item.completed) "Completed" else "Incomplete",
                                    tint = if (item.completed) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (item.completed) FontWeight.Normal else FontWeight.SemiBold,
                                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (item.completed) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${cat.emoji} ${cat.label} • Added by ${item.creatorName.ifBlank { "You" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteItem(item.id) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .glossyPressEffect(cornerRadius = 18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete dream",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
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

/**
 * 2. Interactive Animated Date Night Idea Spinner Wheel with 1-Tap Invite to Private Chat!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSpinnerSheet(
    partnerName: String,
    onSendInviteToChat: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val ideas = DateSpinnerCatalog.ideas
    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var pickedIdea by remember { mutableStateOf<DateIdeaOption?>(ideas.first()) }

    val spinWheel: () -> Unit = {
        if (!spinning) {
            spinning = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            val chosenIdx = Random.nextInt(ideas.size)
            val sliceSweep = 360f / ideas.size
            // Pointer sits at top (270 deg). Align chosen slice center to 270 deg.
            val sliceCenter = chosenIdx * sliceSweep + sliceSweep / 2f
            val desiredMod = ((270f - sliceCenter) % 360f + 360f) % 360f
            val currentMod = ((rotation.value % 360f) + 360f) % 360f
            val deltaToTarget = ((desiredMod - currentMod) + 360f) % 360f
            val extraSpins = (4 + Random.nextInt(3)) * 360f
            val targetAngle = rotation.value + extraSpins + deltaToTarget

            scope.launch {
                rotation.animateTo(
                    targetValue = targetAngle,
                    animationSpec = tween(
                        durationMillis = 2600,
                        easing = CubicBezierEasing(0.12f, 0.88f, 0.18f, 1f)
                    )
                )
                pickedIdea = ideas[chosenIdx]
                spinning = false
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
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
                .padding(bottom = 28.dp)
                .testTag("date_spinner_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Casino,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Date Night Spinner",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Can't decide tonight? Spin the wheel & invite $partnerName!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fortune Wheel Canvas + Gold Pointer Needle + Glossy Center Spin Hub
            Box(
                modifier = Modifier.size(254.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    val diameter = minOf(size.width, size.height)
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)
                    val sweep = 360f / ideas.size

                    rotate(degrees = rotation.value, pivot = center) {
                        ideas.forEachIndexed { idx, option ->
                            drawArc(
                                color = option.wheelColor,
                                startAngle = idx * sweep,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = topLeft,
                                size = arcSize
                            )
                            drawArc(
                                color = Color.White.copy(alpha = 0.55f),
                                startAngle = idx * sweep,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // Outer gleaming glass bezel
                    drawCircle(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFFE599), Color.White, Color(0xFFFFB4C6))
                        ),
                        radius = diameter / 2f,
                        center = center,
                        style = Stroke(width = 5.dp.toPx())
                    )
                }

                // Top Gold Pointer Triangle
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(26.dp)
                ) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(path, color = Color(0xFFFFD86B))
                    drawPath(path, color = Color(0xFF472C00), style = Stroke(width = 1.5.dp.toPx()))
                }

                // Glossy Center Spin Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .glossyPressEffect(cornerRadius = 36.dp, pressScale = 0.90f)
                        .clip(CircleShape)
                        .border(2.5.dp, Color(0xFFFFD86B), CircleShape)
                        .clickable(enabled = !spinning, onClick = spinWheel)
                        .testTag("date_spinner_spin_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (spinning) "✨" else "SPIN",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Result Card
            pickedIdea?.let { idea ->
                AnimatedContent(
                    targetState = idea,
                    transitionSpec = { (fadeIn(tween(260)) + scaleIn(initialScale = 0.94f)) togetherWith fadeOut(tween(180)) },
                    label = "date_spinner_result"
                ) { current ->
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = current.wheelColor.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.4.dp, current.wheelColor.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = current.emoji, fontSize = 36.sp)
                            Surface(
                                shape = CircleShape,
                                color = current.wheelColor.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "${current.vibe} Vibe",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = current.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = current.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = spinWheel,
                        enabled = !spinning,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .glossyPressEffect(cornerRadius = 16.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (spinning) "Spinning…" else "Spin Again")
                    }
                    Button(
                        onClick = {
                            onSendInviteToChat(
                                "${idea.emoji} Date Night Invite: ${idea.title} — ${idea.description} Are you in, sweetheart?"
                            )
                            onDismiss()
                        },
                        enabled = !spinning,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .glossyPressEffect(cornerRadius = 16.dp)
                            .testTag("date_spinner_send_invite_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Invite in Chat")
                    }
                }
            }
        }
    }
}

/**
 * 3. Couple Games & Love Coupons Sheet — Truth-or-Love Card Deck + Redeemable Digital Love Coupons!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoveGamesSheet(
    partnerName: String,
    onSendToPrivateChat: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Truth or Love Deck, 1 = Love Coupons
    var selectedKind by remember { mutableStateOf<CoupleCardKind?>(null) }
    var cardIndex by remember { mutableIntStateOf(0) }

    val filteredDeck = remember(selectedKind) {
        if (selectedKind == null) TruthOrLoveCatalog.deck
        else TruthOrLoveCatalog.deck.filter { it.kind == selectedKind }
    }
    val safeIndex = if (filteredDeck.isEmpty()) 0 else cardIndex % filteredDeck.size
    val activeCard = filteredDeck.getOrNull(safeIndex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .testTag("love_games_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
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
                        imageVector = if (selectedSection == 0) Icons.Rounded.Style else Icons.Rounded.CardGiftcard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = "Love Deck & Coupons",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Spark playful talks or gift instant VIP coupons to $partnerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Segmented Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("🃏 Truth or Love" to "games_tab_deck", "🎟️ Love Coupons" to "games_tab_coupons")
                    .forEachIndexed { idx, (label, tag) ->
                        val active = selectedSection == idx
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            contentColor = if (active) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .glossyPressEffect(cornerRadius = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedSection = idx }
                                .testTag(tag)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
            }

            if (selectedSection == 0) {
                // Truth or Love Deck
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (selectedKind == null) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                selectedKind = null
                                cardIndex = 0
                            }
                    ) {
                        Text(
                            text = "All (${TruthOrLoveCatalog.deck.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                    CoupleCardKind.entries.forEach { kind ->
                        val active = selectedKind == kind
                        Surface(
                            shape = CircleShape,
                            color = if (active) kind.accent.copy(alpha = 0.24f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    width = if (active) 1.4.dp else 0.dp,
                                    color = if (active) kind.accent else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedKind = kind
                                    cardIndex = 0
                                }
                        ) {
                            Text(
                                text = "${kind.emoji} ${kind.label}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                if (activeCard != null) {
                    AnimatedContent(
                        targetState = activeCard,
                        transitionSpec = {
                            (fadeIn(tween(240)) + scaleIn(initialScale = 0.94f)) togetherWith fadeOut(tween(160))
                        },
                        label = "truth_or_love_card",
                        modifier = Modifier.weight(1f)
                    ) { card ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            card.kind.accent,
                                            card.kind.accent.copy(alpha = 0.72f),
                                            Color(0xFF1D1026)
                                        )
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = "${card.kind.emoji} ${card.kind.label.uppercase()} • ${safeIndex + 1}/${filteredDeck.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                                Text(
                                    text = card.prompt,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                cardIndex = (safeIndex + 1) % filteredDeck.size
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .glossyPressEffect(cornerRadius = 16.dp)
                                .testTag("love_deck_next_button")
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Next Card")
                        }
                        Button(
                            onClick = {
                                onSendToPrivateChat("${activeCard.kind.emoji} [${activeCard.kind.label}] ${activeCard.prompt}")
                                onDismiss()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.25f)
                                .height(50.dp)
                                .glossyPressEffect(cornerRadius = 16.dp)
                                .testTag("love_deck_ask_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ask in Chat")
                        }
                    }
                }
            } else {
                // Digital Romantic Love Coupons
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(LoveCouponCatalog.all, key = { it.id }) { coupon ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(listOf(coupon.gradientStart, coupon.gradientEnd))
                                )
                                .border(1.3.dp, Color.White.copy(alpha = 0.52f), RoundedCornerShape(22.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = coupon.emoji, fontSize = 24.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "VIP LOVE VOUCHER",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFFF3C4),
                                            letterSpacing = 1.2.sp
                                        )
                                        Text(
                                            text = coupon.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Text(
                                    text = coupon.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.90f)
                                )
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSendToPrivateChat(
                                            "🎟️ VIP Love Coupon for you: ${coupon.emoji} ${coupon.title} — ${coupon.subtitle}"
                                        )
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.24f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .glossyPressEffect(cornerRadius = 14.dp)
                                        .testTag("coupon_gift_${coupon.id}")
                                ) {
                                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Gift to ${partnerName.substringBefore(" ")} in Chat", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
