package pics.spear.astral.util

import android.content.Context
import android.os.Environment
import java.io.File

object AstralStorage {
    fun baseDir(context: Context): File {
        val external = File(Environment.getExternalStorageDirectory(), "astral")
        val internal = File(context.filesDir, "astral")
        val legacy = File(context.filesDir, "nova")

        if (!external.exists()) {
            when {
                internal.exists() -> internal.renameTo(external)
                legacy.exists() -> legacy.renameTo(external)
                else -> external.mkdirs()
            }
        }

        if (!external.exists()) {
            external.mkdirs()
        }

        ensureStructure(external)
        return external
    }

    fun botScriptsDir(context: Context): File {
        return File(baseDir(context), "bot_script").apply { mkdirs() }
    }

    fun settingsDir(context: Context): File {
        return File(baseDir(context), "setting").apply { mkdirs() }
    }

    fun depsDir(context: Context): File {
        return File(baseDir(context), "deps").apply { mkdirs() }
    }

    private fun ensureStructure(base: File) {
        File(base, "bot_script").mkdirs()
        File(base, "setting").mkdirs()
        File(base, "deps").mkdirs()
    }
}
