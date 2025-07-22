package `in`.co.xyon.application.android.module.deviceconfig.domain.usecase

import `in`.co.xyon.application.android.module.deviceconfig.domain.model.ProvisionResult
import `in`.co.xyon.application.android.module.deviceconfig.domain.network.ProvisioningHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.lang.Exception
import javax.inject.Inject

class DoProvisioning @Inject constructor(
    private val provisioningHandler: ProvisioningHandler
){

    @ExperimentalCoroutinesApi
    operator fun invoke(ssid: String, pwd: String): Flow<ProvisionResult> {

            //Timber.d("trying provisioning in DoProvisioning...")
        return try {
                provisioningHandler.doProvisioning(ssid, pwd)
            }catch (e: Exception){
                //Timber.e(e, "error invoking flow - provisionHandler.doProvisioning...")
                flow{
                    emit(ProvisionResult.OnProvisioningFailed(e))
                }
            }

    }
}