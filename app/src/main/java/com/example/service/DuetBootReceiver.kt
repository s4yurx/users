package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notifications.DuetNotificationStateStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * Restarts Duet's persistent background sync service after device reboot or app update
 * when the user is signed in and has background sync enabled.
 */
class DuetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val appCtx = context.applicationContext
            if (Firebase.auth.currentUser != null && DuetNotificationStateStore(appCtx).isBackgroundSyncEnabled()) {
                DuetSyncService.start(appCtx)
            }
        }
    }
}
