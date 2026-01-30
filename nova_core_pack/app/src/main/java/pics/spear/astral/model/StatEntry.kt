package pics.spear.astral.model

import kotlinx.serialization.Serializable

@Serializable
data class StatEntry(
    val timestamp: Long,
    val botId: String,
    val botName: String,
    val room: String,
    val isSuccess: Boolean,
    val error: String? = null
)


