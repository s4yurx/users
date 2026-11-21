package com.example.widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.crypto.DuetCryptoManager
import com.example.data.CoupleSpace
import com.example.data.DecryptedMessageItem
import com.example.data.DuetRepository
import com.example.data.IndiaTimeFormatter
import com.example.notifications.DuetNotificationHelper
import com.example.ui.components.glossyPressEffect
import com.example.ui.theme.DuetAppearanceStore
import com.example.ui.theme.DuetTheme
import com.example.ui.theme.EncryptedEmerald
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Translucent Home-Screen Private Quick-Chat Popup launched directly from the Quick-Chat Widget.
 * Allows reading recent encrypted messages and sending real ECDH/AES-256-GCM replies without
 * leaving the launcher.
 */
class WidgetQuickChatActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialCoupleId = intent?.getStringExtra(EXTRA_COUPLE_ID)

        setContent {
            val appearance by DuetAppearanceStore.state(this).collectAsStateWithLifecycle()
            DuetTheme(themePreset = appearance.themePreset) {
                WidgetQuickChatOverlay(
                    initialCoupleId = initialCoupleId,
                    onOpenFullChat = {
                        val openIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(DuetNotificationHelper.EXTRA_OPEN_TAB, "chat")
                        }
                        startActivity(openIntent)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_COUPLE_ID = "extra_couple_id"
    }
}

@Composable
private fun WidgetQuickChatOverlay(
    initialCoupleId: String?,
    onOpenFullChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val activityContext = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser
    val uid = currentUser?.uid

    var couple by remember { mutableStateOf<CoupleSpace?>(null) }
    var recentMessages by remember { mutableStateOf<List<DecryptedMessageItem>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid, initialCoupleId) {
        if (uid.isNullOrBlank()) {
            statusBanner = "Sign in to Duet to chat privately from your widget."
            return@LaunchedEffect
        }
        val repo = DuetRepository(activityContext)
        val crypto = DuetCryptoManager(activityContext, uid)
        val profile = repo.fetchUserProfileOnce(uid)
        val resolvedCoupleId = profile?.coupleId?.takeIf { it.isNotBlank() }
            ?: initialCoupleId?.takeIf { it.isNotBlank() }
            ?: WidgetStateStore(activityContext).readSnapshot().coupleId.takeIf { it.isNotBlank() }

        if (resolvedCoupleId.isNullOrBlank()) {
            statusBanner = "Pair with your partner in Duet first."
            return@LaunchedEffect
        }

        val loadedCouple = repo.fetchCoupleOnce(resolvedCoupleId)
        couple = loadedCouple
        if (loadedCouple == null) {
            statusBanner = "Could not load your private space."
            return@LaunchedEffect
        }

        repo.observeMessages(resolvedCoupleId, uid)
            .catch { statusBanner = "Reconnect to view latest messages." }
            .collect { rawList ->
                val decrypted = rawList.takeLast(4).map { raw ->
                    val plain = crypto.decrypt(
                        ciphertext = raw.ciphertext,
                        ivBase64 = raw.iv,
                        keyMode = raw.keyMode,
                        coupleId = loadedCouple.id,
                        inviteCode = loadedCouple.inviteCode,
                        wrappedVaultKey = loadedCouple.wrappedVaultKey,
                        partnerPublicKey = loadedCouple.otherPublicKey(uid)
                    )
                    val text = if (plain != null) {
                        runCatching { JSONObject(plain).optString("text", "") }.getOrDefault(plain)
                    } else {
                        "Encrypted message"
                    }
                    DecryptedMessageItem(raw = raw, text = text.ifBlank { "📷 Encrypted photo" })
                }
                recentMessages = decrypted

                // Mark unread partner messages as Seen when viewing quick chat
                val unreadPartnerIds = rawList
                    .filter { it.senderUid != uid && it.seenByUid.isNullOrBlank() && it.id.isNotBlank() }
                    .map { it.id }
                if (unreadPartnerIds.isNotEmpty()) {
                    scope.launch { repo.markMessagesSeen(resolvedCoupleId, unreadPartnerIds) }
                }
            }
    }

    val sendPrivateText: (String) -> Unit = { rawMessage ->
        val clean = rawMessage.trim()
        val activeCouple = couple
        if (clean.isNotEmpty() && activeCouple != null && !uid.isNullOrBlank() && !sending) {
            sending = true
            statusBanner = null
            scope.launch {
                val repo = DuetRepository(activityContext)
                val crypto = DuetCryptoManager(activityContext, uid)
                val payloadJson = JSONObject().apply { put("text", clean) }.toString()
                val envelope = crypto.encrypt(
                    plaintext = payloadJson,
                    coupleId = activeCouple.id,
                    inviteCode = activeCouple.inviteCode,
                    wrappedVaultKey = activeCouple.wrappedVaultKey,
                    partnerPublicKey = activeCouple.otherPublicKey(uid)
                )
                val res = repo.sendEncryptedMessage(
                    couple = activeCouple,
                    senderName = activeCouple.myDisplayName(uid),
                    messageType = "TEXT",
                    envelope = envelope
                )
                if (res.isSuccess) {
                    draft = ""
                    val store = WidgetStateStore(activityContext)
                    val prev = store.readSnapshot()
                    store.updateChatPreview(
                        msg1 = prev.lastMsg2,
                        msg2 = "You: $clean",
                        receipt = "✓ Sent",
                        coupleId = activeCouple.id,
                        partnerName = activeCouple.partnerDisplayName(uid)
                    )
                    WidgetStateStore.updateAllDuetWidgets(activityContext)
                } else {
                    statusBanner = "Could not send message. Try again."
                }
                sending = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.44f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 22.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .border(
                    width = 1.4.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { /* consume taps inside card */ }
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = couple?.partnerDisplayName(uid ?: "") ?: "Private Widget Chat",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = EncryptedEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "End-to-End Encrypted Quick Chat",
                                style = MaterialTheme.typography.labelSmall,
                                color = EncryptedEmerald
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onOpenFullChat,
                            modifier = Modifier
                                .glossyPressEffect(cornerRadius = 24.dp)
                                .testTag("widget_overlay_open_app")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = "Open full chat"
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .glossyPressEffect(cornerRadius = 24.dp)
                                .testTag("widget_overlay_close")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close quick chat"
                            )
                        }
                    }
                }

                // Recent decrypted messages
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (recentMessages.isEmpty()) {
                        Text(
                            text = statusBanner ?: "Say something sweet — your message is encrypted on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        recentMessages.forEach { msg ->
                            val isMine = msg.raw.senderUid == uid
                            val isSeen = !msg.raw.seenByUid.isNullOrBlank()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isMine) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isMine) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = IndiaTimeFormatter.formatTime(msg.raw.createdAt?.toDate()),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = (if (isMine) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.72f)
                                            )
                                            if (isMine) {
                                                Icon(
                                                    imageVector = if (isSeen) Icons.Rounded.DoneAll else Icons.Rounded.Done,
                                                    contentDescription = if (isSeen) "Seen" else "Sent",
                                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = if (isSeen) "Seen" else "Sent",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick romantic reply pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("❤️ Miss you", "😘 Kiss", "🫂 Big hug", "🌙 Thinking of you").forEach { chip ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                            modifier = Modifier
                                .glossyPressEffect(cornerRadius = 20.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !sending && couple != null) {
                                    sendPrivateText(chip)
                                }
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Input row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 500) draft = it },
                        placeholder = { Text("Type private reply…") },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("widget_overlay_message_input")
                    )
                    FilledIconButton(
                        onClick = { sendPrivateText(draft) },
                        enabled = draft.trim().isNotEmpty() && !sending && couple != null,
                        modifier = Modifier
                            .size(48.dp)
                            .glossyPressEffect(cornerRadius = 24.dp)
                            .testTag("widget_overlay_send_button")
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send encrypted reply"
                            )
                        }
                    }
                }
            }
        }
    }
}
