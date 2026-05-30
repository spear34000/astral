package pics.spear.astral.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pics.spear.astral.store.LogStore
import pics.spear.astral.ui.components.*
import pics.spear.astral.ui.theme.*

@Composable
fun LogsScreen(logStore: LogStore) {
    val logs by logStore.logs.collectAsState()
    val scope = rememberCoroutineScope()

    AstralScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 52.dp),
        ) {
            SectionHeader(
                title = "Logs",
                subtitle = if (logs.isNotEmpty()) "${logs.size} entr${if (logs.size != 1) "ies" else "y"}"
                else null,
                action = {
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = { scope.launch { logStore.clear() } }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                "Clear",
                                tint = ErrorRed,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            if (logs.isEmpty()) {
                EmptyState(
                    icon = {
                        Icon(Icons.Default.Terminal, null, tint = TextTertiary, modifier = Modifier.size(32.dp))
                    },
                    title = "No logs",
                    subtitle = "Bot activity and errors will appear here",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(logs.reversed(), key = { it.id }) { entry ->
                        LogCard(entry.botName, entry.message, entry.level, entry.timestamp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogCard(botName: String, message: String, level: String, timestamp: Long) {
    val levelColor = when (level) {
        "error" -> ErrorRed
        "warn" -> WarningAmber
        else -> Blue60
    }
    val levelBg = when (level) {
        "error" -> ErrorRed.copy(alpha = 0.12f)
        "warn" -> WarningAmber.copy(alpha = 0.12f)
        else -> Blue60.copy(alpha = 0.12f)
    }

    GlassCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(levelColor),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = botName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = levelColor,
                    )
                }
                Text(
                    text = formatTime(timestamp),
                    fontSize = 11.sp,
                    color = TextTertiary,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                lineHeight = 18.sp,
            )
        }
    }
}

private fun formatTime(ts: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    return "%02d:%02d:%02d".format(
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE),
        cal.get(java.util.Calendar.SECOND),
    )
}
