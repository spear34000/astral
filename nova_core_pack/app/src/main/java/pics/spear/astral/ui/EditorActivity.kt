package pics.spear.astral.ui

import android.os.Bundle
import android.widget.Toast
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import pics.spear.astral.store.BotScriptStore
import pics.spear.astral.script.AstralScriptRuntime
import pics.spear.astral.prefs.AstralPrefs
import pics.spear.astral.model.ErrorLog
import pics.spear.astral.store.ErrorStore
import pics.spear.astral.ui.editor.ScriptLanguage
import pics.spear.astral.ui.editor.TextMateRegistry
import pics.spear.astral.ui.theme.AstralTheme
import pics.spear.astral.ui.components.AstralScreen
import pics.spear.astral.ui.components.VanguardCard
import java.util.UUID
import pics.spear.astral.util.AstralStorage
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import kotlin.math.max
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val botId = intent.getStringExtra("botId") ?: return finish()
        val botName = intent.getStringExtra("botName") ?: "Unknown Bot"
        val botLang = intent.getStringExtra("botLang") ?: "js"

        setContent {
            AstralTheme {
                AdvancedEditorScreen(
                    botId = botId,
                    botName = botName,
                    botLang = botLang,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedEditorScreen(botId: String, botName: String, botLang: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var codeState by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var searchCaseInsensitive by remember { mutableStateOf(false) }
    var searchRegex by remember { mutableStateOf(false) }
    var searchWholeWord by remember { mutableStateOf(false) }
    var searchMatchCount by remember { mutableStateOf(0) }
    var searchMatchIndex by remember { mutableStateOf(0) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var showLibrary by remember { mutableStateOf(false) }
    var showNewFile by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var showRenameFile by remember { mutableStateOf(false) }
    var pendingFileName by remember { mutableStateOf("") }
    var pendingFolderName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var newFolderParent by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var files by remember { mutableStateOf<List<String>>(emptyList()) }
    var entryFile by remember { mutableStateOf<String?>(null) }
    var currentFile by remember { mutableStateOf<String?>(null) }
    var openFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var syntaxStatus by remember { mutableStateOf<String?>(null) }
    var syntaxCheckJob by remember { mutableStateOf<Job?>(null) }
    var lastSyntaxSignature by remember { mutableStateOf<String?>(null) }
    var liveCheckJob by remember { mutableStateOf<Job?>(null) }
    var showProblems by remember { mutableStateOf(false) }
    var diagnostics by remember { mutableStateOf<List<DiagnosticItem>>(emptyList()) }
    var editorRef by remember { mutableStateOf<CodeEditor?>(null) }
    var terminalSessions by remember {
        mutableStateOf(listOf(TerminalSession(id = UUID.randomUUID().toString(), name = "세션 1")))
    }
    var activeTerminalId by remember { mutableStateOf(terminalSessions.first().id) }
    val terminalJobs = remember { mutableStateMapOf<String, Job?>() }
    TextMateRegistry.ensureInitialized(context.applicationContext)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scopeName = remember(botLang) {
        if (botLang.lowercase() == "py" || botLang.lowercase() == "python") {
            "source.python"
        } else {
            "source.js"
        }
    }
    val editorFontSize = remember { AstralPrefs.getEditorFontSize(context) }
    val editorWordWrap = remember { AstralPrefs.getEditorWordWrap(context) }
    val editorLineNumbers = remember { AstralPrefs.getEditorLineNumbers(context) }
    val language = remember(botLang) {
        val base = TextMateLanguage.create(scopeName, true)
        if (botLang.lowercase() == "py" || botLang.lowercase() == "python") {
            ScriptLanguage(base, ScriptLanguage.Kind.PYTHON)
        } else {
            ScriptLanguage(base, ScriptLanguage.Kind.JAVASCRIPT)
        }
    }

    fun refreshSearchStats(editor: CodeEditor?) {
        val searcher = editor?.getSearcher() ?: return
        if (!searcher.hasQuery()) {
            searchMatchCount = 0
            searchMatchIndex = 0
            return
        }
        val count = runCatching { searcher.matchedPositionCount }.getOrNull() ?: 0
        val index = runCatching { searcher.currentMatchedPositionIndex }.getOrNull() ?: -1
        searchMatchCount = count
        searchMatchIndex = if (index >= 0) index + 1 else 0
    }

    fun applyDiagnostics(editor: CodeEditor, message: String?, isError: Boolean) {
        val container = DiagnosticsContainer(true)
        if (message.isNullOrBlank() || !isError) {
            editor.setDiagnostics(container)
            return
        }

        val lineRegex = Regex("Line\\s+(\\d+)(?::(\\d+))?")
        val pathRegex = Regex("(/[^:\\s]+):(\\d+)(?::(\\d+))?")
        val lineMatch = lineRegex.find(message)
        val pathMatch = if (lineMatch == null) pathRegex.find(message) else null
        val line = when {
            lineMatch != null -> lineMatch.groupValues.getOrNull(1)?.toIntOrNull()
            pathMatch != null -> pathMatch.groupValues.getOrNull(2)?.toIntOrNull()
            else -> null
        }
        val col = when {
            lineMatch != null -> lineMatch.groupValues.getOrNull(2)?.toIntOrNull()
            pathMatch != null -> pathMatch.groupValues.getOrNull(3)?.toIntOrNull()
            else -> null
        }
        val text = editor.text
        if (line == null) {
            editor.setDiagnostics(container)
            return
        }

        val safeLine = (line - 1).coerceIn(0, text.lineCount - 1)
        val lineText = text.getLine(safeLine).toString()
        val columnCount = lineText.length
        val colIndex = (col?.minus(1) ?: 0).coerceIn(0, columnCount)
        val startIndex = text.getCharIndex(safeLine, colIndex)
        val endIndex = if (col != null && colIndex < columnCount) {
            val tokenEnd = run {
                var i = colIndex
                while (i < columnCount && (lineText[i].isLetterOrDigit() || lineText[i] == '_' || lineText[i] == '.')) {
                    i++
                }
                if (i == colIndex) (colIndex + 1).coerceAtMost(columnCount) else i
            }
            text.getCharIndex(safeLine, tokenEnd)
        } else {
            text.getCharIndex(safeLine, columnCount)
        }
        container.addDiagnostic(
            DiagnosticRegion(startIndex, max(startIndex + 1, endIndex), DiagnosticRegion.SEVERITY_ERROR)
        )
        editor.setDiagnostics(container)
    }

    fun parseDiagnostics(output: String): List<DiagnosticItem> {
        if (output.isBlank()) return emptyList()
        val items = mutableListOf<DiagnosticItem>()
        val lineRegex = Regex("Line\\s+(\\d+)(?::(\\d+))?:\\s*(.*)")
        val pathRegex = Regex("(/[^:\\s]+):(\\d+)(?::(\\d+))?")
        var hasLocation = false
        output.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            if (trimmed.startsWith("Syntax (js):", true) || trimmed.startsWith("Syntax (py):", true)) return@forEach
            if (trimmed.startsWith("at ")) return@forEach
            if (trimmed.contains("(internal/")) return@forEach
            if (trimmed.all { it == '^' }) return@forEach

            val match = lineRegex.find(trimmed)
            if (match != null) {
                val lineNum = match.groupValues[1].toIntOrNull() ?: 0
                val colNum = match.groupValues[2].toIntOrNull() ?: 0
                val msg = match.groupValues[3].ifBlank { trimmed }
                val severity = if (msg.contains("warning", true)) DiagnosticSeverity.Warning else DiagnosticSeverity.Error
                items.add(DiagnosticItem(lineNum, colNum, msg, severity))
                hasLocation = true
                return@forEach
            }
            val pathMatch = pathRegex.find(trimmed)
            if (pathMatch != null) {
                val path = pathMatch.groupValues[1]
                if (!path.contains("/internal/")) {
                    val lineNum = pathMatch.groupValues[2].toIntOrNull() ?: 0
                    val colNum = pathMatch.groupValues[3].toIntOrNull() ?: 0
                    val msg = trimmed
                    items.add(DiagnosticItem(lineNum, colNum, msg, DiagnosticSeverity.Error))
                    hasLocation = true
                }
            } else if (!hasLocation && (trimmed.contains("SyntaxError") || trimmed.contains("IndentationError"))) {
                items.add(DiagnosticItem(0, 0, trimmed, DiagnosticSeverity.Error))
            }
        }
        return items
    }

    suspend fun remapSyntaxOutput(context: Context, botId: String, language: String, output: String): String {
        return try {
            val workspace = BotScriptStore.getWorkspaceDir(context, botId)
            val entry = BotScriptStore.getEntry(context, botId, language)
            val entryPath = File(workspace, entry).absolutePath.replace("\\", "/")
            output
                .replace(Regex("/root/workspace_[^:\\s]+"), workspace.absolutePath.replace("\\", "/"))
                .replace(Regex("/root/script_[^:\\s]+\\.(js|py)"), entryPath)
                .replace(Regex("/root/wrapper_[^:\\s]+\\.(js|py)"), entryPath)
                .replace("/root/script_global.js", entryPath)
                .replace("/root/script_global.py", entryPath)
        } catch (_: Exception) {
            output
        }
    }

    fun updateSession(id: String, transform: (TerminalSession) -> TerminalSession) {
        terminalSessions = terminalSessions.map { session ->
            if (session.id == id) transform(session) else session
        }
    }

    fun addSession() {
        val next = TerminalSession(id = UUID.randomUUID().toString(), name = "세션 ${terminalSessions.size + 1}")
        terminalSessions = terminalSessions + next
        activeTerminalId = next.id
    }

    fun closeSession(id: String) {
        if (terminalSessions.size <= 1) return
        val next = terminalSessions.filterNot { it.id == id }
        terminalSessions = next
        if (activeTerminalId == id) {
            activeTerminalId = next.first().id
        }
    }

    LaunchedEffect(botId) {
        val entry = BotScriptStore.getEntry(context, botId, botLang)
        entryFile = entry
        currentFile = entry
        openFiles = listOf(entry)
        val code = BotScriptStore.readFile(context, botId, entry)
        codeState = code
        editorRef?.setText(code)
        files = BotScriptStore.listFiles(context, botId)
    }

    LaunchedEffect(showSearch, searchQuery, searchRegex, searchCaseInsensitive, searchWholeWord, editorRef) {
        val editor = editorRef ?: return@LaunchedEffect
        val searcher = editor.getSearcher()
        if (!showSearch || searchQuery.isBlank()) {
            searcher.stopSearch()
            refreshSearchStats(editor)
            searchError = null
            return@LaunchedEffect
        }
        val type = when {
            searchRegex -> EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION
            searchWholeWord -> EditorSearcher.SearchOptions.TYPE_WHOLE_WORD
            else -> EditorSearcher.SearchOptions.TYPE_NORMAL
        }
        runCatching {
            searcher.search(searchQuery, EditorSearcher.SearchOptions(type, searchCaseInsensitive))
            searchError = null
        }.onFailure { err ->
            searchError = err.message ?: "검색 실패"
        }
    }

    AstralScreen {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                WorkspaceDrawer(
                    botName = botName,
                    files = files,
                    currentFile = currentFile,
                    onOpen = { filePath ->
                        scope.launch {
                            val activePath = currentFile ?: entryFile
                            if (activePath != null) {
                                val currentCode = editorRef?.text?.toString() ?: codeState
                                BotScriptStore.writeFile(context, botId, activePath, currentCode)
                            }
                            val code = BotScriptStore.readFile(context, botId, filePath)
                            currentFile = filePath
                            codeState = code
                            editorRef?.setText(code)
                            if (!openFiles.contains(filePath)) {
                                openFiles = openFiles + filePath
                            }
                            drawerState.close()
                        }
                    },
                    onCreate = {
                        pendingFileName = ""
                        showNewFile = true
                    },
                    onCreateFolder = {
                        pendingFolderName = ""
                        newFolderParent = null
                        showNewFolder = true
                    },
                    onRefresh = {
                        scope.launch {
                            files = BotScriptStore.listFiles(context, botId)
                        }
                    },
                    onRequestNewFile = { basePath ->
                        pendingFileName = if (basePath.isBlank()) "" else "$basePath/"
                        showNewFile = true
                    },
                    onRequestNewFolder = { basePath ->
                        pendingFolderName = ""
                        newFolderParent = basePath.ifBlank { null }
                        showNewFolder = true
                    },
                    onRequestRename = { targetPath ->
                        renameTarget = targetPath
                        pendingFileName = targetPath
                        showRenameFile = true
                    },
                    onRequestDelete = { targetPath ->
                        deleteTarget = targetPath
                        showDeleteConfirm = true
                    }
                )
            }
        ) {
            val sheetState = rememberBottomSheetScaffoldState()
            BottomSheetScaffold(
                scaffoldState = sheetState,
                containerColor = Color.Transparent,
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                sheetPeekHeight = 24.dp,
                sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                sheetContent = {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                        val activeSession = terminalSessions.firstOrNull { it.id == activeTerminalId } ?: terminalSessions.first()
                        TerminalPanel(
                            sessions = terminalSessions,
                            activeId = activeSession.id,
                            onSelect = { activeTerminalId = it },
                            onAdd = { addSession() },
                            onCompile = {
                                if (activeSession.busy) return@TerminalPanel
                                val sessionId = activeSession.id
                                updateSession(sessionId) { it.copy(busy = true, output = it.output + "↻ 컴파일 시작") }
                                val compileJob = scope.launch {
                                    try {
                                        val currentCode = editorRef?.text?.toString() ?: codeState
                                        val selectedFile = currentFile ?: entryFile
                                        if (selectedFile != null) {
                                            BotScriptStore.writeFile(context, botId, selectedFile, currentCode)
                                        }

                                        val langLower = botLang.lowercase()
                                        val runtime = AstralScriptRuntime.getInstance(context)
                                        val check = withContext(Dispatchers.IO) {
                                            if (langLower == "py" || langLower == "python") {
                                                runtime.checkPythonSyntax(botId, currentCode)
                                            } else {
                                                runtime.checkJavaScriptSyntax(botId, currentCode)
                                            }
                                        }

                                        val rawResult = check.getOrNull()?.trim().orEmpty()
                                        val errorMessage = check.exceptionOrNull()?.message
                                            ?.removePrefix("Command failed: ")
                                            ?.trim()
                                        val combined = when {
                                            !errorMessage.isNullOrBlank() -> errorMessage
                                            rawResult.isNotBlank() -> rawResult
                                            else -> "OK"
                                        }

                                        val mapped = remapSyntaxOutput(context, botId, botLang, combined)
                                        val success = check.isSuccess && (mapped.equals("OK", true) || mapped.isBlank())

                                        syntaxStatus = if (success) "컴파일 성공" else "컴파일 오류"

                                        if (success) {
                                            diagnostics = emptyList()
                                            editorRef?.let { applyDiagnostics(it, "", false) }
                                        } else {
                                            diagnostics = parseDiagnostics(mapped)
                                            editorRef?.let { applyDiagnostics(it, mapped, true) }
                                        }

                                        val displayLines = when {
                                            success -> listOf("✅ 컴파일 성공", "결과: OK")
                                            else -> {
                                                val lines = mapped.lines().map { it.trim() }.filter { it.isNotBlank() }
                                                if (lines.isEmpty()) listOf("❌ 컴파일 실패") else listOf("❌ 컴파일 실패") + lines
                                            }
                                        }

                                        updateSession(sessionId) { it.copy(output = it.output + displayLines, busy = false) }
                                    } catch (e: Exception) {
                                        val message = e.message ?: "컴파일 중 오류 발생"
                                        syntaxStatus = "컴파일 오류"
                                        updateSession(sessionId) { it.copy(output = it.output + listOf("❌ 컴파일 실패", message), busy = false) }
                                    } finally {
                                        terminalJobs[sessionId] = null
                                    }
                                }
                                terminalJobs[sessionId] = compileJob
                            },
                            onClose = { closeSession(it) },
                            onCommandChange = { value ->
                                updateSession(activeSession.id) { it.copy(command = value) }
                            },
                            onRun = {
                                if (activeSession.command.isBlank() || activeSession.busy) return@TerminalPanel
                                val sessionId = activeSession.id
                                val command = activeSession.command
                                updateSession(sessionId) { it.copy(command = "", busy = true, output = it.output + "$ $command") }
                                val job = scope.launch {
                                    val result = AstralScriptRuntime.getInstance(context).executeUserlandCommandStreaming(command, botId, currentFile) { line ->
                                        scope.launch {
                                            updateSession(sessionId) { it.copy(output = it.output + line) }
                                        }
                                    }
                                    if (result.isFailure) {
                                        val message = result.exceptionOrNull()?.message ?: "Command failed"
                                        updateSession(sessionId) { it.copy(output = it.output + message) }
                                    }
                                    updateSession(sessionId) { it.copy(busy = false) }
                                }
                                terminalJobs[sessionId] = job
                            },
                            onStop = {
                                val sessionId = activeSession.id
                                terminalJobs[sessionId]?.cancel()
                                terminalJobs[sessionId] = null
                                updateSession(sessionId) { it.copy(busy = false, output = it.output + "(stopped)") }
                            },
                            onClear = {
                                updateSession(activeSession.id) { it.copy(output = emptyList()) }
                            }
                        )
                    }
                },
                topBar = {
                    val languageLabel = if (botLang.lowercase() == "py" || botLang.lowercase() == "python") "PY" else "JS"
                    val activeFile = currentFile ?: entryFile ?: "-"
                    val fileName = activeFile.substringAfterLast('/')
                    val filePath = activeFile.substringBeforeLast('/', missingDelimiterValue = "")
                    VanguardCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), borderAlpha = 0.2f) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(fileName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(filePath.ifBlank { botName }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                ModePill(label = languageLabel)
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { showSearch = !showSearch }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { showProblems = true }) {
                                    val hasIssues = diagnostics.isNotEmpty()
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Problems",
                                        tint = if (hasIssues) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (diagnostics.isNotEmpty()) {
                                    Text(
                                        "${diagnostics.size}",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    IconButton(onClick = {
                                        scope.launch {
                                            isSaving = true
                                            val currentCode = editorRef?.text?.toString() ?: codeState
                                            val selectedFile = currentFile ?: entryFile
                                            if (selectedFile != null) {
                                                BotScriptStore.writeFile(context, botId, selectedFile, currentCode)
                                            }
                                            val lang = botLang.lowercase()
                                            val supportsSyntax = lang == "py" || lang == "python" || lang == "js" || lang == "javascript"
                                            isSaving = false
                                            if (supportsSyntax && selectedFile != null) {
                                                val signature = "$selectedFile:${currentCode.hashCode()}"
                                                if (signature != lastSyntaxSignature) {
                                                    syntaxCheckJob?.cancel()
                                                    syntaxCheckJob = scope.launch {
                                                        delay(1200)
                                                        lastSyntaxSignature = signature
                                                        val check = if (lang == "py" || lang == "python") {
                                                            AstralScriptRuntime.getInstance(context).checkPythonSyntax(botId, currentCode)
                                                        } else {
                                                            AstralScriptRuntime.getInstance(context).checkJavaScriptSyntax(botId, currentCode)
                                                        }
                                                        val output = check.getOrNull()?.trim().orEmpty()
                                                        val cleanOutput = output.removePrefix("Command failed: ").trim()
                                                    val errorMessage = check.exceptionOrNull()?.message
                                                        ?.removePrefix("Command failed: ")
                                                        ?.trim()
                                                        ?.takeIf { it.isNotBlank() }
                                                        ?: cleanOutput.takeIf { it.isNotBlank() && !it.equals("OK", true) }
                                                    val mapped = remapSyntaxOutput(context, botId, botLang, errorMessage ?: cleanOutput)
                                                    syntaxStatus = if (check.isSuccess && errorMessage == null) {
                                                        "문법 검사: 이상 없음"
                                                    } else {
                                                        val cleanError = mapped.ifBlank { "알 수 없는 문법 오류" }
                                                        ErrorStore.append(
                                                            context,
                                                            ErrorLog(
                                                                ts = System.currentTimeMillis(),
                                                                botName = botName,
                                                                error = "Syntax ($botLang): $cleanError"
                                                            )
                                                        )
                                                        "문법 오류: $cleanError"
                                                    }
                                                    diagnostics = if (check.isSuccess && errorMessage == null) emptyList() else parseDiagnostics(mapped)
                                                    editorRef?.let { applyDiagnostics(it, mapped, check.isFailure || errorMessage != null) }
                                                    Toast.makeText(context, if (check.isSuccess) "문법 검사 완료" else "문법 오류 발견", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                syntaxStatus = null
                                                editorRef?.setDiagnostics(DiagnosticsContainer(true))
                                            }
                                            Toast.makeText(context, "저장 완료", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.Save, contentDescription = "저장", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            if (openFiles.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    openFiles.forEach { filePath ->
                                        VscTab(
                                            title = filePath,
                                            active = filePath == currentFile,
                                            onSelect = {
                                                scope.launch {
                                                    val currentCode = editorRef?.text?.toString() ?: codeState
                                                    val activePath = currentFile ?: entryFile
                                                    if (activePath != null) {
                                                        BotScriptStore.writeFile(context, botId, activePath, currentCode)
                                                    }
                                                    val code = BotScriptStore.readFile(context, botId, filePath)
                                                    currentFile = filePath
                                                    codeState = code
                                                    editorRef?.setText(code)
                                                }
                                            },
                                            onClose = {
                                                val nextTabs = openFiles.filterNot { it == filePath }
                                                openFiles = nextTabs
                                                if (currentFile == filePath) {
                                                    val fallback = nextTabs.firstOrNull() ?: entryFile
                                                    if (fallback != null) {
                                                        scope.launch {
                                                            val code = BotScriptStore.readFile(context, botId, fallback)
                                                            currentFile = fallback
                                                            codeState = code
                                                            editorRef?.setText(code)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {

                // Search bar
                AnimatedVisibility(
                    visible = showSearch,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SearchBar(
                        query = searchQuery,
                        replace = replaceQuery,
                        matchInfo = if (searchMatchCount > 0) "${searchMatchIndex}/${searchMatchCount}" else "0",
                        errorMessage = searchError,
                        onQueryChange = { searchQuery = it },
                        onReplaceChange = { replaceQuery = it },
                        onNext = {
                            editorRef?.getSearcher()?.gotoNext()
                            refreshSearchStats(editorRef)
                        },
                        onPrev = {
                            editorRef?.getSearcher()?.gotoPrevious()
                            refreshSearchStats(editorRef)
                        },
                        onReplace = {
                            editorRef?.getSearcher()?.replaceCurrentMatch(replaceQuery)
                            refreshSearchStats(editorRef)
                        },
                        onReplaceAll = {
                            val editor = editorRef ?: return@SearchBar
                            editor.getSearcher().replaceAll(replaceQuery, Runnable {
                                refreshSearchStats(editor)
                            })
                        },
                        caseInsensitive = searchCaseInsensitive,
                        regex = searchRegex,
                        wholeWord = searchWholeWord,
                        onCaseChange = { searchCaseInsensitive = it },
                        onRegexChange = { searchRegex = it },
                        onWholeWordChange = { searchWholeWord = it },
                        onClose = { showSearch = false }
                    )
                }
                
                // Code editor with line numbers
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            CodeEditor(ctx).apply {
                                val editor = this
                                editorRef = editor
                                setText(codeState)
                                setTextSize(editorFontSize)
                                isWordwrap = editorWordWrap
                                setLineNumberEnabled(editorLineNumbers)
                                colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                                setEditorLanguage(language)
                                getComponent(EditorAutoCompletion::class.java).isEnabled = true
                                subscribeAlways(PublishSearchResultEvent::class.java) {
                                    refreshSearchStats(editor)
                                }
                                subscribeAlways(ContentChangeEvent::class.java) {
                                    val active = currentFile ?: entryFile
                                    if (active == null) return@subscribeAlways
                                    val lang = botLang.lowercase()
                                    if (lang != "py" && lang != "python" && lang != "js" && lang != "javascript") return@subscribeAlways
                                    val code = editor.text.toString()
                                    val signature = "$active:${code.hashCode()}:$lang"
                                    liveCheckJob?.cancel()
                                    liveCheckJob = scope.launch {
                                        if (signature == lastSyntaxSignature) return@launch
                                        lastSyntaxSignature = signature
                                        val result = if (lang == "py" || lang == "python") {
                                            AstralScriptRuntime.getInstance(context).checkPythonSyntaxFast(botId, code)
                                        } else {
                                            AstralScriptRuntime.getInstance(context).checkJavaScriptSyntaxFast(botId, code)
                                        }
                                        val output = result.getOrNull()?.trim().orEmpty()
                                        val cleanOutput = output.removePrefix("Command failed: ").trim()
                                        val errorMessage = result.exceptionOrNull()?.message
                                            ?.removePrefix("Command failed: ")
                                            ?.trim()
                                            ?.takeIf { it.isNotBlank() }
                                            ?: cleanOutput.takeIf { it.isNotBlank() && !it.equals("OK", true) }
                                        val mapped = remapSyntaxOutput(context, botId, botLang, errorMessage ?: cleanOutput)
                                        syntaxStatus = if (result.isSuccess && errorMessage == null) {
                                            "문법 검사: 이상 없음"
                                        } else {
                                            "문법 오류: ${mapped.ifBlank { "알 수 없는 문법 오류" }}"
                                        }
                                        diagnostics = if (result.isSuccess && errorMessage == null) emptyList() else parseDiagnostics(mapped)
                                        applyDiagnostics(editor, mapped, result.isFailure || errorMessage != null)
                                    }
                                }
                            }
                        },
                        update = { editor ->
                            editorRef = editor
                            if (editor.editorLanguage !== language) {
                                editor.setEditorLanguage(language)
                            }
                        },
                        onRelease = { editor ->
                            editor.release()
                        }
                    )
                }
                
            }
        }
    }

    }

    if (showLibrary) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLibrary = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SnippetLibrary(language = botLang)
        }
    }

    if (showProblems) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showProblems = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Error/Warning", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${diagnostics.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (diagnostics.isEmpty()) {
                    Text("문법 오류 없음", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(diagnostics) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = if (item.severity == DiagnosticSeverity.Warning) Icons.Default.Warning else Icons.Default.Error
                                val tint = if (item.severity == DiagnosticSeverity.Warning) Color(0xFFF4B860) else MaterialTheme.colorScheme.error
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        if (item.line > 0) "#${item.line}" else "#?",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(item.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }


    if (showNewFile) {
        AlertDialog(
            onDismissRequest = { showNewFile = false },
            title = { Text("새 파일") },
            text = {
                OutlinedTextField(
                    value = pendingFileName,
                    onValueChange = { pendingFileName = it },
                    placeholder = { Text("예: utils.js") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val name = pendingFileName.trim()
                        if (name.isNotEmpty()) {
                            BotScriptStore.writeFile(context, botId, name, "")
                            files = BotScriptStore.listFiles(context, botId)
                            currentFile = name
                            codeState = ""
                            editorRef?.setText("")
                            if (!openFiles.contains(name)) {
                                openFiles = openFiles + name
                            }
                        }
                        showNewFile = false
                    }
                }) { Text("생성") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFile = false }) { Text("취소") }
            }
        )
    }

    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("새 폴더") },
            text = {
                OutlinedTextField(
                    value = pendingFolderName,
                    onValueChange = { pendingFolderName = it },
                    placeholder = {
                        val hintBase = newFolderParent?.let { "$it/" } ?: ""
                        Text("예: ${hintBase}utils")
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val name = pendingFolderName.trim()
                        val target = if (newFolderParent.isNullOrBlank()) name else "${newFolderParent}/${name}"
                        if (name.isNotEmpty()) {
                            val result = BotScriptStore.createFolder(context, botId, target)
                            if (result.isSuccess) {
                                files = BotScriptStore.listFiles(context, botId)
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "폴더 생성 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showNewFolder = false
                        newFolderParent = null
                    }
                }) { Text("생성") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewFolder = false
                    newFolderParent = null
                }) { Text("취소") }
            }
        )
    }

    if (showDeleteConfirm) {
        val target = deleteTarget
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                deleteTarget = null
            },
            title = { Text("삭제 확인") },
            text = { Text("'${target ?: ""}' 을(를) 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (target != null) {
                            val result = BotScriptStore.deletePath(context, botId, target)
                            if (result.isSuccess) {
                                files = BotScriptStore.listFiles(context, botId)
                                val prefix = if (target.endsWith("/")) target else "$target/"
                                openFiles = openFiles.filterNot { it == target || it.startsWith(prefix) }
                                val active = currentFile
                                if (active == target || (active != null && active.startsWith(prefix))) {
                                    currentFile = entryFile
                                    entryFile?.let { fallback ->
                                        val code = BotScriptStore.readFile(context, botId, fallback)
                                        codeState = code
                                        editorRef?.setText(code)
                                    }
                                }
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "삭제 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDeleteConfirm = false
                        deleteTarget = null
                    }
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    deleteTarget = null
                }) { Text("취소") }
            }
        )
    }

    if (showRenameFile) {
        AlertDialog(
            onDismissRequest = { showRenameFile = false },
            title = { Text("이름 변경") },
            text = {
                OutlinedTextField(
                    value = pendingFileName,
                    onValueChange = { pendingFileName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val target = renameTarget
                        val next = pendingFileName.trim()
                        if (target != null && next.isNotEmpty()) {
                            val result = BotScriptStore.renameFile(context, botId, target, next)
                            if (result.isSuccess) {
                                if (target == currentFile) currentFile = next
                                if (target == entryFile) {
                                    val set = BotScriptStore.setEntry(context, botId, botLang, next)
                                    if (set.isSuccess) {
                                        entryFile = next
                                    }
                                }
                                openFiles = openFiles.map { if (it == target) next else it }
                                files = BotScriptStore.listFiles(context, botId)
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "이름 변경 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showRenameFile = false
                    }
                }) { Text("변경") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFile = false }) { Text("취소") }
            }
        )
    }
}

@Composable
fun LineNumberColumn(lineCount: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        repeat(lineCount) { index ->
            Text(
                text = "${index + 1}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                ),
                modifier = Modifier
                    .defaultMinSize(minWidth = 32.dp)
                    .height(19.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    replace: String,
    matchInfo: String,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    caseInsensitive: Boolean,
    regex: Boolean,
    wholeWord: Boolean,
    onCaseChange: (Boolean) -> Unit,
    onRegexChange: (Boolean) -> Unit,
    onWholeWordChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    VanguardCard(modifier = Modifier.fillMaxWidth(), borderAlpha = 0.2f) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("탐색 콘솔", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.weight(1f))
            Text(matchInfo, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("코드 검색...") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPrev) { Text("이전", fontFamily = FontFamily.Monospace) }
            TextButton(onClick = onNext) { Text("다음", fontFamily = FontFamily.Monospace) }
            Spacer(modifier = Modifier.weight(1f))
            SearchToggle("대소", caseInsensitive, onCaseChange)
            SearchToggle("정규", regex, onRegexChange)
            SearchToggle("단어", wholeWord, onWholeWordChange)
        }

        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = replace,
            onValueChange = onReplaceChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("바꿀 내용...") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReplace, modifier = Modifier.weight(1f)) { Text("바꾸기") }
            Button(onClick = onReplaceAll, modifier = Modifier.weight(1f)) { Text("전체 바꾸기") }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
}

@Composable
private fun SearchToggle(label: String, selected: Boolean, onToggle: (Boolean) -> Unit) {
    TextButton(
        onClick = { onToggle(!selected) },
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(label, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun VscTab(
    title: String,
    active: Boolean,
    onSelect: () -> Unit,
    onClose: (() -> Unit)?
) {
    val context = LocalContext.current
    val fileName = title.substringAfterLast('/')
    val icon = fileIconFor(fileName)
    val tabShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    val background = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val borderColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Row(
        modifier = Modifier
            .clip(tabShape)
            .background(background)
            .border(1.dp, borderColor, tabShape)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon.assetPath != null) {
            val request = remember(icon.assetPath) {
                ImageRequest.Builder(context)
                    .data(icon.assetPath)
                    .decoderFactory(SvgDecoder.Factory())
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(icon.fallbackIcon, contentDescription = null, tint = icon.tint, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            fileName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onClose != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = "탭 닫기",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(14.dp)
                    .clickable(onClick = onClose)
            )
        }
    }
}

@Composable
private fun ModePill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private sealed class WorkspaceNode {
    data class Folder(val name: String, val path: String, val children: MutableList<WorkspaceNode>) : WorkspaceNode()
    data class File(val name: String, val path: String) : WorkspaceNode()
}

private fun buildWorkspaceTree(rootName: String, files: List<String>): WorkspaceNode.Folder {
    val root = WorkspaceNode.Folder(rootName, "", mutableListOf())
    files.forEach { rawPath ->
        val isDirEntry = rawPath.endsWith("/")
        val filePath = rawPath.trimEnd('/')
        val parts = filePath.split('/').filter { it.isNotBlank() }
        if (parts.isEmpty()) return@forEach
        var current = root
        parts.forEachIndexed { index, part ->
            val isLast = index == parts.lastIndex
            if (isLast) {
                if (isDirEntry) {
                    val existing = current.children
                        .filterIsInstance<WorkspaceNode.Folder>()
                        .firstOrNull { it.name == part }
                    if (existing == null) {
                        current.children.add(
                            WorkspaceNode.Folder(
                                name = part,
                                path = if (current.path.isBlank()) part else "${current.path}/$part",
                                children = mutableListOf()
                            )
                        )
                    }
                } else {
                    current.children.add(WorkspaceNode.File(part, filePath))
                }
            } else {
                val existing = current.children
                    .filterIsInstance<WorkspaceNode.Folder>()
                    .firstOrNull { it.name == part }
                val folder = existing ?: WorkspaceNode.Folder(
                    name = part,
                    path = if (current.path.isBlank()) part else "${current.path}/$part",
                    children = mutableListOf()
                ).also { current.children.add(it) }
                current = folder
            }
        }
    }
    sortWorkspaceFolder(root)
    return root
}

private fun sortWorkspaceFolder(folder: WorkspaceNode.Folder) {
    folder.children.sortWith { a, b ->
        val aIsFolder = a is WorkspaceNode.Folder
        val bIsFolder = b is WorkspaceNode.Folder
        if (aIsFolder != bIsFolder) {
            if (aIsFolder) -1 else 1
        } else {
            val aName = when (a) {
                is WorkspaceNode.Folder -> a.name
                is WorkspaceNode.File -> a.name
            }
            val bName = when (b) {
                is WorkspaceNode.Folder -> b.name
                is WorkspaceNode.File -> b.name
            }
            aName.lowercase().compareTo(bName.lowercase())
        }
    }
    folder.children.filterIsInstance<WorkspaceNode.Folder>().forEach { sortWorkspaceFolder(it) }
}

@Composable
private fun WorkspaceDrawer(
    botName: String,
    files: List<String>,
    currentFile: String?,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onCreateFolder: () -> Unit,
    onRefresh: () -> Unit,
    onRequestNewFile: (String) -> Unit,
    onRequestNewFolder: (String) -> Unit,
    onRequestRename: (String) -> Unit,
    onRequestDelete: (String) -> Unit
) {
    val tree = remember(files, botName) { buildWorkspaceTree(botName, files) }
    var expanded by remember { mutableStateOf(setOf("")) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("탐색기", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Refresh, contentDescription = "새로고침", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onCreate, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, contentDescription = "새 파일", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onCreateFolder, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Folder, contentDescription = "새 폴더", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("${files.count { !it.endsWith("/") }} 파일 · ${files.count { it.endsWith("/") }} 폴더", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))

        if (files.isEmpty()) {
            Text("파일이 없습니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                WorkspaceTreeNode(
                    node = tree,
                    depth = 0,
                    expanded = expanded,
                    currentFile = currentFile,
                    onToggle = { path ->
                        expanded = if (expanded.contains(path)) {
                            expanded - path
                        } else {
                            expanded + path
                        }
                    },
                    onOpen = onOpen,
                    onRequestNewFile = onRequestNewFile,
                    onRequestNewFolder = onRequestNewFolder,
                    onRequestRename = onRequestRename,
                    onRequestDelete = onRequestDelete
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun WorkspaceTreeNode(
    node: WorkspaceNode,
    depth: Int,
    expanded: Set<String>,
    currentFile: String?,
    onToggle: (String) -> Unit,
    onOpen: (String) -> Unit,
    onRequestNewFile: (String) -> Unit,
    onRequestNewFolder: (String) -> Unit,
    onRequestRename: (String) -> Unit,
    onRequestDelete: (String) -> Unit
) {
    when (node) {
        is WorkspaceNode.Folder -> {
            val isExpanded = expanded.contains(node.path)
            var menuOpen by remember(node.path) { mutableStateOf(false) }
            val caretIcon = if (isExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight
            val folderIcon = if (isExpanded) Icons.Outlined.FolderOpen else Icons.Outlined.Folder
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (depth * 12).dp, top = 4.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .combinedClickable(
                            onClick = { onToggle(node.path) },
                            onLongClick = { menuOpen = true }
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(caretIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(folderIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(node.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("새 파일") },
                        onClick = {
                            menuOpen = false
                            onRequestNewFile(node.path)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("새 폴더") },
                        onClick = {
                            menuOpen = false
                            onRequestNewFolder(node.path)
                        }
                    )
                    if (node.path.isNotBlank()) {
                        Divider()
                        DropdownMenuItem(
                            text = { Text("이름 변경") },
                            onClick = {
                                menuOpen = false
                                onRequestRename(node.path)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제") },
                            onClick = {
                                menuOpen = false
                                onRequestDelete(node.path)
                            }
                        )
                    }
                }
            }
            if (isExpanded) {
                node.children.forEach { child ->
                    WorkspaceTreeNode(
                        node = child,
                        depth = depth + 1,
                        expanded = expanded,
                        currentFile = currentFile,
                        onToggle = onToggle,
                        onOpen = onOpen,
                        onRequestNewFile = onRequestNewFile,
                        onRequestNewFolder = onRequestNewFolder,
                        onRequestRename = onRequestRename,
                        onRequestDelete = onRequestDelete
                    )
                }

            }
        }
        is WorkspaceNode.File -> {
            val isActive = node.path == currentFile
            val icon = fileIconFor(node.name)
            val context = LocalContext.current
            var menuOpen by remember(node.path) { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (depth * 12 + 16).dp, top = 2.dp, bottom = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else Color.Transparent)
                        .combinedClickable(
                            onClick = { onOpen(node.path) },
                            onLongClick = { menuOpen = true }
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon.assetPath != null) {
                        val request = remember(icon.assetPath) {
                            ImageRequest.Builder(context)
                                .data(icon.assetPath)
                                .decoderFactory(SvgDecoder.Factory())
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(icon.fallbackIcon, contentDescription = null, tint = icon.tint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        node.name,
                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("열기") },
                        onClick = {
                            menuOpen = false
                            onOpen(node.path)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("이름 변경") },
                        onClick = {
                            menuOpen = false
                            onRequestRename(node.path)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("삭제") },
                        onClick = {
                            menuOpen = false
                            onRequestDelete(node.path)
                        }
                    )
                }

            }
        }
    }
}

private data class FileIcon(val assetPath: String?, val fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector, val tint: Color)

private fun fileIconFor(name: String): FileIcon {
    val lower = name.lowercase()
    val ext = lower.substringAfterLast('.', "")
    return when {
        ext == "js" -> FileIcon("file:///android_asset/techicons/javascript.svg", Icons.Outlined.Code, Color(0xFFF0DB4F))
        ext == "ts" -> FileIcon("file:///android_asset/techicons/typescript.svg", Icons.Outlined.Code, Color(0xFF3178C6))
        ext == "py" -> FileIcon("file:///android_asset/techicons/python.svg", Icons.Outlined.Code, Color(0xFF3776AB))
        ext == "json" -> FileIcon("file:///android_asset/techicons/json.svg", Icons.Outlined.DataObject, Color(0xFFE9C46A))
        ext == "md" -> FileIcon("file:///android_asset/techicons/markdown.svg", Icons.Outlined.Article, Color(0xFF3B3B3B))
        ext == "html" || ext == "htm" -> FileIcon("file:///android_asset/techicons/html5.svg", Icons.Outlined.Code, Color(0xFFE34F26))
        ext == "css" -> FileIcon("file:///android_asset/techicons/css3.svg", Icons.Outlined.Code, Color(0xFF1572B6))
        ext == "xml" -> FileIcon("file:///android_asset/techicons/xml.svg", Icons.Outlined.DataObject, Color(0xFF8E24AA))
        ext == "yml" || ext == "yaml" -> FileIcon("file:///android_asset/techicons/yaml.svg", Icons.Outlined.Description, Color(0xFFCB171E))
        lower.startsWith(".env") -> FileIcon(null, Icons.Outlined.Settings, Color(0xFF3A9D8F))
        ext == "png" || ext == "jpg" || ext == "jpeg" || ext == "gif" -> FileIcon(null, Icons.Outlined.Image, Color(0xFF6C757D))
        ext == "txt" -> FileIcon(null, Icons.Outlined.TextSnippet, Color(0xFF6D6875))
        ext == "log" -> FileIcon(null, Icons.Outlined.Description, Color(0xFF495057))
        ext == "ini" || ext == "conf" -> FileIcon(null, Icons.Outlined.Description, Color(0xFF495057))
        else -> FileIcon(null, Icons.Outlined.InsertDriveFile, Color(0xFF6B7280))
    }
}

@Composable
fun QuickKeysBar(onInsert: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickKeyButton("->") { onInsert("->") }
        QuickKeyButton("=>") { onInsert("=>") }
        QuickKeyButton("<") { onInsert("<") }
        QuickKeyButton(">") { onInsert(">") }
        QuickKeyButton("{") { onInsert("{") }
        QuickKeyButton("}") { onInsert("}") }
        QuickKeyButton("(") { onInsert("(") }
        QuickKeyButton(")") { onInsert(")") }
        QuickKeyButton("[") { onInsert("[") }
        QuickKeyButton("]") { onInsert("]") }
        QuickKeyButton(";") { onInsert(";") }
    }
}

@Composable
fun QuickKeyButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Text(label, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge)
    }
}


private suspend fun fetchNpmSearch(query: String): Result<List<PackageResult>> = withContext(Dispatchers.IO) {
    try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://registry.npmjs.org/-/v1/search?text=$encoded&size=6")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "Astral")

        val code = conn.responseCode
        if (code != 200) {
            return@withContext Result.failure(Exception("NPM 검색 실패: HTTP $code"))
        }

        val body = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(body)
        val objects = json.optJSONArray("objects") ?: JSONArray()

        val results = mutableListOf<PackageResult>()
        for (i in 0 until objects.length()) {
            val obj = objects.getJSONObject(i)
            val pkg = obj.getJSONObject("package")
            val name = pkg.optString("name")
            val version = pkg.optString("version")
            val desc = pkg.optString("description")
            val links = pkg.optJSONObject("links")
            val homepage = links?.optString("homepage") ?: links?.optString("repository") ?: ""
            val license = pkg.optString("license")
            results.add(
                PackageResult(
                    name = name,
                    version = version,
                    description = desc,
                    homepage = homepage,
                    license = license
                )
            )
        }

        Result.success(results)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun fetchPypiInfo(name: String): Result<List<PackageResult>> = withContext(Dispatchers.IO) {
    try {
        val encoded = URLEncoder.encode(name, "UTF-8")
        val url = URL("https://pypi.org/pypi/$encoded/json")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "Astral")

        val code = conn.responseCode
        if (code != 200) {
            return@withContext Result.failure(Exception("PyPI 검색 실패: HTTP $code"))
        }

        val body = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(body)
        val info = json.getJSONObject("info")
        val result = PackageResult(
            name = info.optString("name"),
            version = info.optString("version"),
            description = info.optString("summary"),
            homepage = info.optString("home_page"),
            license = info.optString("license")
        )

        Result.success(listOf(result))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private data class NpmPackage(val name: String, val description: String)
private data class PipPackage(val name: String, val description: String)
private data class PackageResult(
    val name: String,
    val version: String,
    val description: String,
    val homepage: String,
    val license: String
)

@Composable
fun SnippetLibrary(language: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var packageQuery by remember { mutableStateOf("") }
    var installStatus by remember { mutableStateOf<String?>(null) }
    var installBusy by remember { mutableStateOf(false) }
    var recoveryBusy by remember { mutableStateOf(false) }
    var recoveryStatus by remember { mutableStateOf<String?>(null) }
    var searchBusy by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<PackageResult>>(emptyList()) }
    val isPython = language.lowercase() == "py" || language.lowercase() == "python"
    val npmPackages = listOf(
        NpmPackage("axios", "HTTP client"),
        NpmPackage("lodash", "utility library"),
        NpmPackage("dayjs", "date/time utils"),
        NpmPackage("cheerio", "HTML parser"),
        NpmPackage("zod", "schema validation"),
        NpmPackage("dotenv", "env loader")
    )
    val pipPackages = listOf(
        PipPackage("requests", "HTTP client"),
        PipPackage("beautifulsoup4", "HTML parser"),
        PipPackage("pydantic", "schema validation"),
        PipPackage("python-dateutil", "date/time utils"),
        PipPackage("lxml", "fast XML/HTML"),
        PipPackage("pyflakes", "Syntax Linter")
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("ASTRAL PACKAGE LIBRARY", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Text("검색하면 패키지 정보가 바로 표시됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = packageQuery,
            onValueChange = { packageQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (isPython) "패키지 이름 (예: requests)" else "패키지 이름 (예: axios)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val query = packageQuery.trim()
                    if (query.isEmpty() || searchBusy) return@Button
                    searchBusy = true
                    searchError = null
                    scope.launch {
                        val result = if (isPython) fetchPypiInfo(query) else fetchNpmSearch(query)
                        searchBusy = false
                        if (result.isSuccess) {
                            searchResults = result.getOrDefault(emptyList())
                        } else {
                            searchResults = emptyList()
                            searchError = result.exceptionOrNull()?.message ?: "검색 실패"
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(if (searchBusy) "검색 중" else "검색", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = {
                    packageQuery = ""
                    searchResults = emptyList()
                    searchError = null
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("초기화", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (searchError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(searchError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(if (isPython) "검색 결과 (Python)" else "검색 결과 (Node.js)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))

            searchResults.forEach { pkg ->
                VanguardCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(pkg.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(pkg.description.ifBlank { "설명 없음" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        val version = pkg.version.trim().takeIf { it.isNotBlank() && it.lowercase() != "null" }
                        val license = pkg.license.trim().takeIf { it.isNotBlank() && it.lowercase() != "null" }
                        if (version != null || license != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (version != null) {
                                    TagChip("v$version", MaterialTheme.colorScheme.secondary)
                                }
                                if (license != null) {
                                    TagChip(license, MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (pkg.homepage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(pkg.homepage, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (installBusy) return@Button
                                installBusy = true
                                installStatus = "설치 중: ${pkg.name}"
                                scope.launch {
                                    val result = if (isPython) {
                                        AstralScriptRuntime.getInstance(context).installPipPackage(pkg.name)
                                    } else {
                                        AstralScriptRuntime.getInstance(context).installNpmPackage(pkg.name)
                                    }
                                    installBusy = false
                                    if (result.isSuccess) {
                                        installStatus = "설치 완료: ${pkg.name}"
                                    } else {
                                        val msg = result.exceptionOrNull()?.message ?: "unknown"
                                        installStatus = if (isPython && (msg.contains("python", true) || msg.contains("dpkg", true))) {
                                            "Python 환경 오류: 복구를 실행하세요"
                                        } else {
                                            "설치 실패: $msg"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text(if (installBusy) "설치 중..." else "설치", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        if (installStatus != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(installStatus ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }

        if (isPython) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (recoveryBusy) return@Button
                    recoveryBusy = true
                    recoveryStatus = "복구 중: userland 초기화"
                    scope.launch {
                        val result = AstralScriptRuntime.getInstance(context).recoverPythonEnvironment()
                        recoveryBusy = false
                        recoveryStatus = if (result.isSuccess) {
                            "복구 완료: Python 환경 준비됨"
                        } else {
                            "복구 실패: ${result.exceptionOrNull()?.message ?: "unknown"}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text(if (recoveryBusy) "복구 중..." else "Python 환경 복구", style = MaterialTheme.typography.labelLarge)
            }

            if (recoveryStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(recoveryStatus ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!isPython) {
            Text("NODE.JS 추천", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("필요한 Node.js 라이브러리를 즉시 설치하세요.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))

            npmPackages.forEach { pkg ->
                VanguardCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pkg.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                            Text(pkg.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(
                            onClick = {
                                if (installBusy) return@TextButton
                                packageQuery = pkg.name
                                installStatus = "설치 중: ${pkg.name}"
                                installBusy = true
                                scope.launch {
                                    val result = AstralScriptRuntime.getInstance(context).installNpmPackage(pkg.name)
                                    installBusy = false
                                    if (result.isSuccess) {
                                        installStatus = "설치 완료: ${pkg.name}"
                                    } else {
                                        installStatus = "설치 실패: ${result.exceptionOrNull()?.message ?: "unknown"}"
                                    }
                                }
                            }
                        ) {
                            Text("설치", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        } else {
            Text("PYTHON 추천", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("pip 패키지를 설치합니다. Python 런타임이 필요합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))

            pipPackages.forEach { pkg ->
                VanguardCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pkg.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                            Text(pkg.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(
                            onClick = {
                                if (installBusy) return@TextButton
                                packageQuery = pkg.name
                                installStatus = "설치 중: ${pkg.name}"
                                installBusy = true
                                scope.launch {
                                    val result = AstralScriptRuntime.getInstance(context).installPipPackage(pkg.name)
                                    installBusy = false
                                    if (result.isSuccess) {
                                        installStatus = "설치 완료: ${pkg.name}"
                                    } else {
                                        installStatus = "설치 실패: ${result.exceptionOrNull()?.message ?: "unknown"}"
                                    }
                                }
                            }
                        ) {
                            Text("설치", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun EditorActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun EditorToolButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(32.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
    }
}

private object EditorSyntaxColors {
    val keyword = Color(0xFFF4B860)
    val builtin = Color(0xFF8CC4FF)
    val api = Color(0xFF36D6B8)
    val string = Color(0xFF7DD38C)
    val comment = Color(0xFF5C6C7E)
    val number = Color(0xFFF2A45A)
    val function = Color(0xFFB4E38E)
    val nodeGlobal = Color(0xFF8FD3FF)
    val nodeModule = Color(0xFFB9E2A0)
    val searchBg = Color(0xFFFFE39A)
}

class EnhancedJsSyntaxHighlighter(private val searchQuery: String = "") : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(highlight(text.text), OffsetMapping.Identity)
    }

    private fun highlight(code: String): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        
        // Keywords
        applyRegex(builder, code, "\\b(var|let|const|function|if|else|for|while|switch|case|break|continue|return|async|await|new|class|extends|static|try|catch|finally|throw|typeof|instanceof|import|export|from)\\b", EditorSyntaxColors.keyword)

        // Built-in objects and values
        applyRegex(builder, code, "\\b(true|false|null|undefined|NaN|Infinity|this|super|Promise|Map|Set|Date|RegExp|Error|JSON|Math)\\b", EditorSyntaxColors.builtin)

        // Bot API
        applyRegex(builder, code, "\\b(bot|ctx|device|native|replier)\\b", EditorSyntaxColors.api)

        // Node.js globals
        applyRegex(builder, code, "\\b(require|module|exports|process|Buffer|__dirname|__filename|console|global|setTimeout|setInterval|clearTimeout|clearInterval|setImmediate|clearImmediate)\\b", EditorSyntaxColors.nodeGlobal)

        // Node.js core modules
        applyRegex(builder, code, "\\b(fs|path|http|https|net|os|crypto|stream|events|util|url|zlib|child_process|worker_threads)\\b", EditorSyntaxColors.nodeModule)

        // Strings
        applyRegex(builder, code, "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`([^`\\\\]|\\\\.)*`", EditorSyntaxColors.string)

        // Comments
        applyRegex(builder, code, "//.*|/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", EditorSyntaxColors.comment)

        // Numbers
        applyRegex(builder, code, "\\b\\d+(\\.\\d+)?\\b", EditorSyntaxColors.number)

        // Function names
        applyRegex(builder, code, "\\b([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\(", EditorSyntaxColors.function, captureGroup = 1)
        
        // Search highlighting
        if (searchQuery.isNotEmpty() && code.contains(searchQuery, ignoreCase = true)) {
            var startIndex = 0
            while (startIndex < code.length) {
                val index = code.indexOf(searchQuery, startIndex, ignoreCase = true)
                if (index == -1) break
                builder.addStyle(
                    SpanStyle(background = EditorSyntaxColors.searchBg.copy(alpha = 0.5f), color = Color.Black),
                    index,
                    index + searchQuery.length
                )
                startIndex = index + searchQuery.length
            }
        }

        return builder.toAnnotatedString()
    }

    private fun applyRegex(builder: AnnotatedString.Builder, code: String, regex: String, color: Color, captureGroup: Int = 0) {
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(code)
        while (matcher.find()) {
            val start = if (captureGroup > 0) matcher.start(captureGroup) else matcher.start()
            val end = if (captureGroup > 0) matcher.end(captureGroup) else matcher.end()
            builder.addStyle(SpanStyle(color = color), start, end)
        }
    }
}

data class AutocompleteItem(val text: String, val description: String = "")

private enum class DiagnosticSeverity { Error, Warning }

private data class DiagnosticItem(
    val line: Int,
    val column: Int,
    val message: String,
    val severity: DiagnosticSeverity
)

private data class TerminalSession(
    val id: String,
    val name: String,
    val command: String = "",
    val output: List<String> = emptyList(),
    val busy: Boolean = false
)

fun formatJavaScript(code: String): String {
    // Simple JS formatter - add proper indentation
    var indentLevel = 0
    val lines = code.lines()
    val formatted = StringBuilder()
    
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            formatted.append("\n")
            continue
        }
        
        // Decrease indent for closing braces
        if (trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]")) {
            indentLevel = maxOf(0, indentLevel - 1)
        }
        
        // Add indentation
        formatted.append("  ".repeat(indentLevel))
        formatted.append(trimmed)
        formatted.append("\n")
        
        // Increase indent for opening braces
        if (trimmed.endsWith("{") || trimmed.endsWith("(") || trimmed.endsWith("[")) {
            indentLevel++
        }
    }
    
    return formatted.toString().trimEnd()
}

@Composable
private fun TerminalPanel(
    sessions: List<TerminalSession>,
    activeId: String,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onCompile: () -> Unit,
    onClose: (String) -> Unit,
    onCommandChange: (String) -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit
) {
    val active = sessions.firstOrNull { it.id == activeId } ?: sessions.first()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val accentColor = Color(0xFF24E3C2)
    VanguardCard(modifier = Modifier.fillMaxWidth(), borderAlpha = 0.2f) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Message,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("디버그룸", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onCompile, enabled = !active.busy) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "컴파일", tint = accentColor)
                }
                TextButton(
                    onClick = {
                        val outputText = active.output.joinToString("\n")
                        clipboardManager.setText(AnnotatedString(outputText))
                        Toast.makeText(context, "출력이 복사되었습니다.", Toast.LENGTH_SHORT).show()
                    },
                    enabled = active.output.isNotEmpty()
                ) {
                    Text("복사", style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "세션 추가", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onClear) { Text("지우기", style = MaterialTheme.typography.labelLarge) }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sessions.forEach { session ->
                    val isActive = session.id == activeId
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable { onSelect(session.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(session.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                        if (sessions.size > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "세션 닫기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onClose(session.id) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            VanguardCard(modifier = Modifier.fillMaxWidth(), borderAlpha = 0.2f) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (active.output.isEmpty()) {
                        Text("디버그 출력이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(active.output) { line ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xFF0F1F25), Color(0xFF0B181D))
                                            )
                                        )
                                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(accentColor)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            line,
                                            color = Color(0xFFD2FFF6),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                BasicTextField(
                    value = active.command,
                    onValueChange = onCommandChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onRun() })
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRun,
                    enabled = active.command.isNotBlank() && !active.busy,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("실행")
                }
                Button(
                    onClick = onStop,
                    enabled = active.busy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("중지")
                }
            }
        }
    }
}
