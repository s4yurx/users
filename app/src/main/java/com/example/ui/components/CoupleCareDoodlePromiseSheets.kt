package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.PhotoCompressor
import com.example.data.CareCheckIn
import com.example.data.CareCheckInCatalog
import com.example.data.CouplePromise
import com.example.data.CoupleQuizCatalog
import com.example.data.LoveDoodle
import com.example.data.PromiseCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

private fun formatCareIstTime(millis: Long?): String {
    if (millis == null || millis <= 0L) return "Just now"
    val fmt = SimpleDateFormat("MMM d • h:mm a", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }
    return "${fmt.format(Date(millis))} IST"
}

// ============================================================================
// 4. CARE & WELLNESS CHECK-INS SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareCheckInSheet(
    checkIns: List<CareCheckIn>,
    myUid: String,
    partnerName: String,
    isBusy: Boolean,
    onSendCheckIn: (careType: String, customMessage: String) -> Unit,
    onAcknowledge: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customDraft by rememberSaveable { mutableStateOf("") }
    val quickPresets = remember { CareCheckInCatalog.presets.take(6) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .testTag("care_checkin_sheet"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Care & Wellness Check-Ins",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Send 1-tap loving reminders to $partnerName with instant alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2-Column Quick Care Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    quickPresets.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = preset.accent.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .glossyPressEffect(cornerRadius = 18.dp, pressScale = 0.96f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .exclusiveAnimatedBorder(
                                            cornerRadius = 18.dp,
                                            strokeWidth = 1.4.dp,
                                            fallbackColor = preset.accent.copy(alpha = 0.45f),
                                            fallbackWidth = 1.dp
                                        )
                                        .clickable(enabled = !isBusy) {
                                            onSendCheckIn(preset.key, preset.defaultMessage)
                                        }
                                        .testTag("care_preset_${preset.key.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = preset.emoji, fontSize = 22.sp)
                                        Column {
                                            Text(
                                                text = preset.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Tap to nudge",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = preset.accent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom Care Check-In Input
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customDraft,
                        onValueChange = { if (it.length <= 180) customDraft = it },
                        placeholder = { Text("Write a custom care reminder for $partnerName…") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("care_custom_input")
                    )
                    Button(
                        onClick = {
                            onSendCheckIn("CUSTOM", customDraft.trim())
                            customDraft = ""
                        },
                        enabled = customDraft.trim().isNotEmpty() && !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .glossyPressEffect(cornerRadius = 14.dp)
                            .testTag("care_custom_send_button")
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = "Send custom care nudge", modifier = Modifier.size(18.dp))
                    }
                }
            }

            item {
                Text(
                    text = "Recent Care Feed (${checkIns.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (checkIns.isEmpty()) {
                item {
                    Text(
                        text = "Tap any care card above to remind $partnerName to hydrate, eat well, or rest!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(checkIns, key = { it.id }) { item ->
                    val preset = CareCheckInCatalog.byKey(item.careType)
                    val isMine = item.senderUid == myUid
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
                                    text = "${preset.emoji} ${preset.label} • ${if (isMine) "From You" else "From ${item.senderName}"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = preset.accent
                                )
                                Text(
                                    text = formatCareIstTime(item.createdAt?.toDate()?.time),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = item.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (item.acknowledged) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Completed with love ❤️",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            } else if (!isMine) {
                                Button(
                                    onClick = { onAcknowledge(item.id) },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .glossyPressEffect(cornerRadius = 20.dp)
                                        .testTag("care_ack_button_${item.id}")
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Done! Thanks ❤️", style = MaterialTheme.typography.labelMedium)
                                }
                            } else {
                                Text(
                                    text = "Waiting for $partnerName to check in…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. SHARED LOVE DOODLE CANVAS & HD GALLERY SHEET
// ============================================================================

private data class DoodleStroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoveDoodleSheet(
    doodles: List<LoveDoodle>,
    myUid: String,
    partnerName: String,
    isBusy: Boolean,
    onSendDoodle: (title: String, imageBase64: String) -> Unit,
    onToggleLoved: (LoveDoodle) -> Unit,
    onDeleteDoodle: (String) -> Unit,
    onFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val strokes = remember { mutableStateListOf<DoodleStroke>() }
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color(0xFFE11D48)) }
    var strokeWidthPx by remember { mutableFloatStateOf(10f) }
    var darkCanvas by rememberSaveable { mutableStateOf(false) }
    var titleDraft by rememberSaveable { mutableStateOf("") }
    var canvasSize by remember { mutableStateOf(IntSize(800, 520)) }
    var lightboxDoodle by remember { mutableStateOf<LoveDoodle?>(null) }

    val palette = remember {
        listOf(
            Color(0xFFE11D48),
            Color(0xFF1E293B),
            Color(0xFFD97706),
            Color(0xFF059669),
            Color(0xFF0284C7),
            Color(0xFF7C3AED),
            Color(0xFFEC4899),
            Color(0xFFFFFFFF)
        )
    }

    lightboxDoodle?.let { item ->
        val artist = if (item.artistUid == myUid) "You" else item.artistName.ifBlank { partnerName }
        PhotoLightboxDialog(
            imageBase64 = item.imageBase64,
            caption = item.title.ifBlank { "Hand-drawn Love Doodle" },
            subtitle = "Drawn by $artist",
            senderLabel = "Drawn by $artist",
            sentAtLabel = formatCareIstTime(item.createdAt?.toDate()?.time),
            locationLabel = "Love Doodle Canvas",
            filePrefix = "Duet_Doodle_HD",
            onSavedFeedback = onFeedback,
            onDismiss = { lightboxDoodle = null }
        )
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
                .testTag("love_doodle_sheet"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Brush,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Shared Love Doodle Canvas",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sketch cute hearts & notes in HD for $partnerName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Interactive Finger-Drawing Canvas
            item {
                val bgColor = if (darkCanvas) Color(0xFF111827) else Color(0xFFFFFBF5)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(bgColor)
                            .exclusiveAnimatedBorder(
                                cornerRadius = 22.dp,
                                strokeWidth = 1.6.dp,
                                fallbackColor = MaterialTheme.colorScheme.outlineVariant,
                                fallbackWidth = 1.5.dp
                            )
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(selectedColor, strokeWidthPx) {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        activePoints = listOf(start)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        activePoints = activePoints + change.position
                                    },
                                    onDragEnd = {
                                        if (activePoints.isNotEmpty()) {
                                            strokes.add(
                                                DoodleStroke(
                                                    points = activePoints,
                                                    color = selectedColor,
                                                    widthPx = strokeWidthPx
                                                )
                                            )
                                        }
                                        activePoints = emptyList()
                                    },
                                    onDragCancel = {
                                        activePoints = emptyList()
                                    }
                                )
                            }
                            .testTag("doodle_canvas")
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            fun drawStrokePath(pts: List<Offset>, color: Color, width: Float) {
                                if (pts.isEmpty()) return
                                if (pts.size == 1) {
                                    drawCircle(color = color, radius = width / 2f, center = pts.first())
                                } else {
                                    val path = Path().apply {
                                        moveTo(pts.first().x, pts.first().y)
                                        for (i in 1 until pts.size) {
                                            lineTo(pts[i].x, pts[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = color,
                                        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                            strokes.forEach { drawStrokePath(it.points, it.color, it.widthPx) }
                            if (activePoints.isNotEmpty()) {
                                drawStrokePath(activePoints, selectedColor, strokeWidthPx)
                            }
                        }

                        if (strokes.isEmpty() && activePoints.isEmpty()) {
                            Text(
                                text = "Draw here with your finger ✨",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (darkCanvas) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.35f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    // Color Palette + Brush Tools
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        palette.forEach { col ->
                            val selected = col == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 34.dp else 28.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = col }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Fine" to 6f, "Med" to 11f, "Bold" to 18f).forEach { (label, w) ->
                                val active = strokeWidthPx == w
                                Surface(
                                    shape = CircleShape,
                                    color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { strokeWidthPx = w }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { darkCanvas = !darkCanvas }
                            ) {
                                Text(
                                    text = if (darkCanvas) "🌙 Dark" else "☀️ Paper",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                                enabled = strokes.isNotEmpty(),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo stroke", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { strokes.clear() },
                                enabled = strokes.isNotEmpty(),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.ClearAll, contentDescription = "Clear doodle canvas", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = titleDraft,
                            onValueChange = { if (it.length <= 80) titleDraft = it },
                            placeholder = { Text("Caption your sketch (optional)…") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("doodle_title_input")
                        )
                        Button(
                            onClick = {
                                val currentStrokes = strokes.toList()
                                val title = titleDraft.trim().ifBlank { "Sketch for $partnerName" }
                                val bgArgb = bgColor.toArgb()
                                val srcW = canvasSize.width.coerceAtLeast(1)
                                val srcH = canvasSize.height.coerceAtLeast(1)
                                scope.launch {
                                    val rendered = renderDoodleToHdBitmap(currentStrokes, srcW, srcH, bgArgb)
                                    val base64 = PhotoCompressor.compressDoodleBitmapToBase64(rendered)
                                    rendered.recycle()
                                    if (base64 != null) {
                                        onSendDoodle(title, base64)
                                        strokes.clear()
                                        titleDraft = ""
                                    } else {
                                        onFeedback("Could not render doodle — try drawing a stroke first")
                                    }
                                }
                            },
                            enabled = strokes.isNotEmpty() && !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .glossyPressEffect(cornerRadius = 14.dp)
                                .testTag("doodle_send_button")
                        ) {
                            Text("Send HD")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Shared Doodle Wall (${doodles.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (doodles.isEmpty()) {
                item {
                    Text(
                        text = "Your shared doodle wall is empty — draw something cute above and send it in HD!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(doodles, key = { it.id }) { doodle ->
                    val isMine = doodle.artistUid == myUid
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .exclusiveAnimatedBorder(cornerRadius = 20.dp, strokeWidth = 1.3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DecryptedPhotoImage(
                                imageBase64 = doodle.imageBase64,
                                contentDescription = doodle.title.ifBlank { "Love doodle" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { lightboxDoodle = doodle }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doodle.title.ifBlank { "Love Sketch" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${if (isMine) "You" else doodle.artistName} • ${formatCareIstTime(doodle.createdAt?.toDate()?.time)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isMine) {
                                        IconButton(onClick = { onToggleLoved(doodle) }) {
                                            Icon(
                                                imageVector = if (doodle.lovedByPartner) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = "Love doodle",
                                                tint = if (doodle.lovedByPartner) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else if (doodle.lovedByPartner) {
                                        Text("❤️", fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteDoodle(doodle.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete doodle",
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
        }
    }
}

private fun renderDoodleToHdBitmap(
    strokes: List<DoodleStroke>,
    srcWidth: Int,
    srcHeight: Int,
    backgroundArgb: Int
): Bitmap {
    val outW = 960
    val outH = ((srcHeight.toFloat() / srcWidth.toFloat()) * outW).toInt().coerceIn(500, 960)
    val scaleX = outW.toFloat() / srcWidth.toFloat()
    val scaleY = outH.toFloat() / srcHeight.toFloat()
    val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(backgroundArgb)

    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
    }

    for (stroke in strokes) {
        if (stroke.points.isEmpty()) continue
        paint.color = stroke.color.toArgb()
        paint.strokeWidth = stroke.widthPx * ((scaleX + scaleY) * 0.5f)
        if (stroke.points.size == 1) {
            val pt = stroke.points.first()
            paint.style = AndroidPaint.Style.FILL
            canvas.drawCircle(pt.x * scaleX, pt.y * scaleY, paint.strokeWidth / 2f, paint)
            paint.style = AndroidPaint.Style.STROKE
        } else {
            val path = AndroidPath().apply {
                moveTo(stroke.points.first().x * scaleX, stroke.points.first().y * scaleY)
                for (i in 1 until stroke.points.size) {
                    lineTo(stroke.points[i].x * scaleX, stroke.points[i].y * scaleY)
                }
            }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}

// ============================================================================
// 6. COUPLE PROMISE VAULT + LOVE QUIZ & BOND STREAK SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouplePromisesSheet(
    promises: List<CouplePromise>,
    myUid: String,
    partnerName: String,
    bondScorePoints: Int,
    isBusy: Boolean,
    onAddPromise: (title: String, category: String) -> Unit,
    onCoSignPromise: (CouplePromise) -> Unit,
    onDeletePromise: (String) -> Unit,
    onShareQuizResultToChat: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Promises, 1 = Love Quiz & Streak
    var promiseDraft by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(PromiseCatalog.categories.first().key) }
    var quizAnswers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val coSignedCount = remember(promises) { promises.count { it.isFullyCoSigned() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .testTag("couple_promises_sheet"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Handshake,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Promise Vault & Love Quiz",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$coSignedCount co-signed pinky promises • Bond Score $bondScorePoints 🔥",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tab Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("🤙 Pinky Promises" to 0, "💘 Love Quiz & Streak" to 1).forEach { (label, idx) ->
                        val active = activeTab == idx
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .glossyPressEffect(cornerRadius = 14.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { activeTab = idx }
                                .testTag("promises_tab_$idx")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (activeTab == 0) {
                // Starters + Composer
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
                            Text(
                                text = "Quick Inspiration",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PromiseCatalog.starters.forEach { (starterText, catKey) ->
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                            .clickable {
                                                promiseDraft = starterText
                                                selectedCategory = catKey
                                            }
                                    ) {
                                        Text(
                                            text = "+ ${starterText.take(28)}…",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PromiseCatalog.categories.forEach { cat ->
                                    val active = selectedCategory == cat.key
                                    Surface(
                                        shape = CircleShape,
                                        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { selectedCategory = cat.key }
                                    ) {
                                        Text(
                                            text = "${cat.emoji} ${cat.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                                    value = promiseDraft,
                                    onValueChange = { if (it.length <= 160) promiseDraft = it },
                                    placeholder = { Text("Write a sacred couple promise…") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("promise_input")
                                )
                                Button(
                                    onClick = {
                                        onAddPromise(promiseDraft.trim(), selectedCategory)
                                        promiseDraft = ""
                                    },
                                    enabled = promiseDraft.trim().isNotEmpty() && !isBusy,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .glossyPressEffect(cornerRadius = 14.dp)
                                        .testTag("promise_add_button")
                                ) {
                                    Text("Seal")
                                }
                            }
                        }
                    }
                }

                if (promises.isEmpty()) {
                    item {
                        Text(
                            text = "No pinky promises sealed yet — pick a starter above or write your own pact with $partnerName!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(promises, key = { it.id }) { promise ->
                        val cat = PromiseCatalog.byKey(promise.category)
                        val alreadySignedByMe = myUid in promise.signedByUids
                        val coSigned = promise.isFullyCoSigned()
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (coSigned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .exclusiveAnimatedBorder(cornerRadius = 18.dp, strokeWidth = 1.4.dp)
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
                                        text = "${cat.emoji} ${cat.label} • by ${if (promise.creatorUid == myUid) "You" else promise.creatorName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { onDeletePromise(promise.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete promise",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = promise.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (coSigned) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Pinky-Swore Together 🤙💍",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                } else if (!alreadySignedByMe) {
                                    Button(
                                        onClick = { onCoSignPromise(promise) },
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .glossyPressEffect(cornerRadius = 20.dp)
                                            .testTag("promise_cosign_button_${promise.id}")
                                    ) {
                                        Text("🤙 Pinky Swear & Co-Sign", style = MaterialTheme.typography.labelMedium)
                                    }
                                } else {
                                    Text(
                                        text = "Signed by You ✓ • Waiting for $partnerName's pinky swear",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Love Quiz & Bond Score Tab
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF881337), Color(0xFF9D174D), Color(0xFFD97706))))
                            .exclusiveAnimatedBorder(cornerRadius = 22.dp, strokeWidth = 1.5.dp)
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "🔥 Duet Couple Bond Score",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$bondScorePoints Love Points",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Earned from your shared pulses, moods, private chats, letters, wishes & promises!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.86f)
                            )
                        }
                    }
                }

                items(CoupleQuizCatalog.questions, key = { it.id }) { q ->
                    val chosen = quizAnswers[q.id]
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = q.question,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(q.optionA, q.optionB).forEach { opt ->
                                    val selected = chosen == opt
                                    if (selected) {
                                        Button(
                                            onClick = { quizAnswers = quizAnswers + (q.id to opt) },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(opt, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { quizAnswers = quizAnswers + (q.id to opt) },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(opt, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val complete = quizAnswers.size == CoupleQuizCatalog.questions.size
                    Button(
                        onClick = {
                            val summary = CoupleQuizCatalog.questions.joinToString("\n") { q ->
                                "• ${q.question} -> ${quizAnswers[q.id] ?: "—"}"
                            }
                            onShareQuizResultToChat("💘 My Duet Compatibility Quiz Answers:\n$summary\nWhat are yours, $partnerName?")
                        },
                        enabled = complete,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .glossyPressEffect(cornerRadius = 16.dp)
                            .testTag("quiz_share_chat_button")
                    ) {
                        Text(
                            if (complete) "Share Quiz Answers to Private Chat 💌"
                            else "Answer all ${CoupleQuizCatalog.questions.size} questions (${quizAnswers.size}/${CoupleQuizCatalog.questions.size})"
                        )
                    }
                }
            }
        }
    }
}
