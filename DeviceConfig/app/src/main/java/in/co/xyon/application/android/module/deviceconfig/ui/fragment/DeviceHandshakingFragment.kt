package `in`.co.xyon.application.android.module.deviceconfig.ui.fragment

import `in`.co.xyon.application.android.module.deviceconfig.databinding.FragmentDeviceHandshakingBinding
import `in`.co.xyon.application.android.module.deviceconfig.presentation.WifiOnlyViewModel
import `in`.co.xyon.application.android.module.deviceconfig.utils.getNetworkConnectPermissions
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.espressif.provisioning.DeviceConnectionEvent
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPProvisionManager
import com.google.android.material.snackbar.Snackbar
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

@AndroidEntryPoint
class DeviceHandshakingFragment : Fragment(){

    private lateinit var navController: NavController
    private val viewModel: WifiOnlyViewModel by activityViewModels<WifiOnlyViewModel>()
    private lateinit var binding: FragmentDeviceHandshakingBinding
    private lateinit var provisionManager: ESPProvisionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceHandshakingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        //viewModel.createEspDevice(viewModel.provisionModeStateFlow.value)
        EventBus.getDefault().register(this)
        provisionManager = ESPProvisionManager.getInstance(requireContext())
        connectDevice()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: DeviceConnectionEvent) {
        Timber.i("On Device Prov Event RECEIVED: ${event.eventType}")

        when(event.eventType){
            ESPConstants.EVENT_DEVICE_CONNECTED -> {
                Timber.i("Device Connected Event Received")
                /*val deviceCaps = viewModel.getDeviceCapabilities()
                if (deviceCaps != null && deviceCaps.contains("wifi_scan")){
                    Timber.d("device capabilities contain wifi-scan")
                }
                else{
                    Timber.d("device capabilities either null or not contain wifi-scan")
                }*/
                val deviceCaps = provisionManager.espDevice.deviceCapabilities ?: emptyList<String>()
                Timber.d("event_device_connected: device capabilities $deviceCaps")

            }
            ESPConstants.EVENT_DEVICE_CONNECTION_FAILED -> {
                Timber.i("Device Connection Failed")
            }
        }

    }

    private fun connectDevice(){
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
                    provisionManager.espDevice.connectWiFiDevice()
                } else Snackbar.make(binding.root,
                    "REQUIRED FOR CONNECTING TO NETWORK: Please grant Location permissions", Snackbar.LENGTH_LONG).show()
            }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        provisionManager.espDevice.disconnectDevice()
        super.onDestroy()
    }

}