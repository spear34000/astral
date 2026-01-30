package pics.spear.astral.ui.editor

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource

object TextMateRegistry {
    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.assets))

            val themePath = "textmate/astral-theme.json"
            val themeText = context.assets.open(themePath).bufferedReader().use { it.readText() }
            val themeSource = IThemeSource.fromString(IThemeSource.ContentType.JSON, themeText)
            val themeModel = ThemeModel(themeSource, "astral-theme").apply {
                setDark(true)
            }
            ThemeRegistry.getInstance().loadTheme(themeModel, true)

            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            initialized = true
        }
    }
}
