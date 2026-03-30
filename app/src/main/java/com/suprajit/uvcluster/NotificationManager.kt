package com.suprajit.uvcluster

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.suprajit.uvcluster.domain.ennumerate.VcuMiscFlags
import com.suprajit.uvcluster.domain.ennumerate.VcuStatusFlags
import com.suprajit.uvcluster.ui.viewModel.CarViewModel
import com.suprajit.uvcluster.ui.viewModel.SharedViewModel
import com.suprajit.uvcluster.utils.Utilities.ChargeStatusFlag
import kotlinx.coroutines.*

object NotificationManager {

    private const val TAG = "VCU_ALERT_SYSTEM"
    private var appContext: Context? = null

    @Volatile
    private var isInitialized = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    // --- State Persistence (Survives Theme Changes) ---
    private var sideStandActive = false
    private var lastMcNoArm: IntArray? = null
    private var isTcMalfunctionShown = false
    private var isAbsTcMalfunctionShown = false
    private var lastChargeValue: UInt = 0u
    private var lastSocCategory: Int = -1

    fun init(context: Context, carViewModel: CarViewModel, sharedViewModel: SharedViewModel) {
        Log.e(TAG, "[MGR] init() called. Instance: ${this.hashCode()}")
        if (isInitialized) {
            Log.e(TAG, "[MGR] WARNING: Re-initialization attempt blocked.")
            return
        }
        appContext = context.applicationContext
        isInitialized = true
        bindToCarViewModel(carViewModel, sharedViewModel)
    }

    fun show(params: ClusterNotification.Params) {
        val context = appContext ?: run {
            Log.e(TAG, "[MGR] ERROR: Cannot show '${params.heading}', Context is NULL")
            return
        }
        Log.e(TAG, "[MGR] Requesting UI Display -> ${params.heading}")
        mainHandler.post {
            ClusterNotification.show(context, params)
        }
    }

    fun dismiss() {
        Log.e(TAG, "[MGR] Requesting UI Dismissal")
        mainHandler.post {
            ClusterNotification.dismiss()
        }
    }

