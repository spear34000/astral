package pics.spear.astral.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pics.spear.astral.engine.ApiServer
import pics.spear.astral.store.BotStore
import pics.spear.astral.store.LogStore
import pics.spear.astral.ui.components.*
import pics.spear.astral.ui.theme.*
import pics.spear.astral.util.Permissions

@Composable
fun HomeScreen(
    botStore: BotStore,
    logStore: LogStore,
    apiServer: ApiServer? = null,
    onNavigateToBots: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToFlow: () -> Unit,
) {
    val bots by botStore.bots.collectAsState()
    val logs by logStore.logs.collectAsState()
    val activeCount = bots.count { it.enabled }
    val errorCount = logs.count { it.level == "error" }
    val ctx = LocalContext.current
    val hasNotif = remember { Permissions.hasNotificationAccess(ctx) }
    val apiEnabled = apiServer?.config?.enabled ?: false
    val apiPort = apiServer?.config?.port ?: 9345

    val scrollState = rememberScrollState()

    // Animated counters
    val activeAnim by animateFloatAsState(
        targetValue = activeCount.toFloat(),
        animationSpec = tween(600, easing = FastOutSlowInEasing), label = "active",
    )
    val totalAnim by animateFloatAsState(
        targetValue = bots.size.toFloat(),
        animationSpec = tween(600, easing = FastOutSlowInEasing), label = "total",
    )

    AstralScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 52.dp)
                .verticalScroll(scrollState),
        ) {
            // ── Header ──────────────────────────────────
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "astral",
                                fontSize = 32.sp, fontWeight = FontWeight.Bold,
                                color = TextPrimary, letterSpacing = (-1).sp,
                            )
                            Text(
                                text = "vibe coding framework · v2.0",
                                fontSize = 12.sp, color = AstralCyan.copy(alpha = 0.8f),
                                fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp,
                            )
                        }
                        VibeMeter(level = 0.8f)
                    }
                    Spacer(Modifier.height(8.dp))
                    VibeQuote()
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Daily Prompt ──────────────────────────────
            Box(Modifier.padding(horizontal = 20.dp)) {
                VibePrompt(
                    prompt = "create a bot that replies with a random cat fact when someone says \"cat\"",
                    gradient = listOf(AstralPurple, AstralBlue),
                    onClick = onNavigateToBots,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Stats Grid (2x2) ─────────────────────────
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    Modifier.weight(1f), "Active Bots", "${activeAnim.toInt()}",
                    { Icon(Icons.Default.PlayArrow, null, tint = AstralEmerald, modifier = Modifier.size(20.dp)) },
                    accent = AstralEmerald,
                )
                StatCard(
                    Modifier.weight(1f), "Total Bots", "${totalAnim.toInt()}",
                    { Icon(Icons.Default.SmartToy, null, tint = AstralBlue, modifier = Modifier.size(20.dp)) },
                    accent = AstralBlue,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    Modifier.weight(1f), "Errors", "$errorCount",
                    { Icon(Icons.Default.BugReport, null, tint = ErrorRed, modifier = Modifier.size(20.dp)) },
                    accent = ErrorRed,
                )
                StatCard(
                    Modifier.weight(1f), "API",
                    if (apiEnabled) ":$apiPort" else "Off",
                    {
                        Icon(
                            if (apiEnabled) Icons.Default.Api else Icons.Default.ApiOff,
                            null,
                            tint = if (apiEnabled) AstralCyan else TextTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    accent = if (apiEnabled) AstralCyan else TextTertiary,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Status Cards ─────────────────────────────
            Box(Modifier.padding(horizontal = 20.dp)) {
                GlassCard(glow = true) {
                    Column(Modifier.padding(16.dp)) {
                        Text("System Status", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        StatusRow(Icons.Default.NotificationsActive, "Notification Listener",
                            if (hasNotif) "Active" else "Inactive",
                            if (hasNotif) AstralEmerald else ErrorRed)
                        Spacer(Modifier.height(8.dp))
                        StatusRow(Icons.Default.Api, "API Server",
                            if (apiEnabled) "Running on port $apiPort" else "Stopped",
                            if (apiEnabled) AstralEmerald else TextTertiary)
                        Spacer(Modifier.height(8.dp))
                        StatusRow(Icons.Default.Memory, "Script Engine",
                            "WebView V8 · ${bots.size} bot${if (bots.size != 1) "s" else ""} loaded",
                            AstralBlue)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Quick Actions ────────────────────────────
            Box(Modifier.padding(horizontal = 20.dp)) {
                GlassCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("Quick Actions", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionChip(Icons.Default.Add, "New Bot", AstralEmerald, onNavigateToBots)
                            ActionChip(Icons.Default.AccountTree, "New Flow", AstralPurple, onNavigateToFlow)
                            ActionChip(Icons.Default.Terminal, "Logs", AstralBlue, onNavigateToLogs)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Recent Activity ──────────────────────────
            if (logs.isNotEmpty()) {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    GlassCard {
                        Column(Modifier.padding(16.dp)) {
                            Text("Recent Activity", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                            Spacer(Modifier.height(12.dp))
                            logs.takeLast(4).reversed().forEach { entry ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp))
                                        .background(
                                            when (entry.level) {
                                                "error" -> ErrorRed
                                                "warn" -> WarningAmber
                                                else -> AstralBlue
                                            }
                                        ))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        entry.botName,
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                                        modifier = Modifier.width(60.dp),
                                    )
                                    Text(
                                        entry.message.take(40) + if (entry.message.length > 40) "..." else "",
                                        fontSize = 12.sp, color = TextSecondary, fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Bots Preview ─────────────────────────────
            if (bots.isNotEmpty()) {
                SectionHeader(
                    "Your Bots",
                    subtitle = "${bots.size} bot${if (bots.size != 1) "s" else ""} · ${activeCount} active",
                    action = {
                        TextButton(onClick = onNavigateToBots) {
                            Text("See All", color = AstralBlue, fontWeight = FontWeight.SemiBold)
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bots.take(3).forEach { bot ->
                        BotCard(bot.name, bot.language, bot.enabled, { botStore.toggle(bot.id) }, onNavigateToBots)
                    }
                }
            } else {
                EmptyState(
                    icon = { Icon(Icons.Default.SmartToy, null, tint = TextTertiary, modifier = Modifier.size(28.dp)) },
                    title = "No bots yet",
                    subtitle = "Create your first bot or use the visual Flow builder",
                    action = { GlowButton("Create Bot", onNavigateToBots) },
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatusRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = valueColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, accent: Color, onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
