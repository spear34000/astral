package pics.spear.astral.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pics.spear.astral.store.BotStore
import pics.spear.astral.ui.components.*
import pics.spear.astral.ui.theme.*
import pics.spear.astral.util.Permissions

@Composable
fun HomeScreen(
    botStore: BotStore,
    onNavigateToBots: () -> Unit,
    onNavigateToLogs: () -> Unit,
) {
    val bots by botStore.bots.collectAsState()
    val activeCount = bots.count { it.enabled }
    val ctx = LocalContext.current
    val hasNotifAccess = remember { Permissions.hasNotificationAccess(ctx) }

    AstralScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 52.dp),
        ) {
            // Header section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Column {
                    Text(
                        text = "Astral",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-1).sp,
                    )
                    Text(
                        text = "KakaoTalk Bot Platform",
                        fontSize = 15.sp,
                        color = TextSecondary,
                        modifier = Modifier.offset(y = (-4).dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Active",
                    value = "$activeCount",
                    accentColor = Emerald60,
                    icon = {
                        Icon(Icons.Default.PlayArrow, null, tint = Emerald60, modifier = Modifier.size(20.dp))
                    },
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Total",
                    value = "${bots.size}",
                    accentColor = Blue60,
                    icon = {
                        Icon(Icons.Default.Inventory2, null, tint = Blue60, modifier = Modifier.size(20.dp))
                    },
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Bots",
                    value = "${bots.size}",
                    accentColor = Purple60,
                    icon = {
                        Icon(Icons.Default.SmartToy, null, tint = Purple60, modifier = Modifier.size(20.dp))
                    },
                )
            }

            Spacer(Modifier.height(20.dp))

            // Notification status card
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    GlassCard(onClick = {
                        if (!hasNotifAccess) {
                            ctx.startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }
                    }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (hasNotifAccess) Emerald60.copy(alpha = 0.15f)
                                    else WarningAmber.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (hasNotifAccess) Icons.Default.NotificationsActive
                                else Icons.Default.NotificationsOff,
                                null,
                                tint = if (hasNotifAccess) Emerald60 else WarningAmber,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (hasNotifAccess) "Notification Listener Active"
                                else "Notification Access Required",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 15.sp,
                            )
                            Text(
                                text = if (hasNotifAccess) "Listening for KakaoTalk messages"
                                else "Tap to enable in settings",
                                color = TextSecondary,
                                fontSize = 13.sp,
                            )
                        }
                        if (!hasNotifAccess) {
                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = TextTertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Bots section header
            SectionHeader(
                title = "Your Bots",
                subtitle = if (bots.isNotEmpty()) "${bots.size} bot${if (bots.size != 1) "s" else ""} created"
                else null,
                action = {
                    TextButton(onClick = onNavigateToBots) {
                        Text("See All", color = Blue60, fontWeight = FontWeight.SemiBold)
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            if (bots.isEmpty()) {
                EmptyState(
                    icon = {
                        Icon(
                            Icons.Default.SmartToy,
                            null,
                            tint = TextTertiary,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    title = "No bots yet",
                    subtitle = "Create your first KakaoTalk bot\nto get started",
                    action = {
                        GlowGradientButton("Create Bot", onNavigateToBots)
                    },
                )
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    bots.take(3).forEach { bot ->
                        BotCard(
                            name = bot.name,
                            language = bot.language,
                            enabled = bot.enabled,
                            onToggle = { botStore.toggle(bot.id) },
                            onClick = onNavigateToBots,
                        )
                    }
                    if (bots.size > 3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            TextButton(onClick = onNavigateToBots) {
                                Text(
                                    "View all ${bots.size} bots",
                                    color = Blue60,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
