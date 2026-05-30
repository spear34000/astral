package pics.spear.astral.model

import kotlinx.serialization.Serializable

@Serializable
enum class NodeType(val label: String, val icon: String, val color: Long) {
    TRIGGER_MESSAGE("On Message", "message", 0xFF6C8CFF),
    TRIGGER_COMMAND("On Command", "command", 0xFF6C8CFF),
    ACTION_REPLY("Reply", "reply", 0xFF34D399),
    ACTION_TOAST("Toast", "toast", 0xFFFBBF24),
    ACTION_DELAY("Delay", "delay", 0xFFF87171),
    ACTION_LOG("Log", "log", 0xFFA78BFA),
    LOGIC_CONDITION("Condition", "condition", 0xFFF472B6),
}

@Serializable
data class FlowPort(
    val id: String,
    val label: String,
    val isOutput: Boolean,
)

@Serializable
data class FlowNode(
    val id: String,
    val type: NodeType,
    val x: Float = 100f,
    val y: Float = 100f,
    val props: Map<String, String> = emptyMap(),
    val ports: List<FlowPort> = emptyList(),
)

@Serializable
data class FlowConnection(
    val id: String,
    val sourceNodeId: String,
    val sourcePortId: String,
    val targetNodeId: String,
    val targetPortId: String,
)

@Serializable
data class Flow(
    val id: String = "",
    val name: String = "New Flow",
    val nodes: List<FlowNode> = emptyList(),
    val connections: List<FlowConnection> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
