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
import com.example.notifications.DuetNotificationHelper

/**
 * Home Screen Widget 5: Quick Mood Check-In Widget (1-tap In Love / Miss U / Cozy / Need Hugs).
 */
class DuetMoodWidgetProvider : AppWidgetProvider() {

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
            val component = ComponentName(context, DuetMoodWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, DuetMoodWidgetProvider::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.widget_duet_mood)

            val titleColor = preset.widgetTitleColorInt()
            val subColor = preset.widgetSubColorInt()
            val chipColor = preset.widgetChipTextColorInt()

            views.setInt(R.id.mood_widget_root, "setBackgroundResource", preset.widgetBgRes)
            views.setTextColor(R.id.mood_widget_title, titleColor)
            views.setTextColor(R.id.mood_widget_open_btn, chipColor)
            views.setTextViewText(R.id.mood_widget_current, snapshot.moodLine)
            views.setTextColor(R.id.mood_widget_current, subColor)

            views.setTextColor(R.id.mood_btn_love, chipColor)
            views.setTextColor(R.id.mood_btn_miss, chipColor)
            views.setTextColor(R.id.mood_btn_cozy, chipColor)
            views.setTextColor(R.id.mood_btn_hugs, chipColor)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DuetNotificationHelper.EXTRA_OPEN_TAB, "home")
            }
            val openPi = PendingIntent.getActivity(
                context,
                5100 + appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.mood_widget_title, openPi)
            views.setOnClickPendingIntent(R.id.mood_widget_open_btn, openPi)

            views.setOnClickPendingIntent(R.id.mood_btn_love, moodPendingIntent(context, 5201 + appWidgetId, "IN_LOVE"))
            views.setOnClickPendingIntent(R.id.mood_btn_miss, moodPendingIntent(context, 5202 + appWidgetId, "MISSING_YOU"))
            views.setOnClickPendingIntent(R.id.mood_btn_cozy, moodPendingIntent(context, 5203 + appWidgetId, "COZY"))
            views.setOnClickPendingIntent(R.id.mood_btn_hugs, moodPendingIntent(context, 5204 + appWidgetId, "NEED_HUGS"))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun moodPendingIntent(context: Context, requestCode: Int, moodKey: String): PendingIntent {
            val intent = Intent(context, DuetWidgetActionReceiver::class.java).apply {
                action = DuetWidgetActionReceiver.ACTION_WIDGET_QUICK_MOOD
                putExtra(DuetWidgetActionReceiver.EXTRA_MOOD_KEY, moodKey)
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
