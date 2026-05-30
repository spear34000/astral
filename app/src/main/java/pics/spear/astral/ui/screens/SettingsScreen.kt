package pics.spear.astral.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pics.spear.astral.engine.ApiServer
import pics.spear.astral.ui.components.*
import pics.spear.astral.ui.theme.*
import pics.spear.astral.util.Permissions

@Composable
fun SettingsScreen(
    apiServer: ApiServer? = null,
) {
    val ctx = LocalContext.current
    val hasNotifAccess = remember { Permissions.hasNotificationAccess(ctx) }

    var apiEnabled by remember(apiServer) { mutableStateOf(apiServer?.config?.enabled ?: false) }
    var apiPort by remember(apiServer) { mutableStateOf((apiServer?.config?.port ?: 9345).toString()) }
    var showPortInput by remember { mutableStateOf(false) }

    AstralScreen {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 52.dp),
        ) {
            SectionHeader("설정", subtitle = "앱 환경설정")

            Spacer(Modifier.height(20.dp))

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Notification
                SettingItem(
                    Icons.Default.NotificationsActive,
                    if (hasNotifAccess) AstralEmerald else ErrorRed,
                    if (hasNotifAccess) AstralEmerald.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                    "카톡 알림 접근",
                    if (hasNotifAccess) "켜짐 — 카톡 메시지를 감지 중입니다" else "꺼짐 — 봇이 동작하려면 필요합니다",
                    onClick = {
                        if (!hasNotifAccess) {
                            ctx.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    },
                )

                // ── API Server ────────────────────────────────
                if (apiServer != null) {
                    GlassCard {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                    .background(AstralBlue.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Api, null, tint = AstralBlue, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("외부 접속 (API)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                                    Text(
                                        if (apiEnabled) "$apiPort 포트에서 실행 중" else "외부 접속 꺼짐",
                                        fontSize = 13.sp, color = if (apiEnabled) AstralEmerald else TextTertiary,
                                    )
                                }
                                AstralSwitch(checked = apiEnabled) { on ->
                                    apiEnabled = on
                                    if (on) apiServer.start(apiPort.toIntOrNull() ?: 9345)
                                    else apiServer.stop()
                                }
                            }
                            if (apiEnabled) {
                                Spacer(Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("http://127.0.0.1:${apiPort}/api/v1", fontSize = 11.sp, color = AstralCyan, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    TextButton(onClick = { showPortInput = !showPortInput }) {
                                        Text("포트 변경", color = AstralBlue, fontSize = 12.sp)
                                    }
                                }
                                if (showPortInput) {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = apiPort,
                                        onValueChange = { apiPort = it.filter { c -> c.isDigit() }.take(5) },
                                        label = { Text("포트") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = AstralBlue, unfocusedBorderColor = SpaceOutline,
                                            cursorColor = AstralBlue,
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    GlowButton("서버 재시작", {
                                        apiPort.toIntOrNull()?.let { apiServer.restart(it) }
                                        showPortInput = false
                                    })
                                }
                            }
                        }
                    }
                }

                // ── Vibe Mode ─────────────────────────────────
                GlassCard {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(AstralPurple.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Text("✦", color = AstralPurple, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("바이브 모드", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                            PulseText("창의력 모드 · 켜짐", color = AstralBlue, fontSize = 11.sp)
                        }
                        AstralSwitch(checked = true) {}
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Info items
                SettingItem(Icons.Default.Info, AstralBlue, AstralBlue.copy(alpha = 0.15f), "버전", "Astral 2.0.0", onClick = {})
                SettingItem(Icons.Default.Code, AstralPurple, AstralPurple.copy(alpha = 0.15f), "오픈소스", "github.com/spear34000/astral", onClick = {})
                SettingItem(Icons.Default.Palette, AstralEmerald, AstralEmerald.copy(alpha = 0.15f), "테마", "우주 다크 모드", onClick = {})
            }

            Spacer(Modifier.height(36.dp))
            SectionHeader("외부 접속 주소 (API)", subtitle = "PC나 다른 앱에서 봇을 제어하는 주소")
            Spacer(Modifier.height(12.dp))

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "GET  /api/v1/ping" to "서버 상태 확인",
                    "GET  /api/v1/bots" to "봇 목록 보기",
                    "POST /api/v1/bots" to "봇 생성 {name, language}",
                    "GET  /api/v1/bots/:id" to "봇 상세 정보",
                    "POST /api/v1/bots/:id/toggle" to "봇 켜기/끄기",
                    "DELETE /api/v1/bots/:id" to "봇 삭제",
                    "POST /api/v1/flows/compile" to "플로우 → 코드 변환",
                    "POST /api/v1/message" to "메시지 전송 {room, message}",
                    "GET  /api/v1/plugins" to "플러그인 목록",
                    "POST /api/v1/server/restart" to "서버 재시작 {port}",
                ).forEach { (endpoint, desc) ->
                    Row {
                        Text(endpoint, fontSize = 11.sp, color = AstralCyan, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        Spacer(Modifier.width(8.dp))
                        Text(desc, fontSize = 11.sp, color = TextTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector, iconTint: Color, iconBg: Color,
    title: String, subtitle: String, onClick: () -> Unit,
) {
    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Text(subtitle, fontSize = 13.sp, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}
