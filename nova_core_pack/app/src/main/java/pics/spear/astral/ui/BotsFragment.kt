package pics.spear.astral.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import pics.spear.astral.ui.components.AstralHeader
import pics.spear.astral.ui.components.AstralScreen
import pics.spear.astral.ui.components.VanguardCard
import pics.spear.astral.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import pics.spear.astral.model.BotMeta
import pics.spear.astral.prefs.AstralPrefs
import pics.spear.astral.store.BotScriptStore
import pics.spear.astral.store.BotStore
import pics.spear.astral.store.ErrorStore
import pics.spear.astral.ui.theme.AstralTheme
import pics.spear.astral.script.AstralScriptRuntime
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import pics.spear.astral.model.ErrorLog

class BotsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AstralTheme {
                    BotsScreen(
                        onEdit = { bot ->
                            startActivity(Intent(requireContext(), EditorActivity::class.java).apply {
                                putExtra("botId", bot.id)
                                putExtra("botName", bot.name)
                                putExtra("botLang", bot.language)
                            })
                        },
                        onDebug = { bot ->
                            startActivity(Intent(requireContext(), DebugActivity::class.java).apply {
                                putExtra("botId", bot.id)
                            })
                        }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BotsScreen(onEdit: (BotMeta) -> Unit, onDebug: (BotMeta) -> Unit) {
    val context = LocalContext.current
    var bots by remember { mutableStateOf(emptyList<BotMeta>()) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newBotName by remember { mutableStateOf("") }
    var newBotLang by remember { mutableStateOf("js") }
    var newBotEnabled by remember { mutableStateOf(true) }
    var newBotAutoStart by remember { mutableStateOf(true) }
    var newBotNotifyError by remember { mutableStateOf(true) }
    var sortMode by remember { mutableStateOf(AstralPrefs.getBotSortMode(context)) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var filterEnabled by remember { mutableStateOf(AstralPrefs.getBotFilterEnabled(context)) }
    var filterLang by remember { mutableStateOf(AstralPrefs.getBotFilterLang(context)) }
    var filterAutoStart by remember { mutableStateOf(AstralPrefs.getBotFilterAutoStart(context)) }
    var filterPriority by remember { mutableStateOf(AstralPrefs.getBotFilterPriority(context)) }
    val scope = rememberCoroutineScope()
    val runtime = remember { AstralScriptRuntime.getInstance(context) }
    var settingsTarget by remember { mutableStateOf<BotMeta?>(null) }
    var settingsAutoStart by remember { mutableStateOf(true) }
    var settingsNotify by remember { mutableStateOf(true) }

    fun load() { scope.launch { bots = BotStore.list(context) } }
    LaunchedEffect(Unit) { load() }

    val filteredBots = bots.filter { bot ->
        (filterEnabled == null || bot.enabled == filterEnabled) &&
        (filterLang == null || bot.language == filterLang) &&
        (filterAutoStart == null || bot.autoStart == filterAutoStart) &&
        (filterPriority == null || bot.priority == filterPriority)
    }

    val visibleBots = when (sortMode) {
        "name" -> filteredBots.sortedBy { it.name.lowercase(Locale.getDefault()) }
        "created" -> filteredBots.sortedByDescending { it.createdAt }
        "updated" -> filteredBots.sortedByDescending { it.updatedAt }
        "priority" -> filteredBots.sortedWith(compareByDescending<BotMeta> { it.priority }
            .thenBy { it.name.lowercase(Locale.getDefault()) })
        else -> filteredBots
    }

    AstralScreen {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            AstralHeader(
                title = "스크립트",
                subtitle = "자동화 시나리오를 관리하고 실행 상태를 확인하세요."
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChoiceChip(label = "전체", selected = filterEnabled == null && filterLang == null && filterAutoStart == null && filterPriority == null) {
                        filterEnabled = null
                        filterLang = null
                        filterAutoStart = null
                        filterPriority = null
                        AstralPrefs.setBotFilterEnabled(context, null)
                        AstralPrefs.setBotFilterLang(context, null)
                        AstralPrefs.setBotFilterAutoStart(context, null)
                        AstralPrefs.setBotFilterPriority(context, null)
                    }
                    ChoiceChip(label = "실행중", selected = filterEnabled == true) {
                        filterEnabled = true
                        AstralPrefs.setBotFilterEnabled(context, true)
                    }
                    ChoiceChip(label = "정지", selected = filterEnabled == false) {
                        filterEnabled = false
                        AstralPrefs.setBotFilterEnabled(context, false)
                    }
                    ChoiceChip(label = "JS", selected = filterLang == "js") {
                        filterLang = "js"
                        AstralPrefs.setBotFilterLang(context, "js")
                    }
                    ChoiceChip(label = "Python", selected = filterLang == "py") {
                        filterLang = "py"
                        AstralPrefs.setBotFilterLang(context, "py")
                    }
                    ChoiceChip(label = "자동", selected = filterAutoStart == true) {
                        filterAutoStart = true
                        AstralPrefs.setBotFilterAutoStart(context, true)
                    }
                    ChoiceChip(label = "수동", selected = filterAutoStart == false) {
                        filterAutoStart = false
                        AstralPrefs.setBotFilterAutoStart(context, false)
                    }
                    ChoiceChip(label = "높음", selected = filterPriority == 2) {
                        filterPriority = 2
                        AstralPrefs.setBotFilterPriority(context, 2)
                    }
                    ChoiceChip(label = "보통", selected = filterPriority == 1) {
                        filterPriority = 1
                        AstralPrefs.setBotFilterPriority(context, 1)
                    }
                    ChoiceChip(label = "낮음", selected = filterPriority == 0) {
                        filterPriority = 0
                        AstralPrefs.setBotFilterPriority(context, 0)
                    }
                }
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "정렬", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("이름순") }, onClick = {
                            sortMode = "name"
                            AstralPrefs.setBotSortMode(context, sortMode)
                            sortMenuExpanded = false
                        })
                        DropdownMenuItem(text = { Text("최근수정") }, onClick = {
                            sortMode = "updated"
                            AstralPrefs.setBotSortMode(context, sortMode)
                            sortMenuExpanded = false
                        })
                        DropdownMenuItem(text = { Text("생성일") }, onClick = {
                            sortMode = "created"
                            AstralPrefs.setBotSortMode(context, sortMode)
                            sortMenuExpanded = false
                        })
                        DropdownMenuItem(text = { Text("우선순위") }, onClick = {
                            sortMode = "priority"
                            AstralPrefs.setBotSortMode(context, sortMode)
                            sortMenuExpanded = false
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            VanguardCard(modifier = Modifier.fillMaxWidth(), borderAlpha = 0.2f) {
                val allEnabled = bots.isNotEmpty() && bots.all { it.enabled }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(R.raw.astral_header_wave)
                            .decoderFactory(SvgDecoder.Factory())
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .alpha(0.65f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("일괄 ON/OFF", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = allEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    bots.forEach { bot ->
                                        val updated = bot.copy(
                                            enabled = enabled,
                                            autoStart = if (enabled) bot.autoStart else false,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        BotStore.upsert(context, updated)
                                        if (enabled) {
                                            runtime.ensureBotRunning(updated)
                                        } else {
                                            runtime.stopBot(bot.id)
                                        }
                                    }
                                    load()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (visibleBots.isEmpty()) {
                EmptyBotsState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleBots, key = { it.id }) { bot ->
                        BotCard(
                            bot = bot,
                            expanded = expandedId == bot.id,
                            onToggleExpand = { expandedId = if (expandedId == bot.id) null else bot.id },
                            onToggle = { enabled ->
                                scope.launch {
                                    val updated = bot.copy(
                                        enabled = enabled,
                                        autoStart = if (enabled) bot.autoStart else false,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    BotStore.upsert(context, updated)
                                    if (enabled) {
                                        runtime.ensureBotRunning(updated)
                                    } else {
                                        runtime.stopBot(bot.id)
                                    }
                                    load()
                                }
                            },
                            onDelete = { scope.launch { BotStore.remove(context, bot.id); load() } },
                            onDebug = { onDebug(bot) },
                            onEdit = { onEdit(bot) },
                            onFlow = {
                                context.startActivity(Intent(context, FlowActivity::class.java).apply {
                                    putExtra("BOT_ID", bot.id)
                                })
                            },
                            onCompile = {
                                scope.launch {
                                    val language = bot.language
                                    val entryPath = withContext(Dispatchers.IO) { BotScriptStore.getEntry(context, bot.id, language) }
                                    val code = withContext(Dispatchers.IO) { BotScriptStore.readFile(context, bot.id, entryPath) }
                                    var primaryResult = withContext(Dispatchers.IO) {
                                        if (language.equals("py", true)) runtime.checkPythonSyntax(bot.id, code)
                                        else runtime.checkJavaScriptSyntax(bot.id, code)
                                    }
                                    var finalResult = primaryResult
                                    var infoMessage: String? = null
                                    if (!primaryResult.isSuccess && language.equals("js", true)) {
                                        val rawError = primaryResult.exceptionOrNull()?.message.orEmpty()
                                        if (rawError.contains("Cannot find module 'eslint'", ignoreCase = true)) {
                                            val fallback = withContext(Dispatchers.IO) { runtime.checkJavaScriptSyntaxFast(bot.id, code) }
                                            finalResult = fallback
                                            infoMessage = if (fallback.isSuccess) {
                                                "ESLint 설치가 아직 완료되지 않아 빠른 문법 검사만 진행했어요."
                                            } else {
                                                rawError
                                            }
                                        }
                                    }
                                    val feedback = finalResult.getOrNull()?.trim().takeUnless { it.isNullOrBlank() }
                                        ?: finalResult.exceptionOrNull()?.message?.removePrefix("Command failed: ")?.trim()?.takeIf { it.isNotBlank() }
                                        ?: "OK"
                                    val toastText = when {
                                        infoMessage.isNullOrBlank() -> feedback
                                        finalResult.isSuccess -> infoMessage + "\n결과: " + feedback
                                        feedback == infoMessage -> feedback
                                        else -> infoMessage + "\n" + feedback
                                    }
                                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                                    if (!finalResult.isSuccess) {
                                        ErrorStore.append(
                                            context,
                                            ErrorLog(
                                                ts = System.currentTimeMillis(),
                                                botName = bot.name,
                                                error = feedback,
                                                level = "error"
                                            )
                                        )
                                    }
                                }
                            },
                            onSettings = {
                                settingsTarget = bot
                                settingsAutoStart = bot.autoStart
                                settingsNotify = bot.notifyOnError
                            },
                            onPriority = { priority ->
                                scope.launch {
                                    BotStore.upsert(context, bot.copy(priority = priority, updatedAt = System.currentTimeMillis()))
                                    load()
                                }
                            }
                        )
                    }
                }
            }
        }
        
        // Avant-Garde FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }

        if (showAddDialog) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showAddDialog = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("새 스크립트", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newBotName,
                        onValueChange = { newBotName = it },
                        placeholder = { Text("스크립트 이름", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChoiceChip(label = "Node.js", selected = newBotLang == "js") { newBotLang = "js" }
                        ChoiceChip(label = "Python", selected = newBotLang == "py") { newBotLang = "py" }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingToggle("즉시 활성화", "생성 후 바로 실행합니다.", newBotEnabled) { newBotEnabled = it }
                    SettingToggle("자동 시작", "앱 시작 시 자동으로 실행합니다.", newBotAutoStart) { newBotAutoStart = it }
                    SettingToggle("에러 알림", "에러 발생 시 알림을 표시합니다.", newBotNotifyError) { newBotNotifyError = it }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (newBotName.isNotBlank()) {
                                scope.launch {
                                    val created = BotStore.create(
                                        context,
                                        newBotName,
                                        language = newBotLang,
                                        enabled = newBotEnabled,
                                        autoStart = newBotAutoStart,
                                        notifyOnError = newBotNotifyError,
                                        plugins = emptyList()
                                    )
                                    val template = BotScriptStore.templateFor(newBotLang, emptyList())
                                    BotScriptStore.save(context, created.id, template, newBotLang)
                                    if (newBotEnabled) {
                                        runtime.ensureBotRunning(created)
                                    }
                                    newBotName = ""
                                    newBotLang = "js"
                                    newBotEnabled = true
                                    newBotAutoStart = true
                                    newBotNotifyError = true
                                    showAddDialog = false
                                    load()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text("생성", style = MaterialTheme.typography.labelLarge)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        settingsTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { settingsTarget = null },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            BotStore.upsert(
                                context,
                                target.copy(
                                    autoStart = settingsAutoStart,
                                    notifyOnError = settingsNotify,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            load()
                        }
                        settingsTarget = null
                    }) {
                        Text("저장", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { settingsTarget = null }) {
                        Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                title = { Text("${target.name} 설정", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("자동 시작", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = settingsAutoStart,
                                onCheckedChange = { settingsAutoStart = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("에러 알림", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = settingsNotify,
                                onCheckedChange = { settingsNotify = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun BotCard(
    bot: BotMeta,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onDebug: () -> Unit,
    onEdit: () -> Unit,
    onFlow: () -> Unit,
    onCompile: () -> Unit,
    onSettings: () -> Unit,
    onPriority: (Int) -> Unit
) {
    VanguardCard(modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() }, borderAlpha = if (bot.enabled) 0.3f else 0.12f) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(bot.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "업데이트 ${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(bot.updatedAt))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                StatusBadge(text = if (bot.enabled) "실행중" else "정지", active = bot.enabled)
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    bot.enabled,
                    onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip(if (bot.language == "py") "Python" else "Node", MaterialTheme.colorScheme.primary)
                TagChip(if (bot.autoStart) "자동" else "수동", MaterialTheme.colorScheme.secondary)
                TagChip(
                    when (bot.priority) {
                        2 -> "높음"
                        1 -> "보통"
                        else -> "낮음"
                    },
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DebugActionTray(
                                enabled = bot.enabled,
                                onCompile = onCompile,
                                onDebug = onDebug,
                                onEdit = onEdit,
                                onFlow = onFlow,
                                onSettings = onSettings
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("우선순위", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(8.dp))
                        ChoiceChip(label = "높음", selected = bot.priority == 2) { onPriority(2) }
                        Spacer(Modifier.width(6.dp))
                        ChoiceChip(label = "보통", selected = bot.priority == 1) { onPriority(1) }
                        Spacer(Modifier.width(6.dp))
                        ChoiceChip(label = "낮음", selected = bot.priority == 0) { onPriority(0) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugActionTray(
    enabled: Boolean,
    onCompile: () -> Unit,
    onDebug: () -> Unit,
    onEdit: () -> Unit,
    onFlow: () -> Unit,
    onSettings: () -> Unit
) {
    val accent = Color(0xFF3A8BFF)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconPill(Icons.Outlined.Refresh, accent, enabled) { onCompile() }
        IconPill(Icons.Filled.Message, accent, true) { onDebug() }
        IconPill(Icons.Default.Edit, accent, true) { onEdit() }
        IconPill(Icons.Default.Share, accent, true) { onFlow() }
        IconPill(Icons.Default.Settings, accent, true) { onSettings() }
    }
}

@Composable
private fun IconPill(icon: ImageVector, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.35f
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = null, tint = tint.copy(alpha = alpha), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun TagChip(text: String, color: Color) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun StatusBadge(text: String, active: Boolean) {
    val bg = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text,
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun ChoiceChip(label: String, selected: Boolean, onSelect: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    Text(
        label,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
fun SettingToggle(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked,
            onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
fun EmptyBotsState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        VanguardCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("스크립트 없음", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "새 스크립트를 만들어 자동화를 시작하세요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AstralDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge) },
        text = { content() },
        confirmButton = { 
            Button(
                onClick = onConfirm, 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = RoundedCornerShape(8.dp)
            ) { 
                Text("확인", style = MaterialTheme.typography.labelLarge)
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            } 
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
    )
}
