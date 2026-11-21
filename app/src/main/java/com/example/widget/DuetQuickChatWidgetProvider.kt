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
 * Interactive Private Quick-Chat Home Screen Widget:
 * - Shows partner status / typing indicator and latest 2 decrypted private messages + Seen/Sent receipt
 * - 3 one-tap encrypted quick-send romantic buttons ("Miss you", "Kiss", "Hug")
 * - Reply bar opens translucent WidgetQuickChatActivity overlay right on the home screen to type & send!
 */
class DuetQuickChatWidgetProvider : AppWidgetProvider() {

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
            val component = ComponentName(context, DuetQuickChatWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, DuetQuickChatWidgetProvider::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.widget_duet_quick_chat)

            val titleColor = preset.widgetTitleColorInt()
            val bodyColor = preset.widgetBodyColorInt()
            val subColor = preset.widgetSubColorInt()
            val chipColor = preset.widgetChipTextColorInt()

            views.setInt(R.id.chat_widget_root, "setBackgroundResource", preset.widgetBgRes)
            views.setTextViewText(R.id.chat_widget_partner, snapshot.partnerName)
            views.setTextColor(R.id.chat_widget_partner, titleColor)

            val statusText = if (snapshot.partnerTyping) "••• Typing right now" else snapshot.partnerStatus
            views.setTextViewText(R.id.chat_widget_status, statusText)
            views.setTextColor(R.id.chat_widget_status, subColor)

            views.setTextColor(R.id.chat_widget_open_pill, chipColor)
            views.setTextViewText(R.id.chat_widget_msg_1, snapshot.lastMsg1)
            views.setTextColor(R.id.chat_widget_msg_1, bodyColor)
            views.setTextViewText(R.id.chat_widget_msg_2, snapshot.lastMsg2)
            views.setTextColor(R.id.chat_widget_msg_2, titleColor)
            views.setTextViewText(R.id.chat_widget_receipt, snapshot.lastMsgReceipt)
            views.setTextColor(R.id.chat_widget_receipt, subColor)

            views.setTextColor(R.id.chat_widget_quick_1, chipColor)
            views.setTextColor(R.id.chat_widget_quick_2, chipColor)
            views.setTextColor(R.id.chat_widget_quick_3, chipColor)
            views.setTextColor(R.id.chat_widget_compose_bar, titleColor)

            // Tap Open pill -> jump straight to Private Chat tab in MainActivity
            val openChatIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DuetNotificationHelper.EXTRA_OPEN_TAB, "chat")
            }
            val openChatPi = PendingIntent.getActivity(
                context,
                2100 + appWidgetId,
                openChatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.chat_widget_open_pill, openChatPi)

            // Tap Reply Bar -> launch translucent quick-chat overlay right on the home screen!
            val composeIntent = Intent(context, WidgetQuickChatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetQuickChatActivity.EXTRA_COUPLE_ID, snapshot.coupleId)
            }
            val composePi = PendingIntent.getActivity(
                context,
                2200 + appWidgetId,
                composeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.chat_widget_compose_bar, composePi)
            views.setOnClickPendingIntent(R.id.chat_widget_msg_2, composePi)

            // 3 one-tap encrypted quick-send pills
            views.setOnClickPendingIntent(
                R.id.chat_widget_quick_1,
                quickSendPendingIntent(context, 2301 + appWidgetId, "❤️ Miss you so much")
            )
            views.setOnClickPendingIntent(
                R.id.chat_widget_quick_2,
                quickSendPendingIntent(context, 2302 + appWidgetId, "😘 Sending you a sweet kiss")
            )
            views.setOnClickPendingIntent(
                R.id.chat_widget_quick_3,
                quickSendPendingIntent(context, 2303 + appWidgetId, "🫂 Sending you a big warm hug")
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun quickSendPendingIntent(
            context: Context,
            requestCode: Int,
            messageText: String
        ): PendingIntent {
            val intent = Intent(context, DuetWidgetActionReceiver::class.java).apply {
                action = DuetWidgetActionReceiver.ACTION_QUICK_SEND_MESSAGE
                putExtra(DuetWidgetActionReceiver.EXTRA_MESSAGE_TEXT, messageText)
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
