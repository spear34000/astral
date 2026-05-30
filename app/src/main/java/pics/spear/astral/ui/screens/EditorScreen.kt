package pics.spear.astral.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pics.spear.astral.store.BotStore
import pics.spear.astral.ui.components.*
import pics.spear.astral.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    botId: String,
    botStore: BotStore,
    onBack: () -> Unit,
) {
    val bots by botStore.bots.collectAsState()
    val bot = bots.find { it.id == botId } ?: run { onBack(); return }

    var script by remember(botId) { mutableStateOf(botStore.loadScript(bot.name)) }
    val scope = rememberCoroutineScope()
    var saved by remember { mutableStateOf(false) }

    val fancyPlaceholders = remember {
        listOf(
            "// 자유롭게 코드를 작성해보세요 ✦",
            "// 여기에 봇의 동작을 정의합니다",
            "// 예: bot.reply('안녕!') 하고 답장해요",
            "// bot.onMessage로 메시지를 처리해요",
            "// 코드가 흐르는 대로 두세요",
            "// 모르면 그냥 켜보세요! 기본 코드 있어요",
        )
    }

    val scrollState = rememberScrollState()
    val lineCount = script.count { it == '\n' } + 1

    // Animated vibe glow on the editor
    val editorGlow by rememberInfiniteTransition(label = "eg").animateFloat(
        initialValue = 0.02f, targetValue = 0.06f,
        animationSpec = infiniteRepeatable(tween(2000), repeatMode = RepeatMode.Reverse),
        label = "egl",
    )

    // Cycling placeholder
    val placeholderIdx by rememberInfiniteTransition(label = "ph").animateFloat(
        initialValue = 0f, targetValue = (fancyPlaceholders.size - 1).toFloat(),
        animationSpec = infiniteRepeatable(tween(12000), repeatMode = RepeatMode.Restart),
        label = "phi",
    )

    var showVibeTip by remember { mutableStateOf(true) }

    AstralScreen {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Surface(
                color = SpaceSurface,
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로", tint = TextPrimary)
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = bot.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${bot.language.replaceFirstChar { it.uppercase() }}  ·  $lineCount 줄",
                                fontSize = 12.sp,
                                color = TextTertiary,
                            )
                            Spacer(Modifier.width(8.dp))
                            PulseText("코딩 중", color = AstralPurple, fontSize = 10.sp)
                        }
                    }

                    // Save status
                    AnimatedContent(
                        targetState = saved,
                        transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
                        label = "saved",
                    ) { isSaved ->
                        if (isSaved) {
                            Text(
                                text = "저장됨 ✦",
                                fontSize = 11.sp,
                                color = AstralEmerald,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }

                    IconButton(onClick = {
                        scope.launch {
                            botStore.saveScript(bot.name, script)
                            botStore.update(bot.id) { it.copy(updatedAt = System.currentTimeMillis()) }
                            saved = true
                        }
                    }) {
                        Icon(
                            Icons.Default.Save,
                            "저장",
                            tint = if (saved) AstralEmerald else TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // Vibe tip toggle
                    IconButton(onClick = { showVibeTip = !showVibeTip }) {
                        Icon(
                            Icons.Default.TipsAndUpdates,
                            "팁",
                            tint = if (showVibeTip) AstralPurple else TextTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Vibe tip
            if (showVibeTip) {
                VibeTip(
                    tip = "도움말: bot.reply()로 답장하고, bot.toast()로 토스트를 띄워요. 봇 이름은 bot.getName()으로 확인!",
                    visible = true,
                    onDismiss = { showVibeTip = false },
                )
            }

            // Line count bar + editor
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 0.dp),
            ) {
                // Line numbers gutter
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight()
                        .background(SpaceSurfaceVariant)
                        .padding(top = 12.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.End,
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = "$i",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextTertiary.copy(alpha = 0.6f),
                            modifier = Modifier
                                .height(22.sp)
                                .padding(end = 8.dp),
                            lineHeight = 22.sp,
                        )
                    }
                }

                // Editor area with animated glow border
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(SpaceSurfaceVariant.copy(alpha = 0.5f))
                        .border(
                            width = 0.5.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    AstralBlue.copy(alpha = editorGlow),
                                    AstralPurple.copy(alpha = editorGlow * 0.7f),
                                    Color.Transparent,
                                )
                            ),
                            shape = RoundedCornerShape(0.dp),
                        ),
                ) {
                    BasicTextField(
                        value = script,
                        onValueChange = { script = it; saved = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState()),
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 22.sp,
                        ),
                        cursorBrush = SolidColor(AstralBlue),
                        decorationBox = { innerTextField ->
                            if (script.isEmpty()) {
                                Text(
                                    text = fancyPlaceholders[placeholderIdx.toInt() % fancyPlaceholders.size],
                                    style = TextStyle(
                                        color = TextTertiary.copy(alpha = 0.4f),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 22.sp,
                                    ),
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }
        }
    }
}
