package pics.spear.astral.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pics.spear.astral.model.BotMeta
import java.io.File
import pics.spear.astral.util.AstralStorage
import java.util.UUID
import android.util.Log

object BotStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun dir(context: Context): File =
        AstralStorage.settingsDir(context)

    private fun file(context: Context): File =
        File(dir(context), "bots.json")

    suspend fun list(context: Context): List<BotMeta> = withContext(Dispatchers.IO) {
        val f = file(context)
        f.parentFile?.mkdirs()
        if (!f.exists()) return@withContext emptyList()
        runCatching {
            json.decodeFromString(ListSerializer(BotMeta.serializer()), f.readText())
        }.getOrDefault(emptyList())
    }

    suspend fun upsert(context: Context, meta: BotMeta) = withContext(Dispatchers.IO) {
        val current = list(context)
        val next = current
            .filterNot { it.id == meta.id }
            .toMutableList()
            .apply { add(meta) }
            .sortedByDescending { it.updatedAt }
        val f = file(context)
        f.parentFile?.mkdirs()
        runCatching {
            f.writeText(json.encodeToString(ListSerializer(BotMeta.serializer()), next))
        }.onFailure { err ->
            Log.e("BotStore", "Failed to write bots.json", err)
        }
    }

    suspend fun create(
        context: Context,
        name: String,
        language: String = "js",
        enabled: Boolean = true,
        autoStart: Boolean = true,
        notifyOnError: Boolean = true,
        plugins: List<String> = emptyList()
    ): BotMeta = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val meta = BotMeta(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "NewBot" },
            enabled = enabled,
            createdAt = now,
            updatedAt = now,
            language = language,
            autoStart = autoStart,
            notifyOnError = notifyOnError,
            plugins = plugins,
            priority = 1,
        )
        upsert(context, meta)
        meta
    }

    suspend fun remove(context: Context, botId: String) = withContext(Dispatchers.IO) {
        val next = list(context).filterNot { it.id == botId }
        val f = file(context)
        f.parentFile?.mkdirs()
        runCatching {
            f.writeText(json.encodeToString(ListSerializer(BotMeta.serializer()), next))
        }.onFailure { err ->
            Log.e("BotStore", "Failed to write bots.json", err)
        }
        BotScriptStore.delete(context, botId)
    }
}



