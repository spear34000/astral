package pics.spear.astral.runtime

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.concurrent.thread

class PythonRuntime(private val context: Context, botId: String) {
    private val prootManager = ProotManager(context)
    private val bridge = NativeBridge.getInstance(context)
    private var currentProcess: Process? = null
    private var pyflakesChecked = false
    private var pyflakesAvailable = false
    private val pyflakesMutex = Mutex()

    companion object {
        private const val TAG = "PythonRuntime"
        private const val BRIDGE_PORT = 33445
    }

    private val safeId = botId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    private val pythonExecutable: String
        get() = prootManager.pythonExecutablePath

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initializing Python runtime...")
        val init = prootManager.initialize()
        if (init.isFailure) return@withContext init

        val ensure = prootManager.ensurePythonEnvironment()
        if (ensure.isFailure) return@withContext ensure

        bridge.start()
        Log.i(TAG, "Python runtime initialized successfully!")
        Result.success(Unit)
    }

    suspend fun runPythonScript(
        scriptPath: String,
        workspaceDir: String? = null,
        onMessage: (String) -> Unit,
        onError: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Running python script: $scriptPath")

            val init = initialize()
            if (init.isFailure) return@withContext init

            val userlandScriptPath = if (workspaceDir != null) {
                val (root, entry) = copyWorkspaceToUserland(workspaceDir, scriptPath)
                "$root/$entry"
            } else {
                copyPythonScriptToUserland(scriptPath)
            }
            val wrapperScript = createPythonWrapperScript(userlandScriptPath, workspaceDir != null)
            val command = prootManager.withVenv("${pythonExecutable} $wrapperScript")

            val processBuilder = ProcessBuilder(buildPRootCommand(command))
            val env = processBuilder.environment()
            env["LD_LIBRARY_PATH"] = context.applicationInfo.nativeLibraryDir
            env["PROOT_LOADER"] = prootManager.prootLoader.absolutePath
            env["PROOT_TMP_DIR"] = prootManager.tmpDir.absolutePath
            env["TMPDIR"] = prootManager.tmpDir.absolutePath
            env["PATH"] = "/usr/bin:/usr/local/bin:/usr/local/sbin:/usr/sbin:/sbin:/bin"

            processBuilder.redirectErrorStream(false)
            currentProcess?.destroy()
            currentProcess = processBuilder.start()

            thread {
                try {
                    currentProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
                        Log.d(TAG, "[PY STDOUT] $line")
                        onMessage(line)
                    }
                } catch (_: java.io.InterruptedIOException) {
                    // ignore when process is closed
                } catch (e: Exception) {
                    Log.w(TAG, "[PY STDOUT] reader closed", e)
                }
            }

            thread {
                try {
                    currentProcess?.errorStream?.bufferedReader()?.forEachLine { line ->
                        Log.e(TAG, "[PY STDERR] $line")
                        onError(line)
                    }
                } catch (_: java.io.InterruptedIOException) {
                    // ignore when process is closed
                } catch (e: Exception) {
                    Log.w(TAG, "[PY STDERR] reader closed", e)
                }
            }

            val exitCode = currentProcess?.waitFor() ?: -1
            Log.i(TAG, "Python script finished with exit code: $exitCode")
            if (exitCode != 0) {
                onError("Python exited with code $exitCode")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run python script", e)
            Result.failure(e)
        }
    }

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
        killPythonWrapper()
    }

    private suspend fun killPythonWrapper() {
        try {
            val cmd = "pkill -f /root/wrapper_$safeId.py || true"
            val res = prootManager.executeInUserland(cmd)
            Log.i(TAG, "Killed Python wrapper for bot $safeId: ${res.isSuccess}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill Python wrapper for bot $safeId", e)
        }
    }

    fun isRunning(): Boolean = currentProcess?.isAlive == true

    suspend fun installPythonPackage(packageName: String): Result<Unit> {
        Log.i(TAG, "Installing pip package: $packageName")
        val init = initialize()
        if (init.isFailure) return init

        val result = prootManager.executeInUserland(prootManager.withVenv("${pythonExecutable} -m pip install $packageName"))
        return if (result.isSuccess) {
            Log.i(TAG, "Pip package installed successfully")
            Result.success(Unit)
        } else {
            Log.e(TAG, "Failed to install pip package: ${result.exceptionOrNull()?.message}")
            result.map { }
        }
    }

    suspend fun recoverPythonEnvironment(): Result<Unit> {
        Log.w(TAG, "Recovering python environment (reset userland)")
        val reset = prootManager.resetUserland()
        if (reset.isFailure) return reset
        val init = initialize()
        if (init.isFailure) return init
        return prootManager.ensurePythonEnvironment()
    }

    suspend fun checkPythonSyntax(scriptPath: String): Result<String> {
        val init = initialize()
        if (init.isFailure) return Result.failure(init.exceptionOrNull() ?: Exception("Runtime not initialized"))

        ensurePyflakesAvailable()

        val userlandScriptPath = copyPythonScriptToUserland(scriptPath)
        val checkerScript = createSyntaxCheckerScript()
        return prootManager.executeInUserland(prootManager.withVenv("${pythonExecutable} $checkerScript $userlandScriptPath"))
    }

    suspend fun checkPythonSyntaxFast(scriptPath: String): Result<String> {
        val init = initialize()
        if (init.isFailure) return Result.failure(init.exceptionOrNull() ?: Exception("Runtime not initialized"))

        val userlandScriptPath = copyPythonScriptToUserland(scriptPath)
        val checkerScript = createQuickPythonSyntaxCheckerScript()
        return prootManager.executeInUserland(prootManager.withVenv("${pythonExecutable} $checkerScript $userlandScriptPath"))
    }

    private fun copyPythonScriptToUserland(scriptPath: String): String {
        val sourceFile = File(scriptPath)
        val destFile = File(prootManager.rootfsDir, "root/script_$safeId.py")
        sourceFile.copyTo(destFile, overwrite = true)
        return "/root/script_$safeId.py"
    }

    private fun copyWorkspaceToUserland(workspaceDir: String, entryPath: String): Pair<String, String> {
        val root = File(prootManager.rootfsDir, "root/workspace_$safeId")
        if (root.exists()) {
            root.deleteRecursively()
        }
        root.mkdirs()

        val workspace = File(workspaceDir)
        workspace.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relative = file.relativeTo(workspace).path.replace("\\", "/")
                val dest = File(root, relative)
                dest.parentFile?.mkdirs()
                file.copyTo(dest, overwrite = true)
            }
        }

        val relativeEntry = runCatching {
            File(entryPath).relativeTo(workspace).path.replace("\\", "/")
        }.getOrNull() ?: File(entryPath).name
        return Pair("/root/workspace_$safeId", relativeEntry)
    }

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

    private fun createPythonWrapperScript(scriptPath: String, useWorkspace: Boolean): String {
        val wrapperContent = """
            |import json
            |import socket
            |import threading
            |import traceback
            |import sys
            |import os
            |import time
            |
            |PORT = $BRIDGE_PORT
            |
            |def connect_bridge():
            |    hosts = ['127.0.0.1', 'localhost', '10.0.2.2']
            |    idx = 0
            |    while True:
            |        host = hosts[idx % len(hosts)]
            |        try:
            |            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            |            s.connect((host, PORT))
            |            return s
            |        except Exception:
            |            try:
            |                s.close()
            |            except Exception:
            |                pass
            |            idx += 1
            |            time.sleep(1)
            |
            |client = connect_bridge()
            |
            |${if (useWorkspace) "os.chdir('/root/workspace_$safeId')\nsys.path.insert(0, '/root/workspace_$safeId')" else ""}
            |
            |def send(msg):
            |    try:
            |        client.send((json.dumps(msg) + "\n").encode())
            |    except Exception:
            |        pass
            |
            |class Context:
            |    def __init__(self, msg):
            |        content = msg.get('content') if isinstance(msg, dict) else None
            |        if content is None:
            |            content = msg.get('message') if isinstance(msg, dict) else None
            |        self.msg = content or ""
            |        self.message = self.msg
            |        self.content = self.msg
            |        self.room = msg.get('room')
            |        self.sender = msg.get('sender')
            |        self.is_group_chat = msg.get('isGroupChat')
            |
            |    def reply(self, text):
            |        send({"type": "reply", "room": self.room, "message": str(text)})
            |
            |class Bot:
            |    def __init__(self):
            |        self.prefixes = ["!", "/"]
            |        self.event_handlers = []
            |        self.command_handlers = {}
            |
            |    def prefix(self, *prefixes):
            |        if len(prefixes) == 1 and isinstance(prefixes[0], (list, tuple)):
            |            self.prefixes = list(prefixes[0])
            |        else:
            |            self.prefixes = list(prefixes)
            |
            |    def on(self, event, handler):
            |        if event == "message":
            |            self.event_handlers.append(handler)
            |
            |    def command(self, cmd, handler):
            |        if isinstance(cmd, (list, tuple)):
            |            for c in cmd:
            |                self.command_handlers[str(c)] = handler
            |        else:
            |            self.command_handlers[str(cmd)] = handler
            |
            |bot = Bot()
            |on_message_handler = None
            |
            |def _set_on_message(handler):
            |    global on_message_handler
            |    if callable(handler):
            |        on_message_handler = handler
            |
            |try:
            |    import types
            |    bot_manager = types.SimpleNamespace()
            |    bot_manager.onMessage = _set_on_message
            |    def _set_on_command(cmd, handler):
            |        if not callable(handler):
            |            return
            |        if isinstance(cmd, (list, tuple)):
            |            for c in cmd:
            |                bot.command_handlers[str(c)] = handler
            |        else:
            |            bot.command_handlers[str(cmd)] = handler
            |    def _set_prefix(prefixes):
            |        bot.prefix(prefixes)
            |    bot_manager.onCommand = _set_on_command
            |    bot_manager.setPrefix = _set_prefix
            |    sys.modules["botManager"] = bot_manager
            |except Exception:
            |    pass
            |
            |def handle_message(data):
            |    ctx = Context(data)
            |    if on_message_handler:
            |        try:
            |            on_message_handler(ctx)
            |        except Exception:
            |            traceback.print_exc()
            |    content = ctx.content or ""
            |    for p in bot.prefixes:
            |        if content.startswith(p):
            |            parts = content[len(p):].split()
            |            if parts:
            |                cmd = parts[0]
            |                handler = bot.command_handlers.get(cmd)
            |                if handler:
            |                    try:
            |                        handler(ctx)
            |                    except Exception:
            |                        traceback.print_exc()
            |            break
            |    for h in bot.event_handlers:
            |        try:
            |            h(ctx)
            |        except Exception:
            |            traceback.print_exc()
            |
            |def listen():
            |    buffer = ""
            |    while True:
            |        data = client.recv(4096)
            |        if not data:
            |            break
            |        buffer += data.decode()
            |        while "\n" in buffer:
            |            line, buffer = buffer.split("\n", 1)
            |            if not line.strip():
            |                continue
            |            try:
            |                msg = json.loads(line)
            |                if msg.get("type") == "kakao_message":
            |                    handle_message(msg.get("data", {}))
            |            except Exception:
            |                pass
            |
            |thread = threading.Thread(target=listen, daemon=True)
            |thread.start()
            |
            |send({"type": "hello", "botId": "${safeId}"})
            |globals()["bot"] = bot
            |
            |try:
            |    with open("$scriptPath", "r", encoding="utf-8") as f:
            |        code = f.read()
            |    exec(compile(code, "$scriptPath", "exec"), globals(), globals())
            |except Exception:
            |    traceback.print_exc()
            |    sys.stderr.flush()
            |    raise
            |
            |thread.join()
        """.trimMargin()

        val wrapperFile = File(prootManager.rootfsDir, "root/wrapper_$safeId.py")
        wrapperFile.writeText(wrapperContent)
        return "/root/wrapper_$safeId.py"
    }

    private fun createSyntaxCheckerScript(): String {
        val checkerContent = """
            import sys
            import os
            from io import StringIO

            if len(sys.argv) < 2:
                print("Usage: syntax_checker.py <file>", file=sys.stderr)
                sys.exit(1)

            file_path = sys.argv[1]

            try:
                from pyflakes import api
                from pyflakes.reporter import Reporter

                out = StringIO()
                err = StringIO()
                rep = Reporter(out, err)
                warnings = api.checkPath(file_path, reporter=rep)
                output = (out.getvalue() + err.getvalue()).strip()
                if output:
                    print(output)
                if warnings > 0:
                    sys.exit(1)
                else:
                    print("OK")
                    sys.exit(0)

            except ImportError:
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        source = f.read()
                    compile(source, file_path, 'exec')
                    print("OK")
                except SyntaxError as e:
                    print(f"Line {e.lineno}: {e.msg}")
                    sys.exit(1)
                except Exception as e:
                    print(str(e))
                    sys.exit(1)
        """.trimIndent()

        val checkerFile = File(prootManager.rootfsDir, "root/syntax_checker.py")
        checkerFile.writeText(checkerContent)
        return "/root/syntax_checker.py"
    }

    private fun createQuickPythonSyntaxCheckerScript(): String {
        val checkerContent = """
            import sys

            if len(sys.argv) < 2:
                print("Line 1: missing file")
                sys.exit(1)

            file_path = sys.argv[1]

            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    source = f.read()
                compile(source, file_path, 'exec')
                print("OK")
            except SyntaxError as e:
                line = e.lineno or 1
                col = e.offset or 1
                print(f"Line {line}:{col}: {e.msg}")
                sys.exit(1)
            except Exception as e:
                print(f"Line 1: {e}")
                sys.exit(1)
        """.trimIndent()

        val checkerFile = File(prootManager.rootfsDir, "root/py_quick_syntax.py")
        checkerFile.writeText(checkerContent)
        return "/root/py_quick_syntax.py"
    }

    private suspend fun ensurePyflakesAvailable(): Boolean {
        return pyflakesMutex.withLock {
            if (pyflakesChecked) return@withLock pyflakesAvailable
            pyflakesChecked = true

            val show = prootManager.executeInUserland(prootManager.withVenv("${pythonExecutable} -m pip show pyflakes"))
            if (show.isSuccess) {
                pyflakesAvailable = true
                return@withLock true
            }

            val install = prootManager.executeInUserland(
                prootManager.withVenv("${pythonExecutable} -m pip install -q --disable-pip-version-check pyflakes")
            )
            pyflakesAvailable = install.isSuccess
            if (!pyflakesAvailable) {
                Log.w(TAG, "Pyflakes install failed: ${install.exceptionOrNull()?.message}")
            }
            pyflakesAvailable
        }
    }
}
