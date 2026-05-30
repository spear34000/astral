package pics.spear.astral.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pics.spear.astral.model.ChatMessage
import pics.spear.astral.util.Storage

class InboxStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file = Storage.inboxFile(context)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var counter = 0L

    init { load() }

    private fun load() {
        _messages.value = if (file.exists()) {
            try {
                json.decodeFromString<List<ChatMessage>>(file.readText())
            } catch (_: Exception) { emptyList() }
        } else emptyList()
        counter = _messages.value.maxOfOrNull { it.id } ?: 0
    }

    suspend fun push(msg: ChatMessage) = withContext(Dispatchers.IO) {
        counter++
        val entry = msg.copy(id = counter)
        val list = _messages.value + entry
        _messages.value = list.takeLast(200)
        file.writeText(json.encodeToString(_messages.value))
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        _messages.value = emptyList()
        file.writeText("[]")
    }
}
