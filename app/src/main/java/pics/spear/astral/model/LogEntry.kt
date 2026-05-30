package pics.spear.astral.model

import kotlinx.serialization.Serializable

@Serializable
data class LogEntry(
    val id: Long = 0,
    val botName: String,
    val message: String,
    val level: String = "info",
    val timestamp: Long = System.currentTimeMillis(),
)
