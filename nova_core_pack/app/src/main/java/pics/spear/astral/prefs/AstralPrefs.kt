package pics.spear.astral.prefs

import android.content.Context

object AstralPrefs {
    private const val PREFS = "astral_prefs"

    private const val KEY_ENGINE_ENABLED = "engine_enabled"
    private const val KEY_LAST_BOT_ID = "last_bot_id"
    private const val KEY_BOT_SORT_MODE = "bot_sort_mode"
    private const val KEY_FILTER_ENABLED = "bot_filter_enabled"
    private const val KEY_FILTER_LANG = "bot_filter_lang"
    private const val KEY_FILTER_AUTOSTART = "bot_filter_autostart"
    private const val KEY_FILTER_PRIORITY = "bot_filter_priority"
    private const val KEY_EDITOR_FONT_SIZE = "editor_font_size"
    private const val KEY_EDITOR_WORD_WRAP = "editor_word_wrap"
    private const val KEY_EDITOR_LINE_NUMBERS = "editor_line_numbers"

    fun isEngineEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENGINE_ENABLED, true)

    fun setEngineEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENGINE_ENABLED, enabled)
            .apply()
    }

    fun getLastBotId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_BOT_ID, null)

    fun setLastBotId(context: Context, botId: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_BOT_ID, botId)
            .apply()
    }

    fun getBotSortMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BOT_SORT_MODE, "name") ?: "name"

    fun setBotSortMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BOT_SORT_MODE, mode)
            .apply()
    }

    fun getBotFilterEnabled(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_FILTER_ENABLED)) prefs.getBoolean(KEY_FILTER_ENABLED, true) else null
    }

    fun setBotFilterEnabled(context: Context, enabled: Boolean?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (enabled == null) remove(KEY_FILTER_ENABLED) else putBoolean(KEY_FILTER_ENABLED, enabled)
            }
            .apply()
    }

    fun getBotFilterLang(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FILTER_LANG, null)

    fun setBotFilterLang(context: Context, lang: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (lang == null) remove(KEY_FILTER_LANG) else putString(KEY_FILTER_LANG, lang)
            }
            .apply()
    }

    fun getBotFilterAutoStart(context: Context): Boolean? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_FILTER_AUTOSTART)) prefs.getBoolean(KEY_FILTER_AUTOSTART, true) else null
    }

    fun setBotFilterAutoStart(context: Context, autoStart: Boolean?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (autoStart == null) remove(KEY_FILTER_AUTOSTART) else putBoolean(KEY_FILTER_AUTOSTART, autoStart)
            }
            .apply()
    }

    fun getBotFilterPriority(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_FILTER_PRIORITY)) prefs.getInt(KEY_FILTER_PRIORITY, 1) else null
    }

    fun setBotFilterPriority(context: Context, priority: Int?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (priority == null) remove(KEY_FILTER_PRIORITY) else putInt(KEY_FILTER_PRIORITY, priority)
            }
            .apply()
    }

    fun getEditorFontSize(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(KEY_EDITOR_FONT_SIZE, 16f)

    fun setEditorFontSize(context: Context, size: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_EDITOR_FONT_SIZE, size)
            .apply()
    }

    fun getEditorWordWrap(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_EDITOR_WORD_WRAP, false)

    fun setEditorWordWrap(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EDITOR_WORD_WRAP, enabled)
            .apply()
    }

    fun getEditorLineNumbers(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_EDITOR_LINE_NUMBERS, true)

    fun setEditorLineNumbers(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EDITOR_LINE_NUMBERS, enabled)
            .apply()
    }
}



