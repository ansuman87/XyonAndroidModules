package `in`.co.xyon.application.android.module.deviceconfig.domain.network

import `in`.co.xyon.application.android.module.deviceconfig.domain.model.ConnectionResult
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException

class WifiConnectorHandler(
    private val appContext: Context
) {
    //ignore warning as the appContext provided is the Application Context
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var connectedNetworkId: Int = -1

    private fun quoted(SSID: String): String = "\"" + SSID + "\""

    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToWiFiQ(ssid: String, key: String?)
            = callbackFlow<ConnectionResult<String>>
    {
        trySend(ConnectionResult.Connecting())
        val networkSpecifierBuilder = WifiNetworkSpecifier.Builder()    //WifiNetworkSpecifier.Builder requires Android api level 29
        networkSpecifierBuilder.setSsid(ssid)
        networkSpecifierBuilder.setWpa2Passphrase(key?:"")

        val wifiNetworkSpecifier = networkSpecifierBuilder.build()

        val networkRequestBuilder = NetworkRequest.Builder()
        networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier)  //NetworkRequestBuilder.setNetworkSpecifier(WifiNetworkSpecifier) requires Android api level 26
        networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)

        val networkRequest = networkRequestBuilder.build()

        // declare callback
        val networkCallback = object : ConnectivityManager.NetworkCallback(){
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Timber.d("network: onAvailable $network")
                connectivityManager.bindProcessToNetwork(network)   //ConnectivityManager.bindProcessToNetwork requires Android api level 23
                //trySend(NetworkCallbackResult.Available(network))
                trySend(ConnectionResult.Connected(ssid))
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Timber.d("network: onLost $network")
                trySend(ConnectionResult.Disconnected())
                //TODO("remove network from device: unbind....otherwise it remembers the password...NOT POSSIBLE PROBABLY")
            }

            /*override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val wifiInfo = networkCapabilities.transportInfo
                if (wifiInfo != null && wifiInfo is WifiInfo) {
                    Timber.d("network: Connected SSID: ${wifiInfo.ssid}")
                    trySend(ConnectionResult.Connected(wifiInfo.ssid?:""))
                }else{
                    Timber.d("wifiInfo is null? ${wifiInfo == null}")
                    Timber.d("wifiInfo is instance of WifiInfo ? ${wifiInfo is WifiInfo}")
                    trySend(ConnectionResult.ErrorConnecting("Error in Network Configuration. Not connected to the right network."))
                }
                super.onCapabilitiesChanged(network, networkCapabilities)
            }*/

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties)
                //Timber.d("network: linkPropertiesChanged - linkProperties = ${linkProperties.toString()}")
                //Timber.d("network: linkPropertiesChanged - network = ${network.toString()}")
                for (linkAddress in linkProperties.linkAddresses) {
                    val inetAddress = linkAddress.address
                    Timber.d("network: linkAddress address= %s", inetAddress)
                    Timber.d("network: linkAddress prefixLen= %s", linkAddress.prefixLength)
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        Timber.d("network: ip4 address= %s", inetAddress)
                        Timber.d("network: ip4 prefixLength= %s", linkAddress.prefixLength)
                        //trySend(ConnectionResult.Connected(inetAddress))
                    }
                }
            }
        }

        //register callback
        connectivityManager.requestNetwork(networkRequest, networkCallback)

        // should always be placed at the end...
        awaitClose {
            Timber.d("awaitClose called...")
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    @ExperimentalCoroutinesApi
    fun connectToWiFi(ssid: String, key: String?)
            = callbackFlow<ConnectionResult<String>>
    {
        Timber.d("connectToWiFi method start...")
        trySend(ConnectionResult.Connecting())

        val netId = prepareNetworkConfig(ssid, key?:"")
        if (netId == -1) {
            trySend(ConnectionResult.ErrorConnecting(message = "ERROR: Something went wrong. Invalid network request."))
            return@callbackFlow
        }
        var connectedNetId: Int = 0
        var count = 0
        //var isProvidedNetworkAvailable: Boolean = false

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()
        // declare callback
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                //isProvidedNetworkAvailable = false
                // if (isConnectedToCorrectSSID(ssid))
                Timber.d("count = $count")
                connectedNetId = wifiManager.connectionInfo.networkId
                Timber.d("network: Connected netId: $connectedNetId, and...")
                Timber.d("network: Connected ssid: ${wifiManager.connectionInfo.ssid}")
                if (connectedNetId == netId){
                    val addr: Int = wifiManager.connectionInfo.ipAddress
                    val inetAddress = toInetAddress(addr)
                    Timber.d("network: inetAddress = $inetAddress, intAdd = $addr")
                    if (inetAddress!=null && !inetAddress.isLoopbackAddress && inetAddress is Inet4Address ){
                        // bind the network to the process
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            connectivityManager.bindProcessToNetwork(network)
                        //trySend(ConnectionResult.Connected(inetAddress))
                        //isProvidedNetworkAvailable = true
                    }
                    val wifiInfo = wifiManager.connectionInfo
                    Timber.d("network: Connected SSID: ${wifiInfo.ssid}")
                    trySend(ConnectionResult.Connected(wifiInfo.ssid?:""))
                }
                count++
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                if(connectedNetId == netId)
                    trySend(ConnectionResult.Disconnected())
            }

            /*override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                //var wifiInfo = networkCapabilities.transportInfo as WifiInfo

            }*/

            /*override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties)
                Timber.d("network: onLinkPropertiesChanged v<@Build.Q")
                for (linkAddress in linkProperties.linkAddresses) {
                    val inetAddress = linkAddress.address
                    if(wifiManager.connectionInfo.networkId == netId) {
                        Timber.d("Network: linkAddress address= %s", inetAddress)
                        Timber.d("Network: linkAddress prefixLen= %s", linkAddress.prefixLength)
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            Timber.d("Network: ip4 address= %s", inetAddress)
                            Timber.d("Network: ip4 prefixLength= %s", linkAddress.prefixLength)
                            trySend(ConnectionResult.Connected(inetAddress))
                        }
                    }
                }
            }*/
        }

        //register callback
        connectivityManager.requestNetwork(networkRequest, networkCallback)

        //delay(3000)
        //attempt to connect to network
        attemptToConnectToWifi(netId)
        //val start = System.nanoTime()
        //var elapsedTime: Long

        // should always be placed at the end???
        awaitClose {
            Timber.d("awaitClose called...")
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }

        /*while (true){
            Timber.d("Network: inside the while loop...")
            elapsedTime = System.nanoTime() - start
            if (!isProvidedNetworkAvailable && elapsedTime/1_000_000 >= 5000) {
                trySend(ConnectionResult.TimeOut("TIMEOUT: $elapsedTime ms elapsed trying to connect. Try again"))
                close()
            }
            delay(200)
        }*/

    }

    private fun isConnectedToCorrectSSID(ssid: String): Boolean {
        val currentSSID = wifiManager.connectionInfo.ssid ?: return false
        Timber.v("Connected to $currentSSID")
        return currentSSID == "\"${ssid}\""
    }

    // connection method for devices below level 29
    private suspend fun prepareNetworkConfig(ssid: String, key: String): Int {
        var securityType = WifiConfiguration.KeyMgmt.NONE
        if (key.isNotBlank() || key.isNotEmpty())
            securityType = WifiConfiguration.KeyMgmt.WPA_PSK
        var highestPriorityNumber = 0
        var selectedConfig: WifiConfiguration?= null//for devices below api level 29

        /*Check to perform/ get wifiManager.configuredNetworks*/
        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            /*final int REQUEST_LOCATION = 2;

            if (ActivityCompat.shouldShowRequestPermissionRationale(context,
                    ACCESS_FINE_LOCATION)) {
                // Display UI and wait for user interaction
            } else {
                ActivityCompat.requestPermissions(
                        context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION);
            }*/

            Timber.d("Network: Permissions not granted...")
            return -2
        }

        /* Check if not connected but has connected to that wifi in the past */
        //selectedConfig = null   //reset selectedConfig to null
        for (config: WifiConfiguration in wifiManager.configuredNetworks) {
            if (config.priority > highestPriorityNumber) highestPriorityNumber = config.priority
            Timber.d("Network: config ssid =${config.SSID?:""} and...")
            Timber.d("Network: config netId = ${config.networkId} and...")
            Timber.d("Network: config priority = ${config.priority} and...")
            Timber.d("Network: config preSharedKey = ${config.preSharedKey?:"null"}")

            Timber.d("Network: config SSID: ${config.SSID}, quoted provided SSID: ${quoted(ssid)}")

            if (config.SSID.equals(quoted(ssid))) {
                /*if (config.preSharedKey != null && config.preSharedKey.equals(quoted(key))){
                    selectedConfig = config
                    Timber.d("Network: 1. selected config = config")
                }
                if (WifiConfiguration.KeyMgmt.WPA_PSK == 1 &&    //only WPA_PSK is allowed
                    config.preSharedKey != null &&
                    config.preSharedKey.equals(quoted(key))){
                    selectedConfig = config

                    //Timber.d("Network: 2. selected config = config")
                }
                else if (securityType == WifiConfiguration.KeyMgmt.NONE)
                    selectedConfig = config*/

                //remove the networks corresponding to the given SSID and make new connection...
                forgetProvidedNetwork(config.networkId)
                highestPriorityNumber-=1
                //delay(500)
            }
        }

        /*if (selectedConfig != null) {
            selectedConfig.priority = highestPriorityNumber + 10
            wifiManager.updateNetwork(selectedConfig)

            val wifiInfo = wifiManager.connectionInfo
            val str: String = wifiInfo.ssid.replace("\"", "")

            if (str.equals(ssid)) {
                Timber.d("Network: already connected to this network...")
                connectedNetworkId = selectedConfig.networkId
                return selectedConfig.networkId
            }

            Timber.d("Network: Connecting to previously connected network...")
            Timber.d("Network: But since it has to be done later after registering network callback listener, we only return the network Id here...")
            return selectedConfig.networkId
            *//*wifiManager.enableNetwork(selectedConfig.networkId, true)
            //val isConnected = wifiManager.reconnect()
            if (wifiManager.reconnect()) {
                connectedNetworkId = selectedConfig.networkId
                return selectedConfig.networkId
            }*//*
        }*/

        /*make new connection*/
        val config = WifiConfiguration()
        config.SSID = quoted(ssid)
        config.priority = highestPriorityNumber + 1
        config.status = WifiConfiguration.Status.ENABLED

        Timber.d("securityType WPA_PSK? = ${WifiConfiguration.KeyMgmt.WPA_PSK}")
        if (securityType == WifiConfiguration.KeyMgmt.WPA_PSK) {
            //if (key.isNotBlank() || key.isNotEmpty()) {
            config.preSharedKey = quoted(key)
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA)

        } else config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)

        val netId = wifiManager.addNetwork(config)
        Timber.d("network: added network ssid: ${config.SSID}, preSharedKey: ${config.preSharedKey?:"null"}")
        Timber.d("network: added network Id: $netId")
        Timber.d("network: added network priority: ${config.priority}")
        //val netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        /*if (netInfo != null && netInfo.isConnected) {
            val wifiInfo = wifiManager.connectionInfo
            val str = wifiInfo.ssid.replace("\"", "")
            if (str.equals(ssid)) {
                Timber.d("Network: already connected to this network...")
                //selectedConfig = config
                connectedNetworkId = config.networkId
                return config.networkId
            }
        }*/

        return netId

    }

    private suspend fun attemptToConnectToWifi(netId: Int){
        Timber.d("Network: connecting to provided network...")
        disConnectNetwork()
        //delay(500)
        wifiManager.enableNetwork(netId, true)
        // val isConnected = wifiManager.reconnect()
        wifiManager.reconnect()
    }

    private fun checkIfSameNetwork(config: WifiConfiguration, ssid: String, key: String) : Boolean{
        if (config.SSID.equals(quoted(ssid))) {
            /*if (config.preSharedKey != null && config.preSharedKey.equals(quoted(key))){
                selectedConfig = config
                Timber.d("Network: 1. selected config = config")
            }*/
            if (WifiConfiguration.KeyMgmt.WPA_PSK == 1 &&    //only WPA_PSK is allowed
                config.preSharedKey != null &&
                config.preSharedKey.equals(quoted(key))){
                return true
                //Timber.d("Network: 2. selected config = config")
            }
            //else if (securityType == WifiConfiguration.KeyMgmt.NONE)
            //    selectedConfig = config
        }
        return false
    }

    private fun forgetProvidedNetwork(netId: Int) :Boolean{
        return if(netId >= 0) {
            val networkDisabled = wifiManager.disableNetwork(netId)
            Timber.d("Network disabled? = $networkDisabled")
            val networkRemoved = wifiManager.removeNetwork(netId)   //this is returned
            Timber.d("Network removed? = $networkRemoved")
            networkRemoved
        } else false
    }

    fun disConnectNetwork(): Boolean{
        return wifiManager.disconnect()
    }

    fun forgetNetwork(): Boolean{
        return forgetProvidedNetwork(connectedNetworkId)
    }

    private fun toIPByteArray(addr: Int): ByteArray? {
        return byteArrayOf(
            addr.toByte(), (addr ushr 8).toByte(), (addr ushr 16).toByte(), (addr ushr 24).toByte()
        )
    }

    private fun toInetAddress(addr: Int): InetAddress? {
        return try {
            InetAddress.getByAddress(toIPByteArray(addr))
        } catch (e: UnknownHostException) {
            //should never happen
            null
        }
    }

}