package `in`.co.xyon.application.android.module.deviceconfig.ui.fragment

import `in`.co.xyon.application.android.module.deviceconfig.R
import `in`.co.xyon.application.android.module.deviceconfig.databinding.FragmentProvisioningBinding
import `in`.co.xyon.application.android.module.deviceconfig.presentation.WifiOnlyViewModel
import `in`.co.xyon.application.android.module.deviceconfig.presentation.states.ProvisioningStateTracker
import `in`.co.xyon.application.android.module.deviceconfig.utils.collectLatestLifecycleFlow
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPProvisionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class ProvisioningFragment : Fragment(), View.OnClickListener {

    private var _navController: NavController? = null
    private val navController get() = _navController!!

    private val viewModel: WifiOnlyViewModel by activityViewModels<WifiOnlyViewModel>()

    private var _binding: FragmentProvisioningBinding?= null
    private val binding get() = _binding!!

    private lateinit var provisionManager: ESPProvisionManager

    private var alertDialog: AlertDialog?= null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProvisioningBinding.inflate(layoutInflater, container, false)
        //return super.onCreateView(inflater, container, savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _navController = Navigation.findNavController(view)
        provisionManager = ESPProvisionManager.getInstance(requireContext())

        observeUiState()
        binding.btnBack.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.isProvisioningUnderWay())
            viewModel.startProvisioning()
    }

    private fun observeUiState() {
        collectLatestLifecycleFlow(viewModel.provisioningUiStateFlow) {
            Timber.d("provisioning state: CreateSession = %s,\n SendWifiConfig = %s, \n WifiConfigApplied = %s, \n ProvisioningStatus = %s, \n ShowDisconnAlertDialog = %s",
                it.createSession, it.sendWifiConfig, it.wifiConfigApplied, it.provisioningStatus, it.showDisconnectAlertDialog)
            when(it.createSession){
                ProvisioningStateTracker.INIT -> {
                    binding.layoutProvProcess.ivTick1.setImageResource(R.drawable.ic_checkbox_unselected)
                    binding.layoutProvProcess.ivTick1.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress1.visibility = View.GONE
                    binding.tvProvError.visibility = View.GONE
                    //binding.btnBack.isEnabled = true
                }
                ProvisioningStateTracker.UNDERWAY -> {
                    binding.layoutProvProcess.ivTick1.visibility = View.GONE
                    binding.layoutProvProcess.provProgress1.visibility = View.VISIBLE
                    binding.layoutProvProcess.tvProvError1.text = resources.getString(R.string.prov_step_1)
                    binding.layoutProvProcess.tvProvError1.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.GONE
                    //binding.btnBack.isEnabled = false
                }
                ProvisioningStateTracker.FAILURE -> {
                    binding.layoutProvProcess.ivTick1.setImageResource(R.drawable.ic_error)
                    binding.layoutProvProcess.ivTick1.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress1.visibility = View.GONE
                    binding.layoutProvProcess.tvProvError1.text = resources.getString(R.string.error_session_creation)
                    binding.layoutProvProcess.tvProvError1.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.VISIBLE
                    //binding.btnBack.isEnabled = true
                }
                ProvisioningStateTracker.SUCCESS -> {
                }
            }
            when(it.sendWifiConfig){
                ProvisioningStateTracker.INIT -> {
                    binding.layoutProvProcess.ivTick1.setImageResource(R.drawable.ic_checkbox_unselected)
                    binding.layoutProvProcess.ivTick1.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress1.visibility = View.GONE
                    binding.tvProvError.visibility = View.GONE
                }
                ProvisioningStateTracker.UNDERWAY -> {
                    binding.layoutProvProcess.ivTick1.visibility = View.GONE
                    binding.layoutProvProcess.provProgress1.visibility = View.VISIBLE
                    binding.layoutProvProcess.tvProvError1.text = resources.getString(R.string.prov_step_1)
                    binding.layoutProvProcess.tvProvError1.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.GONE
                    //binding.btnBack.isEnabled = false
                }
                ProvisioningStateTracker.FAILURE -> {
                    binding.layoutProvProcess.ivTick1.setImageResource(R.drawable.ic_error)
                    binding.layoutProvProcess.ivTick1.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress1.visibility = View.GONE
                    binding.layoutProvProcess.tvProvError1.text = resources.getString(R.string.error_prov_step_1)
                    binding.layoutProvProcess.tvProvError1.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.VISIBLE
                    binding.btnBack.isEnabled = true
                }
                ProvisioningStateTracker.SUCCESS -> {
                    binding.layoutProvProcess.ivTick1.setImageResource(R.drawable.ic_checkbox_on)
                    binding.layoutProvProcess.ivTick1.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress1.visibility = View.GONE
                    binding.layoutProvProcess.tvProvError1.text = resources.getString(R.string.prov_step_1)
                    binding.layoutProvProcess.tvProvError1.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.GONE
                   // binding.btnBack.isEnabled = false
                }
            }
            when(it.wifiConfigApplied){
                ProvisioningStateTracker.INIT -> {
                    binding.layoutProvProcess.ivTick2.setImageResource(R.drawable.ic_checkbox_unselected)
                    binding.layoutProvProcess.ivTick2.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress2.visibility = View.GONE
                    binding.tvProvError.visibility = View.GONE
                }
                ProvisioningStateTracker.UNDERWAY -> {
                    binding.layoutProvProcess.ivTick2.visibility = View.GONE
                    binding.layoutProvProcess.provProgress2.visibility = View.VISIBLE
                    binding.layoutProvProcess.tvProvError2.text = resources.getString(R.string.prov_step_2)
                    binding.layoutProvProcess.tvProvError2.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.GONE
                    //binding.btnBack.isEnabled = false
                }
                ProvisioningStateTracker.FAILURE -> {
                    binding.layoutProvProcess.ivTick2.setImageResource(R.drawable.ic_error)
                    binding.layoutProvProcess.ivTick2.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress2.visibility = View.GONE
                    binding.layoutProvProcess.tvProvError2.text = resources.getString(R.string.error_prov_step_2)
                    binding.layoutProvProcess.tvProvError2.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.VISIBLE
                    //binding.btnBack.isEnabled = true
                }
                ProvisioningStateTracker.SUCCESS -> {
                    binding.layoutProvProcess.ivTick2.setImageResource(R.drawable.ic_checkbox_on)
                    binding.layoutProvProcess.ivTick2.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress2.visibility = View.GONE
                    binding.layoutProvProcess.tvProvError2.text = resources.getString(R.string.prov_step_2)
                    binding.layoutProvProcess.tvProvError2.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.GONE
                    //binding.btnBack.isEnabled = false
                }
            }
            when(it.provisioningStatus){
                ProvisioningStateTracker.INIT -> {
                    binding.layoutProvProcess.ivTick3.setImageResource(R.drawable.ic_checkbox_unselected)
                    binding.layoutProvProcess.ivTick3.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress3.visibility = View.GONE
                    binding.tvProvError.visibility = View.GONE
                }
                ProvisioningStateTracker.UNDERWAY -> {
                    binding.layoutProvProcess.ivTick3.visibility = View.GONE
                    binding.layoutProvProcess.provProgress3.visibility = View.VISIBLE
                    binding.layoutProvProcess.tvProvError3.text = resources.getString(R.string.prov_step_3)
                    binding.layoutProvProcess.tvProvError3.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.GONE
                    //binding.btnBack.isEnabled = false
                }
                ProvisioningStateTracker.FAILURE -> {
                    var errMsg = resources.getString(R.string.error_prov_step_3)
                    if(viewModel.provisionFailureReason != null) {
                        if (viewModel.provisionFailureReason == ESPConstants.ProvisionFailureReason.AUTH_FAILED)
                            errMsg = resources.getString(R.string.error_authentication_failed)
                        else if (viewModel.provisionFailureReason == ESPConstants.ProvisionFailureReason.NETWORK_NOT_FOUND)
                            errMsg = resources.getString(R.string.error_network_not_found)
                    }

                    binding.layoutProvProcess.ivTick3.setImageResource(R.drawable.ic_error)
                    binding.layoutProvProcess.ivTick3.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress3.visibility = View.GONE
                    binding.layoutProvProcess.tvProvError3.text = errMsg
                    binding.layoutProvProcess.tvProvError3.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.VISIBLE
                    //binding.btnBack.isEnabled = false
                }
                ProvisioningStateTracker.SUCCESS -> {
                    binding.layoutProvProcess.ivTick3.setImageResource(R.drawable.ic_checkbox_on)
                    binding.layoutProvProcess.ivTick3.visibility = View.VISIBLE
                    binding.layoutProvProcess.provProgress3.visibility = View.GONE
                    binding.layoutProvProcess.tvProvError3.text = resources.getString(R.string.prov_step_3)
                    binding.layoutProvProcess.tvProvError3.visibility = View.VISIBLE
                    binding.tvProvError.visibility = View.GONE
                    //binding.btnBack.isEnabled = false
                    showSuccessSnackbarAndLeave()
                }
            }
            if (it.showDisconnectAlertDialog) showDialogIfProvisionIncomplete()
        }
    }

    private fun showDialogIfProvisionIncomplete() {
        if (viewModel.isProvisioningSuccessful) return

        val dialogBuilder =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.dialog_title_device_disconnected))
                .setMessage(resources.getString(R.string.dialog_message_device_disconnected))
                .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                    leaveFragmentAndGoToStart()
                    dialog.dismiss()
                }
                .setCancelable(false)

        alertDialog = dialogBuilder.create()
        alertDialog?.show()
    }

    private fun showSuccessSnackbarAndLeave(){
        Snackbar.make(binding.root, resources.getString(
            R.string.snackbar_message_provision_success
        ), Snackbar.LENGTH_LONG).show()
//        leaveFragment()
        leaveFragmentAndGoToStart()
    }

    private fun leaveFragmentAndGoToStart() {
        resetCompleteProvData()
        viewModel.endSessionAndCloseConnection()
        findNavController().navigate(R.id.action_provisioningFragment_to_deviceTypeSelectionFragment)
    }

    private fun resetCompleteProvData() {
        viewModel.resetConnectedDeviceFrag()
        viewModel.resetScanListFragment()
        viewModel.resetProvisioningFragment()
    }

    private fun goToPreviousFragment() {
        viewModel.resetProvisioningFragmentStateOnly()
        navController.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        /** required for avoiding memory leak? **/
        _navController = null
        _binding = null
    }

    override fun onClick(v: View?) {
        goToPreviousFragment()
    }
}