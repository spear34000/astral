package pics.spear.astral.runtime

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Node.js Runtime - Manages Node.js processes and bot scripts
 * 
 * This class provides:
 * 1. Script execution in Node.js environment
 * 2. Communication bridge between Android and Node.js
 * 3. Bot API implementation via native bridge
 */
class NodeJsRuntime(private val context: Context, botId: String) {
    
    private val prootManager = ProotManager(context)
    private val nodeExecutable: String
        get() = prootManager.nodeExecutablePath
    private val bridge = NativeBridge.getInstance(context)
    private var currentProcess: Process? = null
    private var eslintChecked = false
    private var eslintAvailable = false
    private val eslintMutex = Mutex()
    
    companion object {
        private const val TAG = "NodeJsRuntime"
        private const val BRIDGE_PORT = 33445
    }

    private val safeId = botId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    
    /**
     * Initialize the runtime (ensure PRoot and Node.js are ready)
     */
    suspend fun initialize(): Result<Unit> {
        Log.i(TAG, "Initializing Node.js runtime...")
        
        // Initialize PRoot userland
        val prootResult = prootManager.initialize()
        if (prootResult.isFailure) {
            return Result.failure(prootResult.exceptionOrNull() ?: Exception("PRoot initialization failed"))
        }
        
        // Check Node.js installation
        val nodeVersion = prootManager.getNodeVersion()
        if (nodeVersion.isSuccess) {
            Log.i(TAG, "Node.js version: ${nodeVersion.getOrNull()}")
        } else {
            return Result.failure(Exception("Node.js not found in userland"))
        }
        
        // Start native bridge server
        bridge.start()
        
        Log.i(TAG, "Node.js runtime initialized successfully!")
        return Result.success(Unit)
    }

    /**
     * Execute an arbitrary userland command
     */
    suspend fun executeCommand(command: String, workspaceDir: File? = null): Result<String> {
        val cmd = if (workspaceDir != null) "cd /workspace && $command" else command
        val binds = if (workspaceDir != null) listOf(workspaceDir to "/workspace") else emptyList()
        return prootManager.executeInUserland(cmd, extraBinds = binds)
    }

    suspend fun executeCommandStreaming(
        command: String,
        workspaceDir: File? = null,
        currentFile: String? = null,
        onLine: (String) -> Unit
    ): Result<Unit> {
        val subDir = currentFile?.let { File(it).parent } ?: ""
        val cmd = if (workspaceDir != null) {
            "cd /workspace/$subDir && $command"
        } else {
            command
        }
        val binds = if (workspaceDir != null) listOf(workspaceDir to "/workspace") else emptyList()
        return prootManager.executeInUserlandStreaming(cmd, extraBinds = binds, onLine = onLine)
    }
    
    /**
     * Run a bot script
     */
    suspend fun runScript(
        scriptPath: String,
        workspaceDir: String? = null,
        onMessage: (String) -> Unit,
        onError: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Running script: $scriptPath")

            stopScript()
            
            val userlandScriptPath = if (workspaceDir != null) {
                val (root, entry) = copyWorkspaceToUserland(workspaceDir, scriptPath)
                "$root/$entry"
            } else {
                copyScriptToUserland(scriptPath)
            }
            
            val wrapperScript = createWrapperScript(userlandScriptPath, workspaceDir != null)
            
            // Execute script in PRoot environment
            val command = "${nodeExecutable} $wrapperScript"
            
            val processBuilder = ProcessBuilder(buildPRootCommand(command))
            val env = processBuilder.environment()
            env["LD_LIBRARY_PATH"] = context.applicationInfo.nativeLibraryDir
            env["PROOT_LOADER"] = prootManager.prootLoader.absolutePath
            env["PROOT_TMP_DIR"] = prootManager.tmpDir.absolutePath
            env["TMPDIR"] = prootManager.tmpDir.absolutePath
            env["PATH"] = "/usr/bin:/usr/local/bin:/usr/local/sbin:/usr/sbin:/sbin:/bin"
            
            processBuilder.redirectErrorStream(false)
            
            currentProcess = processBuilder.start()
            
            // Read stdout
            thread {
                try {
                    currentProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
                        Log.d(TAG, "[STDOUT] $line")
                        onMessage(line)
                    }
                } catch (_: java.io.InterruptedIOException) {
                    // ignore when process is closed during stop
                } catch (e: Exception) {
                    Log.w(TAG, "[STDOUT] reader closed", e)
                }
            }
            
