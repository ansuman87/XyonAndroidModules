package `in`.co.xyon.application.android.module.deviceconfig.domain.usecase

import `in`.co.xyon.application.android.module.deviceconfig.domain.network.ProvisioningHandler
import javax.inject.Inject

class StartHandshaking @Inject constructor(
    private val provisioningHandler: ProvisioningHandler
){

    /*@ExperimentalCoroutinesApi
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    operator fun invoke() : Flow<ConnectionResult<List<String>>> {
        //return provisioningHandler.startAndObserveProvisioningEvents()
    }*/
}