package com.suprajit.uvcluster.ui.features.settings.wifi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suprajit.uvcluster.domain.repository.WifiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

/**
 * ViewModel responsible for managing Wi-Fi operations and exposing scan results to the UI.
 * Acts as a mediator between the UI and [com.suprajit.uvcluster.domain.repository.WifiRepository].
 *
 * @param wifiRepository The repository handling Wi-Fi-related operations.
 */
class WifiViewModel(private val wifiRepository: WifiRepository): ViewModel() {
//    private var _scanResult  = MutableLiveData<List<String>>()
//
//    /**
//     * LiveData containing the list of discovered Wi-Fi SSIDs.
//     */
//    val scanResult : LiveData<List<String>>
//        get() = _scanResult


    private val _currentSignalLevel = MutableStateFlow(0)
    val currentSignalLevel: StateFlow<Int> = _currentSignalLevel.asStateFlow()
    private val _scanResult = MutableLiveData<List<WifiUiModel>>()

    val scanResult: LiveData<List<WifiUiModel>>
        get() = _scanResult

    //27/01/2026
    private var _connectionState = MutableStateFlow<Boolean>(false)

    val connectionState : StateFlow<Boolean>
            = _connectionState.asStateFlow()

    private var _wifiState = MutableStateFlow(isWifiEnabled())
    val onWifiStateChange : StateFlow<Boolean>
        get() = _wifiState.asStateFlow()


    fun startSignalMonitoring() {
        viewModelScope.launch {
            while (true) {
                if (wifiRepository.isWifiEnabled()) {
                    val level = wifiRepository.getCurrentSignalLevel()
                    _currentSignalLevel.value = level
                } else {
                    _currentSignalLevel.value = 0
                }
                delay(2000)
            }
        }
    }
    fun wifiStateChange(){
        wifiRepository.wifiStateChange {
            _wifiState.value = it
        }
    }

    fun registerWifiStateChangeReceiver(){
        wifiRepository.registerWifiStateChangeReceiver()
    }

    fun unregisterWifiStateChangeReceiver(){
        wifiRepository.unregisterWifiStateChangeReceiver()
    }



    /**
     * Checks whether Wi-Fi is currently enabled on the device.
     *
     * @return true if Wi-Fi is enabled, false otherwise.
     */
    fun isWifiEnabled(): Boolean = wifiRepository.isWifiEnabled()

    /**
     * Connects to a specified Wi-Fi hotspot using SSID and password.
     *
     * @param ssid The SSID of the hotspot.
     * @param password The password for the hotspot.
     */
    fun connectHotspot(ssid: String, password: String) = wifiRepository.connectHotspot(ssid, password)

    /**
     * Initiates a scan to discover available Wi-Fi networks.
     */
    fun startScan() = wifiRepository.startScan()

    /**
     * Returns the SSID of the currently connected Wi-Fi network.
     *
     * @return SSID as a string, or null if not connected.
     */
    fun getConnectedWifiSSID() = wifiRepository.getConnectedWifiSSID()

    /**
     * Forgets the currently connected or previously remembered Wi-Fi hotspot.
     */
    fun forgetHotspot() = wifiRepository.forgetHotspot()

    /**
     * Fetches the scan results asynchronously and updates [scanResult] LiveData.
     */
//    fun scanResult() {
//        //for bug no 42 - Fix the Duplicates wifi  network
//        wifiRepository.scanResult { result ->
//            _scanResult.value = result.distinct()
//        }
//    }
    fun scanResult() {
        wifiRepository.scanResult { results ->

            val connectedSsid = getConnectedWifiSSID()

            val wifiList = results
                .filter { it.SSID.isNotBlank() }
                .map { scanResult ->

                    val level = android.net.wifi.WifiManager
                        .calculateSignalLevel(scanResult.level, 6)

                    // If this is connected network → update toolbar signal
                    if (scanResult.SSID == connectedSsid) {
                        _currentSignalLevel.value = level
                    }

                    WifiUiModel(
                        ssid = scanResult.SSID,
                        level = level,
                        isSecured = scanResult.capabilities.contains("WPA")
                    )
                }
                .distinctBy { it.ssid }

            _scanResult.value = wifiList
        }
    }

    fun enableWifi(enable:Boolean){
        wifiRepository.enableWifi(enable)
    }

    //27/01/2026
    fun wifiConnected(){
        wifiRepository.connectionState {isConnected->
            _connectionState.value = isConnected
        }
    }

}

