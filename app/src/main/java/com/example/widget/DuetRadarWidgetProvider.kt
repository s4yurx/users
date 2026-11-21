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
 * Home Screen Widget 7: Partner Battery, Network, Device & Active App Radar Widget + 1-tap Care Nudge & Pulse.
 */
class DuetRadarWidgetProvider : AppWidgetProvider() {

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
            val component = ComponentName(context, DuetRadarWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, DuetRadarWidgetProvider::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.widget_duet_radar)

            val titleColor = preset.widgetTitleColorInt()
            val subColor = preset.widgetSubColorInt()
            val chipColor = preset.widgetChipTextColorInt()

            views.setInt(R.id.radar_widget_root, "setBackgroundResource", preset.widgetBgRes)
            views.setTextViewText(R.id.radar_widget_name, "${snapshot.partnerName} • Radar")
            views.setTextColor(R.id.radar_widget_name, titleColor)
            views.setTextViewText(R.id.radar_widget_battery, "🔋 ${snapshot.batteryBadge}")
            views.setTextColor(R.id.radar_widget_battery, chipColor)
            views.setTextViewText(R.id.radar_widget_app, snapshot.partnerActiveApp)
            views.setTextColor(R.id.radar_widget_app, titleColor)
            views.setTextViewText(R.id.radar_widget_device, "📶 ${snapshot.partnerNetwork} • 📱 ${snapshot.partnerDeviceName}")
            views.setTextColor(R.id.radar_widget_device, subColor)

            views.setTextColor(R.id.radar_btn_care, chipColor)
            views.setTextColor(R.id.radar_btn_pulse, titleColor)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DuetNotificationHelper.EXTRA_OPEN_TAB, "home")
            }
            views.setOnClickPendingIntent(
                R.id.radar_widget_name,
                PendingIntent.getActivity(
                    context,
                    7100 + appWidgetId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val careIntent = Intent(context, DuetWidgetActionReceiver::class.java).apply {
                action = DuetWidgetActionReceiver.ACTION_WIDGET_CARE_NUDGE
                putExtra(DuetWidgetActionReceiver.EXTRA_CARE_TYPE, "WATER")
            }
            views.setOnClickPendingIntent(
                R.id.radar_btn_care,
                PendingIntent.getBroadcast(
                    context,
                    7201 + appWidgetId,
                    careIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val pulseIntent = Intent(context, DuetWidgetActionReceiver::class.java).apply {
                action = DuetWidgetActionReceiver.ACTION_WIDGET_PULSE
            }
            views.setOnClickPendingIntent(
                R.id.radar_btn_pulse,
                PendingIntent.getBroadcast(
                    context,
                    7202 + appWidgetId,
                    pulseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
