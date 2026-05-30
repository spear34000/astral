package pics.spear.astral.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pics.spear.astral.AstralApp
import pics.spear.astral.MainActivity
import pics.spear.astral.engine.BotApi
import pics.spear.astral.model.ChatMessage
import pics.spear.astral.store.InboxStore

class AstralNotificationListener : NotificationListenerService() {
    private lateinit var api: BotApi
    private lateinit var inbox: InboxStore

    override fun onCreate() {
        super.onCreate()
        api = BotApiProvider.getInstance(this)
        inbox = InboxStoreProvider.getInstance(this)
        startForeground(NOTIFICATION_ID, createForegroundNotification())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != KAKAO_PACKAGE) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE, "")
        val text = extras.getString(Notification.EXTRA_TEXT, "")
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT, "")

        if (text.isBlank()) return

        val room = if (subText.isNotBlank()) subText else title
        val sender = if (subText.isNotBlank()) title else ""
        val isGroup = subText.isNotBlank()

        sbn.notification.actions?.forEachIndexed { index, action ->
            action.remoteInputs?.forEach { ri ->
                NotificationReplier.cacheReply(room, sbn.id, index, ri.resultKey)
            }
        }

        api.enqueueEvent(room, sender, text, isGroup)

        CoroutineScope(Dispatchers.IO).launch {
            inbox.push(ChatMessage(room = room, sender = sender, content = text, isGroupChat = isGroup))
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(componentName)
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, AstralApp.CHANNEL_LISTENER)
            .setContentTitle("Astral")
            .setContentText("Listening for KakaoTalk messages...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val KAKAO_PACKAGE = "com.kakao.talk"
        private const val NOTIFICATION_ID = 1001
    }
}

object BotApiProvider {
    private var instance: BotApi? = null
    fun getInstance(context: Context): BotApi {
        if (instance == null) instance = BotApi(context.applicationContext)
        return instance!!
    }
}

object InboxStoreProvider {
    private var instance: InboxStore? = null
    fun getInstance(context: Context): InboxStore {
        if (instance == null) instance = InboxStore(context.applicationContext)
        return instance!!
    }
}
