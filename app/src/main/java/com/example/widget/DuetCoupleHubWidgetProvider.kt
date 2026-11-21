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
 * Home Screen Widget 8: Duet Couple Hub Widget — Love Jar highlight, Song Vibe dedication,
 * Bucket List progress, and 1-tap Hydrate / Pulse / Open shortcuts.
 */
class DuetCoupleHubWidgetProvider : AppWidgetProvider() {

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
            val component = ComponentName(context, DuetCoupleHubWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, DuetCoupleHubWidgetProvider::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.widget_duet_hub)

            val titleColor = preset.widgetTitleColorInt()
            val subColor = preset.widgetSubColorInt()
            val chipColor = preset.widgetChipTextColorInt()

            views.setInt(R.id.hub_widget_root, "setBackgroundResource", preset.widgetBgRes)
            views.setTextViewText(R.id.hub_widget_title, snapshot.coupleTitle)
            views.setTextColor(R.id.hub_widget_title, titleColor)
            views.setTextViewText(R.id.hub_widget_bucket, "🌟 ${snapshot.bucketSummary}")
            views.setTextColor(R.id.hub_widget_bucket, chipColor)
            views.setTextViewText(R.id.hub_widget_jar, snapshot.loveJarHighlight)
            views.setTextColor(R.id.hub_widget_jar, titleColor)
            views.setTextViewText(R.id.hub_widget_song, snapshot.songVibeHighlight)
            views.setTextColor(R.id.hub_widget_song, subColor)

            views.setTextColor(R.id.hub_btn_water, chipColor)
            views.setTextColor(R.id.hub_btn_pulse, chipColor)
            views.setTextColor(R.id.hub_btn_open, titleColor)

            val waterIntent = Intent(context, DuetWidgetActionReceiver::class.java).apply {
                action = DuetWidgetActionReceiver.ACTION_WIDGET_CARE_NUDGE
                putExtra(DuetWidgetActionReceiver.EXTRA_CARE_TYPE, "WATER")
            }
            views.setOnClickPendingIntent(
                R.id.hub_btn_water,
                PendingIntent.getBroadcast(
                    context,
                    8201 + appWidgetId,
                    waterIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val pulseIntent = Intent(context, DuetWidgetActionReceiver::class.java).apply {
                action = DuetWidgetActionReceiver.ACTION_WIDGET_PULSE
            }
            views.setOnClickPendingIntent(
                R.id.hub_btn_pulse,
                PendingIntent.getBroadcast(
                    context,
                    8202 + appWidgetId,
                    pulseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DuetNotificationHelper.EXTRA_OPEN_TAB, "home")
            }
            val openPi = PendingIntent.getActivity(
                context,
                8203 + appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.hub_btn_open, openPi)
            views.setOnClickPendingIntent(R.id.hub_widget_title, openPi)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
