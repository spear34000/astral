package pics.spear.astral.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pics.spear.astral.model.*
import pics.spear.astral.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalTextApi::class)
@Composable
fun FlowScreen(
    onBack: () -> Unit,
) {
    var flow by remember {
        mutableStateOf(
            Flow(
                id = UUID.randomUUID().toString().take(8),
                nodes = listOf(
                    FlowNode(id = "n1", type = NodeType.TRIGGER_MESSAGE, x = 200f, y = 80f),
                    FlowNode(id = "n2", type = NodeType.ACTION_REPLY, x = 200f, y = 320f),
                ),
                connections = listOf(
                    FlowConnection(id = "c1", sourceNodeId = "n1", sourcePortId = "out", targetNodeId = "n2", targetPortId = "in"),
                ),
            )
        )
    }

    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var draggingNodeId by remember { mutableStateOf<String?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }

    val canvasOffset = remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    // Particles for flowing connection animation
    val connectionParticles = remember {
        mutableStateListOf<Pair<Float, Float>>().also { list ->
            repeat(20) { list.add(0f to 0f) }
        }
    }

    var showCompileSuccess by remember { mutableStateOf(false) }

    // Flow particles animation
    val particleTick by rememberInfiniteTransition(label = "fp").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "pt",
    )

    val compileAnim by animateFloatAsState(
        targetValue = if (showCompileSuccess) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ca",
    )

    AstralScreen {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                Surface(color = SpaceSurface, shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = flow.name,
                                    fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                                PulseText("flow", color = AstralCyan, fontSize = 10.sp)
                            }
                            Text(
                                text = "${flow.nodes.size} nodes  ·  ${flow.connections.size} connections",
                                fontSize = 12.sp, color = TextTertiary,
                            )
                        }
                        TextButton(onClick = {
                            val js = FlowCompiler.compile(flow)
                            println("=== Compiled Flow ===")
                            println(js)
                            showCompileSuccess = true
                            kotlinx.coroutines.MainScope().launch {
                                kotlinx.coroutines.delay(2000)
                                showCompileSuccess = false
                            }
                        }) {
                            Icon(Icons.Default.Code, null, tint = Blue60, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Compile", color = Blue60)
                        }
                        TextButton(onClick = { showAddMenu = true }) {
                            Icon(Icons.Default.Add, null, tint = Emerald60, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // Compile success overlay
                AnimatedVisibility(
                    visible = showCompileSuccess,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                ) {
                    Surface(
                        modifier = Modifier.padding(horizontal = 40.dp).clip(RoundedCornerShape(12.dp)),
                        color = AstralEmerald.copy(alpha = 0.2f),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("✦", color = AstralEmerald, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("compiled successfully — keep the vibes flowing", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Canvas
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpaceSurfaceVariant.copy(alpha = 0.3f))
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val tapped = flow.nodes.findLast { node ->
                                        val r = Rect(node.x - 60f, node.y - 20f, node.x + 60f, node.y + 40f)
                                        r.contains(offset - canvasOffset.value)
                                    }
                                    selectedNodeId = tapped?.id
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        draggingNodeId = flow.nodes.findLast { node ->
                                            val r = Rect(node.x - 60f, node.y - 20f, node.x + 60f, node.y + 40f)
                                            r.contains(offset - canvasOffset.value)
                                        }?.id
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (draggingNodeId != null) {
                                            flow = flow.copy(
                                                nodes = flow.nodes.map {
                                                    if (it.id == draggingNodeId) it.copy(
                                                        x = it.x + dragAmount.x,
                                                        y = it.y + dragAmount.y,
                                                    ) else it
                                                }
                                            )
                                        } else {
                                            canvasOffset.value += dragAmount
                                        }
                                    },
                                    onDragEnd = { draggingNodeId = null },
                                )
                            },
                    ) {
                        val offset = canvasOffset.value

                        // Draw grid
                        val gridSize = 30f
                        for (x in 0..(size.width / gridSize).toInt()) {
                            for (y in 0..(size.height / gridSize).toInt()) {
                                drawCircle(
                                    color = GlassBorder,
                                    radius = 1f,
                                    center = Offset(x * gridSize + offset.x % gridSize, y * gridSize + offset.y % gridSize),
                                )
                            }
                        }

                        // Draw connections with flowing particles
                        for (conn in flow.connections) {
                            val src = flow.nodes.find { it.id == conn.sourceNodeId } ?: continue
                            val tgt = flow.nodes.find { it.id == conn.targetNodeId } ?: continue
                            val start = Offset(src.x + offset.x, src.y + 30f + offset.y)
                            val end = Offset(tgt.x + offset.x, tgt.y - 20f + offset.y)
                            val cp1 = Offset(start.x, start.y + (end.y - start.y) * 0.5f)
                            val cp2 = Offset(end.x, start.y + (end.y - start.y) * 0.5f)
                            val path = Path().apply {
                                moveTo(start.x, start.y)
                                cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, end.x, end.y)
                            }

                            // Connection line with glow
                            val srcNode = flow.nodes.find { it.id == conn.sourceNodeId }
                            val lineColor = Color(srcNode?.type?.color ?: 0xFF6C8CFF)
                            drawPath(path, lineColor.copy(alpha = 0.3f), style = Stroke(4f))
                            drawPath(path, lineColor.copy(alpha = 0.6f), style = Stroke(2f))

                            // Flowing particles along the connection
                            val t = ((particleTick + conn.hashCode() % 360) % 360f) / 360f
                            val p = bezierPoint(start, cp1, cp2, end, t)
                            drawCircle(lineColor.copy(alpha = 0.8f), 3f, p)
                            val t2 = ((particleTick + 120f + conn.hashCode() % 360) % 360f) / 360f
                            val p2 = bezierPoint(start, cp1, cp2, end, t2)
                            drawCircle(lineColor.copy(alpha = 0.5f), 2f, p2)

                            // Arrow dot
                            drawCircle(lineColor.copy(alpha = 0.8f), 4f, end)
                        }

                        // Draw nodes
                        for (node in flow.nodes) {
                            val x = node.x + offset.x
                            val y = node.y + offset.y
                            val isSelected = node.id == selectedNodeId
                            val nodeWidth = 130f
                            val nodeHeight = 56f

                            // Shadow
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.3f),
                                topLeft = Offset(x + 2f, y + 2f),
                                size = Size(nodeWidth, nodeHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                            )

                            // Body
                            drawRoundRect(
                                color = Color(node.type.color).copy(alpha = 0.15f),
                                topLeft = Offset(x, y),
                                size = Size(nodeWidth, nodeHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                            )
                            drawRoundRect(
                                color = if (isSelected) Color(node.type.color) else GlassBorder,
                                topLeft = Offset(x, y),
                                size = Size(nodeWidth, nodeHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                                style = Stroke(if (isSelected) 2f else 1f),
                            )

                            // Type indicator dot
                            drawCircle(Color(node.type.color), 5f, Offset(x + 16f, y + nodeHeight / 2f))

                            // Text
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = 0xFFE8ECF4.toInt()
                                    textSize = 28f
                                    isAntiAlias = true
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }
                                val subPaint = android.graphics.Paint().apply {
                                    color = 0xFF9CA3B4.toInt()
                                    textSize = 20f
                                    isAntiAlias = true
                                }
                                drawText(paint, node.type.label, x + 28f, y + 30f)
                                drawText(subPaint, node.type.name, x + 28f, y + 48f)
                            }

                            // Port dots (input / output)
                            drawCircle(Color.White.copy(alpha = 0.5f), 6f, Offset(x + nodeWidth / 2f, y))
                            drawCircle(Color.White.copy(alpha = 0.5f), 6f, Offset(x + nodeWidth / 2f, y + nodeHeight))
                        }
                    }
                }
            }

            // Node property sheet
            if (selectedNodeId != null) {
                val selectedNode = flow.nodes.find { it.id == selectedNodeId }
                if (selectedNode != null) {
                    NodePropertySheet(
                        node = selectedNode,
                        onUpdate = { updated ->
                            flow = flow.copy(nodes = flow.nodes.map { if (it.id == updated.id) updated else it })
                        },
                        onClose = { selectedNodeId = null },
                    )
                }
            }
        }
    }

    if (showAddMenu) {
        AddNodeSheet(
            onDismiss = { showAddMenu = false },
            onAdd = { type ->
                val node = FlowNode(
                    id = UUID.randomUUID().toString().take(8),
                    type = type,
                    x = 100f + (flow.nodes.size * 30f) % 400f,
                    y = 200f + (flow.nodes.size * 80f) % 600f,
                    props = defaultProps(type),
                )
                flow = flow.copy(nodes = flow.nodes + node)
                showAddMenu = false
            },
        )
    }
}

