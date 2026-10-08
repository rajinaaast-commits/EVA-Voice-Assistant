package com.example.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat

/**
 * Native Android NotificationListenerService for EVA AI.
 * Enables EVA to observe incoming notifications, announce callers/messages,
 * and integrate with Android's notification access subsystem.
 */
class EVANotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val pkg = sbn.packageName ?: return
        // Do not process our own notifications
        if (pkg == packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isNotBlank() || text.isNotBlank()) {
            lastNotificationText = "$title: $text"
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    companion object {
        var isConnected: Boolean = false
            private set

        var lastNotificationText: String? = null
            private set

        /**
         * Real system check whether EVA AI has been granted Notification Access by the user.
         */
        fun isNotificationAccessGranted(context: Context): Boolean {
            val myPackage = context.packageName
            val componentName = ComponentName(context, EVANotificationListenerService::class.java)

            // 1. Check NotificationManagerCompat enabled packages
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            if (enabledPackages.contains(myPackage)) {
                return true
            }

            // 2. Direct check from Settings.Secure enabled_notification_listeners
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            if (!enabledListeners.isNullOrEmpty()) {
                val flatComponent = componentName.flattenToString()
                val flatShort = componentName.flattenToShortString()
                if (enabledListeners.contains(flatComponent) ||
                    enabledListeners.contains(flatShort) ||
                    enabledListeners.contains(myPackage)
                ) {
                    return true
                }
            }

            return false
        }
    }
}
