package pics.spear.astral.device

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import pics.spear.astral.util.AstralStorage

class DeviceBridge(private val appContext: Context) {

    @JavascriptInterface
    fun info(): String {
        val pm = appContext.packageManager
        val pkg = appContext.packageName
        val pInfo = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull()
        val obj = JSONObject()
            .put("packageName", pkg)
            .put("versionName", pInfo?.versionName ?: "")
            .put("versionCode", if (Build.VERSION.SDK_INT >= 28) (pInfo?.longVersionCode ?: 0L) else (pInfo?.versionCode ?: 0))
            .put("sdkInt", Build.VERSION.SDK_INT)
            .put("device", Build.DEVICE ?: "")
            .put("model", Build.MODEL ?: "")
            .put("manufacturer", Build.MANUFACTURER ?: "")
        return obj.toString()
    }

    @JavascriptInterface
    fun toast(text: String?) {
        if (text.isNullOrBlank()) return
        Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun vibrate(ms: Int) {
        val vib = if (Build.VERSION.SDK_INT >= 31) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        
        val dur = ms.coerceIn(1, 5_000)
        if (Build.VERSION.SDK_INT >= 26) vib.vibrate(VibrationEffect.createOneShot(dur.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vib.vibrate(dur.toLong())
    }

    @JavascriptInterface
    fun setClipboard(text: String?) {
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("astral", text.orEmpty()))
    }

    @JavascriptInterface
    fun getClipboard(): String {
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return ""
        val clip = cm.primaryClip ?: return ""
        if (clip.itemCount <= 0) return ""
        return clip.getItemAt(0).coerceToText(appContext)?.toString().orEmpty()
    }

    @JavascriptInterface
    fun openUrl(url: String?): Boolean {
        val u = url?.trim().orEmpty()
        if (u.isBlank()) return false
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { appContext.startActivity(intent) }.isSuccess
    }

    @JavascriptInterface
    fun openAppSettings(): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${appContext.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { appContext.startActivity(intent) }.isSuccess
    }

    @JavascriptInterface
    fun openNotificationListenerSettings(): Boolean {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { appContext.startActivity(intent) }.isSuccess
    }

    @JavascriptInterface
    fun readText(path: String?): String {
        val f = resolveSandboxFile(path) ?: return ""
        if (!f.exists() || !f.isFile) return ""
        return runCatching { f.readText() }.getOrDefault("")
    }

    @JavascriptInterface
    fun writeText(path: String?, content: String?): Boolean {
        val f = resolveSandboxFile(path) ?: return false
        return runCatching {
            f.parentFile?.mkdirs()
            f.writeText(content.orEmpty())
            true
        }.getOrDefault(false)
    }

    @JavascriptInterface
    fun listFiles(path: String?): String {
        val f = resolveSandboxFile(path) ?: return "[]"
        if (!f.exists() || !f.isDirectory) return "[]"
        val arr = JSONArray()
        f.listFiles()?.sortedBy { it.name }?.forEach { child ->
            arr.put(
                JSONObject()
                    .put("name", child.name)
                    .put("isDir", child.isDirectory)
                    .put("size", if (child.isFile) child.length() else 0L)
            )
        }
        return arr.toString()
    }

    private fun resolveSandboxFile(path: String?): File? {
        val rel = path?.trim().orEmpty()
        if (rel.isBlank()) return null
        if (rel.contains("..")) return null
        val base = AstralStorage.baseDir(appContext)
        return File(base, rel)
    }
}


