package pics.spear.astral.engine

import android.util.Log
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pics.spear.astral.model.Bot
import pics.spear.astral.model.Flow
import pics.spear.astral.store.BotStore
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.concurrent.Executors

/**
 * Lightweight REST API server for external access.
 * Uses Android built-in com.sun.net.httpserver (API 24+).
 *
 * Default port: 9345
 * Access: http://<device-ip>:9345
 */
class ApiServer(
    private val botStore: BotStore,
    private val scriptEngine: ScriptEngine,
    private val api: BotApi,
) {
    private var server: HttpServer? = null
    private var job: Job? = null
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    data class ApiConfig(
        val enabled: Boolean = false,
        val port: Int = 9345,
        val authToken: String = "",
    )

    var config: ApiConfig = ApiConfig()
        private set

    fun start(port: Int = 9345, token: String = "") {
        if (server != null) stop()
        try {
            config = ApiConfig(enabled = true, port = port, authToken = token)
            val addr = InetSocketAddress(port)
            server = HttpServer.create(addr, 0).apply {
                executor = Executors.newCachedThreadPool()
                createContext("/api/v1") { handleRequest(it) }
                start()
            }
            Log.i("Astral", "API server started on port $port")
            Log.i("Astral", "Local: http://127.0.0.1:$port/api/v1")
            getLocalIps().forEach { ip ->
                Log.i("Astral", "Network: http://$ip:$port/api/v1")
            }
            PluginManager.trigger(PluginManager.Hook.APP_START, mapOf("api_port" to port))
        } catch (e: Exception) {
            Log.e("Astral", "API server start failed: ${e.message}")
            config = ApiConfig(enabled = false)
        }
    }

    fun stop() {
        server?.stop(0)
        server = null
        job?.cancel()
        config = ApiConfig(enabled = false)
        Log.i("Astral", "API server stopped")
    }

    fun restart(port: Int = config.port, token: String = config.authToken) {
        stop()
        start(port, token)
    }

    // ── Request handler ──────────────────────────────────
    private fun handleRequest(exchange: HttpExchange) {
        try {
            // CORS
            exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
            exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type, Authorization")

            if (exchange.requestMethod == "OPTIONS") {
                exchange.sendResponseHeaders(204, -1)
                return
            }

            // Auth check
            if (config.authToken.isNotBlank()) {
                val auth = exchange.requestHeaders.getFirst("Authorization") ?: ""
                if (auth != "Bearer ${config.authToken}") {
                    respond(exchange, 403, mapOf("error" to "Forbidden"))
                    return
                }
            }

            val path = exchange.requestURI.path.removePrefix("/api/v1").trimEnd('/')
            val method = exchange.requestMethod

            val response: Map<String, Any?> = when {
                path == "/ping" || path == "" -> status()
                path == "/bots" && method == "GET" -> listBots()
                path == "/bots" && method == "POST" -> createBot(readBody(exchange))
                path.startsWith("/bots/") && method == "GET" -> getBot(path.removePrefix("/bots/"))
                path.startsWith("/bots/") && method == "DELETE" -> deleteBot(path.removePrefix("/bots/"))
                path.startsWith("/bots/") && path.endsWith("/toggle") && method == "POST" -> toggleBot(path.removePrefix("/bots/").removeSuffix("/toggle"))
                path == "/flows" && method == "POST" -> compileFlow(readBody(exchange))
                path == "/flows/compile" && method == "POST" -> compileFlow(readBody(exchange))
                path == "/message" && method == "POST" -> sendMessage(readBody(exchange))
                path == "/plugins" -> listPlugins()
                path == "/server/restart" && method == "POST" -> restartServer(readBody(exchange))
                else -> mapOf("error" to "Not found", "path" to path)
            }

            respond(exchange, if (response.containsKey("error")) 404 else 200, response)
        } catch (e: Exception) {
            respond(exchange, 500, mapOf("error" to e.message ?: "Internal error"))
        }
    }

    // ── API handlers ─────────────────────────────────────
    private fun status() = mapOf(
        "name" to "Astral API",
        "version" to "2.0.0",
        "status" to "running",
        "bots" to botStore.bots.value.size,
        "activeBots" to botStore.bots.value.count { it.enabled },
        "plugins" to PluginManager.getPlugins().size,
        "uptime" to System.currentTimeMillis(),
    )

    private fun listBots() = mapOf(
        "bots" to botStore.bots.value.map { bot ->
            mapOf(
                "id" to bot.id, "name" to bot.name, "enabled" to bot.enabled,
                "language" to bot.language, "createdAt" to bot.createdAt,
                "running" to scriptEngine.isRunning(bot.id),
            )
        }
    )

    private fun getBot(id: String): Map<String, Any?> {
        val bot = botStore.bots.value.find { it.id == id } ?: return mapOf("error" to "Bot not found")
        return mapOf(
            "id" to bot.id, "name" to bot.name, "enabled" to bot.enabled,
            "language" to bot.language, "script" to botStore.loadScript(bot.name),
            "running" to scriptEngine.isRunning(bot.id),
        )
    }

    private fun createBot(body: String): Map<String, Any?> {
        try {
            val data = json.decodeFromString<Map<String, String>>(body)
            val name = data["name"] ?: return mapOf("error" to "name required")
            val lang = data["language"] ?: "javascript"
            val bot = Bot(
                id = java.util.UUID.randomUUID().toString().take(8),
                name = name, language = lang,
            )
            runBlocking(Dispatchers.IO) { botStore.add(bot) }
            PluginManager.trigger(PluginManager.Hook.BOT_CREATED, mapOf("bot" to bot))
            return mapOf("success" to true, "bot" to bot.id)
        } catch (e: Exception) {
            return mapOf("error" to e.message)
        }
    }

    private fun deleteBot(id: String): Map<String, Any?> {
        scriptEngine.stopBot(id)
        runBlocking(Dispatchers.IO) { botStore.remove(id) }
        PluginManager.trigger(PluginManager.Hook.BOT_DELETED, mapOf("botId" to id))
        return mapOf("success" to true)
    }

    private fun toggleBot(id: String): Map<String, Any?> {
        runBlocking(Dispatchers.IO) { botStore.toggle(id) }
        val bot = botStore.bots.value.find { it.id == id }
        PluginManager.trigger(
            if (bot?.enabled == true) PluginManager.Hook.BOT_ENABLED else PluginManager.Hook.BOT_DISABLED,
            mapOf("botId" to id),
        )
        return mapOf("success" to true, "enabled" to bot?.enabled)
    }

    private fun compileFlow(body: String): Map<String, Any?> {
        try {
            val flow = json.decodeFromString<Flow>(body)
            val code = FlowCompiler.compile(flow)
            PluginManager.trigger(PluginManager.Hook.FLOW_COMPILED, mapOf("flow" to flow.name, "code" to code))
            return mapOf("success" to true, "code" to code)
        } catch (e: Exception) {
            return mapOf("error" to e.message)
        }
    }

    private fun sendMessage(body: String): Map<String, Any?> {
        try {
            val data = json.decodeFromString<Map<String, String>>(body)
            val room = data["room"] ?: return mapOf("error" to "room required")
            val message = data["message"] ?: return mapOf("error" to "message required")
            api.sendReply(room, message)
            return mapOf("success" to true)
        } catch (e: Exception) {
            return mapOf("error" to e.message)
        }
    }

    private fun listPlugins() = mapOf(
        "plugins" to PluginManager.getPlugins().map { mapOf("id" to it.id, "name" to it.name, "version" to it.version) }
    )

    private fun restartServer(body: String): Map<String, Any?> {
        try {
            val data = json.decodeFromString<Map<String, Int>>(body)
            restart(data["port"] ?: config.port)
            return mapOf("success" to true, "port" to config.port)
        } catch (e: Exception) {
            return mapOf("error" to e.message)
        }
    }

    // ── HTTP helpers ─────────────────────────────────────
    private fun respond(exchange: HttpExchange, code: Int, data: Map<String, Any?>) {
        val body = try { json.encodeToString(data) } catch (_: Exception) { "{}" }
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.write(bytes)
        exchange.responseBody.close()
    }

    private fun readBody(exchange: HttpExchange): String {
        return exchange.requestBody.reader().readText()
    }

    private fun getLocalIps(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                if (ni.isLoopback || !ni.isUp) continue
                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is java.net.Inet4Address) ips.add(addr.hostAddress ?: "")
                }
            }
        } catch (_: Exception) {}
        return ips
    }
}