            // Read stderr
            thread {
                try {
                    currentProcess?.errorStream?.bufferedReader()?.forEachLine { line ->
                        if (line.contains("proot warning:", ignoreCase = true)) {
                            Log.w(TAG, "[PROOT WARN] $line")
                        } else {
                            Log.e(TAG, "[STDERR] $line")
                            onError(line)
                        }
                    }
                } catch (_: java.io.InterruptedIOException) {
                    // ignore when process is closed during stop
                } catch (e: Exception) {
                    Log.w(TAG, "[STDERR] reader closed", e)
                }
            }
            
            val exitCode = currentProcess?.waitFor() ?: -1
            Log.i(TAG, "Script finished with exit code: $exitCode")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run script", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop the currently running script
     */
    suspend fun stopScript() = withContext(Dispatchers.IO) {
        val process = currentProcess
        currentProcess = null
        if (process != null) {
            runCatching { process.destroy() }
            if (process.isAlive) {
                delay(300)
                if (process.isAlive) {
                    runCatching { process.destroyForcibly() }
                }
            }
            runCatching { process.waitFor() }
        }
        killBotWrappers()
    }

    // Kill any wrapper processes associated with this bot to ensure strict isolation
    private suspend fun killBotWrappers() {
        try {
            val jsCmd = "pkill -f /root/wrapper_$safeId.js || true"
            val pyCmd = "pkill -f /root/wrapper_$safeId.py || true"
            val resJs = prootManager.executeInUserland(jsCmd)
            val resPy = prootManager.executeInUserland(pyCmd)
            Log.i(TAG, "Kill wrappers: js=${resJs.isSuccess}, py=${resPy.isSuccess}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill bot wrappers", e)
        }
    }

    fun isRunning(): Boolean = currentProcess?.isAlive == true
    
    /**
     * Install an npm package
     */
    suspend fun installPackage(packageName: String): Result<Unit> {
        Log.i(TAG, "Installing npm package: $packageName")
        if (!prootManager.isInitialized()) {
            val init = initialize()
            if (init.isFailure) return init
        }
        val npmWhich = prootManager.executeInUserland("which npm")
        val npmCmd = npmWhich.getOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: "npm"
        val result = prootManager.executeInUserland("$npmCmd install -g $packageName")
        return if (result.isSuccess) {
            Log.i(TAG, "Package installed successfully")
            Result.success(Unit)
        } else {
            Log.e(TAG, "Failed to install package: ${result.exceptionOrNull()?.message}")
            result.map { }
        }
    }


    suspend fun checkJavaScriptSyntax(scriptPath: String): Result<String> {
        if (!prootManager.isInitialized()) {
            val init = initialize()
            if (init.isFailure) return Result.failure(init.exceptionOrNull() ?: Exception("Runtime not initialized"))
        }

        val userlandScriptPath = copyScriptToUserland(scriptPath)
        val syntax = prootManager.executeInUserland("${nodeExecutable} --check $userlandScriptPath")
        if (syntax.isFailure) {
            return syntax
        }

        ensureEslintAvailable()
        if (!eslintAvailable) {
            return syntax
        }

        return prootManager.executeInUserland(
            "eslint --no-eslintrc --env es2021,node --rule 'no-undef:error' --rule 'no-unused-vars:error' $userlandScriptPath"
        )
    }

