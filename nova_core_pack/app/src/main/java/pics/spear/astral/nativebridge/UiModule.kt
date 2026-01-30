package pics.spear.astral.nativebridge

import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class UiModule {
    private val main = Handler(Looper.getMainLooper())

    fun alert(message: String): Boolean {
        val activity = ActivityProvider.get() ?: return false
        main.post {
            AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton("?�인", null)
                .show()
        }
        return true
    }

    fun confirm(message: String, timeoutMs: Int): Boolean? {
        val activity = ActivityProvider.get() ?: return null
        val latch = CountDownLatch(1)
        var result: Boolean? = null
        main.post {
            AlertDialog.Builder(activity)
                .setMessage(message)
                .setNegativeButton("취소") { _, _ -> result = false; latch.countDown() }
                .setPositiveButton("?�인") { _, _ -> result = true; latch.countDown() }
                .setOnCancelListener { result = false; latch.countDown() }
                .show()
        }
        latch.await(timeoutMs.coerceIn(1_000, 60_000).toLong(), TimeUnit.MILLISECONDS)
        return result
    }
}



