package com.suprajit.uvcluster

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.suprajit.uvcluster.domain.dataModel.Severity
import com.suprajit.uvcluster.domain.ennumerate.VcuMiscFlags
import com.suprajit.uvcluster.domain.ennumerate.VcuStatusFlags
import com.suprajit.uvcluster.ui.viewModel.CarViewModel
import com.suprajit.uvcluster.ui.viewModel.SharedViewModel
import com.suprajit.uvcluster.utils.Utilities.ChargeStatusFlag
import com.suprajit.uvcluster.utils.Utilities.applyMinMax
import kotlinx.coroutines.launch

object NotificationManager {

    private const val TAG = "ClusterNotificationManager"
    private var appContext: Context? = null

    // State Tracking to prevent redundant UI updates
    private var sideStandActive = false
    private var isTcMalfunctionShown = false
    private var isAbsTcMalfunctionShown = false
    private var lastChargeValue: UInt = 0u
    private var lastMotorArmParams: ClusterNotification.Params? = null
    private var lastSocCategory: Int = -1 // 0: Critical, 1: Glide, 2: Combat, 3: Regen, -1: Normal

    data class VcuFlag(val bit: Int, val label: String, val severity: Severity)

    fun init(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        carViewModel: CarViewModel,
        sharedViewModel: SharedViewModel
    ) {
        Log.i(TAG, "NotificationManager Initialized")
        appContext = context.applicationContext
        bindToCarViewModel(lifecycleOwner, carViewModel, sharedViewModel)
    }

