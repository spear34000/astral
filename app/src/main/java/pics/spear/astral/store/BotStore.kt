package pics.spear.astral.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import pics.spear.astral.model.Bot
import pics.spear.astral.util.Storage
import java.io.File

class BotStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File = Storage.botMetaFile(context)

    private val _bots = MutableStateFlow<List<Bot>>(emptyList())
    val bots: StateFlow<List<Bot>> = _bots.asStateFlow()

    init { load() }

    private fun load() {
        _bots.value = if (file.exists()) {
            try {
                json.decodeFromString<List<Bot>>(file.readText())
            } catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    private suspend fun save() = withContext(Dispatchers.IO) {
        file.writeText(json.encodeToString(_bots.value))
    }

    suspend fun add(bot: Bot) {
        _bots.value = _bots.value + bot
        save()
    }

    suspend fun update(id: String, transform: (Bot) -> Bot) {
        _bots.value = _bots.value.map { if (it.id == id) transform(it) else it }
        save()
    }

    suspend fun remove(id: String) {
        _bots.value = _bots.value.filter { it.id != id }
        save()
        withContext(Dispatchers.IO) {
            _bots.value.find { it.id == id }?.let {
                Storage.botFile(context, it.name).delete()
            }
        }
    }

    suspend fun toggle(id: String) {
        update(id) { it.copy(enabled = !it.enabled) }
    }

    suspend fun saveScript(botName: String, script: String) = withContext(Dispatchers.IO) {
        Storage.botFile(context, botName).writeText(script)
    }

    fun loadScript(botName: String): String {
        val f = Storage.botFile(context, botName)
        return if (f.exists()) f.readText() else ""
    }
}
