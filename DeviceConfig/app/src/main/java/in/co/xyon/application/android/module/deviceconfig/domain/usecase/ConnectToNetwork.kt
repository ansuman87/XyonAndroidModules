package `in`.co.xyon.application.android.module.deviceconfig.domain.usecase

import `in`.co.xyon.application.android.module.deviceconfig.domain.network.WifiConnectorHandler
import `in`.co.xyon.application.android.module.deviceconfig.domain.model.ConnectionResult
import android.os.Build
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ConnectToNetwork @Inject constructor(
    private val wifiConnectorHandler: WifiConnectorHandler
) {

    @ExperimentalCoroutinesApi
    operator fun invoke(ssid: String, key: String): Flow<ConnectionResult<String>> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiConnectorHandler.connectToWiFiQ(ssid, key)
        } else {
            wifiConnectorHandler.connectToWiFi(ssid, key)
        }
    }
}