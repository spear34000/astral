package pics.spear.astral.service

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pics.spear.astral.model.InboxMessage
import pics.spear.astral.prefs.AstralPrefs
import pics.spear.astral.script.AstralScriptRuntime
import pics.spear.astral.store.BotScriptStore
import pics.spear.astral.store.BotStore
import pics.spear.astral.store.InboxStore
import pics.spear.astral.store.ErrorStore
import pics.spear.astral.model.ErrorLog
import android.util.Log
import pics.spear.astral.model.BotMeta
import pics.spear.astral.model.StatEntry
import pics.spear.astral.store.StatsStore
import android.app.RemoteInput
import android.os.Bundle
import android.content.Intent

class AstralNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        private val roomActions = mutableMapOf<String, Notification.Action>()
        @Volatile private var lastAction: Notification.Action? = null

        private fun normalizeRoom(room: String): String = room.trim()
        
        fun sendReply(context: Context, room: String, message: String): Boolean {
            val key = normalizeRoom(room)
            val action = roomActions[key] ?: lastAction
            if (action == null) {
                Log.w("AstralService", "Reply failed: no action for room=$room")
                return false
            }
            try {
                val remoteInputs = action.remoteInputs ?: return false
                val remoteInput = remoteInputs.find { it.resultKey != null } ?: return false
                
                val results = Bundle().apply {
                    putCharSequence(remoteInput.resultKey, message)
                }
                
                val intent = Intent().apply {
                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                }
                RemoteInput.addResultsToIntent(remoteInputs, intent, results)
                
                action.actionIntent.send(context, 0, intent)
                Log.i("AstralService", "Reply sent to $room: $message")
                return true
            } catch (e: Exception) {
                Log.e("AstralService", "Failed to send reply to $room", e)
                return false
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) {
            Log.d("AstralService", "Ignored: sbn is null")
            return
        }
        if (sbn.packageName != "com.kakao.talk") {
            Log.d("AstralService", "Ignored: package=${sbn.packageName}")
            return
        }
        if (!AstralPrefs.isEngineEnabled(this)) {
            Log.w("AstralService", "Ignored: engine disabled")
            return
        }

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        var roomName = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        if (roomName == null) {
            roomName = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        }
        
        val isGroupChat = roomName != null
        val room = (roomName ?: title).trim()
        val sender = title.trim()
        val msg = text.trim()

        if (room.isBlank() || msg.isBlank()) {
            Log.w("AstralService", "Ignored: room or msg blank (room='$room', msg='$msg')")
            return
        }

        val channelId = sbn.notification.channelId ?: ""
        val packageName = sbn.packageName

        Log.d("AstralService", "Notification: room=$room, sender=$sender, msg=$msg, isGroup=$isGroupChat")

        // Capture reply action
        sbn.notification.actions?.find { it.remoteInputs?.any { ri -> ri.resultKey != null } == true }?.let { action ->
            val key = normalizeRoom(room)
            roomActions[key] = action
            lastAction = action
            // Also cache for sender if 1:1 to ensure reliability
            if (!isGroupChat) {
                roomActions[normalizeRoom(sender)] = action
            }
            Log.d("AstralService", "Captured reply action for room: $room")
        }

        scope.launch {
            try {
                InboxStore.append(
                    this@AstralNotificationListenerService,
                    InboxMessage(
                        ts = System.currentTimeMillis(),
                        packageName = sbn.packageName,
                        roomName = room,
                        sender = sender,
                        message = msg,
                    ),
                )

                val bots = BotStore.list(this@AstralNotificationListenerService)
                    .filter { it.enabled }
                if (bots.isEmpty()) {
                    Log.w("AstralService", "No enabled bots. Message dropped.")
                }

                scope.launch(Dispatchers.IO) {
                    // Ensure runtimes for all enabled bots
                    val runtime = AstralScriptRuntime.getInstance(this@AstralNotificationListenerService)
                    bots.forEach {
                        Log.i("AstralService", "Ensuring runtime for bot: ${it.name}")
                        runtime.ensureBotRunning(it)
                    }

                    // Wait briefly for all clients to connect
                    val bridge = pics.spear.astral.runtime.NativeBridge.getInstance(this@AstralNotificationListenerService)
                    val target = bots.size
                    val start = System.currentTimeMillis()
                    while (bridge.connectedClientCount() < target && System.currentTimeMillis() - start < 4000) {
                        kotlinx.coroutines.delay(50)
                    }

                    // Forward to bridge
                    runtime.handleKakaoMessage(room, sender, msg, isGroupChat)
                    Log.d("AstralService", "Successfully forwarded message to Node.js runtime")
                }
            } catch (e: Exception) {
                Log.e("AstralService", "Error processing notification", e)
                ErrorStore.append(this@AstralNotificationListenerService, ErrorLog(
                    ts = System.currentTimeMillis(),
                    botName = "System",
                    error = e.message ?: e.toString()
                ))
            }
        }
    }

}
