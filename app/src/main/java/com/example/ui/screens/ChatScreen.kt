package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.crypto.PhotoCompressor
import com.example.data.DecryptedMessageItem
import com.example.data.IndiaTimeFormatter
import com.example.ui.DuetUiState
import com.example.ui.components.DecryptedPhotoImage
import com.example.ui.components.PhotoLightboxDialog
import com.example.ui.components.exclusiveAnimatedBorder
import com.example.ui.components.glossyPressEffect
import com.example.ui.theme.BatteryHealthyGreen
import com.example.ui.theme.ChatWallpaperPreset
import com.example.ui.theme.DuetAppearanceState
import com.example.ui.theme.EncryptedEmerald
import com.example.ui.theme.EncryptedEmeraldContainer
import com.example.ui.theme.StatusLiveCoral
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CHAT_REACTION_OPTIONS = listOf(
    "HEART" to "❤️",
    "LAUGH" to "😂",
    "WOW" to "😮",
    "SAD" to "😢",
    "PRAY" to "🙏",
    "FIRE" to "🔥"
)

@Composable
fun ChatScreen(
    state: DuetUiState,
    appearance: DuetAppearanceState = DuetAppearanceState(),
    extraBottomNavPadding: Dp = 0.dp,
    onSendText: (String) -> Unit,
    onSendPhoto: (Uri?, File?, String) -> Unit,
    onSendHeartPulse: () -> Unit,
    onReactMessage: (messageId: String, currentReactions: Map<String, String>, reactionKey: String) -> Unit,
    onDeleteMessage: (messageId: String) -> Unit = {},
    onTypingChanged: (Boolean) -> Unit,
    onMarkMessagesSeen: (List<DecryptedMessageItem>) -> Unit,
    onOpenEncryptionInfo: () -> Unit,
    onOpenChatSettings: () -> Unit = {},
    onTapPartnerHeader: () -> Unit,
    onFeedback: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val couple = state.couple ?: return
    val myUid = state.currentUserId
    val partnerName = state.resolvedPartnerName()
    val partnerPresence = state.partnerPresence
    val listState = rememberLazyListState()

    var draftText by rememberSaveable { mutableStateOf("") }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingStagedFile by remember { mutableStateOf<File?>(null) }
    var pendingPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var stagingPhoto by remember { mutableStateOf(false) }
    var lightboxItem by remember { mutableStateOf<DecryptedMessageItem?>(null) }
    var actionTargetMessageId by remember { mutableStateOf<String?>(null) }

    val selectedActionItem = remember(actionTargetMessageId, state.messages) {
        actionTargetMessageId?.let { id -> state.messages.firstOrNull { it.raw.id == id } }
    }

    // 2-second ticker so stale partner typing automatically fades out cleanly
    var typingClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(partnerPresence?.typingActive, partnerPresence?.typingUpdatedAt) {
        typingClockMs = System.currentTimeMillis()
        while (partnerPresence?.typingActive == true) {
            delay(2_000L)
            typingClockMs = System.currentTimeMillis()
        }
    }
    val isPartnerTyping = partnerPresence?.isPartnerActivelyTyping(typingClockMs) == true

    // Automatically mark incoming partner messages as Seen while viewing Chat
    LaunchedEffect(state.messages) {
        onMarkMessagesSeen(state.messages)
    }

    // Clear typing broadcast when navigating away from Chat
    DisposableEffect(Unit) {
        onDispose {
            onTypingChanged(false)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                pendingPhotoUri = uri
                pendingStagedFile?.delete()
                pendingStagedFile = null
                pendingPreviewBitmap = null
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

    val lastOutgoingIndex = remember(state.messages, myUid) {
        state.messages.indexOfLast { it.raw.senderUid == myUid }
    }

    LaunchedEffect(state.messages.size, isPartnerTyping) {
        val totalItems = state.messages.size + if (isPartnerTyping) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    lightboxItem?.let { item ->
        item.imageBase64?.let { b64 ->
            val senderName = if (item.raw.senderUid == myUid) "You" else partnerName
            val sentDateTime = IndiaTimeFormatter.formatDateTime(item.raw.createdAt?.toDate())
            PhotoLightboxDialog(
                imageBase64 = b64,
                caption = item.text,
                subtitle = "Sent by $senderName • $sentDateTime IST",
                senderLabel = "Sent by $senderName",
                sentAtLabel = "$sentDateTime IST",
                filePrefix = "Duet_Chat_HD",
                onSavedFeedback = onFeedback,
                onDismiss = { lightboxItem = null }
            )
        }
    }

    selectedActionItem?.let { targetItem ->
        MessageActionOverlayDialog(
            item = targetItem,
            myUid = myUid,
            partnerName = partnerName,
            onPickReaction = { key ->
                onReactMessage(targetItem.raw.id, targetItem.raw.effectiveReactions(), key)
                actionTargetMessageId = null
            },
            onCopyText = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Duet Message", targetItem.text))
                onFeedback("Message copied")
                actionTargetMessageId = null
            },
            onDeleteMessage = {
                onDeleteMessage(targetItem.raw.id)
                actionTargetMessageId = null
            },
            onDismiss = { actionTargetMessageId = null }
        )
    }

    val hasCustomBackdrop = appearance.chatWallpaper != ChatWallpaperPreset.THEME_DEFAULT
    val isDarkBackdrop = when (appearance.chatWallpaper) {
        ChatWallpaperPreset.THEME_DEFAULT -> appearance.themePreset.isDark
        ChatWallpaperPreset.CUSTOM_PHOTO -> true
        else -> appearance.chatWallpaper.isDarkCanvas
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            // Top Rounded Floating Private Chat Header with Nickname, Typing Status, Active App, IST Last Seen, E2EE & Compact Chat Settings
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .exclusiveAnimatedBorder(
                        cornerRadius = 24.dp,
                        strokeWidth = 1.6.dp,
                        fallbackColor = Color.White.copy(alpha = if (appearance.themePreset.isDark) 0.18f else 0.78f),
                        fallbackWidth = 1.dp
                    )
                    .testTag("chat_rounded_header")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onTapPartnerHeader)
                            .testTag("chat_partner_header"),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val isPartnerBanned = state.partnerBanned != null
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = partnerName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isPartnerBanned) {
                                Spacer(Modifier.width(7.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.testTag("chat_header_banned_badge")
                                ) {
                                    Text(
                                        text = stringResource(R.string.partner_banned_badge),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isPartnerTyping || (partnerPresence != null && (partnerPresence.deviceStatus == "ACTIVE_IN_DUET" || partnerPresence.deviceStatus == "IN_USE"))) {
                                Spacer(Modifier.width(7.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isPartnerTyping) MaterialTheme.colorScheme.primary else BatteryHealthyGreen)
                                )
                            }
                        }

                        if (isPartnerBanned) {
                            Text(
                                text = "${stringResource(R.string.partner_banned_admin_mark)} • Tap for details",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (isPartnerTyping) {
                            Text(
                                text = "Typing…",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                modifier = Modifier.testTag("chat_header_typing_status")
                            )
                        } else if (couple.status != "paired") {
                            Text(
                                text = "Invite Code ${couple.inviteCode} • Waiting for partner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (partnerPresence == null) {
                            Text(
                                text = "Paired privately • Tap for status",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (partnerPresence.deviceStatus == "ACTIVE_IN_DUET") {
                            Text(
                                text = "Active in Duet • Online",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = BatteryHealthyGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = partnerPresence.liveActivityLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = partnerPresence.lastSeenIstLabel(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("chat_partner_last_seen")
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EncryptedEmeraldContainer,
                            modifier = Modifier
                                .glossyPressEffect(cornerRadius = 16.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenEncryptionInfo)
                                .testTag("chat_e2ee_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = "End-to-end encrypted",
                                    tint = EncryptedEmerald,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "E2EE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = EncryptedEmerald
                                )
                            }
                        }

                        // Compact sleek chat settings pill button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier
                                .size(36.dp)
                                .glossyPressEffect(cornerRadius = 18.dp, pressScale = 0.90f)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenChatSettings)
                                .testTag("chat_settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Private chat settings",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Messages Area wrapped with Live Chat Background Wallpaper
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ChatWallpaperBackdrop(
                    appearance = appearance,
                    modifier = Modifier.fillMaxSize()
                )

                if (state.messages.isEmpty() && !isPartnerTyping) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = if (hasCustomBackdrop) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                            } else {
                                Color.Transparent
                            },
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(EncryptedEmeraldContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = EncryptedEmerald,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.chat_empty_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.chat_empty_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Double-tap any message to ❤️ • Long-press to react or delete",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(state.messages, key = { _, item -> item.raw.id }) { index, item ->
                            val isMine = item.raw.senderUid == myUid
                            val isSeen = item.raw.isSeenByOther(myUid)
                            val curDayKey = IndiaTimeFormatter.dayBucketKey(item.raw.createdAt?.toDate())
                            val prevDayKey = if (index > 0) {
                                IndiaTimeFormatter.dayBucketKey(state.messages[index - 1].raw.createdAt?.toDate())
                            } else {
                                null
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (curDayKey != prevDayKey) {
                                    ChatDayDividerPill(
                                        label = IndiaTimeFormatter.formatDayHeader(item.raw.createdAt?.toDate()),
                                        isDarkBackdrop = isDarkBackdrop
                                    )
                                }
                                MessageBubble(
                                    item = item,
                                    isMine = isMine,
                                    isSeen = isSeen,
                                    isDarkBackdrop = isDarkBackdrop,
                                    hasCustomBackdrop = hasCustomBackdrop,
                                    showInstagramReceiptFooter = isMine && index == lastOutgoingIndex,
                                    onTapPhoto = { lightboxItem = item },
                                    onQuickHeart = {
                                        onReactMessage(item.raw.id, item.raw.effectiveReactions(), "HEART")
                                    },
                                    onOpenActions = { actionTargetMessageId = item.raw.id }
                                )
                            }
                        }

                        if (isPartnerTyping) {
                            item(key = "instagram_typing_indicator") {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(tween(200)) + slideInVertically { it / 2 },
                                    exit = fadeOut(tween(180)) + slideOutVertically { it / 2 }
                                ) {
                                    InstagramTypingBubble(
                                        partnerName = partnerName,
                                        isDarkBackdrop = isDarkBackdrop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selected DM Photo Attachment Preview
            if (pendingPhotoUri != null || pendingStagedFile != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val bmp = pendingPreviewBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Selected photo preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (stagingPhoto) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (stagingPhoto) "Preparing HD photo…" else "HD photo ready for encrypted DM",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "HD 1920px • AES-256-GCM end-to-end encrypted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                pendingStagedFile?.delete()
                                pendingStagedFile = null
                                pendingPhotoUri = null
                                pendingPreviewBitmap = null
                                stagingPhoto = false
                            },
                            modifier = Modifier.glossyPressEffect(cornerRadius = 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel photo"
                            )
                        }
                    }
                }
            }

            // Rounded Floating Composer Bar (lifts cleanly above floating pill nav when keyboard is closed)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = extraBottomNavPadding + 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 5.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .exclusiveAnimatedBorder(
                            cornerRadius = 26.dp,
                            strokeWidth = 1.6.dp,
                            fallbackColor = Color.White.copy(alpha = if (appearance.themePreset.isDark) 0.16f else 0.78f),
                            fallbackWidth = 0.9.dp
                        )
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .glossyPressEffect(cornerRadius = 24.dp)
                            .testTag("chat_pick_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Share picture in DM",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onSendHeartPulse,
                        modifier = Modifier
                            .glossyPressEffect(cornerRadius = 24.dp)
                            .testTag("chat_heart_pulse_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Send heartbeat in chat",
                            tint = StatusLiveCoral
                        )
                    }

                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { next ->
                            if (next.length <= 1000) {
                                draftText = next
                                onTypingChanged(next.isNotBlank())
                            }
                        },
                        placeholder = {
                            Text(
                                if (pendingPhotoUri != null || pendingStagedFile != null) "Add caption for photo…"
                                else stringResource(R.string.chat_input_hint)
                            )
                        },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    FilledIconButton(
                        onClick = {
                            val photo = pendingPhotoUri
                            val staged = pendingStagedFile
                            if (photo != null || staged != null) {
                                onSendPhoto(photo, staged, draftText)
                                pendingPhotoUri = null
                                pendingStagedFile = null
                                pendingPreviewBitmap = null
                                draftText = ""
                                onTypingChanged(false)
                            } else if (draftText.trim().isNotEmpty()) {
                                onSendText(draftText)
                                draftText = ""
                                onTypingChanged(false)
                            }
                        },
                        enabled = !state.busyAction && !stagingPhoto &&
                            (pendingPhotoUri != null || pendingStagedFile != null || draftText.trim().isNotEmpty()),
                        modifier = Modifier
                            .size(48.dp)
                            .glossyPressEffect(cornerRadius = 24.dp)
                            .testTag("chat_send_button")
                    ) {
                        if (state.busyAction || stagingPhoto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send encrypted message"
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

/**
 * Renders the selected chat background: theme default, artistic vector wallpaper preset, or custom photo.
 */
@Composable
private fun ChatWallpaperBackdrop(
    appearance: DuetAppearanceState,
    modifier: Modifier = Modifier
) {
    val preset = appearance.chatWallpaper
    if (preset == ChatWallpaperPreset.THEME_DEFAULT) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.background))
        return
    }

    if (preset == ChatWallpaperPreset.CUSTOM_PHOTO && !appearance.customChatWallpaperPath.isNullOrBlank()) {
        var customBitmap by remember(appearance.customChatWallpaperPath, appearance.customChatWallpaperVersion) {
            mutableStateOf<Bitmap?>(null)
        }
        LaunchedEffect(appearance.customChatWallpaperPath, appearance.customChatWallpaperVersion) {
            customBitmap = withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(appearance.customChatWallpaperPath) }.getOrNull()
            }
        }
        Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
            val bmp = customBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = appearance.chatWallpaperDimAlpha.coerceIn(0f, 0.75f)))
            )
        }
        return
    }

    // Curated Artistic Preset Backgrounds rendered cleanly via Compose Canvas
    Box(
        modifier = modifier.background(Brush.verticalGradient(preset.gradientColors))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (preset) {
                ChatWallpaperPreset.VELVET_STARS -> {
                    val starCoords = listOf(
                        0.12f to 0.08f, 0.84f to 0.12f, 0.36f to 0.19f, 0.68f to 0.27f,
                        0.18f to 0.38f, 0.91f to 0.42f, 0.48f to 0.51f, 0.25f to 0.64f,
                        0.76f to 0.68f, 0.14f to 0.82f, 0.61f to 0.86f, 0.87f to 0.91f
                    )
                    starCoords.forEachIndexed { idx, (fx, fy) ->
                        val r = if (idx % 3 == 0) 3.8.dp.toPx() else 2.2.dp.toPx()
                        val center = Offset(w * fx, h * fy)
                        drawCircle(
                            color = preset.accentDotColor.copy(alpha = if (idx % 2 == 0) 0.42f else 0.22f),
                            radius = r,
                            center = center
                        )
                        if (idx % 3 == 0) {
                            drawCircle(
                                color = preset.accentDotColor.copy(alpha = 0.12f),
                                radius = r * 3.4f,
                                center = center
                            )
                        }
                    }
                }

                ChatWallpaperPreset.MIDNIGHT_AURORA -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x444DF3FF), Color.Transparent),
                            center = Offset(w * 0.2f, h * 0.22f),
                            radius = w * 0.65f
                        ),
                        radius = w * 0.65f,
                        center = Offset(w * 0.2f, h * 0.22f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x40F66CFF), Color.Transparent),
                            center = Offset(w * 0.82f, h * 0.68f),
                            radius = w * 0.72f
                        ),
                        radius = w * 0.72f,
                        center = Offset(w * 0.82f, h * 0.68f)
                    )
                }

                ChatWallpaperPreset.ROYAL_GOLD_WEAVE -> {
                    val step = 54.dp.toPx()
                    val strokeColor = Color(0xFFF5CA6E).copy(alpha = 0.12f)
                    var y = 0f
                    while (y < h + step) {
                        var x = 0f
                        while (x < w + step) {
                            val p = Path().apply {
                                moveTo(x, y - step / 2f)
                                lineTo(x + step / 2f, y)
                                lineTo(x, y + step / 2f)
                                lineTo(x - step / 2f, y)
                                close()
                            }
                            drawPath(p, color = strokeColor, style = Stroke(width = 1.dp.toPx()))
                            x += step
                        }
                        y += step
                    }
                }

                ChatWallpaperPreset.WHISPER_HEARTS -> {
                    val spots = listOf(
                        0.16f to 0.14f, 0.78f to 0.20f, 0.42f to 0.36f,
                        0.85f to 0.54f, 0.22f to 0.66f, 0.64f to 0.80f
                    )
                    spots.forEachIndexed { i, (fx, fy) ->
                        val s = (12 + (i % 3) * 5).dp.toPx()
                        val cx = w * fx
                        val cy = h * fy
                        val heartPath = Path().apply {
                            moveTo(cx, cy + s * 0.45f)
                            cubicTo(cx - s, cy - s * 0.1f, cx - s * 0.55f, cy - s * 0.9f, cx, cy - s * 0.32f)
                            cubicTo(cx + s * 0.55f, cy - s * 0.9f, cx + s, cy - s * 0.1f, cx, cy + s * 0.45f)
                            close()
                        }
                        drawPath(heartPath, color = preset.accentDotColor.copy(alpha = 0.14f))
                    }
                }

                ChatWallpaperPreset.ROSE_SILK, ChatWallpaperPreset.SAGE_MIST -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(preset.accentDotColor.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(w * 0.85f, h * 0.18f),
                            radius = w * 0.55f
                        ),
                        radius = w * 0.55f,
                        center = Offset(w * 0.85f, h * 0.18f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(preset.accentDotColor.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(w * 0.15f, h * 0.78f),
                            radius = w * 0.6f
                        ),
                        radius = w * 0.6f,
                        center = Offset(w * 0.15f, h * 0.78f)
                    )
                }

                else -> Unit
            }
        }
        val dimAlpha = (appearance.chatWallpaperDimAlpha * 0.65f).coerceIn(0f, 0.55f)
        if (dimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }
    }
}

