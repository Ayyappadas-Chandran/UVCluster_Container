package com.suprajit.uvcluster.ui.features.settings.wifi

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.suprajit.uvcluster.R
import com.suprajit.uvcluster.domain.ennumerate.ButtonNavigation
import com.suprajit.uvcluster.ui.adapter.WifiDeviceAdapter
import com.suprajit.uvcluster.ui.viewModel.SharedViewModel
import com.suprajit.uvcluster.utils.Utilities.setOnSoundClickListener
import com.suprajit.uvcluster.utils.ViewModelFactory
import kotlinx.coroutines.launch

class WifiFragment : Fragment() {
    private lateinit var tvMyNetwork: TextView
    private lateinit var tvWifiName: TextView
    private lateinit var tvWifiOn: TextView
    private lateinit var tvWifiOff: TextView
    private lateinit var tvAvailableDevices: TextView
    private lateinit var tvSearching: TextView
    private lateinit var ivWifiOnSelected: ImageView
    private lateinit var ivDelete: ImageView
    private lateinit var ivWifiOffSelected: ImageView
    private lateinit var rvAvailableDevices: RecyclerView
    private lateinit var clMyNetwork: ConstraintLayout
    private var wifiDialog: AlertDialog? = null
    private val sharedViewModel by activityViewModels<SharedViewModel> { ViewModelFactory(context = requireContext()) }
    private val wifiViewModel by activityViewModels<WifiViewModel> { ViewModelFactory(context = requireContext()) }
    private var clickedUiState: ClickedUiState = ClickedUiState.WifiStateClicked
    private var isWifiStateClicked = true
    private val wifiDeviceAdapter = WifiDeviceAdapter { ssid ->
        clickedUiState = ClickedUiState.WifiDeviceClicked
        handleUiState()
        sharedViewModel.handleSettingsChildClick(true)
        showWifiDialog(ssid)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_wifi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        initObserver()
        wifiViewModel.scanResult()
        wifiViewModel.startScan()
        wifiViewModel.wifiConnected()
        handleUiState()
        rvAvailableDevices.adapter = wifiDeviceAdapter
        initClickListener()
        showConnectedNetwork()
    }


    /**
     * Binds UI components from the provided root view using their IDs.
     *
     * @param view The root view containing the layout elements.
     *
     * Initializes:
     * - tvMyNetwork,tvWifiName,tvWifiOn,tvWifiOff,tvSearching,tvAvailableDevices (TextViews)
     * - ivWifiOnSelected,ivWifiOffSelected,ivDelete (ImageViews)
     * - rvAvailableDevices (RecyclerView)
     * - clMyNetwork (ConstraintLayout)
     */
    private fun initViews(view: View) {
        tvMyNetwork = view.findViewById(R.id.tvMyNetwork)
        tvWifiName = view.findViewById(R.id.tvWifiName)
        tvWifiOn = view.findViewById(R.id.tvWifiOn)
        tvWifiOff = view.findViewById(R.id.tvWifiOff)
        tvSearching = view.findViewById(R.id.tvSearching)
        tvAvailableDevices = view.findViewById(R.id.tvAvailableDevices)

        ivWifiOnSelected = view.findViewById(R.id.ivWifiOnSelected)
        ivWifiOffSelected = view.findViewById(R.id.ivWifiOffSelected)
        ivDelete = view.findViewById(R.id.ivDeleteWifiNetwork)

        rvAvailableDevices = view.findViewById(R.id.rvAvailableDevices)
        clMyNetwork = view.findViewById(R.id.clMyNetwork)
    }

