package pics.spear.astral.nativebridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pics.spear.astral.R

class NotifyModule(private val appContext: Context) {
    private val channelId = "astral_default"

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Astral", NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannel(channel)
    }

    fun notify(id: Int, title: String, text: String): Boolean {
        ensureChannel()
        val notif = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(appContext).notify(id, notif)
        return true
    }

    fun cancel(id: Int): Boolean {
        NotificationManagerCompat.from(appContext).cancel(id)
        return true
    }
}