    suspend fun checkJavaScriptSyntaxFast(scriptPath: String): Result<String> {
        if (!prootManager.isInitialized()) {
            val init = initialize()
            if (init.isFailure) return Result.failure(init.exceptionOrNull() ?: Exception("Runtime not initialized"))
        }

        val userlandScriptPath = copyScriptToUserland(scriptPath)
        val checkerScript = createQuickJsSyntaxCheckerScript()
        return prootManager.executeInUserland("${nodeExecutable} $checkerScript $userlandScriptPath")
    }

    private fun createQuickJsSyntaxCheckerScript(): String {
        val checkerContent = """
            const fs = require('fs');
            const vm = require('vm');

            const filePath = process.argv[2];
            if (!filePath) {
              console.error('Line 1: missing file');
              process.exit(1);
            }

            try {
              const code = fs.readFileSync(filePath, 'utf8');
              new vm.Script(code, { filename: filePath });
              console.log('OK');
            } catch (e) {
              let line = e.lineNumber || 0;
              let col = e.columnNumber || 0;
              if ((!line || !col) && e.stack) {
                const m = e.stack.match(/:(\d+):(\d+)/);
                if (m) {
                  line = parseInt(m[1], 10);
                  col = parseInt(m[2], 10);
                }
              }
              const loc = line ? `Line ${'$'}{line}${'$'}{col ? ':' + col : ''}` : 'Line 1';
              console.log(`${'$'}{loc}: ${'$'}{e.message}`);
              process.exit(1);
            }
        """.trimIndent()

        val checkerFile = File(prootManager.rootfsDir, "root/js_quick_syntax.js")
        checkerFile.writeText(checkerContent)
        return "/root/js_quick_syntax.js"
    }

    private suspend fun ensureEslintAvailable(): Boolean {
        return eslintMutex.withLock {
            if (eslintChecked) return@withLock eslintAvailable
            eslintChecked = true

            val show = prootManager.executeInUserland("${nodeExecutable} -e \"require('eslint'); console.log('ok')\"")
            if (show.isSuccess) {
                eslintAvailable = true
                return@withLock true
            }

            val npmWhich = prootManager.executeInUserland("which npm")
            val npmCmd = npmWhich.getOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: "npm"
            val install = prootManager.executeInUserland("$npmCmd install -g eslint --silent")
            eslintAvailable = install.isSuccess
            if (!eslintAvailable) {
                Log.w(TAG, "ESLint install failed: ${install.exceptionOrNull()?.message}")
            }
            eslintAvailable
        }
    }

    
    /**
     * Copy script from Android filesystem to userland
     */
    private fun copyScriptToUserland(scriptPath: String): String {
        val sourceFile = File(scriptPath)
        val destFile = File(prootManager.rootfsDir, "root/script_$safeId.js")
        
        sourceFile.copyTo(destFile, overwrite = true)
        
        return "/root/script_$safeId.js"
    }