/**
 * Instagram-DM-inspired 3-dot animated wave typing bubble.
 */
@Composable
private fun InstagramTypingBubble(
    partnerName: String,
    isDarkBackdrop: Boolean
) {
    val transition = rememberInfiniteTransition(label = "instagram_typing_dots")

    val dot0 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, delayMillis = 0, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing_dot_0"
    )
    val dot1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing_dot_1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing_dot_2"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_typing_indicator_bubble"),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 6.dp,
                bottomEnd = 20.dp
            ),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(dot0, dot1, dot2).forEach { progress ->
                    Box(
                        modifier = Modifier
                            .offset(y = (-5.5f * progress).dp)
                            .scale(0.84f + 0.28f * progress)
                            .alpha(0.45f + 0.55f * progress)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = "${partnerName.substringBefore(" ")} is typing…",
            style = MaterialTheme.typography.labelSmall,
            color = if (isDarkBackdrop) Color.White.copy(alpha = 0.82f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun ChatDayDividerPill(
    label: String,
    isDarkBackdrop: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (isDarkBackdrop) Color.White.copy(alpha = 0.16f)
            else Color.White.copy(alpha = 0.82f),
            modifier = Modifier.border(
                width = 0.9.dp,
                color = Color.White.copy(alpha = if (isDarkBackdrop) 0.34f else 0.92f),
                shape = CircleShape
            )
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkBackdrop) Color.White.copy(alpha = 0.92f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    item: DecryptedMessageItem,
    isMine: Boolean,
    isSeen: Boolean,
    isDarkBackdrop: Boolean,
    hasCustomBackdrop: Boolean,
    showInstagramReceiptFooter: Boolean,
    onTapPhoto: () -> Unit,
    onQuickHeart: () -> Unit,
    onOpenActions: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val timeText = IndiaTimeFormatter.formatTime(item.raw.createdAt?.toDate())
    val seenTimeText = IndiaTimeFormatter.formatTime(item.raw.seenAt?.toDate())
    val effectiveReactions = item.raw.effectiveReactions()
    val hasReactions = effectiveReactions.isNotEmpty()

    val bubbleColor = when {
        item.raw.messageType == "LOVE_PULSE" -> StatusLiveCoral
        isMine -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        item.raw.messageType == "LOVE_PULSE" -> Color.White
        isMine -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isMine) 20.dp else 6.dp,
        bottomEnd = if (isMine) 6.dp else 20.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_message_${item.raw.id}"),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                contentColor = contentColor,
                shadowElevation = if (hasCustomBackdrop) 3.dp else 0.dp,
                modifier = Modifier
                    .padding(bottom = if (hasReactions) 14.dp else 0.dp)
                    .widthIn(max = 300.dp)
                    .glossyPressEffect(cornerRadius = 20.dp, pressScale = 0.98f, restingSheenAlpha = 0.08f)
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = {},
                        onDoubleClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onQuickHeart()
                        },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenActions()
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (item.imageBase64 != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .combinedClickable(
                                    onClick = onTapPhoto,
                                    onDoubleClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onQuickHeart()
                                    },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOpenActions()
                                    }
                                )
                        ) {
                            DecryptedPhotoImage(
                                imageBase64 = item.imageBase64,
                                contentDescription = item.text.ifBlank { "Encrypted HD DM photo" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.56f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "HD",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFE082),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (item.text.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (item.raw.messageType == "LOVE_PULSE") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = item.text.ifBlank { "Thinking of you" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor
                            )
                        }
                    } else if (item.text.isNotBlank()) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Instagram / WhatsApp style overlapping corner reaction pill
            if (hasReactions) {
                ReactionCornerPill(
                    reactions = effectiveReactions,
                    onClick = onOpenActions,
                    modifier = Modifier
                        .align(if (isMine) Alignment.BottomStart else Alignment.BottomEnd)
                        .padding(horizontal = 8.dp)
                        .testTag("message_reaction_pill_${item.raw.id}")
                )
            }
        }

        // Single clean Sent / Seen status shown strictly AFTER the sent message (never duplicated inside bubble)
        if (showInstagramReceiptFooter) {
            val receiptTint = when {
                hasCustomBackdrop && isDarkBackdrop -> Color.White.copy(alpha = 0.94f)
                isSeen -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, end = 4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDarkBackdrop) Color.White.copy(alpha = 0.14f)
                        else Color.White.copy(alpha = 0.72f)
                    )
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = if (isDarkBackdrop) 0.28f else 0.90f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 9.dp, vertical = 3.dp)
                    .testTag("chat_latest_receipt_label"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isSeen) Icons.Rounded.DoneAll else Icons.Rounded.Done,
                    contentDescription = if (isSeen) "Seen" else "Sent",
                    tint = receiptTint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = when {
                        isSeen && seenTimeText.isNotBlank() -> "Seen • $seenTimeText"
                        isSeen -> "Seen"
                        else -> "Sent"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSeen) FontWeight.SemiBold else FontWeight.Medium,
                    color = receiptTint
                )
            }
        }
    }
}

