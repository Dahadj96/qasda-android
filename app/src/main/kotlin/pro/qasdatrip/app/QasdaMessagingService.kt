package pro.qasdatrip.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pro.qasdatrip.app.data.Settings

class QasdaMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val settings = Settings(this)
        settings.pushToken = token
        scope.launch { registerToken(settings, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val kind = data["kind"] ?: return
        val settings = Settings(this)
        val enabled = when (kind) {
            "seat" -> settings.alertSeats
            "price" -> settings.alertDrops
            "ended" -> settings.alertEnded
            else -> false
        }
        if (!enabled || !notificationsAllowed()) return

        val channel = when (kind) {
            "seat" -> CHANNEL_AVAILABILITY
            "price" -> CHANNEL_PRICES
            else -> CHANNEL_UPDATES
        }
        ensureChannels(this)
        val from = data["origin"].orEmpty()
        val to = data["destination"].orEmpty()
        val date = data["departDate"].orEmpty()
        val uri = Uri.parse("${BuildConfig.API_BASE}/fr/vols/$from/$to/$date")
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            this.data = uri
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            this, (from + to + date + kind).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = data["title"] ?: message.notification?.title ?: getString(R.string.app_name)
        val body = data["body"] ?: message.notification?.body.orEmpty()
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()
        NotificationManagerCompat.from(this).notify(message.messageId?.hashCode() ?: System.nanoTime().toInt(), notification)
    }

    private fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private suspend fun registerToken(settings: Settings, token: String) {
        val app = application as QasdaApplication
        app.api.registerDevice(settings.deviceKey, token, locale = settings.language?.tag ?: "fr")
            ?.let { settings.deviceKey = it }
    }

    companion object {
        const val CHANNEL_AVAILABILITY = "availability_alerts"
        const val CHANNEL_PRICES = "price_alerts"
        const val CHANNEL_UPDATES = "tracking_updates"

        fun ensureChannels(context: android.content.Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(CHANNEL_AVAILABILITY, "Flight availability", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "A tracked route has flights available"
                        enableVibration(true)
                    },
                    NotificationChannel(CHANNEL_PRICES, "Price drops", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "A tracked flight price has dropped"
                        enableVibration(true)
                    },
                    NotificationChannel(CHANNEL_UPDATES, "Tracking updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "A route tracker has ended"
                    },
                )
            )
        }
    }
}
