package pro.qasdatrip.app

import android.app.Application
import android.content.Context
import pro.qasdatrip.core.Airports
import pro.qasdatrip.core.QasdaApi
import java.util.UUID

class QasdaApplication : Application() {
    lateinit var api: QasdaApi
        private set

    override fun onCreate() {
        super.onCreate()
        // 213 airports, shipped with the app: on a phone this is an asset, not
        // a download, and everything Algeria flies is in it.
        Airports.load(assets.open("airports.json").bufferedReader().use { it.readText() })
        QasdaMessagingService.ensureChannels(this)
        api = QasdaApi(
            baseUrl = BuildConfig.API_BASE,
            apiKey = BuildConfig.API_KEY,
            deviceId = deviceId(this),
        )
    }

    /**
     * A random string per install, for rate-limit bucketing only. It is not an
     * account, it is not tied to the device, and it goes when the app does.
     */
    private fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences("qasda", Context.MODE_PRIVATE)
        return prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
    }
}
