package pics.spear.astral.engine

import android.content.Context
import android.util.Log
import org.python.core.Py
import org.python.core.PySystemState
import org.python.util.PythonInterpreter

/**
 * Python runtime using Jython (Python in pure Java).
 * Supports Python 2.7 syntax with bot API bridge.
 */
class PythonRuntime(
    private val appContext: Context,
    private val bridge: BotBridge,
) : Runtime {

    private var interpreter: PythonInterpreter? = null
    override var isReady = false
        private set

    // Stores registered callback function names
    private var onMessageFn: String? = null
    private var onCommandFn: String? = null

    override fun initialize() {
        try {
            val props = java.util.Properties()
            props.setProperty("python.import.site", "false")
            props.setProperty("python.console.encoding", "UTF-8")
            props.setProperty("python.cachedir.skip", "true")
            props.setProperty("python.path", "")

            PySystemState.initialize(PySystemState.getBaseProperties(), props, arrayOf())
            interpreter = PythonInterpreter()

            // Inject bot API as Python objects
            interpreter!!.exec("""
                import sys
                class _Bot:
                    _name = ''
                    _room = ''
                    _prefix = ['!', '/']
                    _on_message = None
                    _on_command = {}
                    
                    def reply(self, msg):
                        _java_bridge.reply(self._room, str(msg))
                    
                    def toast(self, text):
                        _java_bridge.toast(str(text))
                    
                    def vibrate(self, ms):
                        _java_bridge.vibrate(int(ms or 200))
                    
                    def log(self, text):
                        _java_bridge.log(self._name, str(text))
                    
                    def get_name(self):
                        return self._name
                    
                    def on_message(self, fn):
                        self._on_message = fn
                    
                    def on_command(self, cmd, fn):
                        self._on_command[str(cmd)] = fn
                    
                    def set_prefix(self, p):
                        if isinstance(p, str):
                            self._prefix = [p]
                        else:
                            self._prefix = list(p)
                
                bot = _Bot()
            """.trimIndent())

            interpreter!!.set("_java_bridge", PythonBridge())
            isReady = true
        } catch (e: Exception) {
            Log.e("Astral", "PythonRuntime init fail: ${e.message}", e)
        }
    }

    override fun evaluate(code: String): Any? {
        if (!isReady || interpreter == null) return null
        return try {
            interpreter!!.eval(code)
        } catch (e: Exception) {
            Log.e("Astral", "Python eval error: ${e.message}")
            null
        }
    }

    override fun callFunction(name: String, vararg args: Any?): Any? {
        if (!isReady) return null
        return try {
            val pyArgs = args.map { Py.java2py(it) }.toTypedArray()
            interpreter!!.invoke(name, *pyArgs)
        } catch (e: Exception) {
            Log.e("Astral", "Python call '$name' error: ${e.message}")
            null
        }
    }

    override fun put(key: String, value: Any?) {
        interpreter?.set(key, Py.java2py(value))
    }

    override fun destroy() {
        isReady = false
        interpreter?.close()
        interpreter = null
    }

    /** Setup bot script — wraps user code with bot wiring. */
    fun setupBot(botId: String, code: String) {
        onMessageFn = "__onmsg_${botId.replace("-", "_")}"
        onCommandFn = "__oncmd_${botId.replace("-", "_")}"

        val wrapped = """
            def $onMessageFn(room, sender, msg, is_group):
                bot._room = room
                if bot._on_message:
                    bot._on_message(room, sender, msg, is_group)
                for p in bot._prefix:
                    if msg.startswith(p):
                        parts = msg[len(p):].split()
                        cmd = parts[0]
                        args = parts[1:]
                        if cmd in bot._on_command:
                            bot._on_command[cmd](room, sender, msg, is_group, args)
                            return True
                        break
                return False
            
            # User code
            $code
        """.trimIndent()

        interpreter?.exec(wrapped)
    }

    /** Dispatch incoming message to Python callback. */
    fun dispatch(room: String, sender: String, content: String, isGroup: Boolean) {
        callFunction(onMessageFn ?: return, room, sender, content, isGroup)
    }

    // ── Python → Java bridge (accessible from Python) ──
    inner class PythonBridge {
        fun reply(room: String, msg: String) { bridge.reply(room, msg) }
        fun toast(text: String) { bridge.toast(text) }
        fun vibrate(ms: Long) { bridge.vibrate(ms) }
        fun log(botName: String, text: String) { bridge.log(botName, text) }
        fun onMessage(room: String, sender: String, content: String, isGroup: Boolean) {
            bridge.onMessage(room, sender, content, isGroup)
        }
    }

    companion object {
        const val TYPE = "python"
    }
}
