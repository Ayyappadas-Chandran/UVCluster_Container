package com.suprajit.uvcluster.ui.features.settings.data

import android.util.Log.d
import androidx.lifecycle.ViewModel
import com.suprajit.uvcluster.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiState(val state: Boolean, val isButton: Boolean)

class DataViewModel(private val dataRepository: DataRepository) : ViewModel() {
    private var _dataStateChange: MutableStateFlow<UiState> = MutableStateFlow(
        UiState(
            state = false,
            isButton = false
        )
    )
    val onDataStateChange: StateFlow<UiState> = _dataStateChange.asStateFlow()

    fun registerReceiver() {
        dataRepository.registerReceiver()
    }

    fun unregisterReceiver() {
        dataRepository.unregisterReceiver()
    }

    fun stateChange(isButton: Boolean = false) {
        d("VM", "stateChange called isButton=$isButton")
     //   _dataStateChange.value = UiState(dataRepository.isDataEnable(), isButton)

    }

    fun setDataState(isEnabled: Boolean) {
        dataRepository.setDataState(isEnabled)
    }
}