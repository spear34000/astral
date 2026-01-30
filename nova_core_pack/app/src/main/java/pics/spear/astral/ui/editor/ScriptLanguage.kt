package pics.spear.astral.ui.editor

import android.os.Bundle
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.QuickQuoteHandler
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.lang.completion.SimpleSnippetCompletionItem
import io.github.rosemoe.sora.lang.completion.SnippetDescription
import io.github.rosemoe.sora.lang.completion.snippet.CodeSnippet
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch

class ScriptLanguage(
    private val base: Language,
    private val kind: Kind
) : Language {

    enum class Kind {
        JAVASCRIPT,
        PYTHON
    }

    private val autoComplete = IdentifierAutoComplete().apply {
        setKeywords(
            if (kind == Kind.PYTHON) PYTHON_KEYWORDS else JAVASCRIPT_KEYWORDS,
            false
        )
    }

    override fun getAnalyzeManager(): AnalyzeManager = base.analyzeManager

    override fun getInterruptionLevel(): Int = base.interruptionLevel

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        base.requireAutoComplete(content, position, publisher, extraArguments)

        val prefix = CompletionHelper.computePrefix(content, position) { ch ->
            isIdentifierPart(ch, kind)
        }

        autoComplete.requireAutoComplete(content, position, prefix, publisher, null)
        addApiCompletions(content, position, prefix, publisher)
        addSnippets(prefix, publisher)
    }

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        return base.getIndentAdvance(content, line, column)
    }

    override fun useTab(): Boolean = base.useTab()

    override fun getFormatter(): Formatter = base.formatter

    override fun getSymbolPairs(): SymbolPairMatch = base.symbolPairs

    override fun getNewlineHandlers(): Array<NewlineHandler>? = base.newlineHandlers

    override fun getQuickQuoteHandler(): QuickQuoteHandler? = base.quickQuoteHandler

    override fun destroy() {
        base.destroy()
    }

    private fun addApiCompletions(
        content: ContentReference,
        position: CharPosition,
        prefix: String,
        publisher: CompletionPublisher
    ) {
        val lineText = content.getLine(position.line).toString()
        val prefixStart = (position.column - prefix.length).coerceAtLeast(0)
        val before = lineText.substring(0, prefixStart)

        if (before.endsWith("bot.")) {
            addApiItems(prefix, publisher, listOf(
                "command" to "Register a command handler",
                "on" to "Listen to events",
                "prefix" to "Set command prefixes"
            ))
        }

        if (before.endsWith("ctx.")) {
            addApiItems(prefix, publisher, listOf(
                "reply" to "Reply to the message",
                "sender" to "Get sender info",
                "room" to "Get room name",
                "message" to "Get message text"
            ))
        }

        if (before.endsWith("device.")) {
            addApiItems(prefix, publisher, listOf(
                "toast" to "Show a toast message",
                "vibrate" to "Vibrate the device",
                "notification" to "Show notification"
            ))
        }
    }

    private fun addApiItems(
        prefix: String,
        publisher: CompletionPublisher,
        items: List<Pair<String, String>>
    ) {
        val prefixLength = prefix.length
        items.forEach { (label, desc) ->
            if (prefix.isEmpty() || label.startsWith(prefix)) {
                publisher.addItem(
                    SimpleCompletionItem(label, desc, prefixLength, label)
                        .kind(CompletionItemKind.Function)
                )
            }
        }
    }

    private fun addSnippets(prefix: String, publisher: CompletionPublisher) {
        if (prefix.isEmpty()) return
        when (kind) {
            Kind.JAVASCRIPT -> {
                if ("onmsg".startsWith(prefix)) {
                    publisher.addItem(
                        SimpleSnippetCompletionItem(
                            "onmsg",
                            "Snippet - bot.on message",
                            SnippetDescription(prefix.length, JS_ON_MESSAGE_SNIPPET, true)
                        )
                    )
                }
                if ("cmd".startsWith(prefix)) {
                    publisher.addItem(
                        SimpleSnippetCompletionItem(
                            "cmd",
                            "Snippet - bot.command",
                            SnippetDescription(prefix.length, JS_COMMAND_SNIPPET, true)
                        )
                    )
                }
                if ("reply".startsWith(prefix)) {
                    publisher.addItem(
                        SimpleSnippetCompletionItem(
                            "reply",
                            "Snippet - ctx.reply",
                            SnippetDescription(prefix.length, JS_REPLY_SNIPPET, true)
                        )
                    )
                }
            }
            Kind.PYTHON -> {
                if ("onmsg".startsWith(prefix)) {
                    publisher.addItem(
                        SimpleSnippetCompletionItem(
                            "onmsg",
                            "Snippet - bot.on message",
                            SnippetDescription(prefix.length, PY_ON_MESSAGE_SNIPPET, true)
                        )
                    )
                }
                if ("cmd".startsWith(prefix)) {
                    publisher.addItem(
                        SimpleSnippetCompletionItem(
                            "cmd",
                            "Snippet - bot.command",
                            SnippetDescription(prefix.length, PY_COMMAND_SNIPPET, true)
                        )
                    )
                }
                if ("reply".startsWith(prefix)) {
                    publisher.addItem(
                        SimpleSnippetCompletionItem(
                            "reply",
                            "Snippet - ctx.reply",
                            SnippetDescription(prefix.length, PY_REPLY_SNIPPET, true)
                        )
                    )
                }
            }
        }
    }

    private fun isIdentifierPart(ch: Char, kind: Kind): Boolean {
        if (ch == '_' || ch.isLetterOrDigit()) return true
        return kind == Kind.JAVASCRIPT && ch == '$'
    }

    companion object {
        private val JAVASCRIPT_KEYWORDS = arrayOf(
            "const", "let", "var", "function", "class", "extends", "new", "return", "if", "else",
            "for", "while", "switch", "case", "break", "continue", "try", "catch", "finally", "throw",
            "async", "await", "import", "export", "from", "typeof", "instanceof", "in", "of",
            "this", "super", "true", "false", "null", "undefined"
        )

        private val PYTHON_KEYWORDS = arrayOf(
            "def", "class", "return", "if", "elif", "else", "for", "while", "try", "except", "finally",
            "import", "from", "as", "pass", "break", "continue", "lambda", "with", "yield", "global",
            "nonlocal", "and", "or", "not", "in", "is", "True", "False", "None"
        )

        private val JS_ON_MESSAGE_SNIPPET: CodeSnippet = CodeSnippetParser.parse(
            "bot.on(\"message\", (ctx) => {\n    $0\n});"
        )
        private val JS_COMMAND_SNIPPET: CodeSnippet = CodeSnippetParser.parse(
            "bot.command(\"cmd\", (ctx) => {\n    $0\n});"
        )
        private val JS_REPLY_SNIPPET: CodeSnippet = CodeSnippetParser.parse(
            "ctx.reply(\"$0\");"
        )

        private val PY_ON_MESSAGE_SNIPPET: CodeSnippet = CodeSnippetParser.parse(
            "def on_message(ctx):\n    $0\n\nbot.on(\"message\", on_message)"
        )
        private val PY_COMMAND_SNIPPET: CodeSnippet = CodeSnippetParser.parse(
            "def on_command(ctx):\n    $0\n\nbot.command(\"cmd\", on_command)"
        )
        private val PY_REPLY_SNIPPET: CodeSnippet = CodeSnippetParser.parse(
            "ctx.reply(\"$0\")"
        )
    }
}
