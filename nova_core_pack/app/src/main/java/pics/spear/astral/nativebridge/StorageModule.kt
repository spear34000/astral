package pics.spear.astral.nativebridge

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import pics.spear.astral.util.AstralStorage

class StorageModule(private val appContext: Context) {
    fun call(method: String, args: JSONArray): Any? {
        return when (method) {
            "readText" -> readText(args.optString(0, ""))
            "writeText" -> writeText(args.optString(0, ""), args.optString(1, ""))
            "listFiles" -> listFiles(args.optString(0, ""))
            else -> throw IllegalArgumentException("unknown_method:storage.$method")
        }
    }

    private fun readText(path: String): String {
        val f = resolveSandboxFile(path) ?: return ""
        if (!f.exists() || !f.isFile) return ""
        return runCatching { f.readText() }.getOrDefault("")
    }

    private fun writeText(path: String, content: String): Boolean {
        val f = resolveSandboxFile(path) ?: return false
        return runCatching {
            f.parentFile?.mkdirs()
            f.writeText(content)
            true
        }.getOrDefault(false)
    }

    private fun listFiles(path: String): JSONArray {
        val f = resolveSandboxFile(path) ?: return JSONArray()
        if (!f.exists() || !f.isDirectory) return JSONArray()
        val arr = JSONArray()
        f.listFiles()?.sortedBy { it.name }?.forEach { child ->
            arr.put(
                JSONObject()
                    .put("name", child.name)
                    .put("isDir", child.isDirectory)
                    .put("size", if (child.isFile) child.length() else 0L)
            )
        }
        return arr
    }

    private fun resolveSandboxFile(path: String): File? {
        val rel = path.trim()
        if (rel.isBlank()) return null
        if (rel.contains("..")) return null
        val base = AstralStorage.baseDir(appContext)
        return File(base, rel)
    }
}



