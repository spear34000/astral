package pics.spear.astral.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import pics.spear.astral.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.launch
import pics.spear.astral.model.ErrorLog
import pics.spear.astral.store.ErrorStore
import pics.spear.astral.store.BotStore
import pics.spear.astral.store.BotScriptStore
import pics.spear.astral.ui.theme.AstralTheme
import pics.spear.astral.ui.components.AstralHeader
import pics.spear.astral.ui.components.AstralScreen
import pics.spear.astral.ui.components.ModernBentoCard
import java.text.SimpleDateFormat
import java.util.*
import java.io.File

class LogsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AstralTheme { LogsScreen() }
            }
        }
    }
}

@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf(emptyList<ErrorLog>()) }
    val fmt = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        val raw = ErrorStore.list(context)
        val bots = BotStore.list(context)
        val botByName = bots.associateBy { it.name.trim() }
        val mapped = raw.map { log ->
            val bot = botByName[log.botName.trim()]
            if (bot != null) {
                val remapped = remapErrorPaths(context, log.error, bot.id, bot.language)
                log.copy(error = remapped)
            } else {
                log
            }
        }
        logs = mapped.sortedByDescending { it.ts }
    }

    AstralScreen {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("로그", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("시스템 이벤트와 오류를 확인합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = {
                    scope.launch {
                        ErrorStore.clear(context)
                        logs = emptyList()
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "로그 삭제", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.raw.astral_trail)
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "총 ${logs.size}건",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (logs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Text("추적된 데이터가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(logs) { log ->
                        LogCard(log, fmt)
                    }
                }
            }
        }
    }
}

@Composable
fun LogCard(log: ErrorLog, fmt: SimpleDateFormat) {
    val isFatal = log.error.contains("fatal", ignoreCase = true) || log.error.contains("error", ignoreCase = true)
    val lines = log.error.lines()
    val highlight = BooleanArray(lines.size)
    for (i in lines.indices) {
        val trimmed = lines[i].trim()
        if (trimmed == "^") {
            highlight[i] = true
            if (i > 0) highlight[i - 1] = true
        }
        if (lines[i].contains("SyntaxError") || lines[i].contains("IndentationError")) {
            highlight[i] = true
        }
    }
    
    ModernBentoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    log.botName.uppercase(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    fmt.format(Date(log.ts)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(6.dp)
                        .background(if (isFatal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                lines.forEachIndexed { index, line ->
                    Text(
                        line,
                        color = if (highlight[index]) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private suspend fun remapErrorPaths(context: android.content.Context, error: String, botId: String, language: String): String {
    return try {
        val safeId = botId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val workspace = BotScriptStore.getWorkspaceDir(context, botId)
        val entry = BotScriptStore.getEntry(context, botId, language)
        val entryPath = File(workspace, entry).absolutePath.replace("\\", "/")
        error
            .replace("/root/workspace_$safeId", workspace.absolutePath)
            .replace("/root/script_$safeId.js", entryPath)
            .replace("/root/script_$safeId.py", entryPath)
            .replace("/root/wrapper_$safeId.js", entryPath)
            .replace("/root/wrapper_$safeId.py", entryPath)
            .replace("/root/script_global.js", entryPath)
            .replace("/root/script_global.py", entryPath)
    } catch (_: Exception) {
        error
    }
}
