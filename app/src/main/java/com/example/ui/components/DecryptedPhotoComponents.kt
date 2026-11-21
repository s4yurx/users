package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.crypto.PhotoCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DecryptedPhotoImage(
    imageBase64: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmapState by produceState<ImageBitmap?>(initialValue = null, key1 = imageBase64) {
        value = withContext(Dispatchers.Default) {
            PhotoCompressor.decodeBase64ToBitmap(imageBase64)?.asImageBitmap()
        }
    }

    val imageBitmap = bitmapState
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

/**
 * Full-featured HD Photo Lightbox Viewer with:
 * - Pinch-to-zoom & pan + double-tap 1x/2.4x toggle
 * - Zero-permission MediaStore HD Gallery Download button with confirmation state
 * - Rich sender attribution ("Sent by <Name>"), exact IST sent date/time, location tag,
 *   decoded resolution pill (HD • W×H), and E2EE privacy badge.
 */
@Composable
fun PhotoLightboxDialog(
    imageBase64: String,
    caption: String,
    subtitle: String,
    senderLabel: String = "",
    sentAtLabel: String = "",
    locationLabel: String = "",
    filePrefix: String = "Duet_HD",
    onSavedFeedback: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showChrome by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var savedSuccessText by remember { mutableStateOf<String?>(null) }

    val dimensions by produceState<Pair<Int, Int>?>(initialValue = null, key1 = imageBase64) {
        value = withContext(Dispatchers.Default) {
            PhotoCompressor.inspectImageDimensions(imageBase64)
        }
    }

    // Parse fallback sender / time cleanly if caller only passed combined subtitle
    val resolvedSender = senderLabel.ifBlank {
        subtitle.substringBefore("•").trim()
    }
    val resolvedSentAt = sentAtLabel.ifBlank {
        if (subtitle.contains("•")) subtitle.substringAfter("•").trim() else ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070B))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showChrome = !showChrome },
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.4f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = nextScale
                        if (nextScale > 1.02f) {
                            val maxOffset = 460f * (nextScale - 1f)
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                y = (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
        ) {
            DecryptedPhotoImage(
                imageBase64 = imageBase64,
                contentDescription = caption.ifBlank { "Shared encrypted HD photo" },
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .padding(10.dp)
            )

            // Top Frosted HD Controls Header
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // HD + Resolution + E2EE Pill
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.border(
                            1.dp,
                            Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(18.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Hd,
                                contentDescription = "HD quality",
                                tint = Color(0xFFFFD875),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = dimensions?.let { (w, h) -> "HD • ${w}×${h}" } ?: "HD Photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "End-to-end encrypted",
                                tint = Color(0xFF7AE8A8),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Zoom Toggle Button
                        IconButton(
                            onClick = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.4f
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .testTag("lightbox_zoom_button")
                        ) {
                            Icon(
                                imageVector = if (scale > 1.2f) Icons.Rounded.ZoomOut else Icons.Rounded.ZoomIn,
                                contentDescription = if (scale > 1.2f) "Reset zoom" else "Zoom in",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Download HD to Gallery Button
                        Surface(
                            onClick = {
                                if (!isSaving) {
                                    isSaving = true
                                    scope.launch {
                                        val result = PhotoCompressor.saveBase64ImageToGallery(
                                            context = context,
                                            imageBase64 = imageBase64,
                                            filePrefix = filePrefix
                                        )
                                        isSaving = false
                                        result.onSuccess { msg ->
                                            savedSuccessText = msg
                                            onSavedFeedback?.invoke(msg)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            val errMsg = err.message ?: "Could not save photo"
                                            onSavedFeedback?.invoke(errMsg)
                                            Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (savedSuccessText != null) Color(0xFF2E7D52) else Color.White.copy(alpha = 0.20f),
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(20.dp))
                                .testTag("lightbox_download_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                when {
                                    isSaving -> CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    savedSuccessText != null -> Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Saved",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Download HD photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = when {
                                        isSaving -> "Saving"
                                        savedSuccessText != null -> "Saved"
                                        else -> "Save HD"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Compact Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .testTag("lightbox_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close photo viewer",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Rich Sender + Timestamp + Caption Metadata Sheet
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xCC11121A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (resolvedSender.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = resolvedSender,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("lightbox_sender_text")
                                    )
                                }
                            }
                            if (resolvedSentAt.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccessTime,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.72f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = resolvedSentAt,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.84f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("lightbox_timestamp_text")
                                    )
                                }
                            }
                        }

                        if (locationLabel.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB2C4),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = locationLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFD5DF)
                                )
                            }
                        }

                        if (caption.isNotBlank()) {
                            Text(
                                text = caption,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.94f),
                                lineHeight = 20.sp
                            )
                        }

                        if (savedSuccessText != null) {
                            Text(
                                text = "✓ $savedSuccessText",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8EF0B6),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Pinch or double-tap to zoom • Tap photo to hide info",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.56f)
                            )
                        }
                    }
                }
            }
        }
    }
}
