package com.suprajit.uvcluster.ui.features.settings.general

import androidx.lifecycle.ViewModel
import com.suprajit.uvcluster.domain.manager.PreferenceManager

class GeneralViewModel(private val preferenceManager: PreferenceManager) :
    ViewModel() {
    val isAutoDateTimeEnabled: Boolean
        get() = preferenceManager.isAutoDateTimeEnabled
    val isChargeEnable: Boolean
        get() = preferenceManager.isChargeEnable
    val isNormalTimeFormat: Boolean
        get() = preferenceManager.isNormalTimeFormat
    val distanceUnit: String
        get()=preferenceManager.distanceUnit

    fun saveDateAndTime(isAutoEnabled: Boolean) {
        preferenceManager.saveDateAndTime(isAutoEnabled)
    }

    fun saveTimeFormat(isNormalTimeFormat: Boolean) {
        preferenceManager.saveTimeFormat(isNormalTimeFormat)
    }

    fun saveChargeEnable(isEnable: Boolean) {
        preferenceManager.saveChargeEnable(isEnable)
    }
    
    fun saveDistanceUnit(distanceUnit: String) {
        preferenceManager.saveDistanceUnit(distanceUnit)
    }

}
