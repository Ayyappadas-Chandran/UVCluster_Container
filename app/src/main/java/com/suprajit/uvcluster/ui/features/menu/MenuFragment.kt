package com.suprajit.uvcluster.ui.features.menu

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.suprajit.uvcluster.R
import com.suprajit.uvcluster.domain.dataModel.vcuData.TellTales
import com.suprajit.uvcluster.domain.ennumerate.ButtonNavigation
import com.suprajit.uvcluster.ui.features.MainActivity
import com.suprajit.uvcluster.ui.viewModel.CarViewModel
import com.suprajit.uvcluster.ui.viewModel.SharedViewModel
import com.suprajit.uvcluster.utils.Utilities
import com.suprajit.uvcluster.utils.Utilities.ARG_BALLISTIC_PLUS
import com.suprajit.uvcluster.utils.Utilities.applyMinMax
import com.suprajit.uvcluster.utils.Utilities.setOnSoundClickListener
import com.suprajit.uvcluster.utils.ViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class MenuFragment : Fragment() {
    private lateinit var tvBikeName: TextView
    private lateinit var tvBattery: TextView
    private lateinit var tvMedia: TextView
    private lateinit var tvNowPlaying: TextView
    private lateinit var tvConnected: TextView
    private lateinit var tvConnectedDevice: TextView
    private lateinit var tvControls: TextView
    private lateinit var tvAbs: TextView
    private lateinit var tvTractionControl: TextView
    private lateinit var tvRegen: TextView
    private lateinit var tvHillHold: TextView
    private lateinit var tvTpms: TextView
    private lateinit var tvTpmsFront: TextView
    private lateinit var tvTpmsRear: TextView
    private lateinit var tvNavigate: TextView
    private lateinit var tvBatteryLimit: TextView
    private lateinit var tvBatteryPercent: TextView
    private lateinit var tvTpmsFrontUnit: TextView
    private lateinit var tvTpmsRearValue: TextView
    private lateinit var tvTpmsRearUnit: TextView
    private lateinit var tvSongName: TextView
    private lateinit var tvAbsValue: TextView
    private lateinit var tvTractionValue: TextView
    private lateinit var tvHillHoldState: TextView
    private lateinit var tvRegenValue: TextView
    private lateinit var tvPercent: TextView
    private lateinit var tvTpmsFrontValue: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvNavigating: TextView
    private lateinit var ivMusicSelected: ImageView
    private lateinit var ivTpmsStateFront: ImageView
    private lateinit var ivTpmsStateRear: ImageView
    private lateinit var ivManeuver: ImageView
    private lateinit var pbBattery: ProgressBar
    private lateinit var clBgMyF77: ConstraintLayout
    private lateinit var clBgBattery: ConstraintLayout
    private lateinit var clBgSetting: ConstraintLayout
    private lateinit var clBgMedia: ConstraintLayout
    private lateinit var clBgControls: ConstraintLayout
    private lateinit var clBgTpms: ConstraintLayout
    private lateinit var clBgNavigate: ConstraintLayout
    private lateinit var clNavigationOff: ConstraintLayout
    private lateinit var clNavigateStart: ConstraintLayout
    private lateinit var clMenu: ConstraintLayout

    //   private lateinit var laF77Anim: LottieAnimationView
    private lateinit var laMusicAnim: LottieAnimationView
    private lateinit var laSettingsAnim: LottieAnimationView
    private lateinit var laNavigateAnim: LottieAnimationView
    private var navigateJob: Job? = null
    private var tpmsJob: Job? = null
    private lateinit var gestureDetector: GestureDetector
    private val animFade by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.fade) }
    private val animSlideUpReverse by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down_fade_reverse)
    }
    private val animSlideDownReverse by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_reverse)
    }
    private val animSlideUp by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade)
    }
    private val animSlideDown by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down_fade)
    }
    private val animPressScale by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.press_scale)
    }
    private val carViewModel by activityViewModels<CarViewModel> { ViewModelFactory(context = requireContext()) }
    private val animReversePressScale by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.release_scale)
    }
    private val sharedViewModel by activityViewModels<SharedViewModel> { ViewModelFactory(context = requireContext()) }
    private val viewModel by activityViewModels<MenuViewModel> { ViewModelFactory(context = requireContext()) }
    private var currentPosition = 0

    private val f77Map = mapOf(
        ButtonNavigation.Right to MenuPosition.Battery,
        ButtonNavigation.Bottom to MenuPosition.Music,
        ButtonNavigation.Enter to MenuPosition.Enter
    )

    private val batteryMap = mapOf(
        ButtonNavigation.Right to MenuPosition.Setting,
        ButtonNavigation.Bottom to MenuPosition.Controls,
        ButtonNavigation.Left to MenuPosition.MyF77,
        ButtonNavigation.Enter to MenuPosition.Enter
    )

    private val settingMap = mapOf(
        ButtonNavigation.Left to MenuPosition.Battery,
        ButtonNavigation.Bottom to MenuPosition.Tpms,
        ButtonNavigation.Enter to MenuPosition.Enter
    )

    private val musicMap = mapOf(
        ButtonNavigation.Right to MenuPosition.Controls,
        ButtonNavigation.Top to MenuPosition.MyF77,
        ButtonNavigation.Enter to MenuPosition.Enter
    )

    private val controlMap = mapOf(
        ButtonNavigation.Right to MenuPosition.Tpms,
        ButtonNavigation.Left to MenuPosition.Music,
        ButtonNavigation.Top to MenuPosition.Battery,
        ButtonNavigation.Enter to MenuPosition.Enter
    )

    private val tpmsMap = mapOf(
        ButtonNavigation.Top to MenuPosition.Setting,
        ButtonNavigation.Bottom to MenuPosition.Navigate,
        ButtonNavigation.Left to MenuPosition.Controls,
        ButtonNavigation.Enter to MenuPosition.Enter
    )

    private val navigateMap = mapOf(
        ButtonNavigation.Left to MenuPosition.Controls,
        ButtonNavigation.Top to MenuPosition.Tpms,
        ButtonNavigation.Enter to MenuPosition.Enter
    )
    private var isBallistic = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_menu, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.handleToolbar(true)
        initView(view)
        initClickListener()
        initObserver()
        addSwipeGesture(clBgBattery)
        addSwipeGesture(clBgTpms)
        addSwipeGesture(clBgMedia)
        addSwipeGesture(clBgMyF77)
        addSwipeGesture(clMenu)
        addSwipeGesture(clBgControls)
        addSwipeGesture(clBgNavigate)
        addSwipeGesture(clBgSetting)
        showToolBar()
    }

    private fun showToolBar() {
        (activity as? MainActivity)?.handleToolbar(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addSwipeGesture(rootView: View?) {
        if (rootView == null) return

        val swipeThreshold = 50
        val swipeVelocityThreshold = 50

        val detector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onClick(rootView)
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val diffY = e2.y - e1.y
                    if (diffY > swipeThreshold && kotlin.math.abs(distanceY) > 0) {
                        onSwipeDown()
                        return true
                    }
                    return false
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val diffY = e2.y - e1.y
                    if (diffY > swipeThreshold && kotlin.math.abs(velocityY) > swipeVelocityThreshold) {
                        onSwipeDown()
                        return true
                    }
                    return false
                }
            }
        )

        rootView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            true
        }

        rootView.isClickable = true
        rootView.isFocusable = true
    }

    private fun onSwipeDown() {
        Log.d("MenuFragmentDown", "Down called")
        findNavController().navigate(R.id.dashboardFragment)
    }

    private fun onClick(rootview: View?) {
        when (rootview?.id) {
            R.id.clBgControls ->
                findNavController().navigate(R.id.controlFragment)

            R.id.clBgMedia ->
                findNavController().navigate(R.id.musicFragment)

            R.id.clBgMyF77 ->
                findNavController().navigate(R.id.myF77MenuFragment)

            R.id.clBgTpms ->
                findNavController().navigate(R.id.tpmsFragment)

            R.id.clBgBattery ->
                findNavController().navigate(R.id.batteryFragment)

            R.id.clMenu ->
                findNavController().navigate(R.id.dashboardFragment)

            R.id.clBgNavigate ->
                findNavController().navigate(R.id.mapFragment)

            R.id.clBgSetting ->
                findNavController().navigate(R.id.settingMenuFragment)


        }
    }


    /**
     * Binds UI components from the provided root view using their IDs.
     * @param view The root view containing the layout elements.
     * Initializes:
     * - tvBikeName, tvBattery, tvMedia, tvNowPlaying, tvConnected, tvConnectedDevice, (TextViews)
     * - tvControls, tvAbs, tvTractionControl, tvRegen, tvHillHold, tvTpms, tvTpmsFront, tvTpmsRear, (TextViews)
     * - tvNavigate, tvBatteryLimit, tvLockdown, tvBatteryPercent,tvNavigating,tvPreset,tvTpmsFrontValue (TextViews)
     * - tvTpmsRearValue, tvTpmsRearUnit, tvSongName, tvAbsValue, tvTractionValue, tvHillHoldState, tvRegenValue, (TextViews)
     * - tvPercent, tvPresetValue, tvDistance, (TextViews)
     * - ivMusicSelected, ivSetting, ivBike, ivNavigateMenu1, ivNavigateMenu (ImageViews)
     * - ivTpmsStateFront, ivTpmsStateRear (ImageViews)
     * - bgMyF77, bgBattery, bgSetting, bgMusic, bgControls, bgTpms, bgNavigate (ImageViews)
     */
    private fun initView(view: View) {
        tvBikeName = view.findViewById(R.id.tvBikeName)
        tvBattery = view.findViewById(R.id.tvBattery)
        tvMedia = view.findViewById(R.id.tvMedia)
        tvNowPlaying = view.findViewById(R.id.tvNowPlaying)
        tvConnected = view.findViewById(R.id.tvConnected)
        tvConnectedDevice = view.findViewById(R.id.tvConnectedDevice)
        tvControls = view.findViewById(R.id.tvControls)
        tvAbs = view.findViewById(R.id.tvAbs)
        tvTractionControl = view.findViewById(R.id.tvTractionControl)
        tvRegen = view.findViewById(R.id.tvRegen)
        tvHillHold = view.findViewById(R.id.tvHillHold)
        tvTpms = view.findViewById(R.id.tvTpms)
        tvTpmsFront = view.findViewById(R.id.tvTpmsFront)
        tvTpmsRear = view.findViewById(R.id.tvTpmsRear)
        tvNavigate = view.findViewById(R.id.tvNavigate)
        tvBatteryLimit = view.findViewById(R.id.tvBatteryLimit)
        tvNavigating = view.findViewById(R.id.tvNavigating)
        tvBatteryPercent = view.findViewById(R.id.tvBatteryPercent)
        tvTpmsFrontValue = view.findViewById(R.id.tvTpmsFrontValue)
        tvTpmsFrontUnit = view.findViewById(R.id.tvTpmsFrontUnit)
        tvTpmsRearValue = view.findViewById(R.id.tvTpmsRearValue)
        tvTpmsRearUnit = view.findViewById(R.id.tvTpmsRearUnit)
        tvSongName = view.findViewById(R.id.tvSongName)
        tvAbsValue = view.findViewById(R.id.tvAbsValue)
        tvTractionValue = view.findViewById(R.id.tvTractionValue)
        tvHillHoldState = view.findViewById(R.id.tvHillHoldState)
        tvRegenValue = view.findViewById(R.id.tvRegenValue)
        tvPercent = view.findViewById(R.id.tvPercent)
        tvDistance = view.findViewById(R.id.tvDistance)
        ivMusicSelected = view.findViewById(R.id.ivMusicSelect)
        ivTpmsStateFront = view.findViewById(R.id.ivTpmsStateFront)
        ivTpmsStateRear = view.findViewById(R.id.ivTpmsStateRear)
        ivManeuver = view.findViewById(R.id.ivManeuver)
        clBgMyF77 = view.findViewById(R.id.clBgMyF77)
        clBgBattery = view.findViewById(R.id.clBgBattery)
        clBgSetting = view.findViewById(R.id.clBgSetting)
        clBgMedia = view.findViewById(R.id.clBgMedia)
        clBgControls = view.findViewById(R.id.clBgControls)
        clBgTpms = view.findViewById(R.id.clBgTpms)
        clBgNavigate = view.findViewById(R.id.clBgNavigate)
        clNavigationOff = view.findViewById(R.id.clNavigationOff)
        clNavigateStart = view.findViewById(R.id.clNavigationStart)
        clMenu = view.findViewById(R.id.clMenu)
        pbBattery = view.findViewById(R.id.pbBattery)
        //laF77Anim = view.findViewById(R.id.laF77Anim)
        laMusicAnim = view.findViewById(R.id.laMusicAnim)
        laSettingsAnim = view.findViewById(R.id.laSettingsAnim)
        laNavigateAnim = view.findViewById(R.id.laNavigateAnim)
        tvBatteryLimit.text = "Limit ${sharedViewModel.batteryLimit} %"
       // tvRegenValue.text = "R${sharedViewModel.regenValue}"
       // tvTractionMode.text ="${sharedViewModel.rideMode}"
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.handleToolbar(true)
        initAnimation()
        updateCurrentState()

        // simulateTpms()
    }

    /**
     * Initializes the animation for the UI elements.
     */
    fun initAnimation() {
        val value = viewModel.currentMenuPosition.value
        when (value) {
            MenuPosition.Setting.ordinal -> {
                doSlideDownReverseAnim(clBgMyF77)
                // doSlideDownReverseAnim(laF77Anim)
                doSlideDownReverseAnim(clBgBattery)
                doSlideDownReverseAnim(clBgSetting)
                doSlideDownReverseAnim(laSettingsAnim)
                doSlideUpReverseAnim(clBgMedia)
                doSlideUpReverseAnim(clBgControls)
                doSlideUpReverseAnim(clBgTpms)
                doSlideUpReverseAnim(clBgNavigate)
                doSlideUpReverseAnim(laNavigateAnim)
                doSlideUpReverseAnim(laMusicAnim, true) {
                    handleSettingsAnimation(R.raw.setting_icon_entry_anim)
                }
            }

            MenuPosition.MyF77.ordinal -> {
                clBgMyF77.startAnimation(animReversePressScale)
                //laF77Anim.startAnimation(animReversePressScale)
                doSlideDownReverseAnim(clBgBattery)
                doSlideDownReverseAnim(clBgSetting)
                doSlideDownReverseAnim(laSettingsAnim)
                doSlideUpReverseAnim(clBgMedia)
                doSlideUpReverseAnim(clBgControls)
                doSlideUpReverseAnim(clBgTpms)
                doSlideUpReverseAnim(clBgNavigate)
                doSlideUpReverseAnim(laNavigateAnim)
                doSlideUpReverseAnim(laMusicAnim, true) {
                    handleMyF77Animation(R.raw.f77_headlight_entry_anim)
                }
            }

            MenuPosition.Music.ordinal -> {
                handleMusicAnimation(R.raw.music_icon_entry_anim)
            }

            MenuPosition.Navigate.ordinal -> {
                handleNavigateAnimation(R.raw.navigation_icon_entry_anim)
            }
        }
    }

    private fun doSlideUpAnim(
        view: View,
        shouldListenAnim: Boolean = false,
        actionOnEnd: (() -> Unit)? = null
    ) {
        if (shouldListenAnim) {
            animSlideUp.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    actionOnEnd?.invoke()
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })
        }
        view.startAnimation(animSlideUp)
    }

    private fun doSlideDownAnim(
        view: View,
        shouldListenAnim: Boolean = false,
        actionOnEnd: (() -> Unit)? = null
    ) {
        if (shouldListenAnim) {
            animSlideDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {
                    actionOnEnd?.invoke()
                }

                override fun onAnimationRepeat(animation: Animation?) {

                }
            })
        }
        view.startAnimation(animSlideDown)
    }


    private fun doSlideUpReverseAnim(
        view: View,
        shouldListenAnim: Boolean = false,
        actionOnEnd: (() -> Unit)? = null
    ) {
        if (shouldListenAnim) {
            animSlideUpReverse.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {
                    actionOnEnd?.invoke()
                }

                override fun onAnimationRepeat(animation: Animation?) {

                }
            })
        }
        view.startAnimation(animSlideUpReverse)
    }

    private fun doSlideDownReverseAnim(
        view: View,
        shouldListenAnim: Boolean = false,
        actionOnEnd: (() -> Unit)? = null
    ) {
        if (shouldListenAnim) {
            animSlideDownReverse.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    actionOnEnd?.invoke()
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })
        }
        view.startAnimation(animSlideDownReverse)
    }

    private fun doPressScaleAnim(
        view: View,
        shouldListenAnim: Boolean = false,
        actionOnEnd: (() -> Unit)? = null
    ) {
        if (shouldListenAnim) {
            animPressScale.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    Log.d("Animation", "pressScaleAnim onAnimationStart")
                    actionOnEnd?.invoke()
                }

                override fun onAnimationEnd(animation: Animation?) {
                    Log.d("Animation", "pressScaleAnim onAnimationEnd")
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })
        }
        view.startAnimation(animPressScale)
    }

    /**
     * Observes changes to the selected menu position from the [MainViewModel].
     *
     * Based on the emitted menu position, this function triggers the corresponding UI update function:
     * - `selectMyF77()` for the MyF77 menu
     * - `selectBattery()` for the Battery menu
     * - `selectSetting()` for the Settings menu
     * - `selectMusic()` for the Music menu
     * - `selectControls()` for the Controls menu
     * - `selectTpms()` for the TPMS menu
     * - `selectNavigate()` for the Navigation menu
     *
     * This allows the UI to reactively update as the user navigates through different sections.
     */

    private fun initObserver() {
        viewModel.currentMenuPosition.observe(viewLifecycleOwner) { position ->
            d("CurrentPosition", "Position:$position")
            currentPosition = position
            selectedPosition(position)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    carViewModel.swiftButton.collect { swiftButton ->
                        val button = Utilities.getButtonState(swiftButton)
                        if (button == ButtonNavigation.None) return@collect
                        handleButtonNavigation(button.ordinal)
                    }
                }
                launch {
                    carViewModel.tellTales.collect { telltale ->
                        // updateBatteryValue(telltale)
                      /*  updateTractionControl(telltale)
                        updateAbs(telltale)*/
                        isBallistic = telltale.rideMode == 3
                    }
                }

                launch {
                    carViewModel.imxDbgMsg.collect { imxDbgMsg ->
                        val batterySoc = (imxDbgMsg.soc.toInt() and 0xFF)
                        pbBattery.progress = batterySoc
                        tvBatteryPercent.text = batterySoc.toString()
                    }
                }
            }
        }
    }

    private fun updateCurrentState() {
        val hillHold = viewModel.hillHoldState
        tvHillHoldState.text = if (!hillHold) getString(R.string.off) else getString(R.string.on)
        val absState = viewModel.absState
        tvAbsValue.text = if(absState) getString(R.string.mono) else getString(R.string.dual)
        val regenValue = viewModel.regenLevel
        tvRegenValue.text = "R$regenValue"
        val tractionLevel = viewModel.tractionLevel
        val tractionLevelValue = when(tractionLevel) {
            getString(R.string.tc1) -> "T1"
            getString(R.string.tc2) -> "T2"
            getString(R.string.tc3) -> "T3"
            else -> "Off"
        }

        tvTractionValue.text = tractionLevelValue
    }

    private fun updateAbs(telltale: TellTales) {
        val abs = telltale.absMode
        val absWarning = telltale.absWarningLamp
        if (absWarning == 0x00) {
            if (abs == 0x00) tvAbsValue.text = getString(R.string.dual) else tvAbsValue.text =
                getString(R.string.mono)
        } else {
            tvAbsValue.text = getString(R.string.off)
        }
    }

    private fun updateTractionControl(t: TellTales) {
        val (value, _) = when (t.mtcMode) {
            0x01 -> "OFF" to R.string.triple_hypen
            0x02 -> "T1" to R.string.sport
            0x03 -> "T2" to R.string.street
            0x04 -> "T3" to R.string.rain
            else -> "OFF" to R.string.triple_hypen
        }
        tvTractionValue.text = value
        //tvTractionMode.text = getString(mode)
    }


    private fun updateBatteryValue(tellTale: TellTales) {
        val batteryLevel = tellTale.batterySoc
        val finalBatteryLevel = batteryLevel.applyMinMax(sharedViewModel.socLimit)
        pbBattery.progress = finalBatteryLevel
        tvBatteryPercent.text = finalBatteryLevel.toString()
    }

    private fun selectedPosition(position: Int?) {
        when (position) {
            MenuPosition.MyF77.ordinal -> {
                selectMyF77()
            }

            MenuPosition.Battery.ordinal -> {
                selectBattery()
            }

            MenuPosition.Setting.ordinal -> {
                selectSetting()
            }

            MenuPosition.Music.ordinal -> {
                selectMusic()
            }

            MenuPosition.Controls.ordinal -> {
                selectControls()
            }

            MenuPosition.Tpms.ordinal -> {
                selectTpms()
            }

            MenuPosition.Navigate.ordinal -> {
                selectNavigate()
            }
        }
    }

    fun handleButtonNavigation(button: Int) {
        if (button == ButtonNavigation.Back.ordinal) {
            findNavController().navigate(R.id.dashboardFragment)
        }
        var lastPosition = viewModel.lastMenuPosition
        when (currentPosition) {
            MenuPosition.MyF77.ordinal -> {
                if (button == ButtonNavigation.Enter.ordinal) {
                    viewModel.lastMenuPosition = lastPosition
                    findNavController().navigate(R.id.action_menuFragment_to_myF77MenuFragment)
                    return
                }
                f77Map[ButtonNavigation.values()[button]]?.let { position ->
                    viewModel.handleCurrentMenuPosition(position.ordinal)
                    lastPosition = position.ordinal
                } ?: return
                viewModel.lastMenuPosition = lastPosition
                refreshLastPosition(MenuPosition.MyF77.ordinal)
            }

            MenuPosition.Battery.ordinal -> {
                if (button == ButtonNavigation.Enter.ordinal) {
                    viewModel.lastMenuPosition = lastPosition
                    findNavController().navigate(R.id.action_menuFragment_to_batteryFragment)
                    return
                }
                batteryMap[ButtonNavigation.values()[button]]?.let { position ->
                    viewModel.handleCurrentMenuPosition(position.ordinal)
                    lastPosition = position.ordinal
                } ?: return
                viewModel.lastMenuPosition = lastPosition
                refreshLastPosition(MenuPosition.Battery.ordinal)
            }

            MenuPosition.Setting.ordinal -> {
                if (button == ButtonNavigation.Enter.ordinal) {
                    sharedViewModel.handleSettingsChildClick(false)
                    viewModel.lastMenuPosition = lastPosition
                    findNavController().navigate(R.id.action_menuFragment_to_settingMenuFragment)
                    return
                }
                settingMap[ButtonNavigation.values()[button]]?.let { position ->
                    viewModel.handleCurrentMenuPosition(position.ordinal)
                    lastPosition = position.ordinal
                } ?: return
                viewModel.lastMenuPosition = lastPosition
                refreshLastPosition(MenuPosition.Setting.ordinal)
            }

            MenuPosition.Music.ordinal -> {
                if (button == ButtonNavigation.Enter.ordinal) {
                    viewModel.lastMenuPosition = lastPosition
                    findNavController().navigate(R.id.musicFragment)
                    return
                }
                musicMap[ButtonNavigation.values()[button]]?.let { position ->
                    viewModel.handleCurrentMenuPosition(position.ordinal)
                    lastPosition = position.ordinal
                } ?: return
                viewModel.lastMenuPosition = lastPosition
                refreshLastPosition(MenuPosition.Music.ordinal)
            }

            MenuPosition.Controls.ordinal -> {
                if (button == ButtonNavigation.Enter.ordinal) {
                    viewModel.lastMenuPosition = lastPosition
                    findNavController().navigate(R.id.action_menuFragment_to_controlFragment)
                    return
                }
                controlMap[ButtonNavigation.values()[button]]?.let { position ->
                    viewModel.handleCurrentMenuPosition(position.ordinal)
                    lastPosition = position.ordinal
                } ?: return
                viewModel.lastMenuPosition = lastPosition
                refreshLastPosition(MenuPosition.Controls.ordinal)
            }

            MenuPosition.Tpms.ordinal -> {
                if (button == ButtonNavigation.Enter.ordinal) {
                    viewModel.lastMenuPosition = lastPosition
                    findNavController().navigate(R.id.action_menuFragment_to_tpmsFragment)
                    return
                }
                tpmsMap[ButtonNavigation.values()[button]]?.let { position ->
                    viewModel.handleCurrentMenuPosition(position.ordinal)
                    lastPosition = position.ordinal
                } ?: return
                viewModel.lastMenuPosition = lastPosition
                refreshLastPosition(MenuPosition.Tpms.ordinal)
            }

            MenuPosition.Navigate.ordinal -> {
                if (button == ButtonNavigation.Enter.ordinal) {
                    viewModel.lastMenuPosition = lastPosition
                    return
                }
                navigateMap[ButtonNavigation.values()[button]]?.let { position ->
                    viewModel.handleCurrentMenuPosition(position.ordinal)
                    lastPosition = position.ordinal
                } ?: return
                viewModel.lastMenuPosition = lastPosition
                refreshLastPosition(MenuPosition.Navigate.ordinal)
            }
        }
    }

    private fun initClickListener() {
        val menuMap = mapOf(
            clBgMyF77 to MenuPosition.MyF77,
            clBgBattery to MenuPosition.Battery,
            clBgSetting to MenuPosition.Setting,
            clBgMedia to MenuPosition.Music,
            clBgControls to MenuPosition.Controls,
            clBgTpms to MenuPosition.Tpms,
            clBgNavigate to MenuPosition.Navigate
        )
        menuMap.forEach { (view, menuItem) ->
            view.setOnSoundClickListener(requireContext()) {
                val newPosition = menuItem.ordinal
                refreshLastPosition(viewModel.lastMenuPosition)
                viewModel.handleCurrentMenuPosition(newPosition)
                viewModel.lastMenuPosition = newPosition
                when (menuItem) {
                    MenuPosition.MyF77 -> animateAndNavigateToMyF77()
                    MenuPosition.Battery -> findNavController().navigate(R.id.action_menuFragment_to_batteryFragment)
                    MenuPosition.Setting -> {
                        sharedViewModel.handleSettingsChildClick(true)
                        animateAndNavigateToSettingMenu()
                    }

                    MenuPosition.Music -> animateAndNavigateToMedia()
                    MenuPosition.Controls -> findNavController().navigate(R.id.action_menuFragment_to_controlFragment)
                    MenuPosition.Tpms -> findNavController().navigate(R.id.action_menuFragment_to_tpmsFragment)
                    MenuPosition.Navigate -> animationAndNavigateToNavigation()
                    MenuPosition.Enter -> return@setOnSoundClickListener
                }
            }
        }
    }


    private fun animateAndNavigateToMedia() {
        handleMusicAnimation(R.raw.music_icon_entry_anim) {
            findNavController().navigate(R.id.musicFragment)
        }
    }

    private fun animationAndNavigateToNavigation() {
        handleNavigateAnimation(R.raw.navigation_icon_entry_anim) {
            laNavigateAnim.isVisible = false
            clNavigationOff.isVisible = false
            val bundle = Bundle()
            bundle.putBoolean(ARG_BALLISTIC_PLUS, isBallistic)
            findNavController().navigate(R.id.mapFragment, bundle)
            //simulateNavigation()
        }
    }

    private fun handleNavigateAnimation(animJsonPath: Int, actionOnEnd: (() -> Unit)? = null) {
        if (laNavigateAnim.isAnimating) {
            laNavigateAnim.cancelAnimation()
        }
        laNavigateAnim.removeAllAnimatorListeners()
        laNavigateAnim.isVisible = true
        laNavigateAnim.setAnimation(animJsonPath)
        laNavigateAnim.speed = 0.75f
        laNavigateAnim.playAnimation()
        laNavigateAnim.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
            }

            override fun onAnimationEnd(p0: Animator) {
                actionOnEnd?.invoke()
            }

            override fun onAnimationCancel(p0: Animator) {
            }

            override fun onAnimationRepeat(p0: Animator) {
            }
        })
    }

    private fun handleMusicAnimation(
        animJsonPath: Int,
        actionOnEnd: (() -> Unit)? = null
    ) {
        if (laMusicAnim.isAnimating) {
            laMusicAnim.cancelAnimation()
        }
        laMusicAnim.removeAllAnimatorListeners()
        laMusicAnim.isVisible = true
        laMusicAnim.setAnimation(animJsonPath)
        laMusicAnim.speed = 0.75f
        laMusicAnim.playAnimation()
        laMusicAnim.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
            }

            override fun onAnimationEnd(p0: Animator) {
                actionOnEnd?.invoke()
            }

            override fun onAnimationCancel(p0: Animator) {
            }

            override fun onAnimationRepeat(p0: Animator) {
            }
        })
    }

    /**
     * Animates and navigates to the MyF77 menu.
     */
    private fun animateAndNavigateToMyF77() {
        findNavController().navigate(R.id.action_menuFragment_to_myF77MenuFragment)
        /*
                handleMyF77Animation(R.raw.f77_headlight_entry_anim) {
                    doPressScaleAnim(clBgMyF77, true) {
                        clBgMyF77.startAnimation(animFade)
                      //  laF77Anim.startAnimation(animFade)
                        doSlideUpAnim(clBgBattery)
                        doSlideUpAnim(clBgSetting)
                        doSlideUpAnim(laSettingsAnim)
                        doSlideDownAnim(clBgMedia)
                        doSlideDownAnim(clBgControls)
                        doSlideDownAnim(clBgTpms)
                        doSlideDownAnim(clBgNavigate)
                        doSlideDownAnim(laNavigateAnim)
                        doSlideDownAnim(laMusicAnim, true) {
                            findNavController().navigate(R.id.action_menuFragment_to_myF77MenuFragment)
                        }
                    }
                }
        */
    }

    private fun handleMyF77Animation(animJsonPath: Int, actionOnEnd: (() -> Unit)? = null) {
        /* if (laF77Anim.isAnimating) laF77Anim.cancelAnimation()
         laF77Anim.removeAllAnimatorListeners()
         laF77Anim.setAnimation(animJsonPath)
         laF77Anim.speed = 1f
         laF77Anim.playAnimation()
         laF77Anim.addAnimatorListener(object : Animator.AnimatorListener {
             override fun onAnimationStart(p0: Animator) {
                 Log.d("Animation", "laF77Anim onAnimationStart")
             }

             override fun onAnimationEnd(p0: Animator) {
                 Log.d("Animation", "laF77Anim onAnimationEnd")
                 actionOnEnd?.invoke()
             }

             override fun onAnimationCancel(p0: Animator) {
             }

             override fun onAnimationRepeat(p0: Animator) {
             }
         })*/
    }

    /**
     * Animates and navigates to the Setting menu.
     */
    private fun animateAndNavigateToSettingMenu() {
        laSettingsAnim.isVisible = true
        handleSettingsAnimation(R.raw.setting_icon_entry_anim) {
            laSettingsAnim.isVisible = false
            doSlideUpAnim(clBgMyF77)
            //  doSlideUpAnim(laF77Anim)
            doSlideUpAnim(clBgBattery)
            doSlideUpAnim(clBgSetting)
            doSlideUpAnim(laSettingsAnim)
            doSlideDownAnim(clBgMedia)
            doSlideDownAnim(clBgControls)
            doSlideDownAnim(clBgTpms)
            doSlideDownAnim(clBgNavigate)
            doSlideDownAnim(laNavigateAnim)
            doSlideDownAnim(laMusicAnim)
            laSettingsAnim.post {
                if (!isAdded) return@post
                val navController = findNavController()
                val currentDest = navController.currentDestination?.id
                if (currentDest == R.id.menuFragment) {
                    navController.navigate(R.id.action_menuFragment_to_settingMenuFragment)
                }
            }
        }
    }

    private fun handleSettingsAnimation(animJsonPath: Int, actionOnEnd: (() -> Unit)? = null) {
        if (laSettingsAnim.isAnimating) laSettingsAnim.cancelAnimation()
        laSettingsAnim.removeAllAnimatorListeners()
        laSettingsAnim.setAnimation(animJsonPath)
        laSettingsAnim.speed = 1f
        laSettingsAnim.playAnimation()
        laSettingsAnim.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {

            }

            override fun onAnimationEnd(p0: Animator) {
                actionOnEnd?.invoke()
            }

            override fun onAnimationCancel(p0: Animator) {

            }

            override fun onAnimationRepeat(p0: Animator) {

            }
        })
    }


    /**
     * Selects the TPMS menu and updates the UI accordingly.
     */
    private fun selectTpms() {
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        clBgTpms.setBackgroundResource(R.drawable.bg_tpms_active)
        tvTpms.setTextColor(selectedTextColor)
        tvTpmsFront.setTextColor(selectedTextColor)
        tvTpmsRear.setTextColor(selectedTextColor)
    }

    /**
     * Simulates the TPMS (Tire Pressure Monitoring System) values.
     */
    private fun simulateTpms() {
        var tpmsFront = 0
        var tpmsBack = 0
        tpmsJob = CoroutineScope(Dispatchers.Main).launch {
            tvTpmsFrontValue.text = tpmsFront.toString()
            tvTpmsRearValue.text = tpmsBack.toString()
            ivTpmsStateFront.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_tpms_pff
                )
            )
            ivTpmsStateRear.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_tpms_pff
                )
            )
            while (isActive) {
                delay(5000)
                tpmsFront = (1..36).random()
                tpmsBack = (1..36).random()
                tvTpmsFrontValue.text = tpmsFront.toString()
                tvTpmsRearValue.text = tpmsBack.toString()
                ivTpmsStateFront.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        if (tpmsFront < 20) R.drawable.ic_tpms_error else R.drawable.ic_tpms_normal
                    )
                )
                ivTpmsStateRear.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        if (tpmsBack < 20) R.drawable.ic_tpms_error else R.drawable.ic_tpms_normal
                    )
                )
            }
        }
    }

    /**
     * Selects the Navigation menu and updates the UI accordingly.
     */
    private fun selectNavigate() {
        clBgNavigate.setBackgroundResource(R.drawable.bg_navigate_active)
        if (ivManeuver.isVisible) {
            val color = ContextCompat.getColor(requireContext(), R.color.white)
            ImageViewCompat.setImageTintList(ivManeuver, ColorStateList.valueOf(color))
            tvNavigating.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
        tvNavigate.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    /**
     * Simulates the navigation process.
     */
    private fun simulateNavigation() {
        navigateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val rotation = if (ivManeuver.scaleX == -1f) 1f else -1f
                delay(1000)
                clNavigationOff.isVisible = false
                clNavigateStart.isVisible = true
                ivManeuver.scaleX = rotation
                tvDistance.text = (1..100).random().toString()
            }
        }
    }

    /**
     * Selects the Music menu and updates the UI accordingly.
     */
    private fun selectMusic() {
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        clBgMedia.setBackgroundResource(R.drawable.bg_music_active)
        tvNowPlaying.setTextColor(selectedTextColor)
        tvMedia.setTextColor(selectedTextColor)
        tvConnected.setTextColor(selectedTextColor)
        tvConnectedDevice.setTextColor(selectedTextColor)
        val drawable =
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_music_design)?.mutate()
        drawable?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        ivMusicSelected.setColorFilter(whiteColor)
    }

    /**
     * Selects the Controls menu and updates the UI accordingly.
     */
    private fun selectControls() {
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        clBgControls.setBackgroundResource(R.drawable.bg_controls_active)
        tvControls.setTextColor(selectedTextColor)
        tvAbs.setTextColor(selectedTextColor)
        tvTractionControl.setTextColor(selectedTextColor)
        tvRegen.setTextColor(selectedTextColor)
        tvHillHold.setTextColor(selectedTextColor)
    }

    /**
     * Selects the Battery menu and updates the UI accordingly.
     */
    private fun selectBattery() {
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        clBgBattery.setBackgroundResource(R.drawable.bg_battery_active)
        tvBattery.setTextColor(selectedTextColor)
        tvBatteryLimit.setTextColor(selectedTextColor)
    }

    /**
     * Selects the Settings menu and updates the UI accordingly.
     */
    private fun selectSetting() {
        clBgSetting.setBackgroundResource(R.drawable.bg_settings_active)
    }

    /**
     * Selects the MyF77 menu and updates the UI accordingly.
     */
    private fun selectMyF77() {
        clBgMyF77.setBackgroundResource(R.drawable.bg_my_f77_active)
        tvBikeName.setTextColor(
            ContextCompat.getColor(
                requireContext(), R.color.white
            )
        )
    }

    fun refreshLastPosition(position: Int) {
        when (position) {
            MenuPosition.MyF77.ordinal -> {
                clBgMyF77.setBackgroundResource(R.drawable.bg_my_f77)
                tvBikeName.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelectedTitle
                    )
                )
                handleMyF77Animation(R.raw.f77_headlight_exit_anim)
            }

            MenuPosition.Battery.ordinal -> {
                tvBattery.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelectedTitle
                    )
                )
                clBgBattery.setBackgroundResource(R.drawable.bg_battery)
                tvBatteryLimit.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelected
                    )
                )
            }

            MenuPosition.Setting.ordinal -> {
                handleSettingsAnimation(R.raw.setting_icon_exit_anim)
                clBgSetting.setBackgroundResource(R.drawable.bg_settings)
            }

            MenuPosition.Controls.ordinal -> {
                tvControls.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelectedTitle
                    )
                )
                clBgControls.setBackgroundResource(R.drawable.bg_controls)
                tvAbs.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelected
                    )
                )
                tvTractionControl.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelected
                    )
                )
                tvRegen.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelected
                    )
                )
                tvHillHold.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelected
                    )
                )
            }

            MenuPosition.Tpms.ordinal -> {
                clBgTpms.setBackgroundResource(R.drawable.bg_tpms)
                tvTpms.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelectedTitle
                    )
                )
                tvTpmsFront.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelected
                    )
                )
                tvTpmsRear.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelected
                    )
                )
            }

            MenuPosition.Navigate.ordinal -> {
                tvNavigate.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelected
                    )
                )
                clNavigationOff.isVisible = true
                clNavigateStart.isVisible = false
                clBgNavigate.setBackgroundResource(R.drawable.bg_navigate)
                handleNavigateAnimation(R.raw.navigation_icon_exit_anim)
            }

            MenuPosition.Music.ordinal -> {
                Log.d("MenuFragment", "Refresh Last position is called")
                handleMusicAnimation(R.raw.music_icon_exit_anim)
                val drawable =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_music_design)
                        ?.mutate()
                drawable?.setTint(ContextCompat.getColor(requireContext(), R.color.mediumRed))
                tvMedia.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelectedTitle
                    )
                )
                tvNowPlaying.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.unSelected
                    )
                )
                tvConnected.setTextColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.green
                    )
                )
                clBgMedia.setBackgroundResource(R.drawable.bg_media)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigateJob?.cancel()
        tpmsJob?.cancel()
    }

    /**
     * Represents the different menu sections available in the application.
     *
     * Used to identify which section is currently selected, allowing the UI
     * to update accordingly via observers or handlers.
     */
    enum class MenuPosition {
        MyF77,
        Battery,
        Setting,
        Music,
        Controls,
        Tpms,
        Navigate,
        Enter
    }
}