@Composable
private fun ReactionCornerPill(
    reactions: Map<String, String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = remember(reactions) {
        reactions.values.groupingBy { it }.eachCount()
    }
    val totalCount = reactions.size

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp,
        tonalElevation = 2.dp,
        modifier = modifier
            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            grouped.entries.forEach { (key, _) ->
                Text(
                    text = reactionKeyToEmoji(key),
                    fontSize = 13.sp
                )
            }
            if (totalCount > 1 && grouped.size == 1) {
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Instagram DM / WhatsApp-style floating reaction bar & message actions dialog shown on long-press.
 */
@Composable
private fun MessageActionOverlayDialog(
    item: DecryptedMessageItem,
    myUid: String,
    partnerName: String,
    onPickReaction: (String) -> Unit,
    onCopyText: () -> Unit,
    onDeleteMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    val reactions = item.raw.effectiveReactions()
    val myCurrentReaction = reactions[myUid]
    val haptics = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss)
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Floating 6-Emoji Instagram / WhatsApp Reaction Pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .testTag("message_reaction_picker_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CHAT_REACTION_OPTIONS.forEach { (key, emoji) ->
                            val isSelected = myCurrentReaction == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .padding(horizontal = 2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onPickReaction(key)
                                    }
                                    .testTag("reaction_pick_${key.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 21.sp
                                )
                            }
                        }
                    }
                }

                // 2. Compact Preview of Selected Message
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (item.raw.senderUid == myUid) "You" else partnerName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when {
                                item.text.isNotBlank() -> item.text
                                item.imageBase64 != null -> "📷 Encrypted photo"
                                else -> "❤️ Heartbeat"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (reactions.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            val breakdown = reactions.entries.joinToString("  •  ") { (uid, rKey) ->
                                val who = if (uid == myUid) "You" else partnerName.substringBefore(" ")
                                "$who ${reactionKeyToEmoji(rKey)}"
                            }
                            Text(
                                text = breakdown,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Action Menu (Copy & Delete Message)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (item.text.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onCopyText)
                                    .padding(horizontal = 18.dp, vertical = 15.dp)
                                    .testTag("copy_chat_message_action"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Copy Message",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onDeleteMessage)
                                .padding(horizontal = 18.dp, vertical = 15.dp)
                                .testTag("delete_chat_message_action"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Delete Message",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun reactionKeyToEmoji(key: String): String = when (key.uppercase()) {
    "HEART" -> "❤️"
    "LAUGH" -> "😂"
    "WOW" -> "😮"
    "SAD" -> "😢"
    "PRAY" -> "🙏"
    "FIRE" -> "🔥"
    "KISS" -> "😘"
    "SPARKLE" -> "✨"
    "HUG" -> "🫂"
    else -> "❤️"
}
