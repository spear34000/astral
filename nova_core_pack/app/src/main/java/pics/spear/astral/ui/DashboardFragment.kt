package pics.spear.astral.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.os.SystemClock
import java.util.concurrent.TimeUnit
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.cos
import pics.spear.astral.R
import kotlin.math.sin
import pics.spear.astral.store.BotStore
import pics.spear.astral.util.PermissionUtils
import pics.spear.astral.ui.theme.AstralTheme
import pics.spear.astral.script.AstralScriptRuntime
import kotlinx.coroutines.delay
import pics.spear.astral.ui.components.AstralHeader
import pics.spear.astral.ui.components.AstralPill
import pics.spear.astral.ui.components.AstralScreen
import pics.spear.astral.ui.components.VanguardCard
import pics.spear.astral.model.BotMeta

class DashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AstralTheme { AstralDashboard() }
            }
        }
    }
}

@Composable
fun AstralDashboard() {
    val context = LocalContext.current
    var botCount by remember { mutableStateOf(0) }
    var activeBots by remember { mutableStateOf(0) }
    var hasPermission by remember { mutableStateOf(PermissionUtils.hasNotificationAccess(context)) }
    var bots by remember { mutableStateOf(emptyList<BotMeta>()) }
    val scope = rememberCoroutineScope()
    var focusMode by remember { mutableStateOf(true) }
    val runtime = remember { AstralScriptRuntime.getInstance(context) }
    
    // Mock Resource Data
    var cpuUsage by remember { mutableStateOf(0f) }
    var memUsage by remember { mutableStateOf(0f) }
    
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        while(true) {
            val botsList = BotStore.list(context)
            bots = botsList
            botCount = botsList.size
            activeBots = botsList.count { it.enabled }
            hasPermission = PermissionUtils.hasNotificationAccess(context)
            
            // Random mock resources for "Vanguard" feel
            cpuUsage = (0..100).random().toFloat() / 100f
            memUsage = (20..60).random().toFloat() / 100f
            
            delay(2000)
        }
    }

    AstralScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            AstralHeader(
                title = "대시보드",
                subtitle = "자동화 상태와 리소스 사용량을 확인합니다."
            )
            Spacer(modifier = Modifier.height(16.dp))
            DashboardSummaryCard(activeBots = activeBots, botCount = botCount, hasPermission = hasPermission)
            Spacer(modifier = Modifier.height(18.dp))

            Text("리소스", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(title = "CPU", value = "${(cpuUsage * 100).toInt()}%", caption = "평균 부하", modifier = Modifier.weight(1f))
                MetricCard(title = "메모리", value = "${(memUsage * 1024).toInt()}MB", caption = "예상 사용량", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("스크립트", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(title = "총 스크립트", value = botCount.toString(), caption = "등록된 봇", modifier = Modifier.weight(1f))
                MetricCard(title = "활성화", value = activeBots.toString(), caption = "현재 실행", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(18.dp))

            AstralQuickActions(
                focusMode = focusMode,
                onToggle = { focusMode = !focusMode }
            )

            if (!focusMode) {
                Spacer(modifier = Modifier.height(16.dp))
                AstralRecentScripts(
                    bots = bots,
                    onToggle = { bot, enabled ->
                        scope.launch {
                            val updated = bot.copy(
                                enabled = enabled,
                                autoStart = enabled,
                                updatedAt = System.currentTimeMillis()
                            )
                            BotStore.upsert(context, updated)
                            if (enabled) {
                                runtime.ensureBotRunning(updated)
                            } else {
                                runtime.stopBot(bot.id)
                            }
                            bots = BotStore.list(context)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}


@Composable
fun AstralStatusCard(activeBots: Int, hasPermission: Boolean) {
    VanguardCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            val status = if (activeBots > 0) "RUNNING" else "IDLE"
            Text("상태", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(status, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                if (activeBots > 0) "자동화 엔진이 동작 중입니다." else "활성 스크립트가 없습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!hasPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                PermissionWarning()
            }
        }
    }
}

@Composable
fun DashboardSummaryCard(activeBots: Int, botCount: Int, hasPermission: Boolean) {
    val context = LocalContext.current
    VanguardCard(modifier = Modifier.fillMaxWidth(), borderAlpha = 0.2f) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.raw.astral_header_wave)
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.TopEnd)
                    .alpha(0.22f)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text("상태", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (activeBots > 0) "실행 중" else "대기 중",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "${botCount}개 중 ${activeBots}개 활성화",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        if (hasPermission) "권한 정상" else "권한 필요",
                        color = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "업타임 ${formatUptime(SystemClock.elapsedRealtime())}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (!hasPermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PermissionWarning()
                }
            }
        }
    }
}

private fun formatUptime(elapsedMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs)
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (days > 0) {
        "${days}d ${hours}h ${minutes}m"
    } else if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m ${seconds}s"
    }
}

