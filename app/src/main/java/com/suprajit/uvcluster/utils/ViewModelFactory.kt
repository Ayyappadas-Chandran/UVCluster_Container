package com.suprajit.uvcluster.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suprajit.uvcluster.DataRepoImpl
import com.suprajit.uvcluster.DataWrapperManager
import com.suprajit.uvcluster.HapticViewModel
import com.suprajit.uvcluster.data.dataSource.BluetoothManagerWrapper
import com.suprajit.uvcluster.data.dataSource.CarServiceWrapper
import com.suprajit.uvcluster.data.dataSource.WifiManagerWrapper
import com.suprajit.uvcluster.data.repository.BluetoothRepoImpl
import com.suprajit.uvcluster.data.repository.CarRepoImpl
import com.suprajit.uvcluster.data.repository.SharedPreferenceRepoImpl
import com.suprajit.uvcluster.data.repository.WifiRepoImpl
import com.suprajit.uvcluster.domain.manager.PreferenceManager
import com.suprajit.uvcluster.domain.repository.BluetoothRepository
import com.suprajit.uvcluster.domain.repository.CarRepository
import com.suprajit.uvcluster.domain.repository.WifiRepository
import com.suprajit.uvcluster.ui.features.dashboard.DashboardViewModel
import com.suprajit.uvcluster.ui.features.controls.advanceFeatures.camera.CameraViewModel
import com.suprajit.uvcluster.ui.features.controls.advanceFeatures.radar.RadarViewModel
import com.suprajit.uvcluster.ui.features.controls.performance.PerformanceViewModel
import com.suprajit.uvcluster.ui.features.controls.rideModes.RideModesViewModel
import com.suprajit.uvcluster.ui.features.controls.trips.TripsViewModel
import com.suprajit.uvcluster.ui.features.menus.battery.BatteryViewModel
import com.suprajit.uvcluster.ui.features.settings.bluetooth.BluetoothViewModel
import com.suprajit.uvcluster.ui.viewModel.CarViewModel
import com.suprajit.uvcluster.ui.features.controlSection.ControlSectionViewModel
import com.suprajit.uvcluster.ui.features.menu.MenuViewModel
import com.suprajit.uvcluster.ui.features.myF77.tutorial.TutorialViewModel
import com.suprajit.uvcluster.ui.features.settings.data.DataViewModel
import com.suprajit.uvcluster.ui.features.settings.display.DisplayViewModel
import com.suprajit.uvcluster.ui.features.settings.general.GeneralViewModel
import com.suprajit.uvcluster.ui.features.settings.incognito.IncognitoViewModel
import com.suprajit.uvcluster.ui.viewModel.SharedViewModel
import com.suprajit.uvcluster.ui.features.settings.wifi.WifiViewModel

/**
 * A factory class for creating instances of various ViewModels with their required repositories.
 *
 * This factory supports creation of the following ViewModels:
 * - [BluetoothViewModel] with [BluetoothRepository]
 * - [WifiViewModel] with [WifiRepository]
 * - [SharedViewModel] with no repository
 * - [CarViewModel] with [CarRepository]
 *
 * It ensures that the required repository is provided when creating the corresponding ViewModel.

 * @throws IllegalArgumentException if an unknown ViewModel class is requested.
 */
class ViewModelFactory(
    private val context: Context? = null
) : ViewModelProvider.Factory {
    /**
     * Creates a new instance of the given [ViewModel] class.
     *
     * @param modelClass The class of the ViewModel to create.
     * @return A new instance of the requested ViewModel.
     * @throws IllegalArgumentException if the ViewModel class is unknown or required repository is null.
     *
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(BluetoothViewModel::class.java) -> {
                val bluetoothRepositoryImpl = BluetoothRepoImpl(BluetoothManagerWrapper(context!!))
                BluetoothViewModel(bluetoothRepositoryImpl) as T
            }
            modelClass.isAssignableFrom(WifiViewModel::class.java) -> {
                val wifiRepositoryImpl = WifiRepoImpl(WifiManagerWrapper(context!!))
                WifiViewModel(wifiRepositoryImpl) as T
            }
            modelClass.isAssignableFrom(CarViewModel::class.java) -> {
                val carRepositoryImpl = CarRepoImpl(CarServiceWrapper(context!!))
                CarViewModel(carRepositoryImpl) as T
            }
            modelClass.isAssignableFrom(CameraViewModel::class.java) ->{
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                CameraViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(RadarViewModel::class.java) ->{
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                RadarViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(PerformanceViewModel::class.java) ->{
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                PerformanceViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(RideModesViewModel::class.java) ->{
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                RideModesViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(TripsViewModel::class.java) ->{
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                TripsViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(BatteryViewModel::class.java) ->{
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                BatteryViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(DataViewModel::class.java) ->{
                DataViewModel(DataRepoImpl(DataWrapperManager(context!!))) as T
            }
            modelClass.isAssignableFrom(GeneralViewModel::class.java) ->{
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                GeneralViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(DisplayViewModel::class.java) ->{
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                DisplayViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(IncognitoViewModel::class.java) ->{
                 val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                 IncognitoViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(SharedViewModel::class.java) ->{
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                SharedViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->{
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                DashboardViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(ControlSectionViewModel::class.java) -> {
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                ControlSectionViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(MenuViewModel::class.java) -> {
                val preferenceManager = PreferenceManager(SharedPreferenceRepoImpl(context!!))
                MenuViewModel(preferenceManager) as T
            }
            modelClass.isAssignableFrom(TutorialViewModel::class.java) -> {
                TutorialViewModel() as T
            }
            modelClass.isAssignableFrom(HapticViewModel::class.java) -> {
                HapticViewModel(PreferenceManager(SharedPreferenceRepoImpl(context!!))) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }}

