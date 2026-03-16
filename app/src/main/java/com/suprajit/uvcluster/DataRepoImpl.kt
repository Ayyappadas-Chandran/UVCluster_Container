package com.suprajit.uvcluster

import android.Manifest
import androidx.annotation.RequiresPermission

class DataRepoImpl(private val dataWrapperManager: DataWrapperManager): DataRepository {
    override fun registerReceiver() {
        dataWrapperManager.registerReceiver()
    }

    override fun unregisterReceiver() {
        dataWrapperManager.unregisterReceiver()
    }

    override fun dataState(callback: (Boolean) -> Unit) {
        dataWrapperManager.dataState(callback)
    }

    override fun setDataState(isEnabled: Boolean) {
            dataWrapperManager.setDataState(isEnabled)
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    override fun isDataEnable(): Boolean {
        return dataWrapperManager.isLteEnabled()
    }
}