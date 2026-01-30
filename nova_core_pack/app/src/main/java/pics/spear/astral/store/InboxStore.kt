package pics.spear.astral.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pics.spear.astral.model.InboxMessage
import java.io.File
import pics.spear.astral.util.AstralStorage
import android.util.Log

object InboxStore {
    private const val MAX = 200
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun file(context: Context): File =
        File(AstralStorage.settingsDir(context), "inbox.json")

    suspend fun list(context: Context): List<InboxMessage> = withContext(Dispatchers.IO) {
        val f = file(context)
        f.parentFile?.mkdirs()
        if (!f.exists()) return@withContext emptyList()
        runCatching {
            json.decodeFromString(ListSerializer(InboxMessage.serializer()), f.readText())
        }.getOrDefault(emptyList())
    }

    suspend fun append(context: Context, msg: InboxMessage) = withContext(Dispatchers.IO) {
        val current = list(context)
        val next = (listOf(msg) + current).take(MAX)
        val f = file(context)
        f.parentFile?.mkdirs()
        runCatching {
            f.writeText(json.encodeToString(ListSerializer(InboxMessage.serializer()), next))
        }.onFailure { err ->
            Log.e("InboxStore", "Failed to write inbox.json", err)
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        val f = file(context)
        if (f.exists()) f.delete()
    }
}



