package `in`.co.xyon.application.android.module.deviceconfig.domain.usecase

import `in`.co.xyon.application.android.module.deviceconfig.domain.network.WifiScanHandler
import `in`.co.xyon.application.android.module.deviceconfig.domain.model.RequestResult
import android.net.wifi.ScanResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ScanWifi @Inject constructor(
    private val wifiScanHandler: WifiScanHandler
) {

    /*@ExperimentalCoroutinesApi
    suspend operator fun invoke() : Resource<List<ScanResult>> {
        return try {
            wifiScanHandler.getCurrentScanResult()
        } catch (e: Exception){
            Resource.Error(e.toString())
        }
    }*/

    @ExperimentalCoroutinesApi
    operator fun invoke() : Flow<RequestResult<List<ScanResult>>> {
        return try {
            wifiScanHandler.getCurrentScanResult()
        } catch (e: Exception){
            flow {
                emit(RequestResult.Error(e.toString()))
            }
        }
    }
}