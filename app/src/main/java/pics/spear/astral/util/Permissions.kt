package pics.spear.astral.util

import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService

object Permissions {
    fun hasNotificationAccess(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(context.packageName)
    }
}
