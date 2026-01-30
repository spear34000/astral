package pics.spear.astral.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.app.RemoteInput

object NotificationReplier {
    fun reply(context: Context, sbn: StatusBarNotification, text: String): Boolean {
        val notification: Notification = sbn.notification
        val actions = notification.actions ?: return false

        val action = actions.firstOrNull { it.remoteInputs?.isNotEmpty() == true } ?: return false
        val remoteInput = action.remoteInputs?.firstOrNull() ?: return false

        val results = Bundle().apply { putCharSequence(remoteInput.resultKey, text) }
        val intent = Intent()
        RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, results)

        val pi: PendingIntent = action.actionIntent ?: return false
        runCatching {
            pi.send(context, 0, intent)
        }.onFailure {
            return false
        }
        return true
    }
}


