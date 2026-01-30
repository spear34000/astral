package pics.spear.astral.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorLog(
    val ts: Long,
    val botName: String,
    val error: String,
    val level: String = "ERROR"
)


