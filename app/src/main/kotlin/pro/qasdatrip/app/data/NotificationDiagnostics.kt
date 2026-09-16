package pro.qasdatrip.app.data

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import pro.qasdatrip.app.BuildConfig
import pro.qasdatrip.app.QasdaApplication
import pro.qasdatrip.app.QasdaMessagingService

object NotificationDiagnostics {
    private fun queued(context: Context): List<JsonObject> = runCatching {
        (Json.parseToJsonElement(context.getSharedPreferences("qasda-diagnostics", Context.MODE_PRIVATE).getString("events", "[]")!!) as JsonArray).filterIsInstance<JsonObject>()
    }.getOrDefault(emptyList())
    @Synchronized fun enqueue(app: QasdaApplication, delivery: String?, event: String) {
        val owner = app.account.serverUserId ?: return
        if (delivery == null || !delivery.all(Char::isDigit)) return
        val item = buildJsonObject { put("deliveryId", delivery); put("event", event); put("owner", owner) }
        val items = (queued(app) + item).distinctBy { "${it["deliveryId"]}:${it["event"]}:${it["owner"]}" }.takeLast(200)
        app.getSharedPreferences("qasda-diagnostics", Context.MODE_PRIVATE).edit().putString("events", JsonArray(items).toString()).apply()
    }
    suspend fun flush(app: QasdaApplication, settings: Settings) {
        val key = settings.deviceKey ?: return
        val owner = app.account.serverUserId ?: return
        for (event in queued(app)) {
            if (event["owner"]?.jsonPrimitive?.content == owner) {
                val recorded = app.api.accountPost("/delivery-events", buildJsonObject {
                    put("deliveryId", event["deliveryId"]!!); put("event", event["event"]!!); put("deviceId", key.deviceId); put("signature", key.signature)
                })
                if (recorded == null) continue
            }
            synchronized(this) {
                val remaining = queued(app).filterNot { it == event }
                app.getSharedPreferences("qasda-diagnostics", Context.MODE_PRIVATE).edit().putString("events", JsonArray(remaining).toString()).apply()
            }
        }
        val manager = app.getSystemService(NotificationManager::class.java)
        app.api.accountPost("/devices/status", buildJsonObject {
            put("deviceId", key.deviceId); put("signature", key.signature); put("appVersion", BuildConfig.VERSION_NAME)
            put("permission", NotificationManagerCompat.from(app).areNotificationsEnabled())
            put("channels", buildJsonObject {
                for (id in listOf(QasdaMessagingService.CHANNEL_AVAILABILITY, QasdaMessagingService.CHANNEL_PRICES, QasdaMessagingService.CHANNEL_UPDATES)) {
                    val channel = manager.getNotificationChannel(id) ?: continue
                    put(id, buildJsonObject { put("importance", channel.importance); put("sound", channel.sound != null); put("vibration", channel.shouldVibrate()) })
                }
            })
        })
    }
}
