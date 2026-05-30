package pics.spear.astral.model

import kotlinx.serialization.Serializable

@Serializable
data class Bot(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val language: String = "javascript",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val script: String = "",
)
