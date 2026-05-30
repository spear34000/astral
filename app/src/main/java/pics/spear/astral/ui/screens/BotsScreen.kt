package pics.spear.astral.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pics.spear.astral.model.Bot
import pics.spear.astral.store.BotStore
import pics.spear.astral.ui.components.*
import pics.spear.astral.ui.theme.*
import java.util.UUID

@Composable
fun BotsScreen(
    botStore: BotStore,
    onEditBot: (String) -> Unit,
) {
    val bots by botStore.bots.collectAsState()
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AstralScreen {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 52.dp),
            ) {
                SectionHeader(
                    title = "Bots",
                    subtitle = if (bots.isNotEmpty()) "${bots.size} bot${if (bots.size != 1) "s" else ""}"
                    else null,
                )
                Spacer(Modifier.height(8.dp))

                if (bots.isEmpty()) {
                    EmptyState(
                        icon = {
                            Icon(Icons.Default.SmartToy, null, tint = Blue60, modifier = Modifier.size(32.dp))
                        },
                        title = "No bots yet",
                        subtitle = "Create your first bot to start automating KakaoTalk",
                        action = { GlowGradientButton("Create Bot") { showCreate = true } },
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(bots, key = { it.id }) { bot ->
                            BotCard(
                                name = bot.name,
                                language = bot.language,
                                enabled = bot.enabled,
                                onToggle = { botStore.toggle(bot.id) },
                                onClick = { onEditBot(bot.id) },
                            )
                        }
                    }
                }
            }
        }

        if (bots.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                Surface(
                    onClick = { showCreate = true },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    shadowElevation = 8.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(45f, Blue60, Purple60)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateBotSheet(
            onDismiss = { showCreate = false },
            onCreate = { name, lang ->
                scope.launch {
                    val bot = Bot(
                        id = UUID.randomUUID().toString().take(8),
                        name = name,
                        language = lang,
                        script = getDefaultScript(lang),
                    )
                    botStore.add(bot)
                    botStore.saveScript(bot.name, bot.script)
                }
                showCreate = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBotSheet(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("javascript") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SpaceSurface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SpaceOutline),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
        ) {
            Text("Create Bot", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Give your bot a name and choose a language", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Bot Name") },
                placeholder = { Text("e.g. MyHelper", color = TextTertiary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Blue60,
                    unfocusedBorderColor = SpaceOutline,
                    focusedLabelColor = Blue60,
                    unfocusedLabelColor = TextTertiary,
                    cursorColor = Blue60,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("Language", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LangChip("javascript", lang == "javascript") { lang = "javascript" }
                LangChip("python", lang == "python") { lang = "python" }
            }
            Spacer(Modifier.height(24.dp))
            GlowGradientButton(
                text = "Create Bot",
                onClick = { onCreate(name.trim(), lang) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LangChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Blue60.copy(alpha = 0.15f) else GlassWhite,
        contentColor = if (selected) Blue60 else TextSecondary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.replaceFirstChar { it.uppercase() },
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
            )
        }
    }
}

private fun getDefaultScript(lang: String): String = when (lang) {
    "python" -> """# Astral Bot - Python
# Responds to all messages
bot.onMessage(lambda room, sender, msg, is_group: bot.reply("Hello! I received: " + msg))

# Command handler
@bot.command("help")
def help(room, sender, msg, is_group, args):
    bot.reply("Available commands: !help, !ping")

@bot.command("ping")
def ping(room, sender, msg, is_group, args):
    bot.reply("Pong!")
"""
    else -> """// Astral Bot - JavaScript
// Responds to all messages
bot.onMessage(function(room, sender, msg, isGroup) {
    bot.reply("Hello! I received: " + msg);
});

// Command handlers
bot.onCommand("help", function(room, sender, msg, isGroup, args) {
    bot.reply("Available commands: !help, !ping");
});

bot.onCommand("ping", function(room, sender, msg, isGroup, args) {
    bot.reply("Pong!");
});
"""
}
