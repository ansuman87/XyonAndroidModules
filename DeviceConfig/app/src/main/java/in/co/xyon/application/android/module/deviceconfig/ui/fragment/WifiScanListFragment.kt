package `in`.co.xyon.application.android.module.deviceconfig.ui.fragment

import android.content.Context
import `in`.co.xyon.application.android.module.deviceconfig.R
import `in`.co.xyon.application.android.module.deviceconfig.databinding.DialogWifiNetworkBinding
import `in`.co.xyon.application.android.module.deviceconfig.databinding.FragmentWifiScanListBinding
import `in`.co.xyon.application.android.module.deviceconfig.presentation.WifiOnlyViewModel
import `in`.co.xyon.application.android.module.deviceconfig.presentation.states.ProvisionNetwork
import `in`.co.xyon.application.android.module.deviceconfig.presentation.states.WifiScanListUiState
import `in`.co.xyon.application.android.module.deviceconfig.presentation.uievents.WifiScanListUIEvent
import `in`.co.xyon.application.android.module.deviceconfig.ui.adapter.WifiListAdapter
import `in`.co.xyon.application.android.module.deviceconfig.utils.collectLatestLifecycleFlow
import `in`.co.xyon.application.android.module.deviceconfig.utils.getNetworkConnectPermissions
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.WiFiAccessPoint
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.AndroidEntryPoint
import `in`.co.xyon.application.android.module.deviceconfig.presentation.states.ProvisioningStateTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class WifiScanListFragment : Fragment() , View.OnClickListener{

    private var _navController: NavController? = null
    private val navController get() = _navController!!

    private val viewModel: WifiOnlyViewModel by activityViewModels<WifiOnlyViewModel>()

    private var _binding: FragmentWifiScanListBinding?= null
    private val binding get() = _binding!!

    private lateinit var provisionManager: ESPProvisionManager

    private var wifiListAdapter: WifiListAdapter ?= null
    //private var wifiApList = mutableListOf<WiFiAccessPoint>()
    //private var selectedAp: String? = null

    private var joinNetworkDialog : AlertDialog?= null
    private var deviceDCDialog : AlertDialog?= null
    private var dialogCancellation: AlertDialog ?= null

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.wifiScanListUiStateFlow.value == WifiScanListUiState.LIST_LOADING)
                    showCancellationDialog()
                else leaveFragment()
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
        _binding = FragmentWifiScanListBinding.inflate(layoutInflater, container, false)
        //populateWifiList()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _navController = Navigation.findNavController(view)
        provisionManager = ESPProvisionManager.getInstance(requireContext())

        observeUiState()
        observeUiEvent()
        populateWifiList()
        binding.btnRefresh.setOnClickListener(this)
        //binding.tvJoinNetworkCenter.setOnClickListener(this)
        binding.tvJoinNetwork.setOnClickListener(this)
        binding.btnBack.setOnClickListener(this)
        //setListClickListener()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.listWifiAp.size == 0)
            getWifiList()
    }

    private fun observeUiState(){
        collectLatestLifecycleFlow(viewModel.wifiScanListUiStateFlow){
            //Timber.d("wifiScanListUIState: ${it.name}")
            when(it){
                WifiScanListUiState.IDLE_STATE -> {
                    binding.loadingWidget.visibility = View.GONE
                    binding.recyclerViewContainer.visibility = View.GONE
                    binding.tvJoinNetwork.visibility = View.GONE
                    binding.btnRefresh.visibility = View.VISIBLE
                    binding.tvErrorMsg.visibility = View.GONE
                    binding.btnBack.isEnabled = true
                }
                WifiScanListUiState.LIST_FOUND -> {

                    /*wifiApList.clear()
                    wifiApList.addAll(viewModel.listWifiAp)*/
                    //populateWifiList()
                    //addLastItem()
                    wifiListAdapter?.wifiApList = viewModel.listWifiAp
                    wifiListAdapter?.notifyDataSetChanged()
                    binding.loadingWidget.visibility = View.GONE
                    binding.recyclerViewContainer.visibility = View.VISIBLE
                    binding.tvJoinNetwork.visibility = View.VISIBLE
                    binding.btnRefresh.visibility = View.VISIBLE
                    binding.tvErrorMsg.visibility = View.GONE
                    binding.btnBack.isEnabled = true
                }
                WifiScanListUiState.LIST_FOUND_ERROR -> {
                    wifiListAdapter?.wifiApList = viewModel.listWifiAp
                    wifiListAdapter?.notifyDataSetChanged()
                    //wifiListAdapter?.notifyDataSetChanged()
                    binding.loadingWidget.visibility = View.GONE
                    binding.recyclerViewContainer.visibility = View.GONE
                    binding.tvJoinNetwork.visibility = View.GONE
                    binding.btnRefresh.visibility = View.VISIBLE
                    binding.tvErrorMsg.visibility = View.VISIBLE
                    binding.btnBack.isEnabled = true
                }
                WifiScanListUiState.LIST_LOADING -> {
                    binding.loadingWidget.visibility = View.VISIBLE
                    binding.recyclerViewContainer.visibility = View.VISIBLE
                    binding.btnRefresh.visibility = View.GONE
                    binding.tvJoinNetwork.visibility = View.GONE
                    binding.tvErrorMsg.visibility = View.GONE
                    binding.btnBack.isEnabled = false
                }
                WifiScanListUiState.DEVICE_DISCONNECTED -> {
                    binding.loadingWidget.visibility = View.GONE
                    binding.recyclerViewContainer.visibility = View.GONE
                    binding.tvJoinNetwork.visibility = View.GONE
                    binding.btnRefresh.visibility = View.GONE
                    binding.tvErrorMsg.visibility = View.VISIBLE
                    showDeviceDCDialog()
                }
                /*WifiScanListUIState.SHOW_JOIN_NETWORK_DIALOG -> {
                    binding.loadingWidget.visibility = View.GONE
                    binding.recyclerViewContainer.visibility = View.VISIBLE
                    binding.tvJoinNetwork.visibility = View.VISIBLE
                    binding.btnRefresh.visibility = View.VISIBLE
                    binding.tvJoinNetworkCenter.visibility = View.GONE

                }*/
            }
        }
    }

    private fun observeUiEvent(){
        collectLatestLifecycleFlow(viewModel.wifiScanListUIEvent){
            when(it){
                is WifiScanListUIEvent.ShowSnackbarError ->{
                    showSnackbar(it.msg)
                }
            }
        }
    }

    private fun getWifiList(){
        if (!checkWifiState()) return

        PermissionX.init(this)
            .permissions(getNetworkConnectPermissions())
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList,
                    "REQUIRED FOR CONNECTING TO NETWORK: Please grant Location permissions",
                    "OK", "Cancel")
            }
            .request{ allGranted, _, _ ->
                if (allGranted) {
                    viewModel.getWifiApsForDevice()
                    wifiListAdapter?.wifiApList = emptyList()
                    wifiListAdapter?.notifyDataSetChanged()
                    //wifiApList.clear()
                    //wifiListAdapter?.notifyDataSetChanged()
                } else Snackbar.make(binding.root,
                    "REQUIRED FOR CONNECTING TO NETWORK: Please grant Location permissions", Snackbar.LENGTH_LONG).show()
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

    private fun populateWifiList(){
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        /*wifiListAdapter = WifiListAdapter(wifiApList) { wifiAp->
            onNetworkSelected(wifiAp)
        }*/
        wifiListAdapter = WifiListAdapter(viewModel.listWifiAp) { wifiAp->
            onNetworkSelected(wifiAp)
        }
        //binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = wifiListAdapter
    }

    private fun onNetworkSelected(wifiAp: WiFiAccessPoint) {
        Timber.d("selected AP: ${wifiAp.wifiName?:""}")
        //viewModel.setSelectedNetwork(wifiAp.wifiName ?: "")
        if(wifiAp.wifiName.isNullOrBlank()) {
            showJoinNetworkDialog()
            return
        }
        if (wifiAp.security != ESPConstants.WIFI_OPEN.toInt()) {
            showJoinNetworkDialog(wifiAp.wifiName)
        }
        else {  // TODO: Check out, if this is absolutely required. We don't want an ESP open network (AP) ever. Right?
            val network = ProvisionNetwork(wifiAp.wifiName)
            viewModel.setSelectedNetwork(network)
            navigateToNextFragment()
        }
    }

    /*private fun setListClickListener() {
        binding.recyclerView.setOnClickListener {

        }
    }*/

    private fun showDeviceDCDialog() {
        val dialogBuilder =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.dialog_title_device_disconnected))
                .setMessage(resources.getString(R.string.dialog_message_device_disconnected))
                .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                    leaveFragment()
                    dialog.dismiss()
                }
                .setCancelable(false)
        deviceDCDialog = dialogBuilder.create()
        deviceDCDialog?.show()
    }

    private fun showCancellationDialog() {
        val dialogBuilder =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Scanning in progress")
                .setMessage("Exiting now may leave your device in an unstable state. We recommend you wait for the process to complete. Would you like to Wait?")
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

    private fun showJoinNetworkDialog(ssid: String="") {

        // make the back button diabled
        binding.btnBack.isEnabled = false

        val bindingDialog = DialogWifiNetworkBinding
            .inflate(LayoutInflater.from(requireContext()))

        if (ssid.isNotEmpty()) {
            bindingDialog.layoutAddNetwork.layoutSsid.visibility = GONE

        }

        var inputSsid: String ?= null
        var inputPwd: String ?= null

        bindingDialog.layoutAddNetwork.etSsid.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                inputSsid = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (s?.length!! >= 32)
                    showSnackbar("Maximum length allowed for network name is 32")
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }
        })

        bindingDialog.layoutAddNetwork.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                inputPwd = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (s?.length!! >= 63)
                    showSnackbar("Maximum password length allowed is 63")
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }
        })

        val dialogBuilder =
            MaterialAlertDialogBuilder(requireContext())
                .setView(bindingDialog.root)
                .setTitle(if (ssid.isEmpty()) resources.getString(R.string.dialog_title_network_info)
                            else ssid)
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
                    binding.btnBack.isEnabled = true
                    dialog?.dismiss()
                }
                .setPositiveButton(resources.getString(R.string.accept)) { dialog, which ->
                    // Respond to positive button press
                    if (ssid.isEmpty()) {
                        if (inputSsid.isNullOrBlank()) {
                            showSnackbar("Network name cannot be blank")
                            return@setPositiveButton
                        }
                    }
                    else {
                        inputSsid = ssid
                        if (inputPwd.isNullOrBlank()) {
                            showSnackbar("Network password cannot be blank")
                            return@setPositiveButton
                        }
                    }
                    val network = ProvisionNetwork(inputSsid!!, inputPwd)
                    Timber.d("input pwd is null?: ${inputPwd==null}")
                    viewModel.setSelectedNetwork(network)
                    navigateToNextFragment()
                }

        joinNetworkDialog = dialogBuilder.create()
        joinNetworkDialog?.show()
    }

    private fun leaveFragment(){
        viewModel.resetConnectedDeviceFrag()
        viewModel.resetScanListFragment()
        navController.popBackStack()
    }

    private fun navigateToNextFragment() {
        joinNetworkDialog?.dismiss()
        Timber.d("navigating to next fragment...")
        val action = WifiScanListFragmentDirections.actionWifiScanListFragToProvFrag()
        navController.navigate(action)
    }

    private fun showSnackbar(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    /*private fun addLastItem(){
        val listOfAp = mutableListOf<WiFiAccessPoint>()
        for (i in 1..10){
            listOfAp.add(WiFiAccessPoint())
            listOfAp[i-1].wifiName = i.toString()
            wifiApList.add(listOfAp[i-1])
            //wifiListAdapter?.notifyItemChanged(wifiApList.size-1)
        }
        wifiListAdapter?.notifyDataSetChanged()
        *//*wifiAp.wifiName = resources.getString(R.string.join_other_network)
        wifiAp.rssi = -1000
        wifiApList.add(wifiAp)
        wifiListAdapter?.notifyItemChanged(wifiApList.size-1)*//*
    }*/

    override fun onClick(v: View?) {
        //if (v == binding.btnRefresh) Timber.d("button refresh clicked...")
        when(v){
            binding.btnRefresh -> {
                Timber.d("button refresh clicked...")
                getWifiList()
            }
            binding.tvJoinNetwork -> {
                Timber.d("textview join_other_network clicked...")
                showJoinNetworkDialog()
            }
            /*binding.tvJoinNetworkCenter -> {
            }*/
            binding.btnBack -> {
                leaveFragment()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deviceDCDialog = null
        dialogCancellation = null
        _navController = null
        _binding = null

    }
}