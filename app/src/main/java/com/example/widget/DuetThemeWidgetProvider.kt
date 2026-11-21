package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.AppThemePreset

/**
 * Interactive Theme & Love Pulse Switcher Home Screen Widget:
 * Switches Duet's live application theme AND all home widgets directly from the launcher,
 * plus fires an instant Love Pulse with a single tap!
 */
class DuetThemeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DuetThemeWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, DuetThemeWidgetProvider::class.java)
            return try {
                manager.requestPinAppWidget(provider, null, null)
            } catch (_: Exception) {
                false
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val snapshot = WidgetStateStore(context).readSnapshot()
            val preset = snapshot.themeStyle.toAppThemePreset()
            val views = RemoteViews(context.packageName, R.layout.widget_duet_theme)

            val titleColor = preset.widgetTitleColorInt()
            val subColor = preset.widgetSubColorInt()
            val chipColor = preset.widgetChipTextColorInt()

            views.setInt(R.id.theme_widget_root, "setBackgroundResource", preset.widgetBgRes)
            views.setTextColor(R.id.theme_widget_title, titleColor)
            views.setTextViewText(R.id.theme_widget_active_name, "Active: ${preset.emoji} ${preset.label}")
            views.setTextColor(R.id.theme_widget_active_name, subColor)

            views.setTextColor(R.id.theme_widget_pulse_btn, chipColor)
            views.setTextColor(R.id.theme_btn_rose, chipColor)
            views.setTextColor(R.id.theme_btn_velvet, chipColor)
            views.setTextColor(R.id.theme_btn_midnight, chipColor)
            views.setTextColor(R.id.theme_btn_lavender, chipColor)
            views.setTextColor(R.id.theme_btn_emerald, chipColor)
            views.setTextColor(R.id.theme_btn_cycle, titleColor)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.theme_widget_title,
                PendingIntent.getActivity(
                    context,
                    3000 + appWidgetId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Instant Love Pulse
            val pulseIntent = Intent(context, DuetWidgetActionReceiver::class.java).apply {
                action = DuetWidgetActionReceiver.ACTION_WIDGET_PULSE
            }
            views.setOnClickPendingIntent(
                R.id.theme_widget_pulse_btn,
                PendingIntent.getBroadcast(
                    context,
                    3100 + appWidgetId,
                    pulseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Direct Theme Buttons
            views.setOnClickPendingIntent(
                R.id.theme_btn_rose,
                switchThemePendingIntent(context, 3201 + appWidgetId, AppThemePreset.BLUSH_ROSE.key)
            )
            views.setOnClickPendingIntent(
                R.id.theme_btn_velvet,
                switchThemePendingIntent(context, 3202 + appWidgetId, AppThemePreset.VELVET_DUSK.key)
            )
            views.setOnClickPendingIntent(
                R.id.theme_btn_midnight,
                switchThemePendingIntent(context, 3203 + appWidgetId, AppThemePreset.MIDNIGHT_OCEAN.key)
            )
            views.setOnClickPendingIntent(
                R.id.theme_btn_lavender,
                switchThemePendingIntent(context, 3204 + appWidgetId, AppThemePreset.LAVENDER_DREAM.key)
            )
            views.setOnClickPendingIntent(
                R.id.theme_btn_emerald,
                switchThemePendingIntent(context, 3205 + appWidgetId, AppThemePreset.EMERALD_FOREST.key)
            )

            // Cycle through all 6 themes
            val cycleIntent = Intent(context, DuetWidgetActionReceiver::class.java).apply {
                action = DuetWidgetActionReceiver.ACTION_CYCLE_THEME
            }
            views.setOnClickPendingIntent(
                R.id.theme_btn_cycle,
                PendingIntent.getBroadcast(
                    context,
                    3206 + appWidgetId,
                    cycleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun switchThemePendingIntent(
            context: Context,
            requestCode: Int,
            themeKey: String
        ): PendingIntent {
            val intent = Intent(context, DuetWidgetActionReceiver::class.java).apply {
                action = DuetWidgetActionReceiver.ACTION_SWITCH_THEME
                putExtra(DuetWidgetActionReceiver.EXTRA_THEME_KEY, themeKey)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
