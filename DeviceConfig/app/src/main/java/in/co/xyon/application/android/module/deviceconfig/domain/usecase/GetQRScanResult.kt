package `in`.co.xyon.application.android.module.deviceconfig.domain.usecase

import `in`.co.xyon.application.android.module.deviceconfig.domain.model.QRScanResult
import `in`.co.xyon.application.android.module.deviceconfig.domain.model.QRScanResult1
import `in`.co.xyon.application.android.module.deviceconfig.domain.model.RequestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetQRScanResult
{
    operator fun invoke(url: String) : Flow<RequestResult<QRScanResult>> {

        // imitating a network call here...
        return flow {
            try {
                emit(RequestResult.Loading())
                delay(2000L)
                emit(RequestResult.Success(QRScanResult( "PROV_0FF0B8", "wifiprov","abcd1234")))  //
            } catch (e: Exception){
                emit(RequestResult.Error(message = e.localizedMessage?:"Unknown Error"))
            }



        }
    }
}