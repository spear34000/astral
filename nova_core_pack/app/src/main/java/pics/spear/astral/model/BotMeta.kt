package pics.spear.astral.model

import kotlinx.serialization.Serializable

@Serializable
data class BotMeta(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val language: String = "js",
    val autoStart: Boolean = true,
    val notifyOnError: Boolean = true,
    val plugins: List<String> = emptyList(),
    val priority: Int = 1,
)