@Composable
private fun AddNodeSheet(onDismiss: () -> Unit, onAdd: (NodeType) -> Unit) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpaceSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 8.dp).width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(SpaceOutline),
            )
        },
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Add Node", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Choose a node type to add to the canvas", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))

            Text("Triggers", fontWeight = FontWeight.SemiBold, color = TextTertiary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NodeTypeChip(NodeType.TRIGGER_MESSAGE) { onAdd(NodeType.TRIGGER_MESSAGE) }
                NodeTypeChip(NodeType.TRIGGER_COMMAND) { onAdd(NodeType.TRIGGER_COMMAND) }
            }

            Spacer(Modifier.height(16.dp))
            Text("Actions", fontWeight = FontWeight.SemiBold, color = TextTertiary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NodeTypeChip(NodeType.ACTION_REPLY) { onAdd(NodeType.ACTION_REPLY) }
                NodeTypeChip(NodeType.ACTION_TOAST) { onAdd(NodeType.ACTION_TOAST) }
                NodeTypeChip(NodeType.ACTION_DELAY) { onAdd(NodeType.ACTION_DELAY) }
                NodeTypeChip(NodeType.ACTION_LOG) { onAdd(NodeType.ACTION_LOG) }
            }

            Spacer(Modifier.height(16.dp))
            Text("Logic", fontWeight = FontWeight.SemiBold, color = TextTertiary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NodeTypeChip(NodeType.LOGIC_CONDITION) { onAdd(NodeType.LOGIC_CONDITION) }
            }
        }
    }
}

