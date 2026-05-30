package pics.spear.astral.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pics.spear.astral.model.Bot
import pics.spear.astral.model.LogEntry
import pics.spear.astral.store.BotStore
import pics.spear.astral.store.LogStore

/**
 * Multi-runtime script engine.
 * Dispatches to NodeRuntime (WebView V8) or PythonRuntime (Jython)
 * based on bot.language ("javascript"/"node" or "python").
 *
 * Implements BotBridge so runtimes can reply / toast / log.
 */
class ScriptEngine(
    private val appContext: Context,
    private val botStore: BotStore,
    private val logStore: LogStore,
    private val api: BotApi,
) : BotBridge {

    private data class Session(
        val botId: String,
        val botName: String,
        val runtime: Runtime,
        val dispatchFn: String?,
        val type: RuntimeType,
    )

    private val sessions = mutableMapOf<String, Session>()
    private var nodeRuntime: NodeRuntime? = null
    private var pythonRuntime: PythonRuntime? = null

    // ── BotBridge: called from runtimes ─────────────────
    override fun reply(room: String, message: String) {
        api.sendReply(room, message)
    }
    override fun toast(text: String) { api.toast(text) }
    override fun vibrate(ms: Long) { api.vibrate(ms) }

    override fun log(botName: String, text: String) {
        Log.d("AstralBot", "[$botName] $text")
        kotlinx.coroutines.MainScope().launch {
            logStore.append(LogEntry(botName = botName, message = text, level = "info"))
        }
    }

    override fun onMessage(room: String, sender: String, content: String, isGroup: Boolean) {
        // forwarded from runtime script callbacks
    }

    // ── Init ────────────────────────────────────────────
    fun initialize() {
        NodeRuntime(appContext, this).also { it.initialize(); nodeRuntime = it }
        PythonRuntime(appContext, this).also { it.initialize(); pythonRuntime = it }
    }

    // ── Bot lifecycle ──────────────────────────────────
    fun startBot(bot: Bot) {
        if (sessions.containsKey(bot.id)) return
        val type = RuntimeType.from(bot.language)
        val code = botStore.loadScript(bot.name)

        when (type) {
            RuntimeType.NODE, RuntimeType.JAVASCRIPT -> startNodeBot(bot, code)
            RuntimeType.PYTHON -> startPythonBot(bot, code)
        }
    }

    private fun startNodeBot(bot: Bot, code: String) {
        val rt = nodeRuntime ?: return
        val fn = "__d_${bot.id.replace("-", "_")}"
        val js = """
            (function(){
                bot._name=${q(bot.name)};
                bot._onMessage=null;
                bot._commands={};
                bot._prefix=['!','/'];
                $code
                window.$fn=function(r,s,m,g){
                    bot._room=r;
                    if(bot._onMessage) bot._onMessage(r,s,m,g);
                    for(var i=0;i<bot._prefix.length;i++){
                        var x=bot._prefix[i];
                        if(m.indexOf(x)===0){
                            var a=m.substring(x.length).split(' ');
                            var c=a[0],aa=a.slice(1);
                            if(bot._commands[c]){bot._commands[c](r,s,m,g,aa);return true}
                            break;
                        }
                    }
                    return false;
                };
            })();
        """.trimIndent()
        rt.evaluate(js)
        sessions[bot.id] = Session(bot.id, bot.name, rt, fn, RuntimeType.NODE)
    }

    private fun startPythonBot(bot: Bot, code: String) {
        val rt = pythonRuntime ?: return
        rt.setupBot(bot.id, code)
        sessions[bot.id] = Session(bot.id, bot.name, rt, null, RuntimeType.PYTHON)
    }

    fun stopBot(id: String) { sessions.remove(id) }
    fun stopAll() { sessions.clear() }
    fun isRunning(id: String) = sessions.containsKey(id)

    fun onDestroy() {
        stopAll()
        nodeRuntime?.destroy(); nodeRuntime = null
        pythonRuntime?.destroy(); pythonRuntime = null
    }

    // ── Event dispatch (Kakao → all bots) ──────────────
    fun startListening(scope: CoroutineScope) {
        scope.launch {
            api.events.collect { e ->
                dispatchToAll(e.room, e.sender, e.content, e.isGroupChat)
            }
        }
    }

    fun dispatchToBot(botId: String, room: String, sender: String, content: String, isGroup: Boolean) {
        val session = sessions[botId] ?: return
        when (session.type) {
            RuntimeType.NODE -> {
                val rt = nodeRuntime ?: return
                rt.callFunction(session.dispatchFn ?: return, room, sender, content, isGroup)
            }
            RuntimeType.PYTHON -> pythonRuntime?.dispatch(room, sender, content, isGroup)
            else -> {}
        }
    }

    fun dispatchToAll(room: String, sender: String, content: String, isGroup: Boolean) {
        sessions.keys.toList().forEach { id -> dispatchToBot(id, room, sender, content, isGroup) }
    }

    fun getSessions(): Map<String, String> = sessions.mapValues { it.value.botName }

    private fun q(s: String) =
        "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r") + "'"
}
