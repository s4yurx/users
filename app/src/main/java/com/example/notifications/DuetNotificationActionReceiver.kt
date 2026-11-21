package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.crypto.DuetCryptoManager
import com.example.data.DuetRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Handles background notification actions (Direct Inline Reply with E2EE encryption,
 * Send Pulse Back, and Send Comfort Hug) even when the Duet UI is closed.
 */
class DuetNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val uid = Firebase.auth.currentUser?.uid
                if (uid.isNullOrBlank()) {
                    Log.w(TAG, "Ignoring notification action while signed out")
                    return@launch
                }

                val repository = DuetRepository(appContext)
                val coupleId = intent.getStringExtra(DuetNotificationHelper.EXTRA_COUPLE_ID)
                    ?.takeIf { it.isNotBlank() }
                    ?: repository.fetchUserProfileOnce(uid)?.coupleId
                    ?: return@launch

                val couple = repository.fetchCoupleOnce(coupleId) ?: return@launch
                val partnerName = intent.getStringExtra(DuetNotificationHelper.EXTRA_PARTNER_NAME)
                    ?.takeIf { it.isNotBlank() }
                    ?: couple.partnerDisplayName(uid)
                val notificationId = intent.getIntExtra(
                    DuetNotificationHelper.EXTRA_NOTIFICATION_ID,
                    DuetNotificationHelper.NOTIFICATION_ID_MESSAGE
                )

                when (action) {
                    DuetNotificationHelper.ACTION_INLINE_REPLY -> {
                        val replyText = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(DuetNotificationHelper.KEY_TEXT_REPLY)
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                        if (replyText.isNotEmpty()) {
                            val crypto = DuetCryptoManager(appContext, uid)
                            val payload = JSONObject().apply { put("text", replyText.take(1000)) }.toString()
                            val envelope = crypto.encrypt(
                                plaintext = payload,
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
                                if (notificationId != DuetNotificationHelper.NOTIFICATION_ID_MESSAGE) {
                                    NotificationManagerCompat.from(appContext).cancel(notificationId)
                                }
                                DuetNotificationHelper.showReplyConfirmedNotification(
                                    context = appContext,
                                    coupleId = couple.id,
                                    partnerName = partnerName,
                                    replyText = replyText
                                )
                            }
                        }
                    }

                    DuetNotificationHelper.ACTION_SEND_PULSE_BACK -> {
                        repository.sendLovePulse(couple)
                        NotificationManagerCompat.from(appContext).cancel(notificationId)
                    }

                    DuetNotificationHelper.ACTION_SEND_HUG -> {
                        val moodId = intent.getStringExtra(DuetNotificationHelper.EXTRA_MOOD_ID)
                        if (!moodId.isNullOrBlank()) {
                            repository.sendComfortHug(couple.id, moodId)
                        }
                        NotificationManagerCompat.from(appContext).cancel(notificationId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed handling notification action $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "DuetNotifReceiver"
    }
}
