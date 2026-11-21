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
 * Home Screen Widget 6: Today's Couple Question + Unlock Status + IST Expiry Countdown Widget.
 */
class DuetQuestionWidgetProvider : AppWidgetProvider() {

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
            val component = ComponentName(context, DuetQuestionWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, DuetQuestionWidgetProvider::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.widget_duet_question)

            val titleColor = preset.widgetTitleColorInt()
            val subColor = preset.widgetSubColorInt()
            val chipColor = preset.widgetChipTextColorInt()

            views.setInt(R.id.question_widget_root, "setBackgroundResource", preset.widgetBgRes)
            views.setTextColor(R.id.question_widget_header, subColor)
            views.setTextViewText(R.id.question_widget_expiry, snapshot.dailyQuestionExpiry)
            views.setTextColor(R.id.question_widget_expiry, chipColor)
            views.setTextViewText(R.id.question_widget_prompt, snapshot.dailyQuestionText)
            views.setTextColor(R.id.question_widget_prompt, titleColor)
            views.setTextViewText(R.id.question_widget_status, snapshot.dailyQuestionStatus)
            views.setTextColor(R.id.question_widget_status, titleColor)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DuetNotificationHelper.EXTRA_OPEN_TAB, "home")
            }
            views.setOnClickPendingIntent(
                R.id.question_widget_root,
                PendingIntent.getActivity(
                    context,
                    6100 + appWidgetId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
