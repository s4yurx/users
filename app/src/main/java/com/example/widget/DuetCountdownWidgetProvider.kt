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
 * Home Screen Widget 4: Days Together + Anniversary & Partner Birthday Countdown Widget.
 */
class DuetCountdownWidgetProvider : AppWidgetProvider() {

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
            val component = ComponentName(context, DuetCountdownWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, DuetCountdownWidgetProvider::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.widget_duet_countdown)

            val titleColor = preset.widgetTitleColorInt()
            val subColor = preset.widgetSubColorInt()
            val chipColor = preset.widgetChipTextColorInt()

            views.setInt(R.id.cd_widget_root, "setBackgroundResource", preset.widgetBgRes)
            views.setTextViewText(R.id.cd_widget_title, snapshot.coupleTitle)
            views.setTextColor(R.id.cd_widget_title, titleColor)
            views.setTextColor(R.id.cd_widget_tag, chipColor)
            views.setTextViewText(R.id.cd_widget_days_together, snapshot.daysTogetherLabel)
            views.setTextColor(R.id.cd_widget_days_together, titleColor)

            views.setTextColor(R.id.cd_widget_ann_label, subColor)
            views.setTextViewText(R.id.cd_widget_ann_value, snapshot.anniversaryCountdown)
            views.setTextColor(R.id.cd_widget_ann_value, chipColor)

            views.setTextColor(R.id.cd_widget_bday_label, subColor)
            views.setTextViewText(R.id.cd_widget_bday_value, snapshot.birthdayCountdown)
            views.setTextColor(R.id.cd_widget_bday_value, chipColor)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DuetNotificationHelper.EXTRA_OPEN_TAB, "dates")
            }
            views.setOnClickPendingIntent(
                R.id.cd_widget_root,
                PendingIntent.getActivity(
                    context,
                    4100 + appWidgetId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
