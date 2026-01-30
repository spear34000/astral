package pics.spear.astral.script

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pics.spear.astral.runtime.NodeJsRuntime
import pics.spear.astral.runtime.NativeBridge
import pics.spear.astral.runtime.PythonRuntime
import pics.spear.astral.model.ErrorLog
import pics.spear.astral.store.BotStore
import pics.spear.astral.store.ErrorStore
import pics.spear.astral.store.BotScriptStore
import pics.spear.astral.util.AstralStorage
import java.io.File

/**
 * Astral Script Runtime - Main entry point for bot script execution
 * 
 * Now powered by real Node.js running in PRoot Linux userland!
 */
class AstralScriptRuntime(private val context: Context) {
    
    private val nodeRuntimes = mutableMapOf<String, NodeJsRuntime>()
    private val pythonRuntimes = mutableMapOf<String, PythonRuntime>()
    private val scope = CoroutineScope(Dispatchers.Main)
    private val botLocks = mutableMapOf<String, Mutex>()
    
    companion object {
        private const val TAG = "AstralScriptRuntime"
        
        @Volatile
        private var instance: AstralScriptRuntime? = null
        
        fun getInstance(context: Context): AstralScriptRuntime {
            return instance ?: synchronized(this) {
                instance ?: AstralScriptRuntime(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Initialize the runtime
     * This will extract PRoot, Alpine rootfs, and install Node.js
     */
    suspend fun initialize(): Result<Unit> {
        Log.i(TAG, "Initializing Astral Script Runtime...")
        return getNodeRuntime("global").initialize()
    }
    
    /**
     * Execute a bot script
     */
    fun executeScript(
        scriptId: String,
        scriptCode: String,
        language: String = "js",
        onOutput: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Executing script: $scriptId")

                val botsSnapshot = runBlocking { BotStore.list(context) }
                val resolvedBotName = botsSnapshot.find { it.id == scriptId }?.name?.ifBlank { null } ?: scriptId
                if (botsSnapshot.find { it.id == scriptId }?.enabled == false) {
                    Log.i(TAG, "Skip executing disabled bot: $scriptId")
                    logBotSnapshot("executeScript skip")
                    return@launch
                }
                var errorLogged = false
                fun recordError(message: String) {
                    if (message.isBlank() || errorLogged) return
                    errorLogged = true
                    scope.launch(Dispatchers.IO) {
                        ErrorStore.append(
                            context,
                            ErrorLog(
                                ts = System.currentTimeMillis(),
                                botName = resolvedBotName,
                                error = message
                            )
                        )
                    }
                }

                val ext = if (language.lowercase() == "py" || language.lowercase() == "python") "py" else "js"
                val init = if (ext == "py") {
                    getPythonRuntime(scriptId).initialize()
                } else {
                    getNodeRuntime(scriptId).initialize()
                }
                if (init.isFailure) {
                    val error = init.exceptionOrNull()?.message ?: "Runtime not initialized"
                    Log.e(TAG, "Runtime init failed: $error")
                    recordError(error)
                    onError(error)
                    return@launch
                }
                val entryPath = BotScriptStore.getEntry(context, scriptId, language)
                BotScriptStore.writeFile(context, scriptId, entryPath, scriptCode)
                val workspaceDir = BotScriptStore.getWorkspaceDir(context, scriptId)

                val result = if (ext == "py") {
                    getPythonRuntime(scriptId).runPythonScript(
                        scriptPath = File(workspaceDir, entryPath).absolutePath,
                        workspaceDir = workspaceDir.absolutePath,
                        onMessage = { msg ->
                            Log.d(TAG, "[PY Output] $msg")
                            onOutput(msg)
                        },
                        onError = { err ->
                            Log.e(TAG, "[PY Error] $err")
                            recordError(err)
                            onError(err)
                        }
                    )
                } else {
                    getNodeRuntime(scriptId).runScript(
                        scriptPath = File(workspaceDir, entryPath).absolutePath,
                        workspaceDir = workspaceDir.absolutePath,
                        onMessage = { msg ->
                            Log.d(TAG, "[Script Output] $msg")
                            onOutput(msg)
                        },
                        onError = { err ->
                            Log.e(TAG, "[Script Error] $err")
                            recordError(err)
                            onError(err)
                        }
                    )
                }
                
                if (result.isFailure) {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Script execution failed: $error")
                    recordError(error)
                    onError(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute script", e)
                val error = e.message ?: "Unknown error"
                scope.launch(Dispatchers.IO) {
                    ErrorStore.append(
                        context,
                        ErrorLog(
                            ts = System.currentTimeMillis(),
                            botName = scriptId,
                            error = error
                        )
                    )
                }
                onError(error)
            }
        }
    }
    
    /**
     * Stop the currently running script
     */
    fun stopScript() {
        scope.launch(Dispatchers.IO) {
            val nodeSnapshot = synchronized(nodeRuntimes) { nodeRuntimes.values.toList() }
            val pythonSnapshot = synchronized(pythonRuntimes) { pythonRuntimes.values.toList() }

            for (rt in nodeSnapshot) {
                try {
                    rt.stopScript()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to stop Node runtime", t)
                }
            }

            for (rt in pythonSnapshot) {
                try {
                    rt.stopScript()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to stop Python runtime", t)
                }
            }
        }
    }

    fun stopBot(botId: String) {
        scope.launch(Dispatchers.IO) {
            logBotSnapshot("stopBot enter ($botId)")
            val nodeRuntime = synchronized(nodeRuntimes) { nodeRuntimes[botId] }
            val pythonRuntime = synchronized(pythonRuntimes) { pythonRuntimes[botId] }

            if (nodeRuntime != null) {
                try {
                    nodeRuntime.stopScript()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to stop Node runtime", t)
                } finally {
                    synchronized(nodeRuntimes) { nodeRuntimes.remove(botId) }
                }
            }

            if (pythonRuntime != null) {
                try {
                    pythonRuntime.stopScript()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to stop Python runtime", t)
                } finally {
                    synchronized(pythonRuntimes) { pythonRuntimes.remove(botId) }
                }
            }
            val meta = runBlocking { BotStore.list(context).find { it.id == botId } }
            if (meta != null && (meta.enabled || meta.autoStart)) {
                BotStore.upsert(context, meta.copy(enabled = false, autoStart = false, updatedAt = System.currentTimeMillis()))
                logBotSnapshot("stopBot persisted disabled ($botId)")
            }
            logBotSnapshot("stopBot exit ($botId)")
        }
    }
    
    /**
     * Install an npm package
     */
    suspend fun installNpmPackage(packageName: String): Result<Unit> {
        return getNodeRuntime("global").installPackage(packageName)
    }

    /**
     * Install a pip package (Python userland required)
     */
    suspend fun installPipPackage(packageName: String): Result<Unit> {
        return getPythonRuntime("global").installPythonPackage(packageName)
    }

    /**
     * Execute a terminal command inside userland
     */
    suspend fun executeUserlandCommand(command: String, botId: String? = null): Result<String> {
        val runtime = getNodeRuntime("global")
        val init = runtime.initialize()
        if (init.isFailure) return Result.failure(init.exceptionOrNull() ?: Exception("Runtime not initialized"))
        val workspace = botId?.let { BotScriptStore.getWorkspaceDir(context, it) }
        return runtime.executeCommand(command, workspace)
    }

        suspend fun executeUserlandCommandStreaming(
            command: String,
            botId: String? = null,
            currentFile: String? = null,
            onLine: (String) -> Unit
        ): Result<Unit> {
            val runtime = getNodeRuntime("global")
            val init = runtime.initialize()
            if (init.isFailure) return Result.failure(init.exceptionOrNull() ?: Exception("Runtime not initialized"))
            val workspace = botId?.let { BotScriptStore.getWorkspaceDir(context, it) }
            return runtime.executeCommandStreaming(command, workspace, currentFile, onLine)
        }

    suspend fun recoverPythonEnvironment(): Result<Unit> {
        return getPythonRuntime("global").recoverPythonEnvironment()
    }

    suspend fun checkPythonSyntax(scriptId: String, scriptCode: String): Result<String> {
        return try {
            val tempDir = File(AstralStorage.baseDir(context), "tmp").apply { mkdirs() }
            val scriptFile = File(tempDir, "$scriptId.py")
            scriptFile.writeText(scriptCode)
            getPythonRuntime(scriptId).checkPythonSyntax(scriptFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkPythonSyntaxFast(scriptId: String, scriptCode: String): Result<String> {
        return try {
            val tempDir = File(AstralStorage.baseDir(context), "tmp").apply { mkdirs() }
            val scriptFile = File(tempDir, "$scriptId.py")
            scriptFile.writeText(scriptCode)
            getPythonRuntime(scriptId).checkPythonSyntaxFast(scriptFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkJavaScriptSyntax(scriptId: String, scriptCode: String): Result<String> {
        return try {
            val tempDir = File(AstralStorage.baseDir(context), "tmp").apply { mkdirs() }
            val scriptFile = File(tempDir, "$scriptId.js")
            scriptFile.writeText(scriptCode)
            getNodeRuntime(scriptId).checkJavaScriptSyntax(scriptFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkJavaScriptSyntaxFast(scriptId: String, scriptCode: String): Result<String> {
        return try {
            val tempDir = File(AstralStorage.baseDir(context), "tmp").apply { mkdirs() }
            val scriptFile = File(tempDir, "$scriptId.js")
            scriptFile.writeText(scriptCode)
            getNodeRuntime(scriptId).checkJavaScriptSyntaxFast(scriptFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun handleKakaoMessage(
        room: String,
        sender: String,
        message: String,
        isGroupChat: Boolean
    ) {
        logBotSnapshot("handleKakaoMessage enter")
        val enabledBots = runBlocking { BotStore.list(context) }.filter { it.enabled }
        val hasRunningRuntime = hasAnyRunningRuntime()
        if (enabledBots.isEmpty() && !hasRunningRuntime) {
            Log.d(TAG, "Skip forwarding message because no bots are active")
            return
        }
        Log.d(TAG, "Forwarding message to Node.js: [$room] $sender: $message")
        // Get the bridge from nodeRuntime (which is also a singleton managed by nodeRuntime's bridge property)
        val bridge = NativeBridge.getInstance(context)
        bridge.sendKakaoMessage(room, sender, message, isGroupChat)
    }

    fun ensureBotRunning(bot: pics.spear.astral.model.BotMeta) {
        if (!bot.enabled) return

        val lock = mutexFor(bot.id)

        scope.launch(Dispatchers.IO) {
            logBotSnapshot("ensureBotRunning enter (${bot.id})")
            lock.withLock {
                val meta = runBlocking { BotStore.list(context).find { it.id == bot.id } } ?: bot
                if (!meta.enabled) {
                    Log.i(TAG, "Skip starting disabled bot: ${bot.id}")
                    logBotSnapshot("ensureBotRunning skip (${bot.id})")
                    return@withLock
                }

                val isPython = meta.language.lowercase() in listOf("py", "python")
                if (!isPython && getNodeRuntime(meta.id).isRunning()) return@withLock

                try {
                    val init = if (isPython) {
                        getPythonRuntime(meta.id).initialize()
                    } else {
                        getNodeRuntime(meta.id).initialize()
                    }
                    if (init.isFailure) {
                        val err = init.exceptionOrNull()?.message ?: "Runtime init failed"
                        Log.e(TAG, "Runtime init failed: $err")
                        ErrorStore.append(
                            context,
                            ErrorLog(
                                ts = System.currentTimeMillis(),
                                botName = meta.name.ifBlank { meta.id },
                                error = err
                            )
                        )
                        return@withLock
                    }

                    val code = BotScriptStore.loadOrCreate(context, meta.id, meta.language)
                    Log.d(TAG, "ensureBotRunning launching ${meta.id}, language=${meta.language}")
                    executeScript(
                        scriptId = meta.id,
                        scriptCode = code,
                        language = meta.language,
                        onOutput = { Log.d(TAG, "[Bot Output] $it") },
                        onError = { Log.e(TAG, "[Bot Error] $it") }
                    )
                    val refreshed = runBlocking { BotStore.list(context).find { it.id == meta.id } }
                    val updatedMeta = (refreshed ?: meta).copy(enabled = true, updatedAt = System.currentTimeMillis())
                    BotStore.upsert(context, updatedMeta)
                    logBotSnapshot("ensureBotRunning set enabled true (${meta.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to ensure bot running", e)
                }
            }
        }
    }

    private fun getNodeRuntime(botId: String): NodeJsRuntime {
        return synchronized(nodeRuntimes) {
            nodeRuntimes.getOrPut(botId) { NodeJsRuntime(context, botId) }
        }
    }

    private fun getPythonRuntime(botId: String): PythonRuntime {
        return synchronized(pythonRuntimes) {
            pythonRuntimes.getOrPut(botId) { PythonRuntime(context, botId) }
        }
    }

    private fun mutexFor(botId: String): Mutex = synchronized(botLocks) {
        botLocks.getOrPut(botId) { Mutex() }
    }

    private fun hasAnyRunningRuntime(): Boolean {
        val nodeRunning = synchronized(nodeRuntimes) { nodeRuntimes.values.any { it.isRunning() } }
        if (nodeRunning) return true
        return synchronized(pythonRuntimes) { pythonRuntimes.values.any { it.isRunning() } }
    }

    private fun logBotSnapshot(label: String) {
        val bots = runBlocking { BotStore.list(context) }
        val summary = bots.joinToString { "${it.id}(enabled=${it.enabled}, auto=${it.autoStart})" }
        Log.d(TAG, "$label - bots: $summary")
    }
}
