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

class DuetLoveWidgetProvider : AppWidgetProvider() {

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
            val component = ComponentName(context, DuetLoveWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, DuetLoveWidgetProvider::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.widget_duet_love)

            views.setInt(R.id.widget_root, "setBackgroundResource", preset.widgetBgRes)
            views.setTextViewText(R.id.widget_couple_title, snapshot.coupleTitle)
            views.setTextColor(R.id.widget_couple_title, preset.widgetTitleColorInt())
            views.setTextViewText(R.id.widget_battery_pill, snapshot.batteryBadge)
            views.setTextColor(R.id.widget_battery_pill, preset.widgetChipTextColorInt())
            views.setTextViewText(R.id.widget_mood_line, snapshot.moodLine)
            views.setTextColor(R.id.widget_mood_line, preset.widgetBodyColorInt())
            views.setTextViewText(R.id.widget_countdown_line, snapshot.countdownLine)
            views.setTextColor(R.id.widget_countdown_line, preset.widgetSubColorInt())
            views.setTextViewText(R.id.widget_status_footer, snapshot.statusFooter)
            views.setTextColor(R.id.widget_status_footer, preset.widgetSubColorInt())

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
