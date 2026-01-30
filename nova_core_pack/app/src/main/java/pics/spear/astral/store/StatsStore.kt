package pics.spear.astral.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pics.spear.astral.model.StatEntry
import java.io.File
import pics.spear.astral.util.AstralStorage
import android.util.Log

object StatsStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun file(context: Context): File =
        File(AstralStorage.settingsDir(context), "stats.json")

    suspend fun list(context: Context): List<StatEntry> = withContext(Dispatchers.IO) {
        val f = file(context)
        f.parentFile?.mkdirs()
        if (!f.exists()) return@withContext emptyList()
        runCatching {
            json.decodeFromString(ListSerializer(StatEntry.serializer()), f.readText())
        }.getOrDefault(emptyList())
    }

    suspend fun append(context: Context, entry: StatEntry) = withContext(Dispatchers.IO) {
        val current = list(context).toMutableList()
        current.add(0, entry)
        // Keep last 1000 entries for performance
        val next = current.take(1000)
        val f = file(context)
        f.parentFile?.mkdirs()
        runCatching {
            f.writeText(json.encodeToString(ListSerializer(StatEntry.serializer()), next))
        }.onFailure { err ->
            Log.e("StatsStore", "Failed to write stats.json", err)
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        val f = file(context)
        if (f.exists()) f.delete()
    }
}


