package com.suprajit.uvcluster

import android.Manifest
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.suprajit.uvcluster.domain.dataModel.vcuData.VcuInfoMsg
import com.suprajit.uvcluster.ui.viewModel.CarViewModel
import com.suprajit.uvcluster.ui.viewModel.SharedViewModel
import com.suprajit.uvcluster.utils.Utilities.ARG_BALLISTIC_PLUS
import com.suprajit.uvcluster.utils.Utilities.applyMinMax
import com.suprajit.uvcluster.utils.ViewModelFactory
import kotlinx.coroutines.launch

class DashCamFragment : Fragment() {
    private val carViewModel: CarViewModel by activityViewModels { ViewModelFactory(requireContext()) }
    private val sharedViewModel: SharedViewModel by activityViewModels {
        ViewModelFactory(
            requireContext()
        )
    }
    private lateinit var tvRange: TextView
    private lateinit var tvOdo: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvWhKm: TextView
    private lateinit var tvRangeBallistic: TextView
    private lateinit var tvSpeedBallistic: TextView
    private lateinit var tvRide: TextView
    private lateinit var tvODo: TextView
    private lateinit var tvFrontBallistic: TextView
    private lateinit var ivRegenLevel1: ImageView
    private lateinit var ivRegenLevel2: ImageView
    private lateinit var ivRegenLevel3: ImageView
    private lateinit var ivRegenLevel4: ImageView
    private lateinit var ivRegenLevel5: ImageView
    private lateinit var ivRegenLevel6: ImageView
    private lateinit var ivRegenLevel7: ImageView
    private lateinit var ivRegenLevel8: ImageView
    private lateinit var ivRegenLevel9: ImageView
    private lateinit var clDashcamWithBallistic: ConstraintLayout
    private lateinit var clDashcamWithoutBallistic: ConstraintLayout


    private lateinit var textureView: TextureView
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var isBallistic = false

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
        }
    }
    private val surfaceTextureListener: TextureView.SurfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
            @RequiresPermission(Manifest.permission.CAMERA)
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dash_cam, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObserver()
        initView(view)
        initClickListeners()
        initCamera()
        isBallistic = arguments?.getBoolean(ARG_BALLISTIC_PLUS) ?: false
        if (isBallistic) {
            clDashcamWithBallistic.visibility = View.VISIBLE
            clDashcamWithoutBallistic.visibility = View.INVISIBLE
        } else {
            clDashcamWithBallistic.visibility = View.INVISIBLE
            clDashcamWithoutBallistic.visibility = View.VISIBLE
        }
    }

    private fun initView(view: View) {
        tvRange = view.findViewById(R.id.tvRange)
        tvOdo = view.findViewById(R.id.tvOdo)
        tvSpeed = view.findViewById(R.id.tvSpeed)
        tvWhKm = view.findViewById(R.id.tvWhKm)
        tvRangeBallistic = view.findViewById(R.id.tvRangeBallistic)
        tvSpeedBallistic = view.findViewById(R.id.tvSpeedBallistic)
        tvRide = view.findViewById(R.id.tvRide)
        tvODo = view.findViewById(R.id.tvODo)
        tvFrontBallistic = view.findViewById(R.id.tvFrontBallistic)
        ivRegenLevel1 = view.findViewById(R.id.ivRegenLevel1)
        ivRegenLevel2 = view.findViewById(R.id.ivRegenLevel2)
        ivRegenLevel3 = view.findViewById(R.id.ivRegenLevel3)
        ivRegenLevel4 = view.findViewById(R.id.ivRegenLevel4)
        ivRegenLevel5 = view.findViewById(R.id.ivRegenLevel5)
        ivRegenLevel6 = view.findViewById(R.id.ivRegenLevel6)
        ivRegenLevel7 = view.findViewById(R.id.ivRegenLevel7)
        ivRegenLevel8 = view.findViewById(R.id.ivRegenLevel8)
        ivRegenLevel9 = view.findViewById(R.id.ivRegenLevel9)
        clDashcamWithBallistic = view.findViewById(R.id.clDashcamWithBallistic)
        clDashcamWithoutBallistic = view.findViewById(R.id.clDashCamWithoutBallistic)
        textureView = view.findViewById(R.id.textureView)
    }

    private fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    carViewModel.vcuInfoMsg.collect { vcuInfoMsg ->
                        updateVcuMsg(vcuInfoMsg)
                    }
                }
                launch {
                    carViewModel.vehicleValue.collect { vehicleValue ->
                        updateVehicleValue(vehicleValue)
                    }
                }
            }
        }
    }

    fun updateVcuMsg(vcuInfoMsg: VcuInfoMsg) {
        val rawOdometer = vcuInfoMsg.odometer.toInt()
        val rawRange = vcuInfoMsg.range.toInt()
        val finalOdo = rawOdometer.applyMinMax(sharedViewModel.odoLimit)
        val finalRange = rawRange.applyMinMax(sharedViewModel.rangeLimit)
        if (isBallistic) {
            tvODo.text = finalOdo.toString()
            tvRangeBallistic.text = finalRange.toString()
        } else {
            tvOdo.text = finalOdo.toString()
            tvRange.text = finalRange.toString()
        }
        if (vcuInfoMsg.speed.isNotEmpty()) {
            val isMiles =
                sharedViewModel.distanceUnit.equals("miles", ignoreCase = true)
            val unitText = if (isMiles) "mph" else "km/h"

            if (vcuInfoMsg.speed[0].toInt() == 0)
            {
                tvSpeed.text = "000"
                //tvSpeedUnit.text = unitText
            }
        }
    }

    private fun updateVehicleValue(value: FloatArray = floatArrayOf()) {
        if (value.isEmpty()) return
        if (value.size < 4) {
            return
        }
        if (::tvSpeed.isInitialized) {
            val rawSpeed = value.getOrNull(0)?.toInt()
            val finalSpeed = rawSpeed?.applyMinMax(sharedViewModel.speedLimit)
            if (isBallistic) {
                tvSpeedBallistic.text = finalSpeed?.let { String.format("%03d", it) } ?: "-"
            } else {
                tvSpeed.text = finalSpeed?.let { String.format("%03d", it) } ?: "-"
            }
        }
    }

    private fun initClickListeners() {
        tvSpeed.setOnClickListener { findNavController().navigate(R.id.advancedFeaturesFragment) }
        tvSpeedBallistic.setOnClickListener { findNavController().navigate(R.id.advancedFeaturesFragment) }

    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun openCamera() {
        try {
            val cameraId = cameraManager.cameraIdList[0]  // First camera
            cameraManager.openCamera(cameraId, stateCallback, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initCamera() {
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private fun createPreviewSession() {
        val texture = textureView.surfaceTexture!!
        texture.setDefaultBufferSize(1920, 1080)

        val surface = Surface(texture)

        previewRequestBuilder =
            cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

        previewRequestBuilder!!.addTarget(surface)

        cameraDevice!!.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    previewRequestBuilder!!.set(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_AUTO
                    )

                    session.setRepeatingRequest(
                        previewRequestBuilder!!.build(),
                        null,
                        null
                    )
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            null
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        captureSession?.close()
        cameraDevice?.close()
    }

}