@Composable
private fun NodeTypeChip(type: NodeType, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(type.color).copy(alpha = 0.12f),
        contentColor = Color(type.color),
    ) {
        Text(
            text = type.label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(type.color),
        )
    }
}

@Composable
private fun NodePropertySheet(
    node: FlowNode,
    onUpdate: (FlowNode) -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SpaceSurface,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(RoundedCornerShape(5.dp))
                        .background(Color(node.type.color)),
                )
                Spacer(Modifier.width(10.dp))
                Text(node.type.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            when (node.type) {
                NodeType.TRIGGER_MESSAGE -> PropField("Keyword filter", node.props["keyword"] ?: "", { v ->
                    onUpdate(node.copy(props = node.props + ("keyword" to v)))
                })
                NodeType.TRIGGER_COMMAND -> PropField("Command name", node.props["command"] ?: "start", { v ->
                    onUpdate(node.copy(props = node.props + ("command" to v)))
                })
                NodeType.ACTION_REPLY -> PropField("Reply message", node.props["message"] ?: "Hello!", { v ->
                    onUpdate(node.copy(props = node.props + ("message" to v)))
                })
                NodeType.ACTION_TOAST -> PropField("Toast text", node.props["message"] ?: "Hello!", { v ->
                    onUpdate(node.copy(props = node.props + ("message" to v)))
                })
                NodeType.ACTION_DELAY -> PropField("Delay (ms)", node.props["ms"] ?: "1000", { v ->
                    onUpdate(node.copy(props = node.props + ("ms" to v)))
                })
                NodeType.ACTION_LOG -> PropField("Log message", node.props["message"] ?: "log", { v ->
                    onUpdate(node.copy(props = node.props + ("message" to v)))
                })
                NodeType.LOGIC_CONDITION -> PropField("Condition (JS)", node.props["condition"] ?: "msg.includes('hi')", { v ->
                    onUpdate(node.copy(props = node.props + ("condition" to v)))
                })
            }
        }
    }
}

@Composable
private fun PropField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                focusedBorderColor = Blue60, unfocusedBorderColor = SpaceOutline,
                cursorColor = Blue60,
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

private fun defaultProps(type: NodeType): Map<String, String> = when (type) {
    NodeType.TRIGGER_MESSAGE -> mapOf("keyword" to "")
    NodeType.TRIGGER_COMMAND -> mapOf("command" to "start")
    NodeType.ACTION_REPLY -> mapOf("message" to "Hello!")
    NodeType.ACTION_TOAST -> mapOf("message" to "Hello!")
    NodeType.ACTION_DELAY -> mapOf("ms" to "1000")
    NodeType.ACTION_LOG -> mapOf("message" to "log")
    NodeType.LOGIC_CONDITION -> mapOf("condition" to "msg.includes('hi')")
}

/** Cubic bezier evaluation at parameter t in [0,1]. */
private fun bezierPoint(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    val tt = t * t
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * t
    return Offset(
        uuu * p0.x + 3f * uu * t * p1.x + 3f * u * tt * p2.x + ttt * p3.x,
        uuu * p0.y + 3f * uu * t * p1.y + 3f * u * tt * p2.y + ttt * p3.y,
    )
}
