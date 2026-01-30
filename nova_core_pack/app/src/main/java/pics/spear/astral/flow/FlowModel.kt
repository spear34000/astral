package pics.spear.astral.flow

import kotlinx.serialization.Serializable

@Serializable
data class Flow(
    val id: String,
    val nodes: MutableList<FlowNode> = mutableListOf(),
    val connections: MutableList<FlowConnection> = mutableListOf()
)

@Serializable
data class FlowNode(
    val id: String,
    val type: NodeType,
    var x: Float = 0f,
    var y: Float = 0f,
    val data: MutableMap<String, String> = mutableMapOf()
)

@Serializable
data class FlowConnection(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val sourceAnchor: String = "right", // top, bottom, left, right
    val targetAnchor: String = "left"
)

enum class NodeType(val label: String, val isTrigger: Boolean = false) {
    TRIGGER_MESSAGE("메시지 감지", true),
    TRIGGER_COMMAND("명령어 감지", true),
    ACTION_REPLY("답장하기"),
    LOGIC_IF("조건 (만약)"),
    ACTION_DELAY("대기 (지연)"),
    ACTION_LOG("기록 (로그)")
}
