package pics.spear.astral.engine

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pics.spear.astral.model.Bot
import pics.spear.astral.model.LogEntry
import pics.spear.astral.store.BotStore
import pics.spear.astral.store.LogStore
import java.util.concurrent.ConcurrentHashMap

/**
 * JavaScript bot engine using Android's built-in WebView (V8).
 * Zero external dependencies — uses system WebView.
 *
 * Bot JS API:
 *   bot.onMessage(function(room, sender, msg, isGroup) { ... })
 *   bot.onCommand("name", function(room, sender, msg, isGroup, args) { ... })
 *   bot.setPrefix(["!", "/"])
 *   bot.reply("message")
 *   bot.toast("text")
 *   bot.vibrate(ms)
 *   bot.log("text")
 *   bot.getName()
 */
class ScriptEngine(
    private val appContext: android.content.Context,
    private val botStore: BotStore,
    private val logStore: LogStore,
    private val api: BotApi,
) {
    private data class BotSession(val name: String, val dispatchFn: String)
    private val sessions = ConcurrentHashMap<String, BotSession>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var webViewReady = false
    private val pendingSetups = mutableListOf<() -> Unit>()

    // ── Java → JS bridge ───────────────────────────────
    private inner class Bridge {
        @JavascriptInterface
        fun reply(room: String, message: String) { api.sendReply(room, message) }

        @JavascriptInterface
        fun toast(text: String) { api.toast(text) }

        @JavascriptInterface
        fun vibrate(ms: Long) { api.vibrate(ms) }

        @JavascriptInterface
        fun log(botName: String, text: String) {
            Log.d("AstralBot", "[$botName] $text")
            kotlinx.coroutines.MainScope().launch {
                logStore.append(LogEntry(botName = botName, message = text, level = "info"))
            }
        }
    }

    // ── Init WebView (must be called from main thread) ─
    fun initialize() {
        if (webViewReady) return
        mainHandler.post {
            try {
                webView = WebView(appContext).apply {
                    setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    setBackgroundColor(0)
                    addJavascriptInterface(Bridge(), "__bridge")

                    loadDataWithBaseURL(null, """
                        <!DOCTYPE html><html><body>
                        <script>
                        var bot = {
                            reply: function(m) { __bridge.reply(bot._room, m); },
                            toast: function(t) { __bridge.toast(t); },
                            vibrate: function(m) { __bridge.vibrate(m || 200); },
                            log: function(t) { __bridge.log(bot._name, t); },
                            getName: function() { return bot._name; },
                            _room: '',
                            _name: ''
                        };
                        <\/script>
                        </body></html>
                    """.trimIndent(), "text/html", "UTF-8", null)
                }
                webViewReady = true
                pendingSetups.toList().forEach { it() }
                pendingSetups.clear()
            } catch (e: Exception) {
                Log.e("Astral", "WV init: ${e.message}")
            }
        }
    }

    fun startBot(bot: Bot) {
        if (sessions.containsKey(bot.id)) return
        val userScript = botStore.loadScript(bot.name)

        val setup = {
            val fn = "__d_${bot.id.replace("-", "_")}"
            val name = jsStr(bot.name)
            val js = """
                (function(){
                    var _m=null,_c={},_p=['!','/'];
                    bot._name=$name;
                    bot.onMessage=function(f){_m=f};
                    bot.onCommand=function(c,f){_c[c]=f};
                    bot.setPrefix=function(p){_p=typeof p=='string'?[p]:p};
                    $userScript
                    window.$fn=function(r,s,m,g){
                        bot._room=r;
                        if(_m)_m(r,s,m,g);
                        for(var i=0;i<_p.length;i++){
                            var x=_p[i];
                            if(m.indexOf(x)===0){
                                var a=m.substring(x.length).split(' ');
                                var c=a[0],aa=a.slice(1);
                                if(_c[c]){_c[c](r,s,m,g,aa);return true}
                                break;
                            }
                        }
                        return false
                    };
                })();
            """.trimIndent()

            evalJs(js) { sessions[bot.id] = BotSession(bot.name, fn) }
        }

        if (webViewReady) setup() else { pendingSetups.add(setup); initialize() }
    }

    fun stopBot(id: String) { sessions.remove(id) }
    fun stopAll() { sessions.clear() }
    fun isRunning(id: String) = sessions.containsKey(id)

    fun onDestroy() { stopAll(); mainHandler.post { webView?.destroy() } }

    // ── Event dispatch ─────────────────────────────────
    fun startListening(scope: CoroutineScope) {
        scope.launch {
            api.events.collect { e ->
                sessions.keys.toList().forEach { id ->
                    val s = sessions[id] ?: return@forEach
                    evalJs("${s.dispatchFn}(${q(e.room)},${q(e.sender)},${q(e.content)},${e.isGroupChat})")
                }
            }
        }
    }

    // ── Helpers ────────────────────────────────────────
    private fun evalJs(js: String, done: (() -> Unit)? = null) {
        mainHandler.post {
            try { webView?.evaluateJavascript(js, null); done?.invoke() }
            catch (e: Exception) { Log.e("Astral", "JS err", e) }
        }
    }

    private fun q(s: String) = "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r") + "'"
    private fun jsStr(s: String) = q(s)
}
