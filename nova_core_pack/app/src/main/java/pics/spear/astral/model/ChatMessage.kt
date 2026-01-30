package pics.spear.astral.model

data class ChatMessage(
    val sender: String,
    val content: String,
    val isSystem: Boolean = false,
    val isBot: Boolean = false
)


