package pics.spear.astral.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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

    val scrollState = rememberScrollState()
    val lineCount = script.count { it == '\n' } + 1

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
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = bot.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary,
                        )
                        Text(
                            text = "${bot.language.replaceFirstChar { it.uppercase() }}  ·  $lineCount lines",
                            fontSize = 12.sp,
                            color = TextTertiary,
                        )
                    }

                    // Save status
                    Text(
                        text = if (saved) "Saved" else "",
                        fontSize = 12.sp,
                        color = Emerald60,
                    )

                    IconButton(onClick = {
                        scope.launch {
                            botStore.saveScript(bot.name, script)
                            botStore.update(bot.id) { it.copy(updatedAt = System.currentTimeMillis()) }
                            saved = true
                        }
                    }) {
                        Icon(
                            Icons.Default.Save,
                            "Save",
                            tint = if (saved) Emerald60 else TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
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

                // Editor area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(SpaceSurfaceVariant.copy(alpha = 0.5f)),
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
                        cursorBrush = SolidColor(Blue60),
                        decorationBox = { innerTextField ->
                            if (script.isEmpty()) {
                                Text(
                                    text = "// Write your bot script here...",
                                    style = TextStyle(
                                        color = TextTertiary.copy(alpha = 0.5f),
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
