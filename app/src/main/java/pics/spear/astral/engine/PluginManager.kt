package pics.spear.astral.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pics.spear.astral.util.Storage
import java.io.File
import java.util.zip.ZipFile

/**
 * Plugin system with .atlp format support.
 *
 * .atlp is a ZIP package containing:
 *   plugin.json  — manifest
 *   main.js      — plugin code (optional)
 *   assets/      — resources (optional)
 */
object PluginManager {

    enum class Hook {
        APP_START, APP_STOP,
        BOT_CREATED, BOT_DELETED, BOT_ENABLED, BOT_DISABLED,
        KAKAO_MESSAGE_RECEIVED, FLOW_COMPILED,
    }

    @Serializable
    data class PluginManifest(
        val id: String,
        val name: String,
        val version: String = "1.0",
        val description: String = "",
        val author: String = "",
        val hooks: List<String> = emptyList(),
        val main: String = "main.js",
        val permissions: List<String> = emptyList(),
    )

    data class Plugin(
        val manifest: PluginManifest,
        val source: File,
        val code: String = "",
        val enabled: Boolean = true,
    )

    private val plugins = mutableListOf<Plugin>()
    private val _events = MutableSharedFlow<Pair<Hook, Map<String, Any?>>>(extraBufferCapacity = 32)
    val events: SharedFlow<Pair<Hook, Map<String, Any?>>> = _events.asSharedFlow()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    // ── .atlp loading ──────────────────────────────────
    fun loadFromFile(file: File): Plugin? {
        try {
            if (!file.name.endsWith(".atlp")) return null
            val zip = ZipFile(file)
            val manifestEntry = zip.getEntry("plugin.json") ?: run {
                zip.close(); Log.w("Astral", "No plugin.json in ${file.name}"); return null
            }
            val manifestText = zip.getInputStream(manifestEntry).reader().readText()
            val manifest = json.decodeFromString<PluginManifest>(manifestText)

            var code = ""
            val mainEntry = zip.getEntry(manifest.main)
            if (mainEntry != null) {
                code = zip.getInputStream(mainEntry).reader().readText()
            }

            zip.close()
            val plugin = Plugin(manifest, file, code)
            if (plugins.none { it.manifest.id == manifest.id }) {
                plugins.add(plugin)
                Log.i("Astral", "Plugin loaded: ${manifest.name} v${manifest.version} (${
                    file.name
                })")
            }
            return plugin
        } catch (e: Exception) {
            Log.e("Astral", "Failed to load plugin ${file.name}: ${e.message}")
            return null
        }
    }

    fun loadAll(context: Context) {
        val dir = Storage.pluginsDir(context)
        if (!dir.exists()) { dir.mkdirs(); return }
        dir.listFiles { f -> f.name.endsWith(".atlp") }?.forEach { loadFromFile(it) }
    }

    fun createSample(context: Context): File {
        val dir = Storage.pluginsDir(context)
        dir.mkdirs()
        val sample = File(dir, "example.atlp")
        if (sample.exists()) return sample

        val manifest = PluginManifest(
            id = "example", name = "Example Plugin", version = "1.0.0",
            description = "Sample .atlp plugin", author = "Astral",
            hooks = listOf("APP_START", "KAKAO_MESSAGE_RECEIVED"),
            permissions = listOf("reply"),
        )
        val code = """
            // Astral Plugin v1
            // This runs when the hook triggers
            console.log('Example plugin loaded!');
            module.exports = {
                onAppStart: function() { bot.log('Plugin: App started'); },
                onMessage: function(room, sender, msg) {
                    if (msg.includes('hello')) bot.reply('Hello from plugin!');
                }
            };
        """.trimIndent()

        try {
            val bytes = java.io.ByteArrayOutputStream()
            val zipOut = java.util.zip.ZipOutputStream(bytes)
            zipOut.putNextEntry(java.util.zip.ZipEntry("plugin.json"))
            zipOut.write(json.encodeToString(manifest).toByteArray())
            zipOut.closeEntry()
            zipOut.putNextEntry(java.util.zip.ZipEntry("main.js"))
            zipOut.write(code.toByteArray())
            zipOut.closeEntry()
            zipOut.close()
            sample.writeBytes(bytes.toByteArray())
            Log.i("Astral", "Sample plugin created: ${sample.absolutePath}")
        } catch (e: Exception) {
            Log.e("Astral", "Failed to create sample plugin", e)
        }
        return sample
    }

    // ── Runtime ────────────────────────────────────────
    fun register(plugin: Plugin): Boolean {
        if (plugins.any { it.manifest.id == plugin.manifest.id }) return false
        plugins.add(plugin)
        Log.i("Astral", "Plugin registered: ${plugin.manifest.name}")
        return true
    }

    fun unregister(id: String) {
        plugins.removeAll { it.manifest.id == id }
    }

    fun remove(context: Context, id: String) {
        val p = plugins.find { it.manifest.id == id } ?: return
        p.source.delete()
        plugins.remove(p)
    }

    fun trigger(hook: Hook, data: Map<String, Any?> = emptyMap()) {
        for (p in plugins) {
            if (!p.enabled) continue
            if (p.manifest.hooks.any { it == hook.name }) {
                Log.d("Astral", "Hook ${hook.name} → ${p.manifest.name}")
            }
        }
        _events.tryEmit(hook to data)
    }

    fun getPlugins(): List<Plugin> = plugins.toList()
    fun isRegistered(id: String): Boolean = plugins.any { it.manifest.id == id }
    fun toggle(id: String) {
        val idx = plugins.indexOfFirst { it.manifest.id == id }
        if (idx >= 0) plugins[idx] = plugins[idx].copy(enabled = !plugins[idx].enabled)
    }
}
