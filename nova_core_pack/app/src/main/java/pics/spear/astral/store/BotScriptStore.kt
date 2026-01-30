package pics.spear.astral.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import pics.spear.astral.util.AstralStorage

object BotScriptStore {
    private fun legacyDir(context: Context): File =
        File(context.filesDir, "astral/bots").apply { mkdirs() }

    private fun workspaceRoot(context: Context): File =
        AstralStorage.botScriptsDir(context)

    private fun sanitizeFolderName(name: String): String {
        val cleaned = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return cleaned.ifBlank { "Bot" }
    }

    private suspend fun resolveBotFolderName(context: Context, botId: String): String = withContext(Dispatchers.IO) {
        val bots = BotStore.list(context)
        val botName = bots.find { it.id == botId }?.name?.trim()
        sanitizeFolderName(botName ?: botId)
    }

    private suspend fun workspaceDir(context: Context, botId: String): File = withContext(Dispatchers.IO) {
        val folderName = resolveBotFolderName(context, botId)
        File(workspaceRoot(context), folderName).apply { mkdirs() }
    }

    private suspend fun manifestFile(context: Context, botId: String): File = withContext(Dispatchers.IO) {
        File(workspaceDir(context, botId), "workspace.json")
    }

    private fun legacyFile(context: Context, botId: String, language: String): File =
        File(legacyDir(context), "$botId.${extensionFor(language)}")

    private suspend fun flowFile(context: Context, botId: String): File = withContext(Dispatchers.IO) {
        val folderName = resolveBotFolderName(context, botId)
        File(workspaceDir(context, botId), "$folderName.json")
    }

    data class WorkspaceManifest(
        val entry: String,
        val language: String
    )

    suspend fun loadOrCreate(context: Context, botId: String, language: String = "js"): String = withContext(Dispatchers.IO) {
        val manifest = ensureWorkspace(context, botId, language)
        val entryFile = File(workspaceDir(context, botId), manifest.entry)
        entryFile.readText()
    }

    suspend fun save(context: Context, botId: String, code: String, language: String = "js") = withContext(Dispatchers.IO) {
        val manifest = ensureWorkspace(context, botId, language)
        val entryFile = File(workspaceDir(context, botId), manifest.entry)
        entryFile.writeText(code)
    }

    suspend fun listFiles(context: Context, botId: String): List<String> = withContext(Dispatchers.IO) {
        val root = workspaceDir(context, botId)
        if (!root.exists()) return@withContext emptyList()
        val entries = mutableListOf<String>()
        root.walkTopDown().forEach { node ->
            if (node == root) return@forEach
            val rel = node.relativeTo(root).path.replace("\\", "/")
            if (node.isDirectory) {
                entries.add("$rel/")
            } else if (node.isFile && node.name != "workspace.json") {
                entries.add(rel)
            }
        }
        entries.sorted()
    }

    suspend fun getWorkspaceDir(context: Context, botId: String): File = withContext(Dispatchers.IO) {
        workspaceDir(context, botId)
    }

    suspend fun readFile(context: Context, botId: String, relativePath: String): String = withContext(Dispatchers.IO) {
        val file = resolveWorkspacePath(context, botId, relativePath)
        file.readText()
    }

