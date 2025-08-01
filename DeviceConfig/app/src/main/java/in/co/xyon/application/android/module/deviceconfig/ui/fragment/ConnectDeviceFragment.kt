package `in`.co.xyon.application.android.module.deviceconfig.ui.fragment

import `in`.co.xyon.application.android.module.deviceconfig.R
import `in`.co.xyon.application.android.module.deviceconfig.databinding.FragmentDeviceConnectBinding
import `in`.co.xyon.application.android.module.deviceconfig.domain.model.ProvisionMode
import `in`.co.xyon.application.android.module.deviceconfig.presentation.uievents.ConnectDeviceUiEvent
import `in`.co.xyon.application.android.module.deviceconfig.presentation.WifiOnlyViewModel
import `in`.co.xyon.application.android.module.deviceconfig.presentation.states.ConnectDeviceUiState
import `in`.co.xyon.application.android.module.deviceconfig.presentation.states.EspHandshakeState
import `in`.co.xyon.application.android.module.deviceconfig.utils.collectLatestLifecycleFlow
import `in`.co.xyon.application.android.module.deviceconfig.utils.getNetworkConnectPermissions
import `in`.co.xyon.application.android.module.deviceconfig.utils.getQRScanningPermissions
import `in`.co.xyon.application.android.module.deviceconfig.utils.getWifiScanPermissions
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class ConnectDeviceFragment : Fragment(), View.OnClickListener{

    private var _navController: NavController? = null
    private val navController get() = _navController!!

    private val viewModel: WifiOnlyViewModel by activityViewModels<WifiOnlyViewModel>()

    private var _binding: FragmentDeviceConnectBinding? = null
    private val binding get() = _binding!!

    private var dialogProvisioningMode: AlertDialog ?= null
    private var dialogDownloadFailed: AlertDialog ?= null
    private var dialogRetryConn: AlertDialog ?= null
    private var dialogCancellation: AlertDialog ?= null
    //val WIFI_SETTINGS_ACTIVITY_REQUEST = 112
    //private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (dialogCancellation == null || !dialogCancellation!!.isShowing)
                    showCancellationDialog()
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Add the callback to the dispatcher. It will be enabled when the fragment is started.
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceConnectBinding.inflate(inflater)
        /*resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onActivityResult(WIFI_SETTINGS_ACTIVITY_REQUEST, result)
        }*/
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _navController = Navigation.findNavController(view)

        binding.loadingWidget.visibility = GONE
        binding.loadingText.visibility = GONE
        //viewModel.resetRetrievedPopFlow()   // not safe to reset the pop everytime view is created
        //viewModel.resetStateAndFlow()   // not safe to reset the pop everytime ....
        // view is created -- should be done while navigating the fragment from the previous fragment

        //viewModel.registerProvisioningEventBus()

        binding.btnRefresh.setOnClickListener(this)

        observeState()
        observeGetPopState()
        observeProvisionModeState()
        observeEventFlow()
        observeDeviceSsidState()
        observeSendHandshakeStateFlow()
    }

    override fun onStart() {
        super.onStart()
        //EventBus.getDefault().register(this)
    }


    /*private fun onActivityResult(requestCode: Int, result: ActivityResult) {
        Timber.d("resultCode: ${result.resultCode} and result_ok: ${Activity.RESULT_OK}")
        //if (result.resultCode == Activity.RESULT_OK) {
         //   val intent = result.data
          //  Timber.d("intent: $intent")
            when (requestCode) {
                WIFI_SETTINGS_ACTIVITY_REQUEST -> {
                    Timber.d("do something...")

                }
           }
       // }
    }*/

    private fun observeState() {
        collectLatestLifecycleFlow(viewModel.connectedDeviceUiStateFlow){ connectDeviceUiState ->

            when(connectDeviceUiState){
                ConnectDeviceUiState.IDLE_STATE ->{
                    binding.loadingText.text = resources.getString(R.string.loading_message_refresh)
                    binding.loadingText.visibility = VISIBLE
                    binding.loadingWidget.visibility = GONE
                    binding.btnRefresh.visibility = VISIBLE
                }
                ConnectDeviceUiState.IS_DOWNLOADING -> {
                    binding.loadingWidget.visibility = VISIBLE
                    binding.loadingText.text = resources.getString(R.string.loading_message_fetching_pop)
                    binding.loadingText.visibility = VISIBLE
                    binding.btnRefresh.visibility = GONE
                    binding.btnBack.isEnabled = false
                }
                ConnectDeviceUiState.IS_SCANNING -> {
                    binding.loadingWidget.visibility = VISIBLE
                    binding.loadingText.text = resources.getString(R.string.loading_message_scanning)
                    binding.loadingText.visibility = VISIBLE
                    binding.btnRefresh.visibility = GONE
                    binding.btnBack.isEnabled = false
                }
                ConnectDeviceUiState.IS_CONNECTING -> {
                    binding.loadingWidget.visibility = VISIBLE
                    binding.loadingText.text = resources.getString(R.string.loading_message_connecting)
                    binding.loadingText.visibility = VISIBLE
                    binding.btnRefresh.visibility = GONE
                    binding.btnBack.isEnabled = false
                }
                ConnectDeviceUiState.HAS_DISCONNECTED ->{
                    binding.loadingText.text = "Disconnected..."
                    binding.loadingText.visibility = VISIBLE
                    binding.loadingWidget.visibility = GONE
                    binding.btnRefresh.visibility = VISIBLE
                    binding.btnBack.isEnabled = true
                    //TODO: dialog to scan and connect again and connect or go back ...
                    // reset the toBeConnectedSsid to "" if reconnect
                }
                ConnectDeviceUiState.IS_CONNECTED -> {
                    binding.loadingText.text = ""
                    binding.loadingText.visibility = GONE
                    binding.loadingWidget.visibility = GONE
                    binding.btnRefresh.visibility = GONE
                    binding.btnBack.isEnabled = true
                    /*if(viewModel.deviceSsidStateFlow.value == viewModel.connectedDeviceSsidStateFlow.value){
                        viewModel.createEspDevice()
                        navigateToNextFragment()
                    }*/
                    sendHandshake()
                }
                ConnectDeviceUiState.SHOW_WIFI_BLE_DIALOG -> {
                    binding.loadingText.text = ""
                    binding.loadingText.visibility = GONE
                    binding.loadingWidget.visibility = GONE
                    binding.btnRefresh.visibility = GONE
                    binding.btnBack.isEnabled = false
                    showProvisionWifiConfirmationDialog()
                }
                ConnectDeviceUiState.SHOW_DOWNLOADING_FAILED_DIALOG -> {
                    binding.loadingText.text = ""
                    binding.loadingText.visibility = GONE
                    binding.loadingWidget.visibility = GONE
                    binding.btnRefresh.visibility = GONE
                    binding.btnBack.isEnabled = true
                    showDownloadingFailedDialog()
                }
            }
        }
    }

    private fun observeGetPopState() {
        collectLatestLifecycleFlow(viewModel.retrievedPopStateFlow){ pop ->
            if (pop.isEmpty()) {
                Timber.d("Open camera for scanning...")
                scanAndGetPop()
            } else{
                Timber.d("Don't Open camera for scanning...")
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun observeProvisionModeState() {
        collectLatestLifecycleFlow(viewModel.provisionModeStateFlow){ provisionMode ->
            when (provisionMode) {
                ProvisionMode.WIFI -> {
                    //viewModel.resetUIState()
                    if(viewModel.connectedDeviceUiStateFlow.value == ConnectDeviceUiState.SHOW_WIFI_BLE_DIALOG)
                        performScanningWifiNetworksFlow()
                    //resultLauncher.launch(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                ProvisionMode.BLUETOOTH -> {
                    //implement ble flow
                }
                else -> {  //ProvisionMode.NONE
                    if(viewModel.retrievedPopStateFlow.value.isNotBlank() ||
                            viewModel.retrievedSsidStateFlow.value.isNotBlank())  // || viewModel.deviceSsidStateFlow.value.isNullOrEmpty() || viewModel.deviceSsidStateFlow.value.isNullOrBlank()
                           viewModel.updateConnectDeviceUiState(ConnectDeviceUiState.SHOW_WIFI_BLE_DIALOG)
                }
            }


        }
    }

    @ExperimentalCoroutinesApi
    private fun performScanningWifiNetworksFlow() {
        if (!checkWifiState()) {
            viewModel.updateConnectDeviceUiState(ConnectDeviceUiState.IDLE_STATE)
            return
        }

        PermissionX.init(this)
            .permissions(getWifiScanPermissions())
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList,
                    "REQUIRED FOR SCANNING NETWORKS: Please grant Location permissions",
                    "OK", "Cancel")
            }
            .request{ allGranted, grantedList, deniedList ->
                if (allGranted) {
                    if (viewModel.getLocationEnabledStatus())
                        viewModel.scanWifiToFindDevice()
                    else Snackbar.make(binding.root,
                        "REQUIRED FOR SCANNING NETWORKS: Please enable Location in Android Settings", Snackbar.LENGTH_LONG).show()
                } else Snackbar.make(binding.root,
                    "REQUIRED FOR SCANNING NETWORKS: Please grant Location permissions", Snackbar.LENGTH_LONG).show()
            }

    }

    private fun checkWifiState(): Boolean {
        return if(!viewModel.getWifiEnabledStatus()) {
            val scanStatus = viewModel.toggleWifiStateOn()
            if (!scanStatus){
                Snackbar.make(binding.root, "Wifi needs to be enabled to add device", Snackbar.LENGTH_SHORT).show()
                false
            }else true
        } else true
    }

   @ExperimentalCoroutinesApi
    private fun observeDeviceSsidState() {
        collectLatestLifecycleFlow(viewModel.toBeConnectedDeviceSsidStateFlow) {
            if(it.isNotEmpty()) {
                if (viewModel.provisionModeStateFlow.value != ProvisionMode.NONE) {
                    connectWithDevice(it)
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun connectWithDevice(ssid: String){
        if (!checkWifiState()) return
        // connect to the network with this ssid...
        PermissionX.init(this)
            .permissions(getNetworkConnectPermissions())
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList,
                    "REQUIRED FOR CONNECTING TO NETWORK: Please grant Location permissions",
                    "OK", "Cancel")
            }
            .request{ allGranted, _, _ ->
                if (allGranted) {
                    viewModel.connectDevice(ssid)
                } else Snackbar.make(binding.root,
                    "REQUIRED FOR CONNECTING TO NETWORK: Please grant Location permissions", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun checkForNetworkRelatedPermissions() {

    }

    private fun checkForWifiScanPermissions(){

    }

    private fun checkForWifiConnectPermissions(){

    }

    private fun checkForLocationPermissions(){

    }

    private fun observeEventFlow() {
        collectLatestLifecycleFlow(viewModel.connectedDeviceUiEventFlow) {
            when(it){
                is ConnectDeviceUiEvent.ShowSnackbarError ->{
                    Snackbar.make(binding.root, it.msg, Snackbar.LENGTH_LONG).show()
                    leaveFragment()
                }
                is ConnectDeviceUiEvent.ShowConfirmationDialog -> {
                    showConnectionRetryConfirmationDialog(it.msg)
                }
            }
        }
    }

    private fun scanAndGetPop() {
        //check for camera access needed for qr code scanning...
        PermissionX.init(this)
            .permissions(getQRScanningPermissions())
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList,
                    resources.getString(R.string.dialog_message_permission_qr_scanning),
                    resources.getString(R.string.ok),
                    resources.getString(R.string.cancel))
            }
            .request{ allGranted, _, _ ->
                if(allGranted) {
                    viewModel.scanAndGetPop()
                } else {
                    Snackbar.make(
                        binding.root,
                        resources.getString(R.string.snackbar_message_permission_qr_scanning),
                        Snackbar.LENGTH_LONG)
                        .show()
                    leaveFragment()
                }
            }
    }

    private fun showProvisionModeSelectionDialog(){
        val choiceItems = arrayOf(ProvisionMode.WIFI, ProvisionMode.BLUETOOTH)
        //val choiceItemsAsChar: Array<String>
        val choiceItemsAsChar = choiceItems.map { it.printableName }.toTypedArray()
        val checkedItem = 0

        dialogProvisioningMode = MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.dialog_title_provisioning))
            .setNeutralButton(resources.getString(R.string.cancel)){ dialog, which ->
                // Respond to neutral button press
            }
            .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                // Respond to positive button press
            }
            // Single-choice items (initialized with checked item)
            .setSingleChoiceItems(choiceItemsAsChar, checkedItem) { dialog, which ->
                // Respond to item chosen
                viewModel.updateProvisionModeState(choiceItems[which])
            }
            .setCancelable(false)  // back button or clicking elsewhere on screen doesn't cancel the dialog
            .create()

        dialogProvisioningMode?.show()

    }

    private fun showProvisionWifiConfirmationDialog() {
        val dialogBuilder =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.dialog_title_provisioning))
                .setMessage(resources.getString(R.string.dialog_message_provisioning))
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
                    dialog?.dismiss()
                    leaveFragment()
                }
                .setNegativeButton(resources.getString(R.string.decline)) { dialog, which ->
                    dialog?.dismiss()
                    leaveFragment()
                }
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    // Respond to positive button press
                    viewModel.updateProvisionModeState(ProvisionMode.WIFI)
                    dialog?.dismiss()
                }
                .setCancelable(false)  // back button or clicking elsewhere on screen doesn't cancel the dialog

        dialogProvisioningMode = dialogBuilder.create()
        dialogProvisioningMode?.show()
    }

    private fun showDownloadingFailedDialog() {
        val dialogBuilder =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.dialog_title_download_failed))
                .setMessage(resources.getString(R.string.dialog_message_download_failed))
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
                    dialog?.dismiss()
                    leaveFragment()
                }
                .setNegativeButton(resources.getString(R.string.decline)) { dialog, which ->
                    dialog?.dismiss()
                    leaveFragment()
                }
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    // Respond to positive button press
                    viewModel.scanAndGetPop()
                    dialog?.dismiss()
                }
                .setCancelable(false)  // back button or clicking elsewhere on screen doesn't cancel the dialog

        dialogDownloadFailed = dialogBuilder.create()
        dialogDownloadFailed?.show()
    }

    private fun showConnectionRetryConfirmationDialog(msg: String){
        val dialogBuilder =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.dialog_title_connection_retry))
                .setMessage(msg)
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
                    dialog?.dismiss()
                    leaveFragment()
                }
                .setNegativeButton(resources.getString(R.string.decline)) { dialog, which ->
                    dialog?.dismiss()
                    leaveFragment()
                }
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    // Respond to positive button press
                    viewModel.connectDevice(viewModel.toBeConnectedDeviceSsidStateFlow.value)
                    dialog?.dismiss()
                }
                .setCancelable(false)  // back button or clicking elsewhere on screen doesn't cancel the dialog

        dialogRetryConn = dialogBuilder.create()
        dialogRetryConn?.show()
    }

    private fun showCancellationDialog() {
        val dialogBuilder =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Connection in progress")
                .setMessage("It is advised to wait till the process is complete. Otherwise, you might have to restart the provisioning process again. Would you like to Wait?")
                .setPositiveButton("Yes, wait") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("No, Exit") { dialog, _ ->
                    leaveFragment()
                    dialog.dismiss()
                }
                .setCancelable(false)

        dialogCancellation = dialogBuilder.create()
        dialogCancellation?.show()
    }

    @SuppressLint("MissingPermission")
    private fun sendHandshake(){
        if(viewModel.toBeConnectedDeviceSsidStateFlow.value == viewModel.connectedDeviceSsidStateFlow.value){
            viewModel.createEspDevice(viewModel.provisionModeStateFlow.value)
            // connect to the network with this ssid...
            PermissionX.init(this)
                .permissions(getNetworkConnectPermissions())
                .onExplainRequestReason { scope, deniedList ->
                    scope.showRequestReasonDialog(deniedList,
                        "REQUIRED FOR CONNECTING TO NETWORK: Please grant Location permissions",
                        "OK", "Cancel")
                }
                .request{ allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        Timber.d("commencing handshake event...")
                        viewModel.startDeviceHandshake()
                    } else Snackbar.make(binding.root,
                        "REQUIRED FOR CONNECTING TO NETWORK: Please grant Location permissions", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun observeSendHandshakeStateFlow(){
        collectLatestLifecycleFlow(viewModel.espHandshakeStateFlow){
            if (viewModel.connectedDeviceUiStateFlow.value != ConnectDeviceUiState.IS_CONNECTED) return@collectLatestLifecycleFlow
            when(it){
                EspHandshakeState.SENDING -> {
                    binding.loadingWidget.visibility = VISIBLE
                    binding.loadingText.text = resources.getString(R.string.loading_message_handshaking)
                    binding.loadingText.visibility = VISIBLE
                }
                EspHandshakeState.SUCCESSFUL -> {
                    binding.loadingText.text = ""
                    binding.loadingText.visibility = GONE
                    binding.loadingWidget.visibility = GONE
                    if (viewModel.listDevCaps.isEmpty() || !viewModel.listDevCaps.contains("wifi_scan")) {
                        //TODO: move to another fragment to start provisioning
                        Timber.d("no deviceCapabilites found")
                        navigateToNextFragment2()
                    } else {
                        //TODO: move to another fragment to start provisioning
                        Timber.d("deviceCapabilites found: ${viewModel.listDevCaps}")
                        navigateToNextFragment1()
                    }
                }
                EspHandshakeState.ERROR -> {
                    binding.loadingText.text = ""
                    binding.loadingText.visibility = GONE
                    binding.loadingWidget.visibility = GONE
                }
                EspHandshakeState.NONE -> {
                    binding.loadingText.text = ""
                    binding.loadingText.visibility = GONE
                    binding.loadingWidget.visibility = GONE
                }
            }
        }
    }

    private fun leaveFragment() {
        viewModel.resetConnectedDeviceFrag()
        navController.popBackStack()
        /*val action = GetPopFragmentDirections
            .actionGetPopFragToDevTypSelFrag()
        navController.navigate(action)*/
    }

    private fun navigateToNextFragment1() {
        //viewModel.unregisterEventBus() //needed in the next fragment as well
        val action = ConnectDeviceFragmentDirections
            .actionConnDevFragmentToWifiScanListFragment()
        navController.navigate(action)
    }

    private fun navigateToNextFragment2() {
        val action = ConnectDeviceFragmentDirections
            .actionConnDevFragmentToWifiConfigFragment()
        navController.navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialogCancellation = null
        dialogDownloadFailed = null
        dialogRetryConn = null
        dialogProvisioningMode = null
        _navController = null
        _binding = null

    }

    override fun onClick(v: View?) {
        when(v){
            binding.btnRefresh ->{
                if(viewModel.retrievedPopStateFlow.value.isNotBlank() ||
                    viewModel.retrievedSsidStateFlow.value.isNotBlank())
                    if (viewModel.provisionModeStateFlow.value == ProvisionMode.WIFI)
                        performScanningWifiNetworksFlow()
                    else
                        showProvisionWifiConfirmationDialog()
                else
                    viewModel.scanAndGetPop()
            }
            binding.btnBack ->{
                leaveFragment()
            }
        }
    }

}