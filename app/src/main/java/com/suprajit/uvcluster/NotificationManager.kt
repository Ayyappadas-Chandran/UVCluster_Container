package com.suprajit.uvcluster

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.navigation.NavController
import com.suprajit.uvcluster.domain.ennumerate.ButtonNavigation
import com.suprajit.uvcluster.domain.ennumerate.VcuMiscFlags
import com.suprajit.uvcluster.ui.viewModel.CarViewModel
import com.suprajit.uvcluster.ui.viewModel.SharedViewModel
import com.suprajit.uvcluster.utils.Utilities
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
    private var navController: NavController? = null
    private var prevSoc: Int? = null
    private var prevRegenUnavailable: Int = 0
    private var prevModeHover: Int = 0
    private var prevRideModes: Int = -1
    private var prevHillHold: Int = -1
    private var isCharging = false

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
                try {
                    // 1. Handle Theme Change Replay
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
                        mcNoArm[8] == 0 -> ClusterNotification.Params("Charger lid open", "Close lid to start riding")
                        else -> null
                    }

                    if (params == null) {
                        Log.e(TAG, "[MGR] MOTOR_ARM: All clear, dismissing UI")
                        dismiss()
                    } else {
                        show(params)
                    }
                } catch (e: IndexOutOfBoundsException) {
                    Log.e(TAG, "[MGR] MOTOR_ARM: Error - Received malformed array with size ${mcNoArm.size}", e)
                    // Optional: Dismiss or show a generic error if the data is corrupt
                } catch (e: Exception) {
                    Log.e(TAG, "[MGR] MOTOR_ARM: Unexpected error", e)
                }
            }
        }

        // 3. Battery SOC
        scope.launch {
            carViewModel.tellTales.collect { tellTales ->
                if (sharedViewModel.hasThemeConfigChanged) return@collect

                // Null safety for object + SOC
                val soc = tellTales?.batterySoc ?: return@collect
                if (soc == 0) return@collect

                // Normalize nullable signals (important for stability)
                val regenUnavailable = tellTales.regenUnavailable ?: 0
                val modeHover = tellTales.modeHover ?: 0
                val rideModes = tellTales.availableRideModes ?: -1
                val hillHold = tellTales.hillHold ?: -1

                // -------------------------------
                // SOC transition
                // -------------------------------
                if (prevSoc != soc) {
                    if (soc == 11) {
                        show(
                            ClusterNotification.Params(
                                "SWITCHING TO GLIDE",
                                "At 10% battery"
                            )
                        )
                    }
                    prevSoc = soc
                }

                // -------------------------------
                // Regen availability transition
                // -------------------------------
                if (prevRegenUnavailable != regenUnavailable) {
                    if (regenUnavailable == 1) {
                        show(
                            ClusterNotification.Params(
                                "REGEN UNAVAILABLE",
                                " "
                            )
                        )
                    } else {
                        show(
                            ClusterNotification.Params(
                                "REGEN AVAILABLE",
                                "Adjust levels with nav keys"
                            )
                        )
                    }
                    prevRegenUnavailable = regenUnavailable
                }

                // -------------------------------
                // Hover mode transition
                // -------------------------------
                if (prevModeHover != modeHover) {
                    if (modeHover == 1) {
                        show(
                            ClusterNotification.Params(
                                "BATTERY CRITICALLY LOW",
                                "Hover mode initiated"
                            )
                        )
                    }
                    prevModeHover = modeHover
                }

                // -------------------------------
                // Ride mode transition
                // -------------------------------
                if (prevRideModes != rideModes) {
                    if (rideModes == 4) {
                        show(
                            ClusterNotification.Params(
                                "SWITCHED TO GLIDE",
                                " "
                            )
                        )
                    }
                    prevRideModes = rideModes
                }

                // -------------------------------
                // Hill hold transition
                // -------------------------------
                if (prevHillHold != hillHold) {
                    if (hillHold == 4) {
                        show(
                            ClusterNotification.Params(
                                "DISENGAGING HILL HOLD IN 5S",
                                "Apply brakes for your safety"
                            )
                        )
                    }
                    prevHillHold = hillHold
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
                    //show(ClusterNotification.Params("TC MALFUNCTION", "Please contact customer support"))
                    isTcMalfunctionShown = true
                }
                /*if (miscInfo.hasFlag(VcuMiscFlags.STAT_VCU_CHARGER_FLAP_OPENED) && !isCharging) {
                    Log.e(TAG, "[MGR] ERROR: Charger flap opened")
                    show(ClusterNotification.Params("MOTOR ARM ERROR", "CHARGER LID OPENED"))
                }*/

                /*if ((miscInfo.hasFlag(VcuStatusFlags.STAT_VCU_ABS_REAR_WHEEL_SPEED_SENSOR_FAILURE) ||
                            miscInfo.hasFlag(VcuStatusFlags.STAT_VCU_ABS_FRONT_WHEEL_SPEED_SENSOR_FAILURE)) && !isAbsTcMalfunctionShown) {
                    Log.e(TAG, "[MGR] ERROR: ABS/TC Sensor Failure")
                    show(ClusterNotification.Params("ABS AND TC MALFUNCTION", "Please contact customer support"))
                    isAbsTcMalfunctionShown = true
                }*/
            }
        }
        scope.launch {
            carViewModel.swiftButton.collect { swiftButton ->
                val button = Utilities.getButtonState(swiftButton)
                if (button == ButtonNavigation.None) return@collect
                handleButtonNavigation(button.ordinal)
            }
        }

        // 6. Hill Hold & Malfunctions
    /*    scope.launch {
            carViewModel.tellTales.collect { telltales ->
                if (sharedViewModel.hasThemeConfigChanged) return@collect
                if (telltales.hillHold == 4) {
                    show(
                        ClusterNotification.Params(
                            "DISENGAGING HILL HOLD IN 5S",
                            "Apply brakes for your safety"
                        )
                    )
                }
            }
        }*/
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
    private fun handleButtonNavigation(button: Int) {
        when (button) {

            ButtonNavigation.Enter.ordinal -> if (navController?.currentDestination?.id == R.id.dashboardFragment){
                show(
                    ClusterNotification.Params(
                        "Cruise Control Unavailable",
                        ""
                    )
                )
            }
        }
    }

    private fun buildSideStandParams() = ClusterNotification.Params("SIDE STAND DEPLOYED", "Motor disarmed")
    private fun isBitSet(value: ULong, bit: Int): Boolean = ((value shr bit) and 1uL) == 1uL
    private fun isBitSet(value: UInt, bit: Int): Boolean = ((value shr bit) and 1u) == 1u
}