    suspend fun writeFile(context: Context, botId: String, relativePath: String, code: String) = withContext(Dispatchers.IO) {
        val file = resolveWorkspacePath(context, botId, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(code)
    }

    suspend fun deleteFile(context: Context, botId: String, relativePath: String) = withContext(Dispatchers.IO) {
        val file = resolveWorkspacePath(context, botId, relativePath)
        if (file.exists()) file.delete()
    }

    suspend fun deletePath(context: Context, botId: String, relativePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleaned = sanitizePath(relativePath).trimEnd('/')
            val file = resolveWorkspacePath(context, botId, cleaned)
            if (!file.exists()) return@withContext Result.failure(Exception("path not found"))
            val ok = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (ok) {
                pruneEmptyAncestors(context, botId, file.parentFile)
                Result.success(Unit)
            } else {
                Result.failure(Exception("delete failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolder(context: Context, botId: String, relativePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleaned = sanitizePath(relativePath).trimEnd('/')
            if (cleaned.isBlank()) return@withContext Result.failure(Exception("invalid folder name"))
            val folder = resolveWorkspacePath(context, botId, cleaned)
            if (folder.exists()) {
                if (folder.isDirectory) Result.success(Unit)
                else Result.failure(Exception("file already exists"))
            } else {
                if (folder.mkdirs()) Result.success(Unit) else Result.failure(Exception("create failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(context: Context, botId: String, fromPath: String, toPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val from = resolveWorkspacePath(context, botId, fromPath)
        val to = resolveWorkspacePath(context, botId, toPath)
        if (!from.exists()) return@withContext Result.failure(Exception("file not found"))
        if (to.exists()) return@withContext Result.failure(Exception("target already exists"))
        to.parentFile?.mkdirs()
        if (!from.renameTo(to)) {
            return@withContext Result.failure(Exception("rename failed"))
        }
        Result.success(Unit)
    }

    suspend fun getEntry(context: Context, botId: String, language: String = "js"): String = withContext(Dispatchers.IO) {
        val manifest = ensureWorkspace(context, botId, language)
        manifest.entry
    }

    suspend fun setEntry(context: Context, botId: String, language: String, entryPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleaned = sanitizePath(entryPath)
        val ext = extensionFor(language)
        if (!cleaned.endsWith(".$ext")) {
            return@withContext Result.failure(Exception("entry file must be .$ext"))
        }
        val file = resolveWorkspacePath(context, botId, cleaned)
        if (!file.exists()) {
            return@withContext Result.failure(Exception("entry file not found"))
        }
        writeManifest(context, botId, WorkspaceManifest(cleaned, language))
        Result.success(Unit)
    }

    suspend fun ensureWorkspace(context: Context, botId: String, language: String): WorkspaceManifest = withContext(Dispatchers.IO) {
        val dir = workspaceDir(context, botId)
        val manifestFile = manifestFile(context, botId)

        // Migrate legacy folder (botId) -> new folder (botName) if needed
        val legacyDir = File(workspaceRoot(context), botId)
        if (legacyDir.exists() && legacyDir.canonicalPath != dir.canonicalPath) {
            if (!dir.exists() || dir.listFiles().isNullOrEmpty()) {
                val renamed = legacyDir.renameTo(dir)
                if (!renamed) {
                    legacyDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val rel = file.relativeTo(legacyDir).path.replace("\\", "/")
                            val dest = File(dir, rel)
                            dest.parentFile?.mkdirs()
                            file.copyTo(dest, overwrite = true)
                        }
                    }
                    legacyDir.deleteRecursively()
                }
            }
        }
        if (manifestFile.exists()) {
            val existing = readManifest(manifestFile)
            if (existing == null || existing.language != language) {
                return@withContext createDefaultWorkspace(context, botId, language)
            }
            val entryFile = File(dir, existing.entry)
            if (!entryFile.exists()) {
                entryFile.parentFile?.mkdirs()
                entryFile.writeText(defaultTemplate(language))
            }
            return@withContext existing
        }
        createDefaultWorkspace(context, botId, language)
    }

    suspend fun loadFlow(context: Context, botId: String): String = withContext(Dispatchers.IO) {
        val f = flowFile(context, botId)
        if (!f.exists()) "{ \"nodes\": [], \"connections\": [] }"
        else f.readText()
    }

    suspend fun saveFlow(context: Context, botId: String, flowJson: String) = withContext(Dispatchers.IO) {
        flowFile(context, botId).writeText(flowJson)
    }

    suspend fun delete(context: Context, botId: String) = withContext(Dispatchers.IO) {
        listOf("js", "py").forEach { ext ->
            val f = File(legacyDir(context), "$botId.$ext")
            if (f.exists()) f.delete()
        }
        val ff = flowFile(context, botId)
        if (ff.exists()) ff.delete()

        val workspace = workspaceDir(context, botId)
        if (workspace.exists()) workspace.deleteRecursively()

        val legacy = File(AstralStorage.botScriptsDir(context), botId)
        if (legacy.exists()) legacy.deleteRecursively()
    }

    fun templateFor(language: String, plugins: List<String>): String {
        val base = defaultTemplate(language)
        if (plugins.isEmpty()) return base
        return if (language.lowercase() == "py" || language.lowercase() == "python") {
            base + "\n\n" + pythonPlugins(plugins)
        } else {
            base + "\n\n" + jsPlugins(plugins)
        }
    }

    private fun defaultTemplate(language: String): String = when (language.lowercase()) {
        "py", "python" -> """
            # Astral Python bot script
            from botManager import onMessage, onCommand, setPrefix

            def handle(event):
                if event.msg.startswith("ㅎㅇ"):
                    event.reply("안녕!")

            def ping(event):
                event.reply("pong")

            setPrefix(["!", "/"])
            onMessage(handle)
            onCommand("ping", ping)
        """.trimIndent()
        else -> """
            // Astral bot script (clean style)
            //
            // Entry point
            // - const { onMessage, onCommand, setPrefix } = require("botManager")
            // - onMessage((event) => { ... })
            // - event.msg, event.room, event.sender, event.isGroupChat
            // - event.reply("text")
            const { onMessage, onCommand, setPrefix } = require("botManager");

            onMessage((event) => {
              if (event.msg.startsWith("ㅎㅇ")) {
                event.reply("안녕!");
              }
            });

            setPrefix(["!", "/"]);
            onCommand("ping", (event) => {
              event.reply("pong");
            });
        """.trimIndent()
    }

    private fun jsPlugins(plugins: List<String>): String {
        val blocks = plugins.mapNotNull { id ->
            when (id) {
                "command_router" -> """
                    // plugin: command router
                    bot.command(["help", "info"], (ctx) => {
                      ctx.reply("Available commands: help, info");
                    });
                """.trimIndent()
                "keyword_filter" -> """
                    // plugin: keyword filter
                    bot.match(/hello|hi/i, (ctx) => {
                      ctx.reply("hello");
                    });
                """.trimIndent()
                "auto_reply" -> """
                    // plugin: auto reply
                    bot.hear("ping", (ctx) => {
                      ctx.reply("pong");
                    });
                """.trimIndent()
                "scheduler" -> """
                    // plugin: scheduler
                    setInterval(() => {
                      // periodic task
                    }, 60 * 1000);
                """.trimIndent()
                "rate_limit" -> """
                    // plugin: cooldown
                    const lastRun = {};
                    bot.on("message", (ctx) => {
                      const key = ctx.sender || "global";
                      const now = Date.now();
                      if (lastRun[key] && now - lastRun[key] < 3000) return;
                      lastRun[key] = now;
                    });
                """.trimIndent()
                else -> null
            }
        }
        return "// plugins\n" + blocks.joinToString("\n\n")
    }

    private fun pythonPlugins(plugins: List<String>): String {
        val blocks = plugins.mapNotNull { id ->
            when (id) {
                "command_router" -> """
                    # plugin: command router
                    bot.command("help", lambda ctx: ctx.reply("Available commands: help"))
                """.trimIndent()
                "keyword_filter" -> """
                    # plugin: keyword filter
                    bot.on("message", lambda ctx: ctx.reply("hello") if ctx.content and ("hello" in ctx.content.lower()) else None)
                """.trimIndent()
                "auto_reply" -> """
                    # plugin: auto reply
                    bot.on("message", lambda ctx: ctx.reply("pong") if ctx.content == "ping" else None)
                """.trimIndent()
                "scheduler" -> """
                    # plugin: scheduler (requires threading or asyncio)
                """.trimIndent()
                "rate_limit" -> """
                    # plugin: cooldown
                """.trimIndent()
                else -> null
            }
        }
        return "# plugins\n" + blocks.joinToString("\n\n")
    }

    private fun extensionFor(language: String): String = when (language.lowercase()) {
        "py", "python" -> "py"
        else -> "js"
    }

    private suspend fun createDefaultWorkspace(context: Context, botId: String, language: String): WorkspaceManifest {
        val folder = resolveBotFolderName(context, botId)
        val entryName = "$folder/main.${extensionFor(language)}"
        val workspace = workspaceDir(context, botId)
        val entryFile = File(workspace, entryName)

        val legacy = legacyFile(context, botId, language)
        if (legacy.exists()) {
            legacy.copyTo(entryFile, overwrite = true)
            legacy.delete()
        } else if (!entryFile.exists()) {
            entryFile.parentFile?.mkdirs()
            entryFile.writeText(defaultTemplate(language))
        }

        val manifest = WorkspaceManifest(entry = entryName, language = language)
        writeManifest(context, botId, manifest)
        return manifest
    }

    private fun readManifest(file: File): WorkspaceManifest? {
        return runCatching {
            val json = JSONObject(file.readText())
            WorkspaceManifest(
                entry = json.optString("entry", ""),
                language = json.optString("language", "js")
            ).takeIf { it.entry.isNotBlank() }
        }.getOrNull()
    }

    private suspend fun writeManifest(context: Context, botId: String, manifest: WorkspaceManifest) {
        val json = JSONObject()
            .put("entry", manifest.entry)
            .put("language", manifest.language)
        manifestFile(context, botId).writeText(json.toString())
    }

    private suspend fun resolveWorkspacePath(context: Context, botId: String, relativePath: String): File {
        val cleaned = sanitizePath(relativePath)
        val root = workspaceDir(context, botId)
        val target = File(root, cleaned)
        val rootPath = root.canonicalFile.path
        val targetPath = target.canonicalFile.path
        if (!targetPath.startsWith(rootPath)) {
            throw IllegalArgumentException("invalid path")
        }
        return target
    }

    private suspend fun pruneEmptyAncestors(context: Context, botId: String, start: File?) {
        val root = workspaceDir(context, botId)
        val rootCanonical = root.canonicalPath
        var node = start
        while (node != null) {
            if (!node.exists()) {
                node = node.parentFile
                continue
            }
            if (!node.isDirectory) break
            val canonical = node.canonicalPath
            if (canonical == rootCanonical) break
            if (node.list()?.isNotEmpty() == true) break
            node.delete()
            node = node.parentFile
        }
    }

    private fun sanitizePath(path: String): String {
        val normalized = path.replace("\\", "/").trim()
        if (normalized.startsWith("/")) throw IllegalArgumentException("absolute path not allowed")
        if (normalized.contains("..")) throw IllegalArgumentException("path traversal not allowed")
        return normalized
    }
}


