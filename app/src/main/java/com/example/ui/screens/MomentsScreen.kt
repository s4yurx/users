package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.crypto.PhotoCompressor
import com.example.data.DecryptedMomentItem
import com.example.data.IndiaTimeFormatter
import com.example.data.SharedMoment
import com.example.ui.DuetUiState
import com.example.ui.components.DecryptedPhotoImage
import com.example.ui.components.FeatureDisabledCard
import com.example.ui.components.PhotoLightboxDialog
import com.example.ui.components.exclusiveAnimatedBorder
import com.example.ui.components.glossyPressEffect
import com.example.ui.theme.EncryptedEmerald
import com.example.ui.theme.StatusLiveCoral
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    state: DuetUiState,
    externalUploadTrigger: Int = 0,
    showInternalFab: Boolean = true,
    featureEnabled: Boolean = true,
    onUploadMoment: (Uri?, File?, caption: String, locationTag: String) -> Unit,
    onToggleLove: (SharedMoment) -> Unit,
    onDeleteMoment: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myUid = state.currentUserId

    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingStagedFile by remember { mutableStateOf<File?>(null) }
    var pendingPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var stagingPhoto by remember { mutableStateOf(false) }
    var captionDraft by remember { mutableStateOf("") }
    var locationDraft by remember { mutableStateOf("") }
    var lightboxMoment by remember { mutableStateOf<DecryptedMomentItem?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                pendingStagedFile?.delete()
                pendingStagedFile = null
                pendingPreviewBitmap = null
                pendingUri = uri
                captionDraft = ""
                locationDraft = ""
                stagingPhoto = true
                scope.launch {
                    val staged = PhotoCompressor.stageUriToCacheFile(context, uri)
                    val thumb = PhotoCompressor.decodePreviewThumbnail(context, uri, staged)
                    pendingStagedFile = staged
                    pendingPreviewBitmap = thumb
                    stagingPhoto = false
                }
            }
        }
    )

    LaunchedEffect(externalUploadTrigger, featureEnabled) {
        if (externalUploadTrigger > 0 && featureEnabled) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    lightboxMoment?.let { item ->
        item.imageBase64?.let { b64 ->
            val uploadedAt = IndiaTimeFormatter.formatDateTime(item.raw.createdAt?.toDate())
            PhotoLightboxDialog(
                imageBase64 = b64,
                caption = item.caption,
                subtitle = "Uploaded by ${item.raw.uploaderName} • $uploadedAt IST",
                senderLabel = "Uploaded by ${item.raw.uploaderName}",
                sentAtLabel = "$uploadedAt IST",
                locationLabel = item.locationTag,
                filePrefix = "Duet_Moment_HD",
                onDismiss = { lightboxMoment = null }
            )
        }
    }

    if (pendingUri != null || pendingStagedFile != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                pendingStagedFile?.delete()
                pendingStagedFile = null
                pendingUri = null
                pendingPreviewBitmap = null
                stagingPhoto = false
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Upload HD Picture for Partner",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Your photo is saved in crisp HD quality and encrypted end-to-end before syncing to your keepsake wall.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Live Preview Thumbnail of Selected Picture
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = pendingPreviewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Selected photo preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (stagingPhoto) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.5.dp)
                            Text(
                                text = "Preparing your picture…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Photo selected & ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = captionDraft,
                    onValueChange = { if (it.length <= 200) captionDraft = it },
                    label = { Text("Sweet caption") },
                    placeholder = { Text("Golden hour coffee date with you…") },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("moment_caption_input")
                )
                OutlinedTextField(
                    value = locationDraft,
                    onValueChange = { if (it.length <= 50) locationDraft = it },
                    label = { Text("Place or memory tag (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("moment_location_input")
                )
                Button(
                    onClick = {
                        val uri = pendingUri
                        val staged = pendingStagedFile
                        onUploadMoment(uri, staged, captionDraft, locationDraft)
                        pendingUri = null
                        pendingStagedFile = null
                        pendingPreviewBitmap = null
                    },
                    enabled = !state.busyAction && !stagingPhoto,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .glossyPressEffect(cornerRadius = 16.dp)
                        .testTag("moment_upload_submit_button")
                ) {
                    if (state.busyAction || stagingPhoto) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Encrypt & Upload for Partner")
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.moments_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Private encrypted keepsakes uploaded for each other",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!featureEnabled) {
                item {
                    FeatureDisabledCard(
                        title = "Shared Journal Paused",
                        subtitle = "Shared Moments & Keepsakes are currently turned off by Bondly Admin.",
                        testTag = "feature_disabled_shared_journal"
                    )
                }
            } else if (state.moments.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .exclusiveAnimatedBorder(cornerRadius = 26.dp, strokeWidth = 1.6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_duet_moments),
                                contentDescription = "Keepsake polaroid photos and love letters",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                            Text(
                                text = stringResource(R.string.moments_empty_title),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = stringResource(R.string.moments_empty_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .glossyPressEffect(cornerRadius = 16.dp)
                                    .testTag("moments_empty_upload_button")
                            ) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Upload Your First Photo")
                            }
                        }
                    }
                }
            } else {
                items(state.moments, key = { it.raw.id }) { item ->
                    PolaroidMomentCard(
                        item = item,
                        isLovedByMe = myUid in item.raw.lovedByUids,
                        canDelete = item.raw.uploaderUid == myUid,
                        onOpenLightbox = { lightboxMoment = item },
                        onToggleLove = { onToggleLove(item.raw) },
                        onDelete = { onDeleteMoment(item.raw.id) }
                    )
                }
            }
        }

        if (showInternalFab && featureEnabled) {
            ExtendedFloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                icon = { Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null) },
                text = { Text(stringResource(R.string.moments_add_button)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .glossyPressEffect(cornerRadius = 16.dp)
                    .testTag("moments_upload_fab")
            )
        }
    }
}

@Composable
private fun PolaroidMomentCard(
    item: DecryptedMomentItem,
    isLovedByMe: Boolean,
    canDelete: Boolean,
    onOpenLightbox: () -> Unit,
    onToggleLove: () -> Unit,
    onDelete: () -> Unit
) {
    val dateLabel = IndiaTimeFormatter.formatDateTime(item.raw.createdAt?.toDate())

    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .exclusiveAnimatedBorder(cornerRadius = 24.dp, strokeWidth = 1.6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (item.imageBase64 != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenLightbox)
                ) {
                    DecryptedPhotoImage(
                        imageBase64 = item.imageBase64,
                        contentDescription = item.caption.ifBlank { "Shared HD moment photo" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = EncryptedEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "HD • E2EE",
                                style = MaterialTheme.typography.labelSmall,
                                color = EncryptedEmerald
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (item.caption.isNotBlank()) {
                        Text(
                            text = item.caption,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${item.raw.uploaderName} • $dateLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.locationTag.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = item.locationTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleLove,
                        modifier = Modifier.glossyPressEffect(cornerRadius = 24.dp)
                    ) {
                        Icon(
                            imageVector = if (isLovedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Love photo",
                            tint = if (isLovedByMe) StatusLiveCoral else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.raw.lovedByUids.isNotEmpty()) {
                        Text(
                            text = "${item.raw.lovedByUids.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.glossyPressEffect(cornerRadius = 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete uploaded moment",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
