package pics.spear.astral.engine

import pics.spear.astral.model.*

/**
 * Compiles a visual Flow into executable JavaScript code.
 */
object FlowCompiler {

    fun compile(flow: Flow): String {
        val triggers = flow.nodes.filter { it.type.name.startsWith("TRIGGER") }
        val actions = flow.nodes.filter { !it.type.name.startsWith("TRIGGER") }
        val edges = buildEdgeMap(flow.connections)

        if (triggers.isEmpty()) return "// No trigger node defined"

        val sb = StringBuilder()

        for (trigger in triggers) {
            val code = compileNode(trigger, flow.nodes, edges, mutableSetOf(), 0)
            when (trigger.type) {
                NodeType.TRIGGER_MESSAGE -> {
                    sb.appendLine("bot.onMessage(function(room, sender, msg, isGroup) {")
                    sb.appendLine(code)
                    sb.appendLine("});")
                }
                NodeType.TRIGGER_COMMAND -> {
                    val cmd = trigger.props["command"] ?: "start"
                    sb.appendLine("bot.onCommand(${q(cmd)}, function(room, sender, msg, isGroup, args) {")
                    sb.appendLine(code)
                    sb.appendLine("});")
                }
                else -> {}
            }
        }

        return sb.toString()
    }

    private fun compileNode(
        node: FlowNode,
        allNodes: List<FlowNode>,
        edges: Map<String, List<String>>,
        visited: MutableSet<String>,
        depth: Int,
    ): String {
        if (node.id in visited || depth > 20) return ""
        visited.add(node.id)

        val sb = StringBuilder()
        val indent = "  ".repeat(depth + 1)

        when (node.type) {
            NodeType.ACTION_REPLY -> {
                val msg = node.props["message"] ?: "Hello!"
                sb.appendLine("${indent}bot.reply(${q(msg)});")
            }
            NodeType.ACTION_TOAST -> {
                val msg = node.props["message"] ?: "Hello!"
                sb.appendLine("${indent}bot.toast(${q(msg)});")
            }
            NodeType.ACTION_DELAY -> {
                val ms = node.props["ms"] ?: "1000"
                sb.appendLine("${indent}bot.log('delay $ms ms');")
            }
            NodeType.ACTION_LOG -> {
                val msg = node.props["message"] ?: "log"
                sb.appendLine("${indent}bot.log(${q(msg)});")
            }
            NodeType.LOGIC_CONDITION -> {
                val cond = node.props["condition"] ?: "msg.includes('hello')"
                sb.appendLine("${indent}if ($cond) {")

                val thenEdges = edges[node.id] ?: emptyList()
                val thenNode = thenEdges.firstOrNull()?.let { id ->
                    allNodes.find { it.id == id }
                }
                if (thenNode != null) {
                    sb.append(compileNode(thenNode, allNodes, edges, visited, depth + 1))
                }
                sb.appendLine("${indent}}")
            }
            else -> {}
        }

        // Follow outgoing connections
        val nextIds = edges[node.id] ?: emptyList()
        for (nextId in nextIds) {
            if (node.type == NodeType.LOGIC_CONDITION) break // handled inside
            val nextNode = allNodes.find { it.id == nextId } ?: continue
            sb.append(compileNode(nextNode, allNodes, edges, visited, depth))
        }

        return sb.toString()
    }

    private fun buildEdgeMap(connections: List<FlowConnection>): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        for (c in connections) {
            map.getOrPut(c.sourceNodeId) { mutableListOf() }.add(c.targetNodeId)
        }
        return map
    }

    private fun q(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
