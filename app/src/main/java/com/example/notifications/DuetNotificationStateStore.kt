package com.example.notifications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists background-sync preference and deduplication markers so real-time Firestore listeners
 * in DuetSyncService notify only on newly arrived partner events and never replay history.
 */
class DuetNotificationStateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isBackgroundSyncEnabled(): Boolean = prefs.getBoolean(KEY_BG_SYNC_ENABLED, true)

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BG_SYNC_ENABLED, enabled).apply()
    }

    fun hasNotified(eventKey: String): Boolean {
        val current = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()).orEmpty()
        return eventKey in current
    }

    fun markNotified(eventKey: String) {
        val mutable = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()).orEmpty().toMutableList()
        if (eventKey !in mutable) {
            mutable.add(eventKey)
            val trimmed = if (mutable.size > MAX_STORED_KEYS) {
                mutable.takeLast(MAX_STORED_KEYS)
            } else {
                mutable
            }
            prefs.edit().putStringSet(KEY_NOTIFIED_IDS, trimmed.toSet()).apply()
        }
    }

    fun getLastPulseCount(coupleId: String): Long =
        prefs.getLong("${KEY_PULSE_PREFIX}$coupleId", -1L)

    fun setLastPulseCount(coupleId: String, count: Long) {
        prefs.edit().putLong("${KEY_PULSE_PREFIX}$coupleId", count).apply()
    }

    fun clearCoupleNotifications() {
        val editor = prefs.edit()
        editor.remove(KEY_NOTIFIED_IDS)
        prefs.all.keys
            .filter { it.startsWith(KEY_PULSE_PREFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    fun setLastExcludedInviteCode(code: String) {
        val digits = code.filter { it.isDigit() }
        if (digits.length == 6) {
            prefs.edit().putString(KEY_LAST_EXCLUDED_CODE, digits).apply()
        }
    }

    fun getLastExcludedInviteCode(): String? =
        prefs.getString(KEY_LAST_EXCLUDED_CODE, null)?.takeIf { it.length == 6 }

    fun setPendingRemovalNotice(message: String) {
        if (message.isNotBlank()) {
            prefs.edit().putString(KEY_PENDING_REMOVAL_NOTICE, message.trim()).apply()
        }
    }

    fun consumePendingRemovalNotice(): String? {
        val msg = prefs.getString(KEY_PENDING_REMOVAL_NOTICE, null)?.takeIf { it.isNotBlank() }
        if (msg != null) {
            prefs.edit().remove(KEY_PENDING_REMOVAL_NOTICE).apply()
        }
        return msg
    }

    fun getDismissedUpdateSignature(): String? =
        prefs.getString(KEY_DISMISSED_UPDATE_SIG, null)?.takeIf { it.isNotBlank() }

    fun setDismissedUpdateSignature(signature: String?) {
        val editor = prefs.edit()
        if (signature.isNullOrBlank()) {
            editor.remove(KEY_DISMISSED_UPDATE_SIG)
        } else {
            editor.putString(KEY_DISMISSED_UPDATE_SIG, signature)
        }
        editor.apply()
    }

    fun getDismissedBannerSignature(): String? =
        prefs.getString(KEY_DISMISSED_BANNER_SIG, null)?.takeIf { it.isNotBlank() }

    fun setDismissedBannerSignature(signature: String?) {
        val editor = prefs.edit()
        if (signature.isNullOrBlank()) {
            editor.remove(KEY_DISMISSED_BANNER_SIG)
        } else {
            editor.putString(KEY_DISMISSED_BANNER_SIG, signature)
        }
        editor.apply()
    }

    fun getDismissedPromoSignature(): String? =
        prefs.getString(KEY_DISMISSED_PROMO_SIG, null)?.takeIf { it.isNotBlank() }

    fun setDismissedPromoSignature(signature: String?) {
        val editor = prefs.edit()
        if (signature.isNullOrBlank()) {
            editor.remove(KEY_DISMISSED_PROMO_SIG)
        } else {
            editor.putString(KEY_DISMISSED_PROMO_SIG, signature)
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "duet_notification_state"
        private const val KEY_BG_SYNC_ENABLED = "bg_sync_enabled"
        private const val KEY_NOTIFIED_IDS = "notified_event_keys"
        private const val KEY_PULSE_PREFIX = "last_pulse_count_"
        private const val KEY_LAST_EXCLUDED_CODE = "last_excluded_invite_code"
        private const val KEY_PENDING_REMOVAL_NOTICE = "pending_removal_notice"
        private const val KEY_DISMISSED_UPDATE_SIG = "dismissed_ota_update_sig"
        private const val KEY_DISMISSED_BANNER_SIG = "dismissed_ota_banner_sig"
        private const val KEY_DISMISSED_PROMO_SIG = "dismissed_ota_promo_sig"
        private const val MAX_STORED_KEYS = 160

        private val _appForegroundFlow = MutableStateFlow(false)
        val appForegroundFlow: StateFlow<Boolean> = _appForegroundFlow.asStateFlow()

        var isAppInForeground: Boolean
            get() = _appForegroundFlow.value
            set(value) {
                _appForegroundFlow.value = value
            }

        @Volatile
        var isChatTabVisible: Boolean = false
    }
}
