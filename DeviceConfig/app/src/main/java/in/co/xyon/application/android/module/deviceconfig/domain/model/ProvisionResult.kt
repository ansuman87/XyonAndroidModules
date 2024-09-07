package `in`.co.xyon.application.android.module.deviceconfig.domain.model

import com.espressif.provisioning.ESPConstants
import java.lang.Exception

sealed class ProvisionResult(val data: ESPConstants.ProvisionFailureReason? = null, val e: Exception? = null) {

    class CreateSessionFailed(e: Exception? = null, data: ESPConstants.ProvisionFailureReason? = null) : ProvisionResult(data, e)
    class WifiConfigSent(data: ESPConstants.ProvisionFailureReason? = null) : ProvisionResult(data)
    class WifiConfigFailed(e: Exception? = null, data: ESPConstants.ProvisionFailureReason? = null) : ProvisionResult(data, e)
    class WifiConfigApplied(data: ESPConstants.ProvisionFailureReason? = null) : ProvisionResult(data)
    class WifiConfigApplyFailed(e: Exception? = null, data: ESPConstants.ProvisionFailureReason? = null) : ProvisionResult(data, e)
    class ProvisioningFailedFromDevice(data: ESPConstants.ProvisionFailureReason? = null) : ProvisionResult(data)
    class DeviceProvisioningSuccess(data: ESPConstants.ProvisionFailureReason? = null) : ProvisionResult(data)
    class OnProvisioningFailed(e: Exception? = null, data: ESPConstants.ProvisionFailureReason? = null) : ProvisionResult(data, e)
}
