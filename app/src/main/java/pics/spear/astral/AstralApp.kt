package pics.spear.astral

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AstralApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_LISTENER,
                "Bot Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running bot listener service"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_LISTENER = "astral_listener"
    }
}
