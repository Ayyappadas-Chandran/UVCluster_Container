package com.suprajit.uvcluster

interface DataRepository {

    fun registerReceiver()
    fun unregisterReceiver()
    fun dataState(callback: (Boolean) -> Unit)
    fun setDataState(isEnabled: Boolean)

    fun isDataEnable():Boolean
}
