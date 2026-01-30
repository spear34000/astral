package pics.spear.astral.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pics.spear.astral.flow.*
import pics.spear.astral.store.BotScriptStore
import pics.spear.astral.store.BotStore
import pics.spear.astral.ui.components.AstralScreen
import pics.spear.astral.ui.theme.AstralTheme
import java.util.UUID
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class FlowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val botId = intent.getStringExtra("BOT_ID") ?: run { finish(); return }
        
        setContent {
            AstralTheme {
                FlowEditorScreen(botId = botId, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowEditorScreen(botId: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // Constants
    val nodeWidth = 150.dp
    val nodeHeight = 80.dp
    val handleSize = 24.dp
    val handleHitSize = 48.dp // Easier hit area
    
    val nodeWidthPx = with(density) { nodeWidth.toPx() }
    val nodeHeightPx = with(density) { nodeHeight.toPx() }
    val handleHitPx = with(density) { handleHitSize.toPx() }

    // State
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var nodes by remember { mutableStateOf<List<FlowNode>>(emptyList()) }
    var connections by remember { mutableStateOf<List<FlowConnection>>(emptyList()) }
    var botLang by remember { mutableStateOf("js") }
    
    // Interaction
    var draggingNodeId by remember { mutableStateOf<String?>(null) }
    
    // Connection Drag State
    var draggingSourceId by remember { mutableStateOf<String?>(null) }
    var draggingSourceAnchor by remember { mutableStateOf("right") }
    var dragCurrentPosition by remember { mutableStateOf(Offset.Zero) }
    var hoverTargetId by remember { mutableStateOf<String?>(null) } // Highlight target node
    
    var isPanning by remember { mutableStateOf(false) }
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var selectedNode by remember { mutableStateOf<FlowNode?>(null) }

    // Load Flow
    LaunchedEffect(Unit) {
        val json = BotScriptStore.loadFlow(context, botId)
        try {
            val flow = Json.decodeFromString<Flow>(json)
            nodes = flow.nodes
            connections = flow.connections
        } catch (e: Exception) { }
    }

    LaunchedEffect(botId) {
        val bots = BotStore.list(context)
        botLang = bots.find { it.id == botId }?.language ?: "js"
    }

    // Save Logic
    fun save() {
        scope.launch {
            val flow = Flow(id = botId, nodes = nodes.toMutableList(), connections = connections.toMutableList())
            val json = Json.encodeToString(flow)
            BotScriptStore.saveFlow(context, botId, json)
            
            val script = FlowCompiler.compile(flow, botLang)
            BotScriptStore.save(context, botId, script, botLang)
            Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper: Calculate handle position
    fun getHandlePosition(node: FlowNode, anchor: String): Offset {
        val x = node.x + offsetX
        val y = node.y + offsetY
        return when (anchor) {
            "top" -> Offset(x + nodeWidthPx / 2, y)
            "bottom" -> Offset(x + nodeWidthPx / 2, y + nodeHeightPx)
            "left" -> Offset(x, y + nodeHeightPx / 2)
            "right" -> Offset(x + nodeWidthPx, y + nodeHeightPx / 2)
            else -> Offset(x + nodeWidthPx, y + nodeHeightPx / 2)
        }
    }

    AstralScreen {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("플로우 에디터", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "뒤로") } },
                    actions = {
                        IconButton(onClick = { save() }) { Icon(Icons.Default.Save, "저장") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddNodeDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, "노드 추가")
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .clip(RoundedCornerShape(0.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                val hitNode = nodes.findLast { node ->
                                    val x = node.x + offsetX
                                    val y = node.y + offsetY
                                    offset.x >= x && offset.x <= x + nodeWidthPx &&
                                    offset.y >= y && offset.y <= y + nodeHeightPx
                                }
                                selectedNode = hitNode
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val safeInset = 20.dp.toPx()
                                
                                val hitSafeNode = nodes.findLast { node ->
                                    val x = node.x + offsetX
                                    val y = node.y + offsetY
                                    offset.x >= x + safeInset && offset.x <= x + nodeWidthPx - safeInset &&
                                    offset.y >= y + safeInset && offset.y <= y + nodeHeightPx - safeInset
                                }
                                
                                if (hitSafeNode != null) {
                                    draggingNodeId = hitSafeNode.id
                                    return@detectDragGestures
                                }

                                for (node in nodes.asReversed()) {
                                    val anchors = listOf("top", "bottom", "left", "right")
                                    for (anchor in anchors) {
                                        val pos = getHandlePosition(node, anchor)
                                        val dist = (pos - offset).getDistance()
                                        if (dist <= handleHitPx) {
                                            draggingSourceId = node.id
                                            draggingSourceAnchor = anchor
                                            dragCurrentPosition = offset
                                            return@detectDragGestures
                                        }
                                    }
                                }

                                val hitNode = nodes.findLast { node ->
                                    val x = node.x + offsetX
                                    val y = node.y + offsetY
                                    offset.x >= x && offset.x <= x + nodeWidthPx &&
                                    offset.y >= y && offset.y <= y + nodeHeightPx
                                }
                                
                                if (hitNode != null) {
                                    draggingNodeId = hitNode.id
                                } else {
                                    isPanning = true
                                }
                            },
                            onDragEnd = {
                                if (draggingSourceId != null) {
                                    val dropTolerance = 50.dp.toPx()
                                    val targetNode = nodes.findLast { node ->
                                        if (node.id == draggingSourceId) return@findLast false
                                        val x = node.x + offsetX
                                        val y = node.y + offsetY
                                        
                                        dragCurrentPosition.x >= x - dropTolerance && 
                                        dragCurrentPosition.x <= x + nodeWidthPx + dropTolerance &&
                                        dragCurrentPosition.y >= y - dropTolerance && 
                                        dragCurrentPosition.y <= y + nodeHeightPx + dropTolerance
                                    }

                                    if (targetNode != null) {
                                        val anchors = listOf("top", "bottom", "left", "right")
                                        var bestAnchor = "left"
                                        var minDest = Float.MAX_VALUE
                                        
                                        anchors.forEach { anchor ->
                                            val pos = getHandlePosition(targetNode, anchor)
                                            val dist = (pos - dragCurrentPosition).getDistance()
                                            if (dist < minDest) {
                                                minDest = dist
                                                bestAnchor = anchor
                                            }
                                        }

                                        val newConn = FlowConnection(
                                            id = UUID.randomUUID().toString(),
                                            sourceId = draggingSourceId!!,
                                            targetId = targetNode.id,
                                            sourceAnchor = draggingSourceAnchor,
                                            targetAnchor = bestAnchor
                                        )
                                        connections = connections + newConn
                                        Toast.makeText(context, "연결됨", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                
                                draggingNodeId = null
                                draggingSourceId = null
                                hoverTargetId = null
                                isPanning = false
                            },
                            onDragCancel = {
                                draggingNodeId = null
                                draggingSourceId = null
                                hoverTargetId = null
                                isPanning = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (draggingSourceId != null) {
                                    dragCurrentPosition += dragAmount
                                } else if (draggingNodeId != null) {
                                    val idx = nodes.indexOfFirst { it.id == draggingNodeId }
                                    if (idx != -1) {
                                        val node = nodes[idx]
                                        val updated = node.copy(x = node.x + dragAmount.x, y = node.y + dragAmount.y)
                                        val list = nodes.toMutableList()
                                        list[idx] = updated
                                        nodes = list
                                    }
                                } else if (isPanning) {
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                }
                            }
                        )
                    }
            ) {
                val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                val connectionColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 40.dp.toPx()
                    val dotSize = 2.dp.toPx()
                    val startX = (offsetX % step) - step
                    val startY = (offsetY % step) - step
                    
                    for (x in startX.toInt()..size.width.toInt() step step.toInt()) {
                        for (y in startY.toInt()..size.height.toInt() step step.toInt()) {
                            drawCircle(gridColor, radius = dotSize / 2, center = Offset(x.toFloat(), y.toFloat()))
                        }
                    }
                }

                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    connections.forEach { conn ->
                        val source = nodes.find { it.id == conn.sourceId }
                        val target = nodes.find { it.id == conn.targetId }
                        if (source != null && target != null) {
                            val startPos = when(conn.sourceAnchor) {
                                "top" -> Offset(source.x + nodeWidthPx/2, source.y)
                                "bottom" -> Offset(source.x + nodeWidthPx/2, source.y + nodeHeightPx)
                                "left" -> Offset(source.x, source.y + nodeHeightPx/2)
                                else -> Offset(source.x + nodeWidthPx, source.y + nodeHeightPx/2)
                            } + Offset(offsetX, offsetY)
                            
                            val endPos = when(conn.targetAnchor) {
                                "top" -> Offset(target.x + nodeWidthPx/2, target.y)
                                "bottom" -> Offset(target.x + nodeWidthPx/2, target.y + nodeHeightPx)
                                "left" -> Offset(target.x, target.y + nodeHeightPx/2)
                                else -> Offset(target.x + nodeWidthPx, target.y + nodeHeightPx/2)
                            } + Offset(offsetX, offsetY)

                            drawBezierConnection(startPos, endPos, connectionColor, conn.sourceAnchor, conn.targetAnchor)
                        }
                    }
                    
                    if (draggingSourceId != null) {
                        val source = nodes.find { it.id == draggingSourceId }
                        if (source != null) {
                            val startPos = when(draggingSourceAnchor) {
                                "top" -> Offset(source.x + nodeWidthPx/2, source.y)
                                "bottom" -> Offset(source.x + nodeWidthPx/2, source.y + nodeHeightPx)
                                "left" -> Offset(source.x, source.y + nodeHeightPx/2)
                                else -> Offset(source.x + nodeWidthPx, source.y + nodeHeightPx/2)
                            } + Offset(offsetX, offsetY)
                            
                            drawBezierConnection(startPos, dragCurrentPosition, primaryColor, draggingSourceAnchor, null)
                        }
                    }
                }

                nodes.forEach { node ->
                    N8nNodeCard(
                        node = node,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        width = nodeWidth,
                        height = nodeHeight,
                        handleSize = handleSize,
                        isSelected = selectedNode?.id == node.id
                    )
                }
            }
        }
    }
    
    if (showAddNodeDialog) {
        AddNodeDialog(onDismiss = { showAddNodeDialog = false }, onAdd = { type ->
            val newNode = FlowNode(
                id = UUID.randomUUID().toString(),
                type = type,
                x = 100f - offsetX + 100, // Slightly offset
                y = 100f - offsetY + 100,
                data = mutableMapOf()
            )
            nodes = nodes + newNode
            showAddNodeDialog = false
        })
    }

    if (selectedNode != null) {
        NodeConfigDialog(
            node = selectedNode!!,
            onDismiss = { selectedNode = null },
            onDelete = {
                nodes = nodes.filter { it.id != selectedNode!!.id }
                connections = connections.filter { it.sourceId != selectedNode!!.id && it.targetId != selectedNode!!.id }
                selectedNode = null
            },
            onSave = { updatedNode ->
                val idx = nodes.indexOfFirst { it.id == updatedNode.id }
                if (idx != -1) {
                    val list = nodes.toMutableList()
                    list[idx] = updatedNode
                    nodes = list
                }
                selectedNode = null
            }
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBezierConnection(
    start: Offset, 
    end: Offset, 
    color: Color,
    startAnchor: String,
    endAnchor: String?
) {
    val path = Path()
    path.moveTo(start.x, start.y)

    // Calculate control points based on anchors
    val dist = (end - start).getDistance()
    val controlDist = dist * 0.5f

    val startControl = when(startAnchor) {
        "top" -> Offset(start.x, start.y - controlDist)
        "bottom" -> Offset(start.x, start.y + controlDist)
        "left" -> Offset(start.x - controlDist, start.y)
        else -> Offset(start.x + controlDist, start.y)
    }

    // Determine end anchor based on relative position if null (during drag)
    val actualEndAnchor = endAnchor ?: if (abs(start.x - end.x) > abs(start.y - end.y)) {
        if (end.x > start.x) "left" else "right"
    } else {
        if (end.y > start.y) "top" else "bottom"
    }

    val endControl = when(actualEndAnchor) {
        "top" -> Offset(end.x, end.y - controlDist)
        "bottom" -> Offset(end.x, end.y + controlDist)
        "left" -> Offset(end.x - controlDist, end.y)
        else -> Offset(end.x + controlDist, end.y)
    }

    path.cubicTo(startControl.x, startControl.y, endControl.x, endControl.y, end.x, end.y)
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 6f, cap = StrokeCap.Round)
    )
}

@Composable
fun N8nNodeCard(
    node: FlowNode,
    offsetX: Float,
    offsetY: Float,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    handleSize: androidx.compose.ui.unit.Dp,
    isSelected: Boolean
) {
    val (headerColor, icon) = when (node.type) {
        NodeType.TRIGGER_MESSAGE -> MaterialTheme.colorScheme.secondary to Icons.Default.Message
        NodeType.TRIGGER_COMMAND -> MaterialTheme.colorScheme.secondary to Icons.Default.Bolt
        NodeType.ACTION_REPLY -> MaterialTheme.colorScheme.primary to Icons.Default.Send
        NodeType.ACTION_LOG -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Code
        NodeType.LOGIC_IF -> MaterialTheme.colorScheme.tertiary to Icons.Default.CallSplit
        NodeType.ACTION_DELAY -> MaterialTheme.colorScheme.secondary to Icons.Default.Timer
        else -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Code
    }

    Box(
        modifier = Modifier
            .offset { IntOffset((node.x + offsetX).roundToInt(), (node.y + offsetY).roundToInt()) }
            .width(width)
            .height(height)
    ) {
        // Main Card
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .shadow(6.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(headerColor.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        node.type.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                
                // Body
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        node.data["value"] ?: node.data["key"] ?: "설정 필요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
            }
        }
        
        // Handles (Visible connectors)
        val handleOffset = 6.dp // Half size
        
        // Right
        Box(modifier = Modifier.align(Alignment.CenterEnd).offset(x = handleOffset).size(handleSize/2).background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape))
        // Left
        Box(modifier = Modifier.align(Alignment.CenterStart).offset(x = -handleOffset).size(handleSize/2).background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape))
        // Top
        Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = -handleOffset).size(handleSize/2).background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape))
        // Bottom
        Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = handleOffset).size(handleSize/2).background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape))
    }
}

