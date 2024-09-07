package `in`.co.xyon.application.android.module.deviceconfig.domain.usecase

import `in`.co.xyon.application.android.module.deviceconfig.domain.network.ProvisioningHandler
import `in`.co.xyon.application.android.module.deviceconfig.domain.model.RequestResult
import com.espressif.provisioning.WiFiAccessPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.ArrayList
import javax.inject.Inject

class GetNetworkListFromDevice @Inject constructor(
    private val provisioningHandler: ProvisioningHandler
) {

    @ExperimentalCoroutinesApi
    suspend operator fun invoke(): RequestResult<ArrayList<WiFiAccessPoint>> {
        return try {
            provisioningHandler.getScanResults()
        }catch (e: Exception){
            RequestResult.Error(e.toString())
        }
    }
}