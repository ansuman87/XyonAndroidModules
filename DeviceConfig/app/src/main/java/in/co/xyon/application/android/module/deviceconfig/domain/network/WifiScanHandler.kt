package `in`.co.xyon.application.android.module.deviceconfig.domain.network

import `in`.co.xyon.application.android.module.deviceconfig.domain.model.RequestResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

class WifiScanHandler(
    private val context: Context
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    //TODO: instead of using cont.isActive() use a Flow for multiple emissions in parallel, otherwise loading can't be used....
    /*@ExperimentalCoroutinesApi
    suspend fun getCurrentScanResult1(): RequestResult<List<ScanResult>> =
        suspendCancellableCoroutine { cont ->
            Timber.d("scan suspendCancellableCoroutine start...")
            Timber.d("scan: currentThread: ${Thread.currentThread()}")
            //define broadcast reciever
            val wifiScanReceiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.action?.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) == true) {
                        context.unregisterReceiver(this)
                        //cont.resume(wifiManager.scanResults)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                            if (success) {
                                //scanSuccess()
                                if (cont.isActive)
                                    cont.resume(RequestResult.Success(wifiManager.scanResults))
                            } else {
                                //scanFailure()
                                if (cont.isActive)
                                    cont.resume(RequestResult.Error("Scan Failed: No new networks found...", wifiManager.scanResults))
                            }
                        } else{
                            //scanSuccess()
                            if (cont.isActive)
                                cont.resume(RequestResult.Success(wifiManager.scanResults))
                        }
                    }
                }
            }


            //setup cancellation action on the continuation
            cont.invokeOnCancellation {
                context.unregisterReceiver(wifiScanReceiver)
            }
            //register broadcast receiver
            Timber.d("scan broadcast receiver registering...")
            context.registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            //kick off scanning to eventually receive the broadcast
            Timber.d("scan starting...")
            val success = wifiManager.startScan()
            //if (cont.isActive)
            //    cont.resume(Resource.Loading())
            if (!success){
                if (cont.isActive)
                    cont.resume(RequestResult.Error("Scan Failed: Please try in some time...", wifiManager.scanResults))
            }
        }*/

    @ExperimentalCoroutinesApi
    fun getCurrentScanResult() = callbackFlow<RequestResult<List<ScanResult>>> {

        Timber.d("scan callbackFlow start...")
        //Timber.d("scan: currentThread: ${Thread.currentThread()}")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action?.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) == true) {
                    //context.unregisterReceiver(this)  // makes the app crash when channel closes
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                        if (success) {
                            //scanSuccess()
                            //Timber.d("trysending1...")
                            trySend(RequestResult.Success(wifiManager.scanResults))
                            //  .isSuccess
                            close()
                        } else {
                            //scanFailure()
                            Timber.d("trysending2...")
                            trySend(RequestResult.Error("Scan Failed: No new networks found...", wifiManager.scanResults))
                            //.isSuccess
                            close()
                        }
                    } else{
                        //scanSuccess()
                        //Timber.d("trysending3...")
                        trySend(RequestResult.Success(wifiManager.scanResults))
                        //.isSuccess
                        close()
                    }
                }
            }
        }

        Timber.d("scan broadcastreceiver registering...")
        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        Timber.d("scan starting...")
        val success = wifiManager.startScan()
        //Timber.d("trysending4...")
        trySend(RequestResult.Loading())
        //.isSuccess
        // probably redundant to check for failure...
        /*if (!success){
            //scanSuccess()
            Timber.d("trysending5...")
            trySend(Resource.Error("Scan Failed: Please try in some time...", wifiManager.scanResults))
                //.isSuccess
            close()
        }
*/
        // should always be placed at the end...
        awaitClose {
            //Timber.d("awaitClose called...")
            context.unregisterReceiver(receiver)
        }
    }


}
