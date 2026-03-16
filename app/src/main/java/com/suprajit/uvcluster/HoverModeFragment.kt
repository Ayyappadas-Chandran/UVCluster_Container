package com.suprajit.uvcluster

import android.os.Bundle
import android.util.Log.d
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.suprajit.uvcluster.domain.dataModel.RangeLimit
import com.suprajit.uvcluster.domain.dataModel.vcuData.VcuInfoMsg
import com.suprajit.uvcluster.ui.viewModel.CarViewModel
import com.suprajit.uvcluster.ui.viewModel.SharedViewModel
import com.suprajit.uvcluster.utils.Utilities.applyMinMax
import com.suprajit.uvcluster.utils.ViewModelFactory
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class HoverModeFragment : Fragment() {
    private lateinit var tvOdo: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvSpeedUnit: TextView
    var unit = ""
    private var speed = 0
    private val carViewModel: CarViewModel by activityViewModels { ViewModelFactory(requireContext()) }
    private val sharedViewModel: SharedViewModel by activityViewModels { ViewModelFactory(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hover_mode, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initObserver()
    }

    private fun initView(view: View) {
        tvSpeed = view.findViewById(R.id.tvSpeed)
        tvOdo = view.findViewById(R.id.tvODo)
        tvSpeedUnit = view.findViewById(R.id.tvSpeedUnit)
        tvSpeed.setOnClickListener { findNavController().navigateUp() }
        unit = sharedViewModel.distanceUnit
    }

    private fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    carViewModel.vehicleValue.collect { vehicleValue ->
                        updateVehicleValue(vehicleValue)
                    }
                }
                launch {
                    carViewModel.vcuInfoMsg.collect { vcuInfoMsg ->
                        updateVcuMsg(vcuInfoMsg)
                    }
                }
                launch {
                    carViewModel.tellTales.collect { tellTales ->
                        val isHoverMode = tellTales.modeHover == 1
                        val isMotorArmed = tellTales.motorArmed == 1
                        //val isHoverMode = true
                        if (!isHoverMode) {
                            findNavController().navigate(R.id.dashboardFragment)

                        }
                        if (isMotorArmed) {
                            val typedValue = TypedValue()
                            requireContext().theme.resolveAttribute(
                                R.attr.appTextColor,
                                typedValue,
                                true
                            )
                            if (tvSpeed.text == "- - -") tvSpeed.text = "000"
                         //   tvSpeed.setTextColor(typedValue.data)
                        } else {
                            tvSpeed.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.lightGreyMedium
                                )
                            )
                            if (speed == 0) {
                                tvSpeed.text = "- - -"
                            }
                        }


                    }


                }
            }
        }
    }

    private fun updateVehicleValue(value: FloatArray = floatArrayOf()) {
        d("update speed", "speed")
        val isMiles = unit == "miles"
        tvSpeedUnit.text = if (isMiles) "mph" else "km/h"


        if (value.size < 4) return
        if (::tvSpeed.isInitialized) {


            val rawSpeedKm = value.getOrNull(0)?.toInt()
            val finalSpeedKm =
                rawSpeedKm?.applyMinMax(sharedViewModel.speedLimit) ?: 0
            speed = finalSpeedKm

            val displaySpeed = if (isMiles) {
                (finalSpeedKm * 0.621371f).roundToInt()
            } else {
                finalSpeedKm
            }

            tvSpeed.text = String.format("%03d", displaySpeed)

        }
    }


    fun updateVcuMsg(vcuInfoMsg: VcuInfoMsg) {

        val rawOdometerKm = vcuInfoMsg.odometer.toInt()
        val finalOdoKm = rawOdometerKm.applyMinMax(sharedViewModel.odoLimit)

        val isMiles = unit == "miles"

        val displayOdo = if (isMiles) {
            (finalOdoKm * 0.621371f).roundToInt()
        } else {
            finalOdoKm
        }

        val unitLabel = if (isMiles) "miles" else "km"

        tvOdo.text = "$displayOdo $unitLabel"

    }


}


