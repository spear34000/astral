package pics.spear.astral.util

import android.content.Context
import java.io.File

object Storage {
    private const val ROOT = "astral"

    fun baseDir(context: Context): File =
        context.getExternalFilesDir(null)?.resolve(ROOT)
            ?: File(context.filesDir, ROOT).also { it.mkdirs() }

    fun botsDir(context: Context): File =
        baseDir(context).resolve("bots").also { it.mkdirs() }

    fun botFile(context: Context, botName: String): File =
        botsDir(context).resolve("$botName.js")

    fun botMetaFile(context: Context): File =
        baseDir(context).resolve("bots.json")

    fun inboxFile(context: Context): File =
        baseDir(context).resolve("inbox.json")

    fun logsFile(context: Context): File =
        baseDir(context).resolve("logs.json")
}
