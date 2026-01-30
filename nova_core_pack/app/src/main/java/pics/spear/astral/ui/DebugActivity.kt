package pics.spear.astral.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pics.spear.astral.model.ChatMessage
import pics.spear.astral.script.AstralScriptRuntime
import pics.spear.astral.runtime.NativeBridge
import pics.spear.astral.store.BotScriptStore
import pics.spear.astral.store.BotStore
import pics.spear.astral.ui.theme.AstralTheme
import pics.spear.astral.ui.components.AstralScreen
import pics.spear.astral.ui.components.PremiumGlassCard

class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val botId = intent.getStringExtra("botId") ?: return finish()

        setContent {
            AstralTheme {
                DebugScreen(botId = botId, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(botId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var botName by remember { mutableStateOf("Bot") }
    var botMeta by remember { mutableStateOf<pics.spear.astral.model.BotMeta?>(null) }
    var messages by remember { mutableStateOf(emptyList<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(botId) {
        val bots = BotStore.list(context)
        botMeta = bots.find { it.id == botId }
        botName = botMeta?.name ?: "Bot"
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Connect to NativeBridge for replies
    DisposableEffect(Unit) {
        val bridge = NativeBridge.getInstance(context)
        bridge.debugReplyListener = { reply ->
            messages = messages + ChatMessage(sender = botName, content = reply)
        }
        
        // Initial script load (force reload for debugging)
        scope.launch {
            val botLang = BotStore.list(context).find { it.id == botId }?.language ?: "js"
            val scriptCode = BotScriptStore.loadOrCreate(context, botId, botLang)
            val runtime = AstralScriptRuntime.getInstance(context)
            runtime.executeScript(
                scriptId = botId,
                scriptCode = scriptCode,
                language = botLang,
                onError = { error ->
                    messages = messages + ChatMessage(sender = "ERROR", content = error)
                }
            )
        }

        onDispose {
            bridge.debugReplyListener = null
        }
    }

    AstralScreen {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(botName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                            Text("DEBUG TERMINAL", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(onClick = { messages = emptyList() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(messages) { msg ->
                            val isMe = msg.sender == "YOU"
                            ChatBubble(msg, isMe)
                        }
                    }
                }

                PremiumGlassCard(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text("메시지 입력...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        IconButton(onClick = {
                            if (inputText.isNotBlank()) {
                                val currentInput = inputText
                                inputText = ""
                                scope.launch {
                                    messages = messages + ChatMessage(sender = "YOU", content = currentInput)
                                    val runtime = AstralScriptRuntime.getInstance(context)
                                    botMeta?.let { runtime.ensureBotRunning(it) }
                                    runtime.handleKakaoMessage("DEBUG_TERMINAL", "YOU", currentInput, false)
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Text(
            text = msg.sender,
            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                .border(1.dp, if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            Text(msg.content, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
