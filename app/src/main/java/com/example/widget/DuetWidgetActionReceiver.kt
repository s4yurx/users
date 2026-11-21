package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.crypto.DuetCryptoManager
import com.example.data.CareCheckInCatalog
import com.example.data.DuetRepository
import com.example.data.MoodCatalog
import com.example.telemetry.DeviceTelemetryCollector
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.DuetAppearanceStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Handles interactive taps from all 8 Android Home Screen Widgets:
 * - Switching Duet's live application theme across app + all 8 widgets
 * - One-tap end-to-end encrypted quick messages straight from Quick-Chat Widget
 * - One-tap Love Pulse from Theme / Radar / Hub Widgets
 * - One-tap Quick Mood check-in from Mood Check-In Widget
 * - One-tap Care & Wellness Nudge (Hydrate / Eat / Rest) from Radar & Couple Hub Widgets
 */
class DuetWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appCtx = context.applicationContext

        when (action) {
            ACTION_SWITCH_THEME -> {
                val key = intent.getStringExtra(EXTRA_THEME_KEY)
                val preset = AppThemePreset.fromKey(key)
                DuetAppearanceStore.selectTheme(appCtx, preset)
                showToast(appCtx, "Duet theme: ${preset.emoji} ${preset.label}")
            }

            ACTION_CYCLE_THEME -> {
                val next = DuetAppearanceStore.cycleTheme(appCtx)
                showToast(appCtx, "Duet theme: ${next.emoji} ${next.label}")
            }

            ACTION_QUICK_SEND_MESSAGE -> {
                val msgText = intent.getStringExtra(EXTRA_MESSAGE_TEXT)?.trim().orEmpty()
                if (msgText.isEmpty()) return
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        sendQuickEncryptedMessage(appCtx, msgText)
                    } catch (e: Exception) {
                        Log.e(TAG, "Widget quick-send failed", e)
                        showToast(appCtx, "Could not send — open Duet to reconnect")
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_WIDGET_PULSE -> {
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        sendQuickPulse(appCtx)
                    } catch (e: Exception) {
                        Log.e(TAG, "Widget pulse failed", e)
                        showToast(appCtx, "Could not send pulse")
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_WIDGET_QUICK_MOOD -> {
                val moodKey = intent.getStringExtra(EXTRA_MOOD_KEY) ?: "IN_LOVE"
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        sendQuickMood(appCtx, moodKey)
                    } catch (e: Exception) {
                        Log.e(TAG, "Widget quick mood failed", e)
                        showToast(appCtx, "Could not share mood right now")
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_WIDGET_CARE_NUDGE -> {
                val careType = intent.getStringExtra(EXTRA_CARE_TYPE) ?: "WATER"
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        sendQuickCareNudge(appCtx, careType)
                    } catch (e: Exception) {
                        Log.e(TAG, "Widget care nudge failed", e)
                        showToast(appCtx, "Could not send care reminder")
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private suspend fun sendQuickEncryptedMessage(context: Context, text: String) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            showToast(context, "Sign in to Duet first to send private messages")
            return
        }
        val repository = DuetRepository(context)
        val profile = repository.fetchUserProfileOnce(uid)
        val coupleId = profile?.coupleId?.takeIf { it.isNotBlank() }
            ?: WidgetStateStore(context).readSnapshot().coupleId.takeIf { it.isNotBlank() }
        if (coupleId.isNullOrBlank()) {
            showToast(context, "Pair with your partner in Duet first")
            return
        }
        val couple = repository.fetchCoupleOnce(coupleId) ?: run {
            showToast(context, "Could not reach your couple space")
            return
        }

        val crypto = DuetCryptoManager(context, uid)
        val payloadJson = JSONObject().apply { put("text", text) }.toString()
        val envelope = crypto.encrypt(
            plaintext = payloadJson,
            coupleId = couple.id,
            inviteCode = couple.inviteCode,
            wrappedVaultKey = couple.wrappedVaultKey,
            partnerPublicKey = couple.otherPublicKey(uid)
        )
        val result = repository.sendEncryptedMessage(
            couple = couple,
            senderName = couple.myDisplayName(uid),
            messageType = "TEXT",
            envelope = envelope
        )
        if (result.isSuccess) {
            val store = WidgetStateStore(context)
            val prev = store.readSnapshot()
            store.updateChatPreview(
                msg1 = prev.lastMsg2,
                msg2 = "You: $text",
                receipt = "✓ Sent",
                coupleId = couple.id,
                partnerName = couple.partnerDisplayName(uid)
            )
            WidgetStateStore.updateAllDuetWidgets(context)
            showToast(context, "Sent privately: $text")
        } else {
            showToast(context, "Could not send right now")
        }
    }

    private suspend fun sendQuickPulse(context: Context) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            showToast(context, "Sign in to Duet first")
            return
        }
        val repository = DuetRepository(context)
        val profile = repository.fetchUserProfileOnce(uid)
        val coupleId = profile?.coupleId?.takeIf { it.isNotBlank() }
            ?: WidgetStateStore(context).readSnapshot().coupleId.takeIf { it.isNotBlank() }
        if (coupleId.isNullOrBlank()) {
            showToast(context, "Pair with your partner in Duet first")
            return
        }
        val couple = repository.fetchCoupleOnce(coupleId) ?: return
        val res = repository.sendLovePulse(couple)
        if (res.isSuccess) {
            showToast(context, "❤️ Love pulse sent to ${couple.partnerDisplayName(uid)}!")
        } else {
            showToast(context, "Could not send pulse")
        }
    }

    private suspend fun sendQuickMood(context: Context, moodKey: String) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            showToast(context, "Sign in to Duet first")
            return
        }
        val repository = DuetRepository(context)
        val profile = repository.fetchUserProfileOnce(uid)
        val coupleId = profile?.coupleId?.takeIf { it.isNotBlank() }
            ?: WidgetStateStore(context).readSnapshot().coupleId.takeIf { it.isNotBlank() }
        if (coupleId.isNullOrBlank()) {
            showToast(context, "Pair with your partner in Duet first")
            return
        }
        val couple = repository.fetchCoupleOnce(coupleId) ?: return
        val mood = MoodCatalog.byKey(moodKey)
        val myName = couple.myDisplayName(uid)
        val res = repository.logMood(couple, myName, mood, "Shared from Duet Mood Widget")
        val telemetry = DeviceTelemetryCollector(context).readSnapshot()
        repository.upsertMyPresence(
            couple = couple,
            displayName = myName,
            telemetry = telemetry,
            moodKey = mood.key,
            moodNote = "Shared from Duet Mood Widget",
            shareStatus = profile?.shareDeviceStatus ?: true,
            markLastSeenNow = true
        )
        if (res.isSuccess) {
            val store = WidgetStateStore(context)
            val prev = store.readSnapshot()
            store.saveSnapshot(prev.copy(moodLine = "${mood.emoji} You: ${mood.label}"))
            WidgetStateStore.updateAllDuetWidgets(context)
            showToast(context, "Mood shared: ${mood.emoji} ${mood.label}")
        } else {
            showToast(context, "Could not update mood")
        }
    }

    private suspend fun sendQuickCareNudge(context: Context, careType: String) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            showToast(context, "Sign in to Duet first")
            return
        }
        val repository = DuetRepository(context)
        val profile = repository.fetchUserProfileOnce(uid)
        val coupleId = profile?.coupleId?.takeIf { it.isNotBlank() }
            ?: WidgetStateStore(context).readSnapshot().coupleId.takeIf { it.isNotBlank() }
        if (coupleId.isNullOrBlank()) {
            showToast(context, "Pair with your partner in Duet first")
            return
        }
        val couple = repository.fetchCoupleOnce(coupleId) ?: return
        val preset = CareCheckInCatalog.byKey(careType)
        val res = repository.sendCareCheckIn(
            couple = couple,
            senderName = couple.myDisplayName(uid),
            careType = preset.key,
            message = preset.defaultMessage
        )
        if (res.isSuccess) {
            showToast(context, "${preset.emoji} ${preset.label} reminder sent to ${couple.partnerDisplayName(uid)}!")
        } else {
            showToast(context, "Could not send care check-in")
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "DuetWidgetAction"
        const val ACTION_SWITCH_THEME = "com.example.widget.ACTION_SWITCH_THEME"
        const val ACTION_CYCLE_THEME = "com.example.widget.ACTION_CYCLE_THEME"
        const val ACTION_QUICK_SEND_MESSAGE = "com.example.widget.ACTION_QUICK_SEND_MESSAGE"
        const val ACTION_WIDGET_PULSE = "com.example.widget.ACTION_WIDGET_PULSE"
        const val ACTION_WIDGET_QUICK_MOOD = "com.example.widget.ACTION_WIDGET_QUICK_MOOD"
        const val ACTION_WIDGET_CARE_NUDGE = "com.example.widget.ACTION_WIDGET_CARE_NUDGE"
        const val EXTRA_THEME_KEY = "extra_theme_key"
        const val EXTRA_MESSAGE_TEXT = "extra_message_text"
        const val EXTRA_MOOD_KEY = "extra_mood_key"
        const val EXTRA_CARE_TYPE = "extra_care_type"
    }
}
