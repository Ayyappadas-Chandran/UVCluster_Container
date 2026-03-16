package com.suprajit.uvcluster

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log.d
import androidx.annotation.RequiresPermission

class DataWrapperManager(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var onDataChange: ((Boolean) -> Unit)? = null
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            d("DataChange","NetworkCallback onAvailable")
            onDataChange?.invoke(true)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            d("DataChange","NetworkCallback onLost")
            onDataChange?.invoke(false)

        }
    }

     fun registerReceiver() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

     fun unregisterReceiver() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun isLteEnabled(): Boolean {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val networkType = telephonyManager.dataNetworkType
        return networkType == TelephonyManager.NETWORK_TYPE_LTE
    }
    fun dataState(callback: (Boolean) -> Unit) {
        onDataChange = callback
    }

    fun setDataState(enabled: Boolean) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            val method = connectivityManager.javaClass
                .getDeclaredMethod("setMobileDataEnabled", Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(connectivityManager, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}