    private fun copyWorkspaceToUserland(workspaceDir: String, scriptPath: String): Pair<String, String> {
        val root = File(prootManager.rootfsDir, "root/workspace_$safeId")
        if (root.exists()) {
            root.deleteRecursively()
        }
        root.mkdirs()

        val workspace = File(workspaceDir)
        if (!workspace.exists() || !workspace.isDirectory) {
            Log.w(TAG, "Workspace directory missing for $safeId: $workspaceDir")
            return Pair(root.absolutePath.replace("\\", "/"), File(scriptPath).name)
        }

        workspace.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relative = file.relativeTo(workspace).path.replace("\\", "/")
                val dest = File(root, relative)
                dest.parentFile?.mkdirs()
                file.copyTo(dest, overwrite = true)
            }
        }

        val relativeEntry = runCatching {
            File(scriptPath).relativeTo(workspace).path.replace("\\", "/")
        }.getOrNull() ?: File(scriptPath).name
        return Pair("/root/workspace_$safeId", relativeEntry)
    }

    private fun createWrapperScript(scriptPath: String, useWorkspace: Boolean): String {
        val workspaceBlock = if (useWorkspace) {
            """
            process.chdir('/root/workspace_$safeId');
            const workspaceRoot = '/root/workspace_$safeId';
            """.trimIndent()
        } else {
            """
            const workspaceRoot = path.dirname('$scriptPath');
            """.trimIndent()
        }

        val wrapperContent = """
            const net = require('net');
            const path = require('path');
            const fs = require('fs');

            let client = null;
            let prefixes = ['!', '/'];
            const commandHandlers = Object.create(null);
            const eventHandlers = Object.create(null);
            let onMessageHandler = null;

            ${workspaceBlock}

            console.log('[Wrapper] Starting');

            const bridgeHosts = ['127.0.0.1', 'localhost', '10.0.2.2'];
            let bridgeHostIndex = 0;

            function connectBridge() {
                const host = bridgeHosts[bridgeHostIndex % bridgeHosts.length];
                console.log('[Bridge] Connecting to native bridge at ' + host + ':${BRIDGE_PORT}...');
                if (client) {
                    try { client.destroy(); } catch (e) {}
                }
                client = new net.Socket();
                client.connect(${BRIDGE_PORT}, host, () => {
                    console.log('[Bridge] Connected to Android native bridge (' + host + ')');
                    sendToBridge({ type: 'hello', botId: '${safeId}' });
                });

                let buffer = '';
                client.on('data', (data) => {
                    buffer += data.toString();
                    while (buffer.includes('\n')) {
                        const idx = buffer.indexOf('\n');
                        const line = buffer.slice(0, idx).trim();
                        buffer = buffer.slice(idx + 1);
                        if (!line) continue;
                        try {
                            const msg = JSON.parse(line);
                            console.log('[Bridge] Received:', msg);
                            if (msg.type === 'kakao_message') {
                                handleKakaoMessage(msg.data);
                            }
                        } catch (e) {
                            console.error('[Bridge] Parse error:', e, 'line=', line);
                        }
                    }
                });

                client.on('error', (err) => {
                    console.error('[Bridge] Connection error:', err.message || err);
                    try { client.destroy(); } catch (e) {}
                    bridgeHostIndex++;
                    setTimeout(connectBridge, 1000);
                });

                client.on('close', () => {
                    console.warn('[Bridge] Connection closed. Reconnecting...');
                    setTimeout(connectBridge, 1000);
                });
            }

            function sendToBridge(obj) {
                const payload = JSON.stringify(obj);
                if (client && !client.destroyed) {
                    client.write(payload + '\n');
                } else {
                    console.warn('[Bridge] Not connected. Drop:', payload);
                }
            }

            connectBridge();

            setInterval(() => {}, 1000);

            global.bot = {
                command: (cmd, handler) => {
                    sendToBridge({
                        type: 'register_command',
                        command: cmd,
                        handler: handler.toString()
                    });
                },
                prefix: (p) => {
                    prefixes = Array.isArray(p) ? p : [p];
                    sendToBridge({
                        type: 'set_prefix',
                        prefixes: prefixes
                    });
                },
                on: (event, handler) => {
                    sendToBridge({
                        type: 'register_event',
                        event: event,
                        handler: handler.toString()
                    });
                }
            };

            global.device = {
                toast: (message) => {
                    sendToBridge({ type: 'toast', message });
                },
                vibrate: (duration = 100) => {
                    sendToBridge({ type: 'vibrate', duration });
                },
                notification: (title, body) => {
                    sendToBridge({ type: 'notification', title, body });
                }
            };

            function buildEvent(msg) {
                return {
                    msg: msg.content,
                    message: msg.content,
                    sender: msg.sender,
                    room: msg.room,
                    isGroupChat: msg.isGroupChat,
                    reply: (text) => {
                        sendToBridge({ type: 'reply', room: msg.room, message: text });
                    }
                };
            }

            global.__astralSetOnMessage = (handler) => {
                if (typeof handler === 'function') {
                    onMessageHandler = handler;
                }
            };

            global.__astralSetOnCommand = (cmd, handler) => {
                if (!cmd || typeof handler !== 'function') return;
                const cmds = Array.isArray(cmd) ? cmd : [cmd];
                cmds.forEach((c) => { commandHandlers[String(c)] = handler; });
            };

            (function ensureBotManager() {
                try {
                    const modDir = path.join(workspaceRoot, 'node_modules', 'botManager');
                    const modFile = path.join(modDir, 'index.js');
                    if (!fs.existsSync(modDir)) fs.mkdirSync(modDir, { recursive: true });
                    if (!fs.existsSync(modFile)) {
                        fs.writeFileSync(modFile, `module.exports = {\n  onMessage: (handler) => {\n    if (typeof handler === 'function') {\n      global.__astralSetOnMessage(handler);\n    }\n  },\n  onCommand: (cmd, handler) => {\n    if (cmd && typeof handler === 'function') {\n      global.__astralSetOnCommand(cmd, handler);\n    }\n  },\n  setPrefix: (p) => {\n    if (p) {\n      if (Array.isArray(p)) {\n        global.bot && global.bot.prefix(p);\n      } else {\n        global.bot && global.bot.prefix([p]);\n      }\n    }\n  }\n};\n`, 'utf8');
                    }
                } catch (e) {
                    console.error('[Wrapper] botManager init failed:', e);
                }
            })();

            function handleKakaoMessage(msgData) {
                const event = buildEvent(msgData);

                if (typeof onMessageHandler === 'function') {
                    try { onMessageHandler(event); } catch (e) { console.error('[Bot] onMessage error:', e); }
                }

                const text = event.msg || '';
                const matchedPrefix = prefixes.find(p => text.startsWith(p));
                if (matchedPrefix) {
                    const rest = text.substring(matchedPrefix.length).trim();
                    if (rest.length) {
                        const parts = rest.split(/\s+/);
                        const cmd = parts[0];
                        const handler = commandHandlers[cmd];
                        if (handler) {
                            const cmdEvent = Object.assign({}, event, { command: cmd, args: parts.slice(1) });
                            try { handler(cmdEvent); } catch (e) { console.error('[Bot] command error:', e); }
                        }
                    }
                }

                if (eventHandlers['message']) {
                    eventHandlers['message'].forEach(h => {
                        try { h(event); } catch (e) { console.error('[Bot] message handler error:', e); }
                    });
                }
            }

            try {
                console.log('[Wrapper] Loading user script: $scriptPath');
                const userModule = require('$scriptPath');
                if (typeof userModule === 'function') {
                    onMessageHandler = userModule;
                } else if (userModule && typeof userModule.onMessage === 'function') {
                    onMessageHandler = userModule.onMessage;
                } else if (typeof global.onMessage === 'function') {
                    onMessageHandler = global.onMessage;
                }
                console.log('[Wrapper] Script loaded successfully');
            } catch (e) {
                console.error('[Wrapper] Script load failed:', e);
                process.exit(1);
            }
        """.trimIndent()

        val wrapperFile = File(prootManager.rootfsDir, "root/wrapper_$safeId.js")
        wrapperFile.writeText(wrapperContent)

        return "/root/wrapper_$safeId.js"
    }

    /**
     * Build PRoot command with proper bindings
     */
    private fun buildPRootCommand(command: String): List<String> {
        return buildList {
            add(prootManager.prootBinary.absolutePath)
            add("-0")
            add("-p")
            add("-k")
            add("4.19.0")
            add("-r")
            add(prootManager.rootfsDir.absolutePath)
            add("-b")
            add("/dev")
            add("-b")
            add("/proc")
            add("-b")
            add("/sys")
            add("-b")
            add("${prootManager.tmpDir.absolutePath}:/tmp")
            add("-w")
            add("/root")
            add("/bin/sh")
            add("-c")
            add(command)
        }
    }

}