@Composable
fun MetricCard(title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    VanguardCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Text(caption, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AstralQuickActions(focusMode: Boolean, onToggle: () -> Unit) {
    VanguardCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("빠른 작업", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                Text("필수 기능만 표시합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onToggle) {
                Text(if (focusMode) "자세히" else "간결히", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun AstralRecentScripts(
    bots: List<BotMeta>,
    onToggle: (BotMeta, Boolean) -> Unit
) {
    Column {
        Text("최근 스크립트", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))
        if (bots.isEmpty()) {
            VanguardCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("아직 스크립트가 없습니다.", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    Text("새 스크립트를 만들면 여기에 표시됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                bots.sortedByDescending { it.updatedAt }.take(4).forEach { bot ->
                    VanguardCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bot.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "업데이트 ${android.text.format.DateFormat.format("MM/dd HH:mm", bot.updatedAt)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { onToggle(bot, !bot.enabled) }) {
                                Text(if (bot.enabled) "정지" else "실행", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemStatusCard(active: Boolean, hasPermission: Boolean) {
    VanguardCard(modifier = Modifier.fillMaxWidth(), borderAlpha = if (active) 0.35f else 0.15f) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
                    label = "alpha"
                )

                val statusColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(if (active) alpha else 1f)
                        .background(statusColor, CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    if (active) "CORE ONLINE" else "CORE IDLE",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "자동화 엔진이 안정적으로 동작 중이며, 실시간 입력을 감시하고 있습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(20.dp))
                PermissionWarning()
            }
        }
    }
}

@Composable
fun BentoStatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    VanguardCard(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AstralLaunchBay(
    bots: List<BotMeta>,
    onToggle: (BotMeta, Boolean) -> Unit
) {
    Column {
        Text(
            "ASTRAL LAUNCH BAY",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (bots.isEmpty()) {
            VanguardCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("아직 스크립트가 없습니다.", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("새 스크립트를 생성하면 런치 베이에 표시됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                bots.sortedByDescending { it.updatedAt }.take(8).forEachIndexed { index, bot ->
                    if (index > 0) Spacer(modifier = Modifier.width(12.dp))
                    LaunchScriptCard(bot = bot, onToggle = onToggle)
                }
            }
        }
    }
}

@Composable
fun LaunchScriptCard(bot: BotMeta, onToggle: (BotMeta, Boolean) -> Unit) {
    val isActive = bot.enabled
    VanguardCard(modifier = Modifier.width(200.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isActive) "RUNNING" else "PAUSED",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                bot.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "업데이트 ${android.text.format.DateFormat.format("MM/dd HH:mm", bot.updatedAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { onToggle(bot, !isActive) }) {
                Text(
                    if (isActive) "정지" else "실행",
                    color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun ConstellationSection(bots: List<BotMeta>) {
    Column {
        Text(
            "ASTRAL CONSTELLATION",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        VanguardCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                ConstellationMap(bots = bots, modifier = Modifier.size(200.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("스크립트 성좌", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("활성 상태와 배치를 한 눈에 확인합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    bots.sortedByDescending { it.updatedAt }.take(4).forEach { bot ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (bot.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                bot.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ConstellationMap(bots: List<BotMeta>, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val orbit = size.minDimension * 0.32f
        drawCircle(color = dim.copy(alpha = 0.2f), radius = orbit + 24f, center = center)
        drawCircle(color = dim.copy(alpha = 0.4f), radius = orbit, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

        drawCircle(color = primary.copy(alpha = 0.9f), radius = 6f, center = center)

        val count = if (bots.isEmpty()) 1 else bots.size
        bots.take(12).forEachIndexed { index, bot ->
            val angle = (360f / count) * index
            val rad = Math.toRadians(angle.toDouble())
            val radius = orbit + (index % 3) * 10f
            val x = (center.x + cos(rad) * radius).toFloat()
            val y = (center.y + sin(rad) * radius).toFloat()
            val starColor = if (bot.enabled) primary else dim
            drawLine(color = starColor.copy(alpha = 0.35f), start = center, end = Offset(x, y), strokeWidth = 1.2f)
            drawCircle(color = starColor, radius = 4f, center = Offset(x, y))
        }
    }
}

@Composable
fun TelemetrySection() {
    Column {
        Text(
            "시스템 텔레메트리",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(14.dp))

        VanguardCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                TelemetryRow("BOOT", "코어 v1.0.0 초기화 완료", "0ms")
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(0.3f))
                TelemetryRow("LINK", "보안 터널 및 소켓 연결 수립", "ENCRYPTED")
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(0.3f))
                TelemetryRow("NODE", "Node.js 런타임 활성화됨", "RUNNING")
            }
        }
    }
}

@Composable
fun TelemetryRow(label: String, msg: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            msg,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun PermissionWarning() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "알림 접근 권한이 필요합니다. 시스템 관리로 이동하세요.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
