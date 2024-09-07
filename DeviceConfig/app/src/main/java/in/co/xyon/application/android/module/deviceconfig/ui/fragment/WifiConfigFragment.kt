package `in`.co.xyon.application.android.module.deviceconfig.ui.fragment

import `in`.co.xyon.application.android.module.deviceconfig.R
import `in`.co.xyon.application.android.module.deviceconfig.databinding.FragmentWifiConfigBinding
import `in`.co.xyon.application.android.module.deviceconfig.presentation.WifiOnlyViewModel
import `in`.co.xyon.application.android.module.deviceconfig.presentation.states.ProvisionNetwork
import `in`.co.xyon.application.android.module.deviceconfig.presentation.states.WifiConfigUiState
import `in`.co.xyon.application.android.module.deviceconfig.utils.collectLatestLifecycleFlow
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.espressif.provisioning.ESPProvisionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class WifiConfigFragment : Fragment(), View.OnClickListener {

    private var _navController: NavController? = null
    private val navController get() = _navController!!

    private val viewModel: WifiOnlyViewModel by activityViewModels<WifiOnlyViewModel>()

    private var _binding: FragmentWifiConfigBinding ?= null
    private val binding get() = _binding!!

    private lateinit var provisionManager: ESPProvisionManager

    private var inputSsid: String ?= null
    private var inputPwd: String ?= null

    private var deviceDCDialog : AlertDialog?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWifiConfigBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _navController = Navigation.findNavController(view)
        provisionManager = ESPProvisionManager.getInstance(requireContext())

        binding.btnBack.setOnClickListener(this)
        binding.btnNext.setOnClickListener(this)

        observeUiState()

        binding.layoutAddNetwork.etSsid.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (s?.length!! >= 32)
                    showSnackbar("Maximum length allowed for network name is 32")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }

            override fun afterTextChanged(s: Editable?) {
                inputSsid = s.toString()
            }
        })

        binding.layoutAddNetwork.etPassword.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (s?.length!! >= 63)
                    showSnackbar("Maximum password length allowed is 63")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }

            override fun afterTextChanged(s: Editable?) {
                inputPwd = s.toString()
            }
        })
    }

    private fun observeUiState(){
        collectLatestLifecycleFlow(viewModel.wifiConfigUiStateFlow){
            when(it){
                WifiConfigUiState.DEVICE_DISCONNECTED -> {
                    binding.btnNext.isEnabled = false
                    showDeviceDCDialog()
                }
                WifiConfigUiState.IDLE_STATE -> {
                    //TODO: probably doing nothing...
                }
            }
        }
    }

    private fun showSnackbar(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

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

    override fun onClick(v: View?) {
        when(v){
            binding.btnBack -> {
                leaveFragment()
            }
            binding.btnNext -> {
                if (inputSsid.isNullOrBlank()) {
                    showSnackbar("Network name cannot be blank")
                    return
                }
                val network = ProvisionNetwork(inputSsid!!, inputPwd)
                Timber.d("input pwd is null?: ${inputPwd==null}")
                viewModel.setSelectedNetwork(network)
                navigateToNextFragment()
            }
        }
    }

    private fun leaveFragment(){
        viewModel.resetConnectedDeviceFrag()
        viewModel.resetScanListFragment()
        navController.popBackStack()
    }

    private fun navigateToNextFragment() {
        Timber.d("navigating to next fragment...")
        val action = WifiConfigFragmentDirections.actionWifiConfigFragToProvFrag()
        navController.navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        /** required for avoiding memory leak? **/
        _navController = null
        _binding = null

    }
}