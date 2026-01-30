package pics.spear.astral.model

import kotlinx.serialization.Serializable

@Serializable
data class InboxMessage(
    val ts: Long,
    val packageName: String,
    val roomName: String,
    val sender: String,
    val message: String,
)



