package com.leathalenterprises.onyx.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Live battery percentage and a connectivity *alert* for the home screen's
 * status readout. Connectivity stays silent (null) when everything is fine —
 * it only speaks up with "no signal" or "poor signal". All system callbacks,
 * no runtime permissions (ACCESS_NETWORK_STATE is install-time).
 */
class StatusMonitor(context: Context) {

    private val context = context.applicationContext

    /** Battery charge, 0-100. */
    val battery: Flow<Int> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ignored: Context?, intent: Intent?) {
                intent?.let { trySend(percent(it)) }
            }
        }
        // ACTION_BATTERY_CHANGED is sticky: registering returns the latest
        // value immediately.
        val sticky = context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        sticky?.let { trySend(percent(it)) }
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate()

    private data class NetState(val cellular: Boolean, val validated: Boolean)

    private val network: Flow<NetState?> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                trySend(capabilities.toState())
            }

            override fun onLost(network: Network) {
                trySend(null)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        trySend(manager.getNetworkCapabilities(manager.activeNetwork)?.toState())
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.conflate()

    /** Cellular signal level 0-4, or null where unavailable. */
    private val cellSignalLevel: Flow<Int?> = callbackFlow {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephony != null) {
            val callback = object :
                TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    trySend(signalStrength.level)
                }
            }
            telephony.registerTelephonyCallback(context.mainExecutor, callback)
            trySend(telephony.signalStrength?.level)
            awaitClose { telephony.unregisterTelephonyCallback(callback) }
        } else {
            trySend(null)
            awaitClose { }
        }
    }.conflate()

    /**
     * "no signal" when there is no validated internet connection, "poor
     * signal" on weak cellular, null when connectivity is unremarkable.
     * Debounced so the moment of switching networks doesn't flash a
     * false alert. (Declared after the flows it combines: property
     * initializers run in declaration order.)
     */
    @OptIn(FlowPreview::class)
    val signalAlert: Flow<String?> =
        combine(network, cellSignalLevel) { net, cellLevel ->
            when {
                net == null || !net.validated -> "no signal"
                net.cellular && cellLevel != null && cellLevel <= POOR_CELL_LEVEL ->
                    "poor signal"
                else -> null
            }
        }
            .debounce(ALERT_DEBOUNCE_MS)
            .distinctUntilChanged()

    private fun percent(intent: Intent): Int {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return if (level >= 0 && scale > 0) level * 100 / scale else 0
    }

    private fun NetworkCapabilities.toState() = NetState(
        cellular = hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
        validated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
    )

    private companion object {
        /** 0-4 scale; 0 and 1 bars count as poor. */
        const val POOR_CELL_LEVEL = 1
        const val ALERT_DEBOUNCE_MS = 2_000L
    }
}