    fun show(params: ClusterNotification.Params) {
        val context = appContext ?: return
        try {
            ClusterNotification.show(context, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
        }
    }

    fun dismiss() {
        ClusterNotification.dismiss()
    }

    private fun bindToCarViewModel(
        lifecycleOwner: LifecycleOwner,
        carViewModel: CarViewModel,
        sharedViewModel: SharedViewModel
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. VCU Status (Side Stand)
                launch {
                    carViewModel.vcuInfoMsg.collect { vcu ->
                        vcu?.let {

                            if (sharedViewModel.hasThemeConfigChanged) return@collect
                            val vcuStatus =
                                (vcu.vcuStatusH.toULong() shl 32) or vcu.vcuStatusL.toULong()
                            val isNowActive = isBitSet(vcuStatus, 29) // Side Stand Bit

                            if (isNowActive && !sideStandActive) {
                                show(buildSideStandParams())
                            } else if (!isNowActive && sideStandActive) {
                                dismiss()
                            }
                            sideStandActive = isNowActive
                        }
                    }
                }

                // 2. Motor Arming Errors
                launch {
                    carViewModel.mcNoArm.collect { mcNoArm ->
                        if (mcNoArm == null || mcNoArm.isEmpty() || sharedViewModel.hasThemeConfigChanged) return@collect

                        val params = when {
                            mcNoArm[0] == 0 -> ClusterNotification.Params(
                                "MOTOR ARM ERROR",
                                "Please keycycle and try again"
                            )

                            mcNoArm[1] == 0 -> ClusterNotification.Params(
                                "Motor Arm Failed",
                                "Vehicle is in lockdown"
                            )

                            mcNoArm[2] == 0 -> ClusterNotification.Params(
                                "MOTOR ARM ERROR",
                                "Release throttle before arming"
                            )

                            mcNoArm[3] == 0 -> ClusterNotification.Params(
                                "SIDE STAND DEPLOYED",
                                "Motor disarmed"
                            )

                            mcNoArm[4] == 0 -> ClusterNotification.Params(
                                "MOTOR ARM ERROR",
                                "Please keycycle and try again"
                            )

                            mcNoArm[5] == 0 -> ClusterNotification.Params(
                                "Motor Arm Failed",
                                "Charger is connected"
                            )

                            else -> null
                        }
                        if (params == null) dismiss() else show(params)
                    }
                }

                // 3. Battery SOC Logic
                launch {
                    carViewModel.tellTales.collect { tellTales ->
                        // 1. Ignore the default state where SOC is 0
                        if (sharedViewModel.hasThemeConfigChanged) return@collect
                        val soc = tellTales?.batterySoc ?: return@collect
                        if (soc == 0) return@collect

                        val currentCategory = when {
                            soc <= 5 -> 0 // Critical
                            soc <= 20 -> 1 // Glide
                            soc <= 30 -> 2 // Combat
                            soc >= 70 -> 3 // Regen
                            else -> -1
                        }

                        // 2. Only trigger if the category is valid and changed
                        if (currentCategory != lastSocCategory && currentCategory != -1) {
                            val params = when (currentCategory) {
                                0 -> ClusterNotification.Params(
                                    "BATTERY CRITICALLY LOW",
                                    "Hover mode initiated"
                                )

                                1 -> ClusterNotification.Params(
                                    "SWITCHING TO GLIDE",
                                    "At 20% battery"
                                )

                                2 -> ClusterNotification.Params(
                                    "SWITCHING TO COMBAT",
                                    "and disengaging surge mode at 30% battery"
                                )

                                3 -> ClusterNotification.Params(
                                    "REGEN AVAILABLE",
                                    "Adjust levels with nav keys"
                                )

                                else -> null
                            }
                            if (params != null) show(params) else dismiss()
                            lastSocCategory = currentCategory
                        }
                    }
                }

                // 4. Hill Hold & Malfunctions
                launch {
                    var lastHillHoldState: Int? = null
                    carViewModel.hillHoldState.collect { hillHoldState ->
                        if (sharedViewModel.hasThemeConfigChanged) return@collect
                        if (lastHillHoldState == 1 && hillHoldState == 0) {
                            show(
                                ClusterNotification.Params(
                                    "DISENGAGING HILL HOLD IN 5S",
                                    "Apply brakes for your safety"
                                )
                            )
                        }
                        lastHillHoldState = hillHoldState
                    }
                }

                launch {
                    carViewModel.sleepWake.collect {
                        if (sharedViewModel.hasThemeConfigChanged) return@collect
                        Log.d(TAG, "sleepWake : " + it)
                        if (it == 0x7F) show(
                            ClusterNotification.Params(
                                "ABS AND TC MALFUNCTION",
                                "Please contact customer support"
                            )
                        )
                    }
                }

                // 5. Charging Logic
                launch {
                    carViewModel.chargeCtx.collect { charge ->
                        if (sharedViewModel.hasThemeConfigChanged) return@collect
                        val chargeStatus = charge.chargerStatus
                        val changed = chargeStatus xor lastChargeValue
                        if (changed != 0u) {
                            ChargeStatusFlag.values().forEach { flag ->
                                if (isBitSet(changed, flag.bit)) {
                                    if (isBitSet(
                                            chargeStatus,
                                            flag.bit
                                        )
                                    ) showForChargeFlag(flag) else dismiss()
                                }
                            }
                        }
                        lastChargeValue = chargeStatus
                    }
                }

                launch {
                    carViewModel.vcuMiscInfo.collect { miscInfo ->
                        if (sharedViewModel.hasThemeConfigChanged) return@collect
                        Log.d(TAG, "VcuMiscInfo : " + miscInfo)

                        val miscStatus = miscInfo.data
                        Log.d(TAG, "VcuMiscInfo Status : " + miscStatus)
                        if (miscInfo.hasFlag(VcuMiscFlags.STAT_VCU_MISC_MTC_ERROR) && !isTcMalfunctionShown) {
                            show(
                                ClusterNotification.Params(
                                    "TC MALFUNCTION",
                                    "Please contact customer support"
                                )
                            )
                            isTcMalfunctionShown = true
                        }
                        if ((miscInfo.hasFlag(VcuStatusFlags.STAT_VCU_ABS_REAR_WHEEL_SPEED_SENSOR_FAILURE) || miscInfo.hasFlag(
                                VcuStatusFlags.STAT_VCU_ABS_FRONT_WHEEL_SPEED_SENSOR_FAILURE
                            )) && !isAbsTcMalfunctionShown
                        ) {
                            show(
                                ClusterNotification.Params(
                                    "ABS AND TC MALFUNCTION",
                                    "Please contact customer support"
                                )
                            )
                            isAbsTcMalfunctionShown = true
                        }

                    }
                }
            }
        }
    }

    private fun showForChargeFlag(flag: ChargeStatusFlag) {
        val params = when (flag) {
            ChargeStatusFlag.EXT_CHRG_CP_VALID_FAULT -> ClusterNotification.Params(
                "CHARGING ERROR",
                "Re-plug Charger"
            )

            ChargeStatusFlag.EXT_CHRG_OVER_TEMPERATURE -> ClusterNotification.Params(
                "CHARGER TEMP HIGH",
                "Cool off charger and try again"
            )

            ChargeStatusFlag.EXT_CHRG_INPUT_VOLTAGE_ERROR -> ClusterNotification.Params(
                "VOLTAGE LOW",
                "Charging might be slow"
            )
            // Combine similar errors
            ChargeStatusFlag.EXT_CHRG_TIMEOUT_ERROR,
            ChargeStatusFlag.EXT_CHRG_HARDWARE_FAILURE -> ClusterNotification.Params(
                "CHARGING ERROR",
                "RPower off charger for 1 min and try again"
            )

            else -> return
        }
        show(params)
    }

    private fun buildSideStandParams() = ClusterNotification.Params(
        heading = "SIDE STAND DEPLOYED",
        subtext = "Motor disarmed",
        priority = ClusterNotification.Priority.IMMEDIATE
    )

    private fun isBitSet(value: ULong, bit: Int): Boolean = ((value shr bit) and 1uL) == 1uL
    private fun isBitSet(value: UInt, bit: Int): Boolean = ((value shr bit) and 1u) == 1u
}

