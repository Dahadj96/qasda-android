package pro.qasdatrip.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether there is a network worth trying.
 *
 * Without this the app only finds out it is offline by starting a search,
 * waiting out a 25-second watchdog and reporting that the connection dropped
 * - which is true, and useless, and takes long enough that people assume the
 * app is broken rather than the signal.
 *
 * VALIDATED rather than merely connected, because the case this exists for is
 * the Algerian mobile network at its worst: a bar of signal, a captive portal
 * in a hotel, or an association that goes nowhere. Connected and useless is
 * the state that fools an app.
 */
fun Context.onlineFlow(): Flow<Boolean> = callbackFlow {
    val manager = getSystemService(ConnectivityManager::class.java)

    fun usable(network: Network?): Boolean {
        val caps = network?.let { manager.getNetworkCapabilities(it) } ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(usable(network)) }
        override fun onLost(network: Network) { trySend(false) }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            trySend(
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            )
        }
    }

    trySend(usable(manager.activeNetwork))
    manager.registerDefaultNetworkCallback(callback)
    awaitClose { manager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
