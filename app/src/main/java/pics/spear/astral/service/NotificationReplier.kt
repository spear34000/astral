package pics.spear.astral.service

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat

object NotificationReplier {
    private data class ReplyInfo(val notificationId: Int, val actionIndex: Int, val resultKey: String)
    private val cache = mutableMapOf<String, ReplyInfo>()

    fun cacheReply(room: String, notificationId: Int, actionIndex: Int, resultKey: String) {
        cache[room] = ReplyInfo(notificationId, actionIndex, resultKey)
    }

    fun send(context: Context, room: String, message: String) {
        val info = cache[room] ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            val sbnList = if (android.os.Build.VERSION.SDK_INT >= 23) {
                nm.activeNotifications
            } else return

            for (sbn in sbnList) {
                if (sbn.id != info.notificationId) continue
                val action = sbn.notification.actions.getOrNull(info.actionIndex) ?: continue
                val ri = action.remoteInputs ?: continue

                val intent = Intent()
                val results = Bundle().apply { putString(info.resultKey, message) }
                RemoteInput.addResultsToIntent(ri, intent, results)
                try {
                    action.actionIntent?.send(context, 0, intent)
                } catch (_: Exception) {}
                return
            }
        } catch (_: Exception) {}
    }
}