    private fun bindToCarViewModel(carViewModel: CarViewModel, sharedViewModel: SharedViewModel) {
        Log.e(TAG, "[MGR] BIND: Flow collectors started.")

        // 1. Side Stand (State-Based)
        scope.launch {
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

        // 2. Motor Arm (Event-Based SharedFlow)
        scope.launch {

            carViewModel.mcNoArm.collect { mcNoArm ->
                // 1. Handle Theme Change Replay
                // Block if it's a theme change and the data matches what we saw before recreation
                if (sharedViewModel.hasThemeConfigChanged) {
                    Log.e(TAG, "[MGR] MOTOR_ARM: Replay blocked (Theme Change match)")
                    return@collect
                }

                // 2. Process the event
                Log.e(TAG, "[MGR] MOTOR_ARM: Processing Event -> ${mcNoArm.contentToString()}")
                lastMcNoArm = mcNoArm

                val params = when {
                    mcNoArm[0] == 0 -> ClusterNotification.Params("MOTOR ARM ERROR", "Please keycycle and try again")
                    mcNoArm[1] == 0 -> ClusterNotification.Params("Motor Arm Failed", "Vehicle is in lockdown")
                    mcNoArm[2] == 0 -> ClusterNotification.Params("MOTOR ARM ERROR", "Release throttle before arming")
                    mcNoArm[3] == 0 -> ClusterNotification.Params("SIDE STAND DEPLOYED", "Motor disarmed")
                    mcNoArm[5] == 0 -> ClusterNotification.Params("Motor Arm Failed", "Charger is connected")
                    else -> null
                }

                if (params == null) {
                    Log.e(TAG, "[MGR] MOTOR_ARM: All clear, dismissing UI")
                    dismiss()
                } else {
                    show(params)
                }
            }
        }

        // 3. Battery SOC
        scope.launch {
            carViewModel.tellTales.collect { tellTales ->
                if (sharedViewModel.hasThemeConfigChanged) return@collect
                val soc = tellTales?.batterySoc ?: return@collect
                if (soc == 0) return@collect

                val currentCategory = when {
                    soc <= 5 -> 0
                    soc <= 10 -> 1
                    soc <= 30 -> 2
                    soc <= 99 -> 3
                    else -> -1
                }

                if (currentCategory != lastSocCategory && currentCategory != -1) {
                    Log.e(TAG, "[MGR] BATT: Category shift $lastSocCategory -> $currentCategory (SOC: $soc%)")
                    val params = when (currentCategory) {
                        0 -> ClusterNotification.Params("BATTERY CRITICALLY LOW", "Hover mode initiated")
                        1 -> ClusterNotification.Params("SWITCHING TO GLIDE", "At 10% battery")
                        3 -> ClusterNotification.Params("REGEN AVAILABLE", "Adjust levels with nav keys")
                        else -> null
                    }
                    params?.let { show(it) }
                    lastSocCategory = currentCategory
                }
            }
        }

        // 4. Charging
        scope.launch {
            carViewModel.chargeCtx.collect { charge ->
                if (sharedViewModel.hasThemeConfigChanged) return@collect
                val chargeStatus = charge.chargerStatus
                val changed = chargeStatus xor lastChargeValue

                if (changed != 0u) {
                    Log.e(TAG, "[MGR] CHRG: Bits changed. New: 0x${chargeStatus.toString(16)}")
                    ChargeStatusFlag.values().forEach { flag ->
                        if (isBitSet(changed, flag.bit)) {
                            if (isBitSet(chargeStatus, flag.bit)) {
                                Log.e(TAG, "[MGR] CHRG: Flag SET -> ${flag.name}")
                                showForChargeFlag(flag)
                            } else {
                                Log.e(TAG, "[MGR] CHRG: Flag CLEARED -> ${flag.name}")
                                dismiss()
                            }
                        }
                    }
                }
                lastChargeValue = chargeStatus
            }
        }

        // 5. Misc Errors
        scope.launch {
            carViewModel.vcuMiscInfo.collect { miscInfo ->
                if (sharedViewModel.hasThemeConfigChanged) return@collect

                if (miscInfo.hasFlag(VcuMiscFlags.STAT_VCU_MISC_MTC_ERROR) && !isTcMalfunctionShown) {
                    Log.e(TAG, "[MGR] ERROR: MTC Failure detected")
                    show(ClusterNotification.Params("TC MALFUNCTION", "Please contact customer support"))
                    isTcMalfunctionShown = true
                }

                /*if ((miscInfo.hasFlag(VcuStatusFlags.STAT_VCU_ABS_REAR_WHEEL_SPEED_SENSOR_FAILURE) ||
                            miscInfo.hasFlag(VcuStatusFlags.STAT_VCU_ABS_FRONT_WHEEL_SPEED_SENSOR_FAILURE)) && !isAbsTcMalfunctionShown) {
                    Log.e(TAG, "[MGR] ERROR: ABS/TC Sensor Failure")
                    show(ClusterNotification.Params("ABS AND TC MALFUNCTION", "Please contact customer support"))
                    isAbsTcMalfunctionShown = true
                }*/
            }
        }

        // 6. Hill Hold & Malfunctions
        scope.launch {
            carViewModel.hillHoldState.collect { hillHoldState ->
                if (sharedViewModel.hasThemeConfigChanged) return@collect
                if (hillHoldState == 1) {
                    show(
                        ClusterNotification.Params(
                            "DISENGAGING HILL HOLD IN 5S",
                            "Apply brakes for your safety"
                        )
                    )
                }
            }
        }
    }

    private fun showForChargeFlag(flag: ChargeStatusFlag) {
        val params = when (flag) {
            //ChargeStatusFlag.EXT_CHRG_CP_VALID_FAULT -> ClusterNotification.Params("CHARGING ERROR", "Re-plug Charger")
            ChargeStatusFlag.EXT_CHRG_OVER_TEMPERATURE -> ClusterNotification.Params("CHARGER TEMP HIGH", "Cool off charger")
            ChargeStatusFlag.EXT_CHRG_TIMEOUT_ERROR, ChargeStatusFlag.EXT_CHRG_HARDWARE_FAILURE ->
                ClusterNotification.Params("CHARGING ERROR", "Power off for 1 min")
            else -> return
        }
        show(params)
    }

    private fun buildSideStandParams() = ClusterNotification.Params("SIDE STAND DEPLOYED", "Motor disarmed")
    private fun isBitSet(value: ULong, bit: Int): Boolean = ((value shr bit) and 1uL) == 1uL
    private fun isBitSet(value: UInt, bit: Int): Boolean = ((value shr bit) and 1u) == 1u
}