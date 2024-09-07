package `in`.co.xyon.application.android.module.deviceconfig.presentation.states

import com.espressif.provisioning.ESPConstants

data class ProvisioningState(
    val createSession : ProvisioningStateTracker = ProvisioningStateTracker.INIT,
    val sendWifiConfig : ProvisioningStateTracker = ProvisioningStateTracker.INIT,
    val wifiConfigApplied : ProvisioningStateTracker = ProvisioningStateTracker.INIT,
    val provisioningStatus : ProvisioningStateTracker = ProvisioningStateTracker.INIT,
    val showDisconnectAlertDialog : Boolean = false
    ){}

enum class ProvisioningStateTracker {
    INIT,
    UNDERWAY,
    SUCCESS,
    FAILURE
}
