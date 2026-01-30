package pics.spear.astral.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pics.spear.astral.model.ErrorLog
import java.io.File
import pics.spear.astral.util.AstralStorage
import android.util.Log

object ErrorStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun file(context: Context): File =
        File(AstralStorage.settingsDir(context), "errors.json")

    suspend fun list(context: Context): List<ErrorLog> = withContext(Dispatchers.IO) {
        val f = file(context)
        f.parentFile?.mkdirs()
        if (!f.exists()) return@withContext emptyList()
        runCatching {
            json.decodeFromString(ListSerializer(ErrorLog.serializer()), f.readText())
        }.getOrDefault(emptyList())
    }

    suspend fun append(context: Context, log: ErrorLog) = withContext(Dispatchers.IO) {
        val current = list(context).toMutableList()
        current.add(0, log) // Add at top
        val next = current.take(500) // Keep last 500
        val f = file(context)
        f.parentFile?.mkdirs()
        runCatching {
            f.writeText(json.encodeToString(ListSerializer(ErrorLog.serializer()), next))
        }.onFailure { err ->
            Log.e("ErrorStore", "Failed to write errors.json", err)
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        val f = file(context)
        if (f.exists()) f.delete()
    }
}


