package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.DuetNotificationHelper
import com.example.ui.DuetRootApp
import com.example.ui.theme.DuetAppearanceStore
import com.example.ui.theme.DuetTheme

class MainActivity : ComponentActivity() {

    private var pendingTabRoute by mutableStateOf<String?>(null)
    private var pendingOpenUpdatePrompt by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DuetNotificationHelper.ensureChannels(this)
        pendingTabRoute = intent?.getStringExtra(DuetNotificationHelper.EXTRA_OPEN_TAB)
        pendingOpenUpdatePrompt = intent?.getBooleanExtra(DuetNotificationHelper.EXTRA_OPEN_UPDATE_PROMPT, false) == true
        enableEdgeToEdge()
        setContent {
            val appearance by DuetAppearanceStore.state(this).collectAsStateWithLifecycle()
            DuetTheme(themePreset = appearance.themePreset) {
                DuetRootApp(
                    appearance = appearance,
                    requestedTabRoute = pendingTabRoute,
                    onRequestedTabConsumed = { pendingTabRoute = null },
                    openUpdatePromptRequested = pendingOpenUpdatePrompt,
                    onUpdatePromptConsumed = { pendingOpenUpdatePrompt = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(DuetNotificationHelper.EXTRA_OPEN_TAB)?.let { route ->
            pendingTabRoute = route
        }
        if (intent.getBooleanExtra(DuetNotificationHelper.EXTRA_OPEN_UPDATE_PROMPT, false)) {
            pendingOpenUpdatePrompt = true
        }
    }
}
