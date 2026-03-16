package com.suprajit.uvcluster.data.dataSource

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.util.Log.d
import android.util.Log.e
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.suprajit.uvcluster.R

/**
 * A wrapper class to manage Wi-Fi operations such as scanning,  connecting to a hotspot,
 * retrieving the connected SSID, and forgetting saved networks.
 *
 * @param context Application context required for accessing system services.
 * @param onScanResult Callback interface to return scan results.
 *
 * Note: Ensure proper permissions are granted before using (e.g., [Manifest.permission.ACCESS_FINE_LOCATION]).
 */
class WifiManagerWrapper(private val context: Context) {
    private val tag = WifiManagerWrapper::class.java.simpleName
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var onScanResult: ((List<String>) -> Unit)? = null
    private var onWifiStateChanged: ((Boolean) -> Unit)? = null
    private var onConnectionState: ((Boolean) -> Unit)? = null

    /** BroadcastReceiver for handling Wi-Fi scan results*/
    //27/01/2026
    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                val wifiState =
                    intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                onWifiStateChanged?.invoke(wifiState == WifiManager.WIFI_STATE_ENABLED)
            }
            val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            val isWifiConnected =
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            d("WifiConnectionState","Is Wifi connected :$isWifiConnected")
            onConnectionState?.invoke(isWifiConnected)
            val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
            if (ActivityCompat.checkSelfPermission(
                    this@WifiManagerWrapper.context, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            if (success) {
                handleScanSuccess()
            } else {
                handleScanFailure()
            }
        }
    }

    /** Returns the state of the Wi-Fi on the device */
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    /**
     * Sets the state of the Wi-Fi on the device.
     */
    fun enableWifi(enable: Boolean) {
        try {
            if (wifiManager.isWifiEnabled != enable) {
                wifiManager.isWifiEnabled = enable
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                context,
                "Permission denied: can't change Wi-Fi state",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



    /** Starts a Wi-Fi scan and registers the broadcast receiver to handle scan results.*/
    fun startScan() {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val results = wifiManager.scanResults
        d(tag, "results :${results.size}")
        val ssids = results.map(ScanResult::SSID).filter { it.isNotEmpty() }
        onScanResult?.invoke(ssids)
    }

    /** Handles successful Wi-Fi scan results. */
    private fun handleScanSuccess() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val results = wifiManager.scanResults
        d(tag, "results :${results.size}")
        val ssids = results.map(ScanResult::SSID).filter { it.isNotEmpty() }
        onScanResult?.invoke(ssids)
    }

    /** Handles Wi-Fi scan failure. */
    private fun handleScanFailure() {
        onScanResult?.invoke(emptyList())
    }

    /**
     * Connects to a given Wi-Fi hotspot.
     *
     * @param ssid The SSID of the hotspot.
     * @param password The password for the hotspot.
     */
    fun connectHotspot(ssid: String, password: String) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiConfig = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            preSharedKey = "\"$password\""
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        }
        val networkId = wifiManager.addNetwork(wifiConfig)
        d(tag, "networkId :$networkId")
        wifiManager.disconnect()
        wifiManager.enableNetwork(networkId, true)
        wifiManager.reconnect()
    }

    fun registerWifiStateChangeReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiReceiver, intentFilter)
    }

    fun unregisterWifiStateChangeReceiver() {
        context.unregisterReceiver(wifiReceiver)
    }

    /**
     * Returns the SSID of the currently connected Wi-Fi network.
     *
     * @return SSID of the connected Wi-Fi or empty string if not connected.
     */
    fun getConnectedWifiSSID(): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return ""
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return ""
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val connectionInfo = wifiManager.connectionInfo
            val ssid = connectionInfo.ssid
            if (ssid != null && ssid != context.getString(R.string.unknown_ssid)) {
                return ssid.trim('"')
            }
        }
        return ""
    }

    /** Forgets the currently connected Wi-Fi network.*/
    fun forgetHotspot() {
        val ssid = getConnectedWifiSSID()
        d(tag, "Forget SSID:$ssid")
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val configuredNetworks = wifiManager.configuredNetworks
        for (config in configuredNetworks) {
            if (config.SSID == "\"$ssid\"") {
                val removed = wifiManager.removeNetwork(config.networkId)
                wifiManager.saveConfiguration()
                d(tag, "Removed network for SSID: $ssid - Success: $removed")
                break
            }
        }
    }

    fun connectHotspotWithSuggestion(ssid: String, password: String) {
        val suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val suggestionsList = listOf(suggestion)
        val status = wifiManager.addNetworkSuggestions(suggestionsList)

        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Toast.makeText(context, "Wi-Fi suggestion added", Toast.LENGTH_SHORT).show()
            d(tag, "Suggestion added successfully")

            // Register receiver to detect post-connection
            val intentFilter =
                IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    Toast.makeText(ctx, "Connected to $ssid", Toast.LENGTH_SHORT).show()
                    d(tag, "Post-connection broadcast received for $ssid")

                    // Check actual Wi-Fi connection status
                    val connectivityManager =
                        ctx?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    val network = connectivityManager?.activeNetwork
                    val capabilities =
                        network?.let { connectivityManager.getNetworkCapabilities(it) }
                    val isWifi =
                        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                    if (isWifi) {
                        val connectedSsid = wifiManager.connectionInfo.ssid
                        d(tag, "Currently connected to $connectedSsid")
                    }

                    // Unregister receiver after use
                    try {
                        ctx?.unregisterReceiver(this)
                    } catch (e: Exception) {
                        e(tag, "exception unregistering wifi broadcast receiver ${e.message}")
                    }
                }
            }
            context.registerReceiver(receiver, intentFilter)

        } else {
            Toast.makeText(context, "Suggestion failed: $status", Toast.LENGTH_LONG).show()
            e(tag, "Suggestion failed with status: $status")
        }
    }
//
//    fun scanResult(callback: (List<String>) -> Unit) {
//        onScanResult = callback
//    }

    fun scanResult(callback: (List<ScanResult>) -> Unit) {
        val results = wifiManager.scanResults
        callback(results)
    }

    fun wifiState(callback: (Boolean) -> Unit) {
        onWifiStateChanged = callback
    }

    //27/01/2026
    fun connectionState(callback: (Boolean) -> Unit) {
        onConnectionState = callback
    }
    fun getCurrentSignalLevel(): Int {
        val rssi = wifiManager.connectionInfo.rssi
        return if (rssi <= -100) {
            0
        } else {
            WifiManager.calculateSignalLevel(rssi, 6)
        }
    }
}

