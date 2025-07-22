package `in`.co.xyon.application.android.module.deviceconfig.domain.network

import `in`.co.xyon.application.android.module.deviceconfig.domain.model.ProvisionResult
import `in`.co.xyon.application.android.module.deviceconfig.domain.model.RequestResult
import android.content.Context
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.WiFiAccessPoint
import com.espressif.provisioning.listeners.ProvisionListener
import com.espressif.provisioning.listeners.WiFiScanListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.lang.Exception
import java.util.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ProvisioningHandler(
    private val appContext: Context
) {

    @ExperimentalCoroutinesApi
    suspend fun getScanResults(): RequestResult<ArrayList<WiFiAccessPoint>> =
        suspendCoroutine { cont ->

            /*cont.invokeOnCancellation{
                //TODO:......
            }*/

            val provisionManager = ESPProvisionManager.getInstance(appContext)

            provisionManager.espDevice.scanNetworks( object: WiFiScanListener {
                override fun onWifiListReceived(wifiList: ArrayList<WiFiAccessPoint>) {
                    cont.resume(RequestResult.Success(wifiList))
                }

                override fun onWiFiScanFailed(e: Exception) {
                    //if (e == null) cont.resume(RequestResult.Error("Unknown error scanning network"))
                    //else
                        cont.resume(RequestResult.Error(e.localizedMessage?:"Unknown error scanning network"))
                }
            })


        }

    @ExperimentalCoroutinesApi
    fun doProvisioning(ssid: String, pwd: String) = callbackFlow<ProvisionResult> {
        val provisionManager = ESPProvisionManager.getInstance(appContext)
        Timber.d("start provisioning...")
        provisionManager.espDevice.provision(ssid, pwd, object : ProvisionListener {
            override fun createSessionFailed(e: Exception?) {
                Timber.e(e,"createSessionFailed...")
                trySend(ProvisionResult.CreateSessionFailed(e))
            }

            override fun wifiConfigSent() {
                Timber.d("wifiConfig sent...")
                trySend(ProvisionResult.WifiConfigSent())
            }

            override fun wifiConfigFailed(e: Exception?) {
                Timber.d("wifiConfig failed...")
                trySend(ProvisionResult.WifiConfigFailed(e))
            }

            override fun wifiConfigApplied() {
                Timber.d("wifiConfig Applied...")
                trySend(ProvisionResult.WifiConfigApplied())
            }

            override fun wifiConfigApplyFailed(e: Exception?) {
                Timber.d("wifiConfig apply - failed...")
                trySend(ProvisionResult.WifiConfigApplyFailed(e))
            }

            override fun provisioningFailedFromDevice(failureReason: ESPConstants.ProvisionFailureReason?) {
                Timber.d("provisioning failed from device...")
                trySend(ProvisionResult.ProvisioningFailedFromDevice(failureReason))
            }

            override fun deviceProvisioningSuccess() {
                Timber.d("provisioning success")
                trySend(ProvisionResult.DeviceProvisioningSuccess())
            }

            override fun onProvisioningFailed(e: Exception?) {
                Timber.d("provisioning failed")
                trySend(ProvisionResult.OnProvisioningFailed(e))
            }

        })

        awaitClose {
            //TODO: no un-registration method in the library implementation
        }
    }

}