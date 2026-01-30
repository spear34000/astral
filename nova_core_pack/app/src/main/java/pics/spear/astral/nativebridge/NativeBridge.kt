package pics.spear.astral.nativebridge

import android.content.Context
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject
import pics.spear.astral.device.DeviceBridge
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.WebView
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * JS <-> Android bridge for `native.call("module.method", ...args)`.
 *
 * Security model: allowlist modules/methods implemented here.
 */
class NativeBridge(
    private val appContext: Context,
    private val webView: WebView,
) {
    private val device = DeviceBridge(appContext)
    private val storage = StorageModule(appContext)
    private val http = HttpBridge()
    private val db = DbModule(appContext)
    private val notify = NotifyModule(appContext)
    private val ui = UiModule()

    private val main = Handler(Looper.getMainLooper())
    private val pending = ConcurrentHashMap<String, Boolean>()

    @JavascriptInterface
    fun call(name: String?, argsJson: String?): String {
        val full = name?.trim().orEmpty()
        if (full.isBlank() || !full.contains('.')) return error("bad_name")

        val args = runCatching {
            if (argsJson.isNullOrBlank()) JSONArray() else JSONArray(argsJson)
        }.getOrElse { JSONArray() }

        val module = full.substringBefore('.')
        val method = full.substringAfter('.', "")
        if (method.isBlank()) return error("bad_method")

        return runCatching {
            val value = when (module) {
                "device" -> callDevice(method, args)
                "storage" -> storage.call(method, args)
                "notify" -> callNotify(method, args)
                "ui" -> callUiSync(method, args)
                "activity" -> callActivity(method, args)
                else -> return error("unknown_module:$module")
            }
            ok(value)
        }.getOrElse { e ->
            error(e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Async for slow ops (http/db/fs etc).
     * JS will receive callback via window.__novaNativeResolve(token, base64(json)).
     */
    @JavascriptInterface
    fun callAsync(name: String?, argsJson: String?, token: String?) {
        val t = token?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        if (pending.putIfAbsent(t, true) != null) return

        thread(name = "nova-native-$t") {
            val result = runCatching { callInternal(name, argsJson) }
                .getOrElse { error(it.message ?: it.javaClass.simpleName) }
            val payload = base64EncodeUriComponent(result)
            main.post {
                webView.evaluateJavascript(
                    "window.__novaNativeResolve && window.__novaNativeResolve(${t.toJsString()}, ${payload.toJsString()});",
                    null
                )
                pending.remove(t)
            }
        }
    }

    private fun callInternal(name: String?, argsJson: String?): String {
        val full = name?.trim().orEmpty()
        if (full.isBlank() || !full.contains('.')) return error("bad_name")
        val args = runCatching {
            if (argsJson.isNullOrBlank()) JSONArray() else JSONArray(argsJson)
        }.getOrElse { JSONArray() }
        val module = full.substringBefore('.')
        val method = full.substringAfter('.', "")
        if (method.isBlank()) return error("bad_method")

        return runCatching {
            val value = when (module) {
                "device" -> callDevice(method, args)
                "storage" -> storage.call(method, args)
                "http" -> callHttp(method, args)
                "db" -> callDb(method, args)
                "notify" -> callNotify(method, args)
                "ui" -> callUiAsync(method, args)
                "activity" -> callActivity(method, args)
                else -> return error("unknown_module:$module")
            }
            ok(value)
        }.getOrElse { e ->
            error(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun callDevice(method: String, args: JSONArray): Any? {
        return when (method) {
            "info" -> device.info()
            "toast" -> {
                device.toast(args.optString(0, ""))
                null
            }
            "vibrate" -> {
                device.vibrate(args.optInt(0, 0))
                null
            }
            "setClipboard" -> {
                device.setClipboard(args.optString(0, ""))
                null
            }
            "getClipboard" -> device.getClipboard()
            "openUrl" -> device.openUrl(args.optString(0, ""))
            "openAppSettings" -> device.openAppSettings()
            "openNotificationSettings" -> device.openNotificationListenerSettings()
            else -> throw IllegalArgumentException("unknown_method:device.$method")
        }
    }

    private fun callHttp(method: String, args: JSONArray): Any? {
        return when (method) {
            "get" -> {
                val url = args.optString(0, "")
                val headers = args.optJSONObject(1)
                http.get(url, headers)
            }
            "post" -> {
                val url = args.optString(0, "")
                val body = args.optString(1, "")
                val contentType = args.optString(2, "application/json; charset=utf-8")
                val headers = args.optJSONObject(3)
                http.post(url, body, contentType, headers)
            }
            else -> throw IllegalArgumentException("unknown_method:http.$method")
        }
    }

    private fun callDb(method: String, args: JSONArray): Any? {
        return when (method) {
            "exec" -> db.exec(args.optString(0, ""), args.optJSONArray(1) ?: JSONArray())
            "query" -> db.query(args.optString(0, ""), args.optJSONArray(1) ?: JSONArray())
            "kvSet", "set" -> db.kvSet(args.optString(0, ""), args.optString(1, ""))
            "kvGet", "get" -> db.kvGet(args.optString(0, ""))
            else -> throw IllegalArgumentException("unknown_method:db.$method")
        }
    }

    private fun callNotify(method: String, args: JSONArray): Any? {
        return when (method) {
            "notify" -> notify.notify(args.optInt(0, 1), args.optString(1, "astral"), args.optString(2, ""))
            "cancel" -> notify.cancel(args.optInt(0, 1))
            else -> throw IllegalArgumentException("unknown_method:notify.$method")
        }
    }

    private fun callActivity(method: String, args: JSONArray): Any? {
        val activity = ActivityProvider.get() ?: return false
        return when (method) {
            "finish" -> {
                main.post { activity.finish() }
                true
            }
            "startUrl" -> {
                val url = args.optString(0, "")
                main.post {
                    runCatching {
                        activity.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                    }
                }
                true
            }
            else -> throw IllegalArgumentException("unknown_method:activity.$method")
        }
    }

    private fun callUiSync(method: String, args: JSONArray): Any? {
        return when (method) {
            "alert" -> ui.alert(args.optString(0, ""))
            else -> throw IllegalArgumentException("unknown_method:ui.$method (use callAsync)")
        }
    }

    private fun callUiAsync(method: String, args: JSONArray): Any? {
        return when (method) {
            "alert" -> ui.alert(args.optString(0, ""))
            "confirm" -> ui.confirm(args.optString(0, ""), args.optInt(1, 15_000))
            else -> throw IllegalArgumentException("unknown_method:ui.$method")
        }
    }

    private fun ok(value: Any?): String {
        val obj = JSONObject().put("ok", true)
        when (value) {
            null -> obj.put("value", JSONObject.NULL)
            is Boolean -> obj.put("value", value)
            is Int -> obj.put("value", value)
            is Long -> obj.put("value", value)
            is Double -> obj.put("value", value)
            is Float -> obj.put("value", value.toDouble())
            is String -> obj.put("value", value)
            is JSONObject -> obj.put("value", value)
            is JSONArray -> obj.put("value", value)
            else -> obj.put("value", value.toString())
        }
        return obj.toString()
    }

    private fun error(message: String): String =
        JSONObject()
            .put("ok", false)
            .put("error", message)
            .toString()

    private fun base64EncodeUriComponent(text: String): String {
        val encoded = encodeURIComponent(text)
        return Base64.encodeToString(encoded.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun encodeURIComponent(text: String): String {
        return URLEncoder.encode(text, Charsets.UTF_8.name())
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")
    }

    private fun String.toJsString(): String =
        buildString {
            append('"')
            for (ch in this@toJsString) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
}


