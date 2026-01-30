package pics.spear.astral.runtime

import java.util.concurrent.ConcurrentHashMap

object EventRateLimiter {
    private val lastHandled = ConcurrentHashMap<String, Long>()
    private const val COOLDOWN_MS = 1000L

    @Synchronized
    fun canProcess(key: String): Boolean {
        val now = System.currentTimeMillis()
        val last = lastHandled[key] ?: 0L
        val ok = now - last >= COOLDOWN_MS
        if (ok) lastHandled[key] = now
        return ok
    }
}
