package pics.spear.astral.engine

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * Node.js-compatible runtime using WebView (system V8).
 * Provides require/module/exports, Buffer, process, setTimeout.
 * Zero external dependencies.
 */
class NodeRuntime(
    private val appContext: android.content.Context,
    private val bridge: BotBridge,
) : Runtime {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    override var isReady = false
        private set

    private val jsCallback = mutableMapOf<String, (String) -> Unit>()
    private var callbackCounter = 0

    // ── JS → Java bridge ───────────────────────────────
    private inner class JsBridge {
        @JavascriptInterface
        fun reply(room: String, msg: String) { bridge.reply(room, msg) }

        @JavascriptInterface
        fun toast(text: String) { bridge.toast(text) }

        @JavascriptInterface
        fun vibrate(ms: Long) { bridge.vibrate(ms) }

        @JavascriptInterface
        fun log(botName: String, text: String) { bridge.log(botName, text) }

        @JavascriptInterface
        fun sendRequest(room: String, sender: String, content: String, isGroup: Boolean) {
            bridge.onMessage(room, sender, content, isGroup)
        }

        @JavascriptInterface
        fun jsCallback(id: String, json: String) {
            jsCallback.remove(id)?.invoke(json)
        }
    }

    // ── Init ────────────────────────────────────────────
    override fun initialize() {
        if (isReady) return
        mainHandler.post {
            try {
                webView = WebView(appContext).apply {
                    setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    setBackgroundColor(0)
                    addJavascriptInterface(JsBridge(), "__bridge")
                    loadDataWithBaseURL("file://", NodePolyfill.HTML, "text/html", "UTF-8", null)
                }
                isReady = true
            } catch (e: Exception) {
                Log.e("Astral", "NodeRuntime init fail: ${e.message}")
            }
        }
    }

    override fun evaluate(code: String): Any? {
        if (!isReady) return null
        val cbId = "cb${++callbackCounter}"
        val js = code.trimEnd(';') + "; window.__bridge.jsCallback('$cbId', JSON.stringify(__result||null));"
        var result: Any? = null
        jsCallback[cbId] = { json -> result = json }
        evalJs(js)
        return result
    }

    override fun callFunction(name: String, vararg args: Any?): Any? {
        val jsonArgs = args.joinToString(",") {
            when (it) {
                is String -> "'${it.replace("'", "\\'").replace("\n", "\\n")}'"
                is Number, is Boolean -> it.toString()
                null -> "null"
                else -> "'${it}'"
            }
        }
        return evaluate("__result=$name($jsonArgs)")
    }

    override fun put(key: String, value: Any?) {
        val v = when (value) {
            is String -> "'${value.replace("'", "\\'")}'"
            is Number, is Boolean -> value.toString()
            null -> "null"
            else -> "'$value'"
        }
        evalJs("var $key=$v;")
    }

    override fun destroy() {
        isReady = false
        mainHandler.post { webView?.destroy(); webView = null }
    }

    private fun evalJs(js: String) {
        mainHandler.post {
            try { webView?.evaluateJavascript(js, null) }
            catch (e: Exception) { Log.e("Astral", "NodeRuntime JS err", e) }
        }
    }

    // ── Bot session management (outside this class) ─────
    companion object {
        const val TYPE = "node"
    }
}

/** Bot bridge interface — same contract regardless of runtime. */
interface BotBridge {
    fun reply(room: String, message: String)
    fun toast(text: String)
    fun vibrate(ms: Long)
    fun log(botName: String, text: String)
    fun onMessage(room: String, sender: String, content: String, isGroup: Boolean)
}
