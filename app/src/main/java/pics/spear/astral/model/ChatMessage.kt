package pics.spear.astral.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Long = 0,
    val room: String,
    val sender: String,
    val content: String,
    val isGroupChat: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isBot: Boolean = false,
)