@Composable
fun AddNodeDialog(onDismiss: () -> Unit, onAdd: (NodeType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("노드 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NodeType.entries.forEach { type ->
                    val color = when(type) {
                        NodeType.TRIGGER_MESSAGE, NodeType.TRIGGER_COMMAND -> Color(0xFF22C55E)
                        NodeType.ACTION_REPLY -> Color(0xFF3B82F6)
                        NodeType.LOGIC_IF -> Color(0xFFF97316)
                        else -> Color.Gray
                    }
                    Button(
                        onClick = { onAdd(type) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(type.label, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun NodeConfigDialog(
    node: FlowNode,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (FlowNode) -> Unit
) {
    var dataValue by remember { mutableStateOf(node.data["value"] ?: "") }
    var dataKey by remember { mutableStateOf(node.data["key"] ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${node.type.label} 설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (node.type) {
                    NodeType.TRIGGER_MESSAGE -> {
                        OutlinedTextField(value = dataValue, onValueChange = { dataValue = it }, label = { Text("포함할 단어") })
                    }
                    NodeType.TRIGGER_COMMAND -> {
                        OutlinedTextField(value = dataValue, onValueChange = { dataValue = it }, label = { Text("명령어 (예: !핑)") })
                    }
                    NodeType.ACTION_REPLY -> {
                        OutlinedTextField(value = dataValue, onValueChange = { dataValue = it }, label = { Text("답장 메시지") })
                    }
                    NodeType.LOGIC_IF -> {
                        OutlinedTextField(value = dataKey, onValueChange = { dataKey = it }, label = { Text("변수명 (예: sender)") })
                        OutlinedTextField(value = dataValue, onValueChange = { dataValue = it }, label = { Text("비교값") })
                    }
                    NodeType.ACTION_LOG -> {
                         OutlinedTextField(value = dataValue, onValueChange = { dataValue = it }, label = { Text("로그 메시지") })
                    }
                    else -> Text("추가 설정이 필요하지 않습니다.")
                }
            }
        },
        confirmButton = { Button(onClick = { node.data["value"] = dataValue; if (dataKey.isNotEmpty()) node.data["key"] = dataKey; onSave(node) }) { Text("저장") } },
        dismissButton = {
            Row {
                 TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("삭제") }
                 TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

object FlowCompiler {
    fun compile(flow: Flow, language: String): String {
        return if (language.lowercase() in listOf("py", "python")) {
            compilePython(flow)
        } else {
            compileJavaScript(flow)
        }
    }

    private fun compileJavaScript(flow: Flow): String {
        val sb = StringBuilder()
        sb.append("// Generated by Astral Flow\n")
        sb.append("const { onMessage, onCommand, setPrefix } = require('botManager');\n\n")

        val triggers = flow.nodes.filter { it.type.isTrigger }
        if (triggers.isEmpty()) {
            sb.append("// 트리거 노드를 추가해주세요.\n")
            return sb.toString()
        }

        val visited = mutableSetOf<String>()
        triggers.forEach { trigger ->
            visited.add(trigger.id)
            when (trigger.type) {
                NodeType.TRIGGER_MESSAGE -> {
                    val keyword = escapeJs(trigger.data["value"] ?: "")
                    sb.append("onMessage((ctx) => {\n")
                    sb.append("  try {\n")
                    if (keyword.isNotEmpty()) {
                        sb.append("    if (ctx.msg && ctx.msg.includes('$keyword')) {\n")
                        compileChildrenJs(sb, flow, trigger.id, "      ", visited)
                        sb.append("    }\n")
                    } else {
                        sb.append("    if (!ctx.msg) return;\n")
                        compileChildrenJs(sb, flow, trigger.id, "    ", visited)
                    }
                    sb.append("  } catch (e) {\n")
                    sb.append("    console.log('Error: ' + e);\n")
                    sb.append("  }\n")
                    sb.append("});\n\n")
                }
                NodeType.TRIGGER_COMMAND -> {
                    val cmd = escapeJs(trigger.data["value"] ?: "cmd")
                    sb.append("onCommand('$cmd', (ctx) => {\n")
                    sb.append("  try {\n")
                    compileChildrenJs(sb, flow, trigger.id, "    ", visited)
                    sb.append("  } catch (e) {\n")
                    sb.append("    console.log('Error: ' + e);\n")
                    sb.append("  }\n")
                    sb.append("});\n\n")
                }
                else -> {}
            }
        }

        sb.append("setPrefix(['!', '/']);\n")
        return sb.toString()
    }

    private fun compileChildrenJs(sb: StringBuilder, flow: Flow, parentId: String, indent: String, visited: MutableSet<String>) {
        val outgoing = flow.connections.filter { it.sourceId == parentId }
        outgoing.forEach { conn ->
            val node = flow.nodes.find { it.id == conn.targetId }
            if (node != null && !visited.contains(node.id)) {
                visited.add(node.id)
                compileNodeJs(sb, flow, node, indent, visited)
                visited.remove(node.id)
            }
        }
    }

    private fun compileNodeJs(sb: StringBuilder, flow: Flow, node: FlowNode, indent: String, visited: MutableSet<String>) {
        when (node.type) {
            NodeType.ACTION_REPLY -> {
                val msg = escapeJs(node.data["value"] ?: "")
                sb.append("${indent}ctx.reply('$msg');\n")
                compileChildrenJs(sb, flow, node.id, indent, visited)
            }
            NodeType.ACTION_LOG -> {
                val msg = escapeJs(node.data["value"] ?: "")
                sb.append("${indent}console.log('$msg');\n")
                compileChildrenJs(sb, flow, node.id, indent, visited)
            }
            NodeType.LOGIC_IF -> {
                val key = escapeJs(node.data["key"] ?: "content")
                val value = escapeJs(node.data["value"] ?: "")
                val check = if (key == "content" || key == "msg") "ctx.msg" else "ctx['$key']"
                sb.append("${indent}if ($check && $check == '$value') {\n")
                compileChildrenJs(sb, flow, node.id, indent + "  ", visited)
                sb.append("${indent}}\n")
            }
            NodeType.ACTION_DELAY -> {
                sb.append("${indent}// Delay placeholder\n")
                compileChildrenJs(sb, flow, node.id, indent, visited)
            }
            else -> {
                compileChildrenJs(sb, flow, node.id, indent, visited)
            }
        }
    }

    private fun compilePython(flow: Flow): String {
        val sb = StringBuilder()
        sb.append("# Generated by Astral Flow\n")
        sb.append("from botManager import onMessage, onCommand, setPrefix\n\n")

        val triggers = flow.nodes.filter { it.type.isTrigger }
        if (triggers.isEmpty()) {
            sb.append("# 트리거 노드를 추가해주세요.\n")
            return sb.toString()
        }

        val visited = mutableSetOf<String>()
        triggers.forEach { trigger ->
            visited.add(trigger.id)
            when (trigger.type) {
                NodeType.TRIGGER_MESSAGE -> {
                    val keyword = escapePy(trigger.data["value"] ?: "")
                    sb.append("def _on_message(ctx):\n")
                    sb.append("    try:\n")
                    sb.append("        if not getattr(ctx, 'msg', None):\n")
                    sb.append("            return\n")
                    if (keyword.isNotEmpty()) {
                        sb.append("        if '$keyword' in ctx.msg:\n")
                        compileChildrenPy(sb, flow, trigger.id, "            ", visited)
                    } else {
                        compileChildrenPy(sb, flow, trigger.id, "        ", visited)
                    }
                    sb.append("    except Exception as e:\n")
                    sb.append("        print(e)\n\n")
                    sb.append("onMessage(_on_message)\n\n")
                }
                NodeType.TRIGGER_COMMAND -> {
                    val cmd = escapePy(trigger.data["value"] ?: "cmd")
                    sb.append("def _on_command(ctx):\n")
                    sb.append("    try:\n")
                    compileChildrenPy(sb, flow, trigger.id, "        ", visited)
                    sb.append("    except Exception as e:\n")
                    sb.append("        print(e)\n\n")
                    sb.append("onCommand('$cmd', _on_command)\n\n")
                }
                else -> {}
            }
        }

        sb.append("setPrefix(['!', '/'])\n")
        return sb.toString()
    }

    private fun compileChildrenPy(sb: StringBuilder, flow: Flow, parentId: String, indent: String, visited: MutableSet<String>) {
        val outgoing = flow.connections.filter { it.sourceId == parentId }
        outgoing.forEach { conn ->
            val node = flow.nodes.find { it.id == conn.targetId }
            if (node != null && !visited.contains(node.id)) {
                visited.add(node.id)
                compileNodePy(sb, flow, node, indent, visited)
                visited.remove(node.id)
            }
        }
    }

    private fun compileNodePy(sb: StringBuilder, flow: Flow, node: FlowNode, indent: String, visited: MutableSet<String>) {
        when (node.type) {
            NodeType.ACTION_REPLY -> {
                val msg = escapePy(node.data["value"] ?: "")
                sb.append("${indent}ctx.reply('$msg')\n")
                compileChildrenPy(sb, flow, node.id, indent, visited)
            }
            NodeType.ACTION_LOG -> {
                val msg = escapePy(node.data["value"] ?: "")
                sb.append("${indent}print('$msg')\n")
                compileChildrenPy(sb, flow, node.id, indent, visited)
            }
            NodeType.LOGIC_IF -> {
                val key = escapePy(node.data["key"] ?: "content")
                val value = escapePy(node.data["value"] ?: "")
                val access = if (key == "content" || key == "msg") "getattr(ctx, 'msg', None)" else "getattr(ctx, '$key', None)"
                sb.append("${indent}if $access == '$value':\n")
                compileChildrenPy(sb, flow, node.id, indent + "    ", visited)
            }
            NodeType.ACTION_DELAY -> {
                sb.append("${indent}# Delay placeholder\n")
                compileChildrenPy(sb, flow, node.id, indent, visited)
            }
            else -> {
                compileChildrenPy(sb, flow, node.id, indent, visited)
            }
        }
    }

    private fun escapeJs(value: String): String = value
        .replace("\\", "\\\\")
        .replace("'", "\\'")

    private fun escapePy(value: String): String = value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
}
