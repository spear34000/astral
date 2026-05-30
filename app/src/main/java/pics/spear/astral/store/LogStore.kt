package pics.spear.astral.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pics.spear.astral.model.LogEntry
import pics.spear.astral.util.Storage

class LogStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file = Storage.logsFile(context)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var counter = 0L

    init { load() }

    private fun load() {
        _logs.value = if (file.exists()) {
            try {
                json.decodeFromString<List<LogEntry>>(file.readText())
            } catch (_: Exception) { emptyList() }
        } else emptyList()
        counter = _logs.value.maxOfOrNull { it.id } ?: 0
    }

    suspend fun append(entry: LogEntry) = withContext(Dispatchers.IO) {
        counter++
        val e = entry.copy(id = counter)
        val list = _logs.value + e
        _logs.value = list.takeLast(500)
        file.writeText(json.encodeToString(_logs.value))
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        _logs.value = emptyList()
        file.writeText("[]")
    }
}
