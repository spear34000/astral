package pics.spear.astral.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import pics.spear.astral.ui.components.*
import pics.spear.astral.ui.theme.*
import pics.spear.astral.util.Permissions

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val hasNotifAccess = remember { Permissions.hasNotificationAccess(ctx) }

    AstralScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 52.dp),
        ) {
            SectionHeader("Settings", subtitle = "App configuration and info")

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingItem(
                    icon = Icons.Default.NotificationsActive,
                    iconTint = if (hasNotifAccess) Emerald60 else ErrorRed,
                    iconBg = if (hasNotifAccess) Emerald60.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                    title = "Notification Access",
                    subtitle = if (hasNotifAccess) "Enabled — listening for KakaoTalk" else "Required for bot functionality",
                    onClick = {
                        if (!hasNotifAccess) {
                            ctx.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    },
                )

                SettingItem(
                    icon = Icons.Default.Info,
                    iconTint = Blue60,
                    iconBg = Blue60.copy(alpha = 0.15f),
                    title = "Version",
                    subtitle = "Astral 2.0.0",
                    onClick = {},
                )

                SettingItem(
                    icon = Icons.Default.Code,
                    iconTint = Purple60,
                    iconBg = Purple60.copy(alpha = 0.15f),
                    title = "Open Source",
                    subtitle = "github.com/spear34000/astral",
                    onClick = {},
                )

                SettingItem(
                    icon = Icons.Default.Palette,
                    iconTint = Emerald60,
                    iconBg = Emerald60.copy(alpha = 0.15f),
                    title = "Theme",
                    subtitle = "Cosmic Dark",
                    onClick = {},
                )
            }

            Spacer(Modifier.height(36.dp))
            SectionHeader("About", subtitle = "Made with \u2764 by Team Stend")

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Astral is a lightweight KakaoTalk bot platform\nfor Android. Built with Jetpack Compose.",
                    color = TextTertiary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    GlassCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
