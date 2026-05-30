package pics.spear.astral.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import pics.spear.astral.service.NotificationReplier

data class KakaoEvent(
    val room: String,
    val sender: String,
    val content: String,
    val isGroupChat: Boolean,
)

/**
 * Central event bus and device API.
 * Single instance shared across the app.
 */
class BotApi(private val appContext: Context) {
    private val _events = MutableSharedFlow<KakaoEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<KakaoEvent> = _events.asSharedFlow()

    fun enqueueEvent(room: String, sender: String, content: String, isGroup: Boolean) {
        _events.tryEmit(KakaoEvent(room, sender, content, isGroup))
    }

    fun sendReply(room: String, message: String) {
        NotificationReplier.send(appContext, room, message)
    }

    fun toast(text: String) {
        Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
    }

    fun vibrate(ms: Long = 200) {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
