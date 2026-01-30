package pics.spear.astral.runtime

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pics.spear.astral.runtime.MaintenanceMode
import pics.spear.astral.runtime.EventRateLimiter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import pics.spear.astral.service.AstralNotificationListenerService
import pics.spear.astral.store.BotStore

/**
 * Native Bridge - Communication bridge between Android and Node.js
 * 
 * This class implements a TCP server that:
 * 1. Receives commands from Node.js (e.g., bot.command, device.toast)
 * 2. Sends messages to Node.js (e.g., KakaoTalk messages)
 */
class NativeBridge(private val context: Context) {
    
    @Volatile private var serverSocket: ServerSocket? = null
    private val clientSockets = mutableSetOf<Socket>()
    private val writers = mutableSetOf<PrintWriter>()
    private val botWriters = mutableMapOf<String, PrintWriter>()
    private val writerToBot = mutableMapOf<PrintWriter, String>()
    @Volatile private var isRunning = false
    @Volatile var debugReplyListener: ((String) -> Unit)? = null
    private val commandHandlers = mutableMapOf<String, (Map<String, Any>) -> Unit>()
    private val pendingMessages = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "NativeBridge"
        private const val PORT = 33445
        
        @Volatile
        private var instance: NativeBridge? = null
        
        fun getInstance(context: Context): NativeBridge {
            return instance ?: synchronized(this) {
                instance ?: NativeBridge(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Start the bridge server
     */
    fun start() {
        if (isRunning) {
            Log.d(TAG, "Bridge server is already running")
            return
        }
        
        stop() // Ensure previous server is stopped before starting new one
        isRunning = true
        
        thread(name = "NativeBridge-Server") {
            try {
                Log.i(TAG, "Starting native bridge server on port $PORT...")
                val server = ServerSocket(PORT).apply {
                    reuseAddress = true
                }
                serverSocket = server
                Log.i(TAG, "Bridge server listening on ${server.inetAddress.hostAddress}:$PORT")
                
                while (isRunning) {
                    Log.i(TAG, "Waiting for Node.js connection...")
                    try {
                        val socket = serverSocket?.accept()
                        if (socket != null) {
                            Log.i(TAG, "Node.js connected from ${socket.inetAddress.hostAddress}:${socket.port}")
                            thread(name = "NativeBridge-Client-${socket.port}") {
                                handleClient(socket)
                            }
                        } else {
                            break
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Accept error: ${e.message}")
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Bridge server error", e)
                } else {
                    Log.d(TAG, "Server socket closed intentionally")
                }
            } finally {
                isRunning = false
                stop()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        var writer: PrintWriter? = null
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            writer = PrintWriter(socket.getOutputStream(), true)

            synchronized(writers) {
                writers.add(writer)
                clientSockets.add(socket)
            }

            // Flush pending messages to this client
            synchronized(pendingMessages) {
                if (pendingMessages.isNotEmpty()) {
                    pendingMessages.forEach { writer.println(it) }
                    pendingMessages.clear()
                }
            }
            
            // Read messages from Node.js
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { handleMessage(it, writer) }
            }
        } catch (e: Exception) {
            if (isRunning) {
                Log.w(TAG, "Client connection closed: ${e.message}")
            }
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignore
            }
            synchronized(writers) {
                if (writer != null) writers.remove(writer)
                clientSockets.remove(socket)
            }
            if (writer != null) {
                synchronized(botWriters) {
                    val botId = writerToBot.remove(writer)
                    if (botId != null) {
                        botWriters.remove(botId)
                        Log.i(TAG, "Bridge client removed for bot=$botId")
                    }
                }
            }
        }
    }
    
    /**
     * Stop the bridge server
     */
    fun stop() {
        isRunning = false
        try {
            synchronized(writers) {
                writers.forEach { runCatching { it.close() } }
                writers.clear()
                clientSockets.forEach { runCatching { it.close() } }
                clientSockets.clear()
            }
            
            serverSocket?.close()
            serverSocket = null
            
            Log.i(TAG, "Bridge server stopped and port released")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping bridge", e)
        }
    }
    
    /**
     * Handle incoming message from Node.js
     */
    private fun handleMessage(json: String, writer: PrintWriter) {
        try {
            Log.d(TAG, "Received: $json")
            val msg = JSONObject(json)
    val type = msg.getString("type")
    val botIdLogger = writerToBot[writer] ?: "unknown"
    if (MaintenanceMode.enabled) {
        Log.i(TAG, "Maintenance mode enabled: ignoring message type=$type from bot=$botIdLogger")
        return
    }
            
            when (type) {
                "hello" -> {
                    val botId = msg.optString("botId").ifBlank { "unknown" }
                    synchronized(botWriters) {
                        botWriters[botId] = writer
                        writerToBot[writer] = botId
                    }
                    Log.i(TAG, "Bridge hello from bot=$botId")
                }
                "register_command" -> {
                    val command = msg.getString("command")
                    // val handler = msg.getString("handler") // Not used in this version
                    Log.i(TAG, "Registered command: $command")
                }
                
                "set_prefix" -> {
                    val prefixes = msg.getJSONArray("prefixes")
                    Log.i(TAG, "Set prefixes: $prefixes")
                }
                
                "toast" -> {
                    val message = msg.getString("message")
                    showToast(message)
                }
                
                "vibrate" -> {
                    val duration = msg.optLong("duration", 100)
                    vibrate(duration)
                }
                
                "notification" -> {
                    val title = msg.getString("title")
                    val body = msg.getString("body")
                    showNotification(title, body)
                }
                
                "reply" -> {
                    val room = msg.getString("room")
                    val message = msg.getString("message")
                    val key = "$botIdLogger:$room:$message"
                    val canProc = EventRateLimiter.canProcess(key)
                    if (!canProc) {
                        Log.d(TAG, "Rate limit: dropping duplicate reply for key=$key")
                    } else {
                        Log.i(TAG, "Reply request from bot=$botIdLogger: room=$room, message=$message")
                        sendReply(room, message)
                    }
                }
                
                else -> {
                    Log.w(TAG, "Unknown message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle message", e)
        }
    }
    
    /**
     * Send a message to Node.js
     */
    fun sendToNodeJs(type: String, data: Map<String, Any>) {
        val payload = try {
            JSONObject().apply {
                put("type", type)
                put("data", JSONObject(data))
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build message for Node.js", e)
            return
        }

        ioScope.launch {
            try {
                val enabledIds = withContext(Dispatchers.IO) {
                    BotStore.list(context).filter { it.enabled }.map { it.id }.toSet()
                }

                val outs = synchronized(botWriters) {
                    botWriters
                        .filterKeys { enabledIds.contains(it) }
                        .values
                        .toList()
                        .ifEmpty {
                            // Fallback to all writers (e.g., during startup) if no enabled mapping found
                            synchronized(writers) { writers.toList() }
                        }
                }

                if (outs.isEmpty()) {
                    Log.w(TAG, "Bridge not connected. Queueing message: $payload")
                    synchronized(pendingMessages) { pendingMessages.add(payload) }
                } else {
                    outs.forEach { it.println(payload) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message to Node.js", e)
            }
        }
    }

    fun connectedClientCount(): Int {
        return synchronized(botWriters) { botWriters.size }
    }
    
    /**
     * Send a KakaoTalk message to Node.js for processing
     */
    fun sendKakaoMessage(room: String, sender: String, message: String, isGroupChat: Boolean) {
        sendToNodeJs("kakao_message", mapOf(
            "room" to room,
            "sender" to sender,
            "content" to message,
            "isGroupChat" to isGroupChat
        ))
    }
    
    // Android-specific actions
    
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun vibrate(duration: Long) {
        handler.post {
            try {
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                    vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator)
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibrate failed", e)
            }
        }
    }
    
    private fun showNotification(title: String, body: String) {
        // TODO: Implement notification
        Log.i(TAG, "Notification: $title - $body")
    }
    
    private fun sendReply(room: String, message: String) {
        if (room == "DEBUG_TERMINAL") {
            debugReplyListener?.invoke(message)
            Log.i(TAG, "Debug Reply: $message")
            return
        }

        // Use the static helper in AstralNotificationListenerService to send the actual reply
        val success = AstralNotificationListenerService.sendReply(context, room, message)
        if (success) {
            Log.i(TAG, "Automation: Successfully replied to $room via Notification Center")
        } else {
            Log.w(TAG, "Automation: Failed to reply to $room (Room not found or no replyable intent)")
        }
    }
}