    /**
     * Observes changes in the Wi-Fi scan results.
     */
    //27/01/2026
    private fun initObserver() {

        sharedViewModel.settingsChildClick.observe(viewLifecycleOwner) { isClicked ->
            if (!isClicked) {
                resetWifiStateClicked()
            } else {
                handleUiState()
            }
        }
        wifiViewModel.scanResult.observe(viewLifecycleOwner) { result ->
            wifiDeviceAdapter.submitList(result)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    wifiViewModel.onWifiStateChange.collect { state ->
                        handleWifiStateClicked()
                    }
                }
                launch {
                    wifiViewModel.connectionState.collect { state ->
                        handleConnectionState(state)
                    }
                }

            }
        }
    }

    private fun handleConnectionState(isConnected: Boolean) {
        clMyNetwork.isVisible = isConnected
        tvMyNetwork.isVisible = isConnected
        val connectedNetwork = wifiViewModel.getConnectedWifiSSID()
        tvWifiName.text = connectedNetwork
    }


    /**
     * show connected Network UI
     */
    private fun showConnectedNetwork() {
        val connectedNetwork = wifiViewModel.getConnectedWifiSSID()
        clMyNetwork.isVisible = connectedNetwork.isNotEmpty()
        tvMyNetwork.isVisible = connectedNetwork.isNotEmpty()
        tvWifiName.text = connectedNetwork

    }

    fun handleButtonNavigation(button: Int) {
        when (button) {
            ButtonNavigation.Left.ordinal -> {
                if (wifiViewModel.isWifiEnabled()) {
                    tvWifiOff.performClick()
                } else {
                    tvWifiOn.performClick()
                }
            }

            ButtonNavigation.Right.ordinal -> {
                if (wifiViewModel.isWifiEnabled()) {
                    tvWifiOff.performClick()
                } else {
                    tvWifiOn.performClick()
                }
            }

            ButtonNavigation.Back.ordinal -> {
                //for bug no 46 - pop up exit on button press
                if (wifiDialog?.isShowing == true) {
                    wifiDialog?.dismiss()
                }
                ivWifiOnSelected.isVisible = false
                ivWifiOffSelected.isVisible = false
            }
        }
    }

    /**
     * Initialize click listeners for UI components.
     */
    private fun initClickListener() {
        tvWifiOn.setOnSoundClickListener(requireContext()) {
            isWifiStateClicked = true
            clickedUiState = ClickedUiState.WifiStateClicked
            sharedViewModel.handleSettingsChildClick(true)
            handleUiState()
            wifiDeviceAdapter.handleParentClick()
            if (wifiViewModel.isWifiEnabled()) {
                return@setOnSoundClickListener
            }
            wifiViewModel.enableWifi(true)
        }

        tvWifiOff.setOnSoundClickListener(requireContext()) {
            isWifiStateClicked = true
            wifiDeviceAdapter.handleParentClick()
            sharedViewModel.handleSettingsChildClick(true)
            clickedUiState = ClickedUiState.WifiStateClicked
            if (!wifiViewModel.isWifiEnabled()) {
                return@setOnSoundClickListener
            }
            wifiViewModel.enableWifi(false)
        }

        ivDelete.setOnSoundClickListener(requireContext()) {
            clickedUiState = ClickedUiState.WifiDeleteClicked
            handleUiState()
            sharedViewModel.handleSettingsChildClick(true)
            wifiViewModel.forgetHotspot()
        }

    }

    /**
     * Handles the UI state based on the clicked UI element.
     */
    private fun handleUiState() {
        when (clickedUiState) {
            ClickedUiState.WifiStateClicked -> {
                handleWifiStateClicked()
            }

            ClickedUiState.WifiDeviceClicked -> {
                resetWifiStateClicked()
            }

            ClickedUiState.WifiDeleteClicked -> {
                resetWifiStateClicked()
            }
        }
    }

    /**
     * Handles the UI state for Wi-Fi state.
     */
    private fun handleWifiStateClicked() {
        ivWifiOnSelected.isVisible = wifiViewModel.isWifiEnabled()
        ivWifiOffSelected.isVisible = !wifiViewModel.isWifiEnabled()
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val selectedBackgroundColor =
            ContextCompat.getColor(requireContext(), R.color.activeSelectionRed)
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.unSelected)
        val unselectedBackgroundColor =
            ContextCompat.getColor(requireContext(), R.color.transparent)
        tvWifiOn.setTextColor(if (wifiViewModel.isWifiEnabled()) selectedTextColor else unselectedTextColor)
        tvWifiOn.setBackgroundColor(if (wifiViewModel.isWifiEnabled()) selectedBackgroundColor else unselectedBackgroundColor)
        tvWifiOff.setTextColor(if (!wifiViewModel.isWifiEnabled()) selectedTextColor else unselectedTextColor)
        tvWifiOff.setBackgroundColor(if (!wifiViewModel.isWifiEnabled()) selectedBackgroundColor else unselectedBackgroundColor)
    }

    /**
     * Resets the UI state for Wi-Fi state.
     */
    private fun resetWifiStateClicked() {
        ivWifiOnSelected.isVisible = false
        ivWifiOnSelected.isVisible = false
        val selectedTextColor = ContextCompat.getColor(
            requireContext(),
            if (isWifiStateClicked) R.color.black else R.color.white
        )
        val selectedBackgroundColor = ContextCompat.getColor(
            requireContext(),
            if (isWifiStateClicked) R.color.white else R.color.grey
        )
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.unSelected)
        val unselectedBackgroundColor =
            ContextCompat.getColor(requireContext(), R.color.transparent)
        tvWifiOn.setTextColor(if (wifiViewModel.isWifiEnabled()) selectedTextColor else unselectedTextColor)
        tvWifiOn.setBackgroundColor(if (wifiViewModel.isWifiEnabled()) selectedBackgroundColor else unselectedBackgroundColor)
        tvWifiOff.setTextColor(if (!wifiViewModel.isWifiEnabled()) selectedTextColor else unselectedTextColor)
        tvWifiOff.setBackgroundColor(if (!wifiViewModel.isWifiEnabled()) selectedBackgroundColor else unselectedBackgroundColor)
    }

    /**
     * Displays a Wi-Fi password input dialog with a blurred background.
     *
     * Prompts the user to enter a password to connect to the specified SSID.
     * Applies a blur effect (Android 12+) to the background while the dialog is shown.
     *
     * @param ssid The Wi-Fi SSID to connect to.
     */
    private fun showWifiDialog(ssid: String) {
        val rootView =
            requireActivity().window.decorView.findViewById<ViewGroup>(android.R.id.content)
        // Blur the background
        val blur = RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
        rootView.setRenderEffect(blur)
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_wifi_password, null)
        val tvConnect = dialogView.findViewById<TextView>(R.id.tvConnect)
        val etPassword = dialogView.findViewById<TextView>(R.id.etPassword)
        val ivEnter = dialogView.findViewById<ImageView>(R.id.ivEnter)
        //for bug no 46 - pop up exit on button press
        wifiDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setOnDismissListener {
                rootView.setRenderEffect(null)
                wifiDialog = null
            }
            .create()
        tvConnect.text = buildString {
            append(getString(R.string.connect_to))
            append(ssid)
        }
        ivEnter.setOnSoundClickListener(requireContext()) {
            val password = etPassword.text.toString()
            wifiViewModel.connectHotspot(ssid, password)
            wifiDialog?.dismiss()

        }
        wifiDialog?.show()
    }

    /**
     * Represents different UI states for handling click events.
     */
    sealed class ClickedUiState() {
        object WifiStateClicked : ClickedUiState()
        object WifiDeviceClicked : ClickedUiState()
        object WifiDeleteClicked : ClickedUiState()
    }
}

