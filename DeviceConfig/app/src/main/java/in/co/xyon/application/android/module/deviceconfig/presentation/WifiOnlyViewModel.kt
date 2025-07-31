package `in`.co.xyon.application.android.module.deviceconfig.presentation

import `in`.co.xyon.application.android.module.deviceconfig.domain.model.*
import `in`.co.xyon.application.android.module.deviceconfig.presentation.states.*
import `in`.co.xyon.application.android.module.deviceconfig.presentation.uievents.ConnectDeviceUiEvent
import `in`.co.xyon.application.android.module.deviceconfig.presentation.uievents.WifiScanListUIEvent
import `in`.co.xyon.application.android.module.deviceconfig.domain.usecase.*
import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.espressif.provisioning.DeviceConnectionEvent
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPConstants.SecurityType
//import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.WiFiAccessPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.co.xyon.application.android.module.deviceconfig.domain.lib.ESPProvisionManagerMod
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class WifiOnlyViewModel @Inject constructor(
    private val scanWifi: ScanWifi,
    private val connectToNetwork: ConnectToNetwork,
    private val getQRScanResult: GetQRScanResult,
    private val getNetworkListFromDevice: GetNetworkListFromDevice,
    private val doProvisioning: DoProvisioning,
    //private val startProvisioning: StartHandshaking,
    //private val provisioningHandler: ProvisioningHandler,
    application: Application
) : AndroidViewModel(application) {

    private val selectedSecurityType: SecurityType = SecurityType.SECURITY_2

    /********* Related to DeviceTypeSelectionFragment*******/
    private val _selectedDeviceType = mutableStateOf<DeviceType>(DeviceType.NONE)
    val selectedDeviceType: State<DeviceType> = _selectedDeviceType

    private val _deviceTypeSelectionStateFlow = MutableStateFlow(DeviceTypeSelectionState(isDataTypeSelected = false))
    val deviceTypeSelectionStateFlow: StateFlow<DeviceTypeSelectionState> = _deviceTypeSelectionStateFlow

    fun setSelectedDeviceType(deviceType: DeviceType){
        _selectedDeviceType.value = deviceType
        _deviceTypeSelectionStateFlow.value = DeviceTypeSelectionState(isDataTypeSelected = true)
        Timber.d("selected deviceType: ${deviceType.printableName}")
    }

    /************* related to retrieving PoP from the server for the device, then scan wifi and connect to device *************
     **************************************************************************************************************************/
    private val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager = application.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _retrievedPopStateFlow = MutableStateFlow("")  // TODO: probably, replace by normal variable
    val retrievedPopStateFlow = _retrievedPopStateFlow.asStateFlow()

    private val _retrievedUnameStateFlow = MutableStateFlow("")   // TODO: probably, replace by normal variable
    val retrievedUnameStateFlow = _retrievedUnameStateFlow.asStateFlow()

    private val _provisionModeStateFlow = MutableStateFlow(ProvisionMode.NONE)
    val provisionModeStateFlow = _provisionModeStateFlow.asStateFlow()

    private val _retrievedSsidStateFlow = MutableStateFlow("")
    val retrievedSsidStateFlow = _retrievedSsidStateFlow.asStateFlow()

    private val _connectedDeviceSsidStateFlow = MutableStateFlow("")
    val connectedDeviceSsidStateFlow = _connectedDeviceSsidStateFlow.asStateFlow()

    private val _toBeConnectedDeviceSsidStateFlow = MutableStateFlow("")
    val toBeConnectedDeviceSsidStateFlow = _toBeConnectedDeviceSsidStateFlow.asStateFlow()

    /***** UI State flows  *****/
    private val _connectedDeviceUiStateFlow = MutableStateFlow(ConnectDeviceUiState.IDLE_STATE)
    val connectedDeviceUiStateFlow = _connectedDeviceUiStateFlow.asStateFlow()

    /***** UI Event flows *****/
    private val _connectedDeviceUiEventFlow = MutableSharedFlow<ConnectDeviceUiEvent>()
    val connectedDeviceUiEventFlow = _connectedDeviceUiEventFlow.asSharedFlow()

    fun resetConnectedDeviceFrag(){
        _retrievedPopStateFlow.value = ""
        _retrievedSsidStateFlow.value = ""
        _retrievedUnameStateFlow.value = ""
        _connectedDeviceSsidStateFlow.value = ""
        _toBeConnectedDeviceSsidStateFlow.value = ""
        _provisionModeStateFlow.value = ProvisionMode.NONE
        _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.IDLE_STATE
        _espHandshakeStateFlow.value = EspHandshakeState.NONE
        _listDevCaps.clear()
        //TODO:cancel all jobs
        cancelScanAndGetPopJob()
        cancelScanWifiToFindDeviceJob()
        cancelConnectDeviceJob()
        disconnectEspDevice()
        unregisterEventBus()
    }

    fun resetRetrievedPopFlow(){
        _retrievedPopStateFlow.value = ""
    }

    fun updateProvisionModeState(mode: ProvisionMode){
        _provisionModeStateFlow.value = mode
    }

    fun resetConnectDeviceUiState(){
        _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.IDLE_STATE
    }

    fun updateConnectDeviceUiState(uiState: ConnectDeviceUiState) {
        _connectedDeviceUiStateFlow.value = uiState
    }

    /*fun updateState(showDialog: Boolean){
       _getPopStateFlow.update { it.copy(showWifiBleDialog = showDialog)}
    }*/

    private var scanAndGetPopJob: Job? = null

    fun scanAndGetPop() {
        //_getPopStateFlow.value = GetPopState(isLoading = true)
        cancelScanAndGetPopJob()
        scanAndGetPopJob = viewModelScope.launch(Dispatchers.IO) {
            //running on UI Thread
            val url = scanQRCode()

            withContext(Dispatchers.IO){
                getQRScanResult(url).collect { result ->
                    when(result) {
                        is RequestResult.Loading ->{
                            //_getPopStateFlow.update { it.copy(isLoading = true, showWifiBleDialog = false) }
                            //_getPopStateFlow.value = GetPopState(isLoading = true)
                            _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.IS_DOWNLOADING
                        }
                        is RequestResult.Error -> {
                            //_getPopStateFlow.update { it.copy(isLoading =  false, showWifiBleDialog = false) }
                            //_getPopStateFlow.value = GetPopState(isLoading = false)
                            _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.SHOW_DOWNLOADING_FAILED_DIALOG
                            _connectedDeviceUiEventFlow.emit(
                                ConnectDeviceUiEvent.ShowSnackbarError(
                                "Error scanning: ${result.message}"
                            ))
                            Timber.e("scanAndGetPop: Error: device ssid with pop and username retrieval")
                        }
                        is RequestResult.Success -> {
                            if(result.data == null){
                                //_getPopStateFlow.update { it.copy(isLoading =  false, showWifiBleDialog = false) }
                                _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.SHOW_DOWNLOADING_FAILED_DIALOG
                                _connectedDeviceUiEventFlow.emit(
                                    ConnectDeviceUiEvent.ShowSnackbarError(
                                    "Scan returned null/empty value: ${result.message}"
                                ))
                                Timber.e("scanAndGetPop: Error: device ssid with pop and username retrieved null values")
                            }else{
                                if(result.data.ssid.isEmpty() || result.data.pop.isEmpty()){  //resource.data.ssid.isEmpty() ||
                                        //_getPopStateFlow.update { it.copy(isLoading =  false, showWifiBleDialog = false) }
                                    _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.SHOW_DOWNLOADING_FAILED_DIALOG
                                    _connectedDeviceUiEventFlow.emit(
                                        ConnectDeviceUiEvent.ShowSnackbarError(
                                            "Scan returned some null/empty values: ${result.message}"
                                        ))
                                    if(selectedSecurityType == SecurityType.SECURITY_2 && result.data.username.isEmpty()) {
                                        _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.SHOW_DOWNLOADING_FAILED_DIALOG
                                        _connectedDeviceUiEventFlow.emit(
                                            ConnectDeviceUiEvent.ShowSnackbarError(
                                                "Scan returned some null/empty values: ${result.message}"
                                            ))
                                    }
                                    Timber.e("scanAndGetPop: Error: device ssid with pop and username retrieved null values")
                                } else {
                                    _retrievedPopStateFlow.value = result.data.pop
                                    _retrievedSsidStateFlow.value = result.data.ssid
                                    if (selectedSecurityType == SecurityType.SECURITY_2)
                                        _retrievedUnameStateFlow.value = result.data.username
                                    _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.SHOW_WIFI_BLE_DIALOG
                                    Timber.d("scanAndGetPop: device ssid with pop and username retrieved")
                                    Timber.d("scanAndGetPop: device: ssid=${result.data.ssid}, pop=${result.data.pop}, username=${result.data.username}")
                                }
                            }

                        }
                    }
                }
            }

        }
    }

    private suspend fun scanQRCode(): String{
        return "random url"
    }

    private fun cancelScanAndGetPopJob(){
        scanAndGetPopJob?.cancel()
    }

    fun getWifiEnabledStatus(): Boolean{
        return wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED
    }

    fun toggleWifiStateOn(): Boolean{
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (wifiManager.wifiState == WifiManager.WIFI_STATE_DISABLED)
                wifiManager.setWifiEnabled(true)
            else true
        } else false
    }

    fun getLocationEnabledStatus(): Boolean {
        var gpsEnabled = false
        var networkEnabled = false
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        /*new AlertDialog.Builder(MainActivity. this )
                        .setMessage( "GPS Enable" )
                        .setPositiveButton( "Settings" , new
                                DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                        startActivity( new Intent(Settings. ACTION_LOCATION_SOURCE_SETTINGS )) ;
                                    }
                                })
                        .setNegativeButton( "Cancel" , null )
                        .show() ;*/
        return gpsEnabled || networkEnabled
    }


    private var scanWifiToFindDeviceJob: Job? = null

    @ExperimentalCoroutinesApi
    fun scanWifiToFindDevice(){
        cancelScanWifiToFindDeviceJob()
        scanWifiToFindDeviceJob = viewModelScope.launch(Dispatchers.Default) {
            scanWifi()
                .buffer()
                .cancellable()
                .collect { result ->
                    //Timber.d("scan receive: currentThread: ${Thread.currentThread()}")
                    when (result) {
                        is RequestResult.Success -> {
                            _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.IDLE_STATE
                            processWifiScanResult(result.data ?: emptyList())
                            Timber.d("${result.data?.size}")
                            //this.coroutineContext.job.cancel()
                            cancelScanWifiToFindDeviceJob()
                        }
                        is RequestResult.Error -> {
                            _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.IDLE_STATE
                            processWifiScanResult(result.data ?: emptyList())
                            Timber.d("${result.message}")
                            Timber.d("${result.data?.size}")
                            //this.coroutineContext.job.cancel()
                            cancelScanWifiToFindDeviceJob()
                        }
                        is RequestResult.Loading -> {
                            _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.IS_SCANNING
                            Timber.d("loading....")
                        }
                    }
                }
                //.launchIn(this)
        }
    }

    private fun cancelScanWifiToFindDeviceJob() {
        scanWifiToFindDeviceJob?.cancel()
    }

    private suspend fun processWifiScanResult(listScanResult: List<ScanResult>) {
        /*
        val deviceType = selectedDeviceType.value
        if (deviceType == DeviceType.NONE) return

        val matchingPattern1 = if(deviceType == DeviceType.UNKNOWN){
            ""
        }else deviceType.printableName!!

        val matchingPattern2 = "XYON"

        var deviceNetwork: ScanResult? = null
        var count = 0
        for (scanResult in listScanResult) {
            Timber.d("Scanned SSID: ${scanResult.SSID?:""}")
            if (scanResult.SSID == null) continue
            if (scanResult.SSID.contains(matchingPattern1, true)
                && scanResult.SSID.contains(matchingPattern2, true)){
                count++
                deviceNetwork = scanResult
            }
        }
        if(count > 1) {
            _uiEventFlow.emit(ConnectDeviceUIEvent.ShowSnackbarError(
                //msg = getApplication<Application>().resources.getString(R.string.some_string)
                msg = "Multiple Networks of the same device type found. Forget all networks with prefix XYON_ on the WiFi System Settings"
            ))
            return
        }
        if(deviceNetwork == null) {
            _uiEventFlow.emit(ConnectDeviceUIEvent.ShowSnackbarError(
                //msg = getApplication<Application>().resources.getString(R.string.some_string)
                msg = "Network not found: Try again or connect manually on Wifi Settings of the device"
            ))
            return
        }
        Timber.d("Found network: ${deviceNetwork.SSID!!}")
        _retrievedSsidStateFlow.value = deviceNetwork.SSID!!
        */

        var deviceNetwork: ScanResult? = null
        var count = 0
        for (scanResult in listScanResult) {
            Timber.d("Scanned SSID: ${scanResult.SSID?:""} and security: ${scanResult.capabilities}")
            if (scanResult.SSID == null) continue
            if (scanResult.SSID == retrievedSsidStateFlow.value){
                count++
                deviceNetwork = scanResult
            }
        }
        if(count > 1) {
            _connectedDeviceUiEventFlow.emit(ConnectDeviceUiEvent.ShowSnackbarError(
                //msg = getApplication<Application>().resources.getString(R.string.some_string)
                msg = "Multiple Networks of the same device type found. Remove duplicate entries on the WiFi System Settings"
            ))
            return
        }
        if(deviceNetwork == null) {
            _connectedDeviceUiEventFlow.emit(ConnectDeviceUiEvent.ShowSnackbarError(
                //msg = getApplication<Application>().resources.getString(R.string.some_string)
                msg = "Network not found: Try again or connect manually on Wifi Settings of the device"
            ))
            return
        }
        _toBeConnectedDeviceSsidStateFlow.value = deviceNetwork.SSID!!

    }

    private var connectDeviceJob: Job? = null
    private var countTimeOuts = 0

    @ExperimentalCoroutinesApi
    fun connectDevice(ssid: String) {
        cancelConnectDeviceJob()

        //TODO: set a timeout and cancel the job otherwise....also emit a UI event to show snackbar

        var startTime = System.nanoTime()  //just initialization
        Timber.d("NetConn: startTime noted...")
        var elapsedTime : Long
        var isConnected : Boolean = false

        connectDeviceJob = viewModelScope.launch(Dispatchers.Default) {
            connectToNetwork(ssid, "")
                .buffer()
                .cancellable()
                .onEach { result ->
                    when(result){
                        is ConnectionResult.Connected ->{
                            isConnected = true
                            _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.IS_CONNECTED
                            _connectedDeviceSsidStateFlow.value = result.data?:""
                            Timber.d("Connected. SSID: ${result.data}")
                        }
                        is ConnectionResult.Disconnected -> {
                            isConnected = false
                            _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.HAS_DISCONNECTED
                            Timber.d("Disconnected.")
                        }
                        is ConnectionResult.Connecting -> {
                            isConnected = false
                            _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.IS_CONNECTING
                            startTime = System.nanoTime()
                        }
                        is ConnectionResult.ErrorConnecting -> {
                            isConnected = false
                            Timber.d("Error Connecting - ${result.message}")
                            _connectedDeviceUiStateFlow.value = ConnectDeviceUiState.HAS_DISCONNECTED
                           // cancelConnectDeviceJob()
                        }
                    }
                }.launchIn(this)

            /*while (true){
                elapsedTime = System.nanoTime() - startTime
                Timber.d("NetConn: elapsed time = ${elapsedTime/1_000_000} ms")
                if (isConnected) return@launch
                if (elapsedTime/1_000_000 >= CONNECTION_TIME_OUT_INTERVAL && !isConnected) {
                    Timber.d("NetConn: Connection Timeout -- Try again...")
                    countTimeOuts+=1
                    cancelConnectDeviceJob()
                    if(countTimeOuts > 1)
                        _getPopEventFlow.emit(GetPopUIEvent.ShowConfirmationDialog(
                            msg = "Confirm to try again. Or else connect manually on Wifi System Settings."
                        ))
                    else if (countTimeOuts == 1)
                        _getPopEventFlow.emit(GetPopUIEvent.ShowConfirmationDialog(
                            msg = "Confirm if already connected. If not, try Again?"
                        ))
                    //forgetNetwork()
                    return@launch
                }
            }*/
        }

    }

    private fun cancelConnectDeviceJob() {
        connectDeviceJob?.cancel()
    }

    /********************* handshake with esp device after getting connected to the device **************
     ****************************************************************************************************/

    /*private val provisionManager = ESPProvisionManager.getInstance(application)

    fun createEspDevice(){
        when(provisionModeStateFlow.value){
            ProvisionMode.WIFI -> {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, ESPConstants.SecurityType.SECURITY_1)
                provisionManager.espDevice.proofOfPossession = _retrievedPopStateFlow.value
                provisionManager.espDevice.deviceName = _connectedDeviceSsidStateFlow.value
            }
            ProvisionMode.BLUETOOTH -> {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_1)
            }
            ProvisionMode.NONE -> {
                //do nothing
            }
        }
    }*/

    private val provisionManager = ESPProvisionManagerMod.getInstance(application)

    private val _espHandshakeStateFlow = MutableStateFlow<EspHandshakeState>(EspHandshakeState.NONE)
    val espHandshakeStateFlow = _espHandshakeStateFlow.asStateFlow()

    private val _listDevCaps = mutableListOf<String>()
    val listDevCaps get() = _listDevCaps

    fun createEspDevice(provisionMode: ProvisionMode){
        when(provisionMode){
            ProvisionMode.WIFI -> {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, selectedSecurityType)
                provisionManager.espDevice.proofOfPossession = retrievedPopStateFlow.value
                provisionManager.espDevice.deviceName = connectedDeviceSsidStateFlow.value
                if (selectedSecurityType == SecurityType.SECURITY_2)
                    provisionManager.espDevice.userName = retrievedUnameStateFlow.value
            }
            ProvisionMode.BLUETOOTH -> {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, selectedSecurityType)
                provisionManager.espDevice.proofOfPossession = _retrievedPopStateFlow.value
                provisionManager.espDevice.deviceName = _connectedDeviceSsidStateFlow.value
                if (selectedSecurityType == SecurityType.SECURITY_2)
                    provisionManager.espDevice.userName = retrievedUnameStateFlow.value
            }
            ProvisionMode.NONE -> {
                //do nothing
            }
        }
    }

    private fun registerEventBus():Boolean{
        /*if(thisContext == null) {
            Timber.e("Error: no context provided to ProvisionHandler...")
            return
        }*/
        //if(isEventBusRegistered) return
        if (EventBus.getDefault().isRegistered(this)) return true
        //else Timber.d("registering eventbus...")
        /*isEventBusRegistered = try {
            //EventBus.getDefault().register(thisContext)
            EventBus.getDefault().register(this)
            true
        }catch (e: Exception){
            Timber.d("unable to register eventBus...")
            false
        }*/
        return try {
            EventBus.getDefault().register(this)
            true
        }catch (e: Exception) {
            Timber.e(e, "unable to register eventBus...")
            false
        }
        //Timber.d("eventBus is registered: ${EventBus.getDefault().isRegistered(this)}")
    }

    private fun unregisterEventBus(){
        /*if(thisContext == null) {
            Timber.e("Error: no context provided to ProvisionHandler...")
            return
        }*/
        //if(!isEventBusRegistered) return
        if (!EventBus.getDefault().isRegistered(this)) return

        /*isEventBusRegistered = try {
            //EventBus.getDefault().unregister(thisContext)
            EventBus.getDefault().unregister(this)
            //removeContextFromProvisionHandler()
            false
        }catch (e: Exception){
            Timber.d("unable to unregister eventBus...")
            true
        }*/
        try {
            //EventBus.getDefault().unregister(thisContext)
            EventBus.getDefault().unregister(this)
        }catch (e: Exception) {
            Timber.e(e, "unable to unregister eventBus...")
        }
        Timber.d("Eventbus is unregistered?: ${!EventBus.getDefault().isRegistered(this)} ")
    }

    fun startDeviceHandshake(){
        if(registerEventBus()) {
            provisionManager.espDevice.connectWiFiDevice()
            _espHandshakeStateFlow.value = EspHandshakeState.SENDING
        }
    }

    /*private var startHandshakingJob: Job? = null

    @ExperimentalCoroutinesApi
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startHandshake() {
        cancelStartHandshakingJob()
        startHandshakingJob = viewModelScope.launch(Dispatchers.Default){
            startProvisioning()
                .buffer()
                .collect {
                    when(it){
                        is ConnectionResult.Connecting ->{
                            _espHandshakeStateFlow.value = EspHandshakeState.SENDING
                            Timber.d("Handshake sending...")
                        }
                        is ConnectionResult.Connected -> {
                            _listDevCaps.clear()
                            it.data?.let { it1 -> _listDevCaps.addAll(it1) }
                            _espHandshakeStateFlow.value = EspHandshakeState.SUCCESSFUL
                            Timber.d("Handshake successful...")
                            Timber.d("esp device capabilities: $_listDevCaps")
                        }
                        is ConnectionResult.Disconnected -> {
                            //never called.....redundant
                            Timber.d("Handshake device disconnected...")
                        }
                        is ConnectionResult.ErrorConnecting -> {
                            _espHandshakeStateFlow.value = EspHandshakeState.ERROR
                            Timber.d("Error sending Handshake...")
                        }
                    }
                }
        }
    }*/

    /*fun cancelStartHandshakingJob() {
        startHandshakingJob?.cancel()
    }*/

    /*fun unregisterProvisioningEventBus(){
        provisioningHandler.unregisterEventBus()
    }

    fun registerProvisioningEventBus() {
        provisioningHandler.registerEventBus()
    }*/

    /** Should be called while going back to home fragment from any
     * downstream fragment once the device has established handshake **/
    private fun disconnectEspDevice(){
        try {
            provisionManager.espDevice.disconnectDevice()
        }catch (e: Exception){
            Timber.d("possibly not connected to the ESP device")
        }
    }

    fun getDeviceCapabilities(): List<String> {
        return listDevCaps
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(event: DeviceConnectionEvent) {
        //trySend(ConnectionResult.Connecting())
        when(event.eventType){
            ESPConstants.EVENT_DEVICE_CONNECTED ->{
                Timber.d("ESP Device Connected...")
                val deviceCaps = provisionManager.espDevice.deviceCapabilities ?: emptyList<String>()
                _listDevCaps.clear()
                _listDevCaps.addAll(deviceCaps)
                Timber.d("deviceCaps: $deviceCaps")
                _espHandshakeStateFlow.value = EspHandshakeState.SUCCESSFUL
                //unregisterEventBus()
            }
            ESPConstants.EVENT_DEVICE_DISCONNECTED ->{
                Timber.d("ESP Device Disconnected...")
                _espHandshakeStateFlow.value = EspHandshakeState.ERROR
                _wifiScanListUiStateFlow.value = WifiScanListUiState.DEVICE_DISCONNECTED
                _wifiConfigUiStateFlow.value = WifiConfigUiState.DEVICE_DISCONNECTED
                _provisioningUiStateFlow.update { it.copy(showDisconnectAlertDialog = true) }
                //unregisterEventBus()
            }
            ESPConstants.EVENT_DEVICE_CONNECTION_FAILED ->{
                Timber.d("ESP Device error - connection failed..")
                _espHandshakeStateFlow.value = EspHandshakeState.ERROR
                //unregisterEventBus()
            }
        }
    }

    /************** start of provisioning -- getting list of wifi networks to be provisioned   ************/

    private val _listScanDeviceWifi = mutableListOf<WiFiAccessPoint>()
    val listWifiAp get() = _listScanDeviceWifi

    private var _selectedNetwork: ProvisionNetwork ?= null
    private val selectedNetwork get() = _selectedNetwork

    private val _wifiScanListUiStateFlow = MutableStateFlow(WifiScanListUiState.IDLE_STATE)
    val wifiScanListUiStateFlow = _wifiScanListUiStateFlow.asStateFlow()

    private val _wifiScanListUiEventFlow = MutableSharedFlow<WifiScanListUIEvent>()
    val wifiScanListUIEvent = _wifiScanListUiEventFlow.asSharedFlow()

    fun updateWifiScanListUiState(state : WifiScanListUiState){
        _wifiScanListUiStateFlow.value = state
    }

    fun setSelectedNetwork(network: ProvisionNetwork){
        _selectedNetwork = network
    }

    fun resetScanListFragment(){
        _wifiScanListUiStateFlow.value = WifiScanListUiState.IDLE_STATE
        _wifiConfigUiStateFlow.value = WifiConfigUiState.IDLE_STATE
        _selectedNetwork = null
        _listScanDeviceWifi.clear()
        cancelScanWifiApsJob()
        disconnectEspDevice()
        unregisterEventBus()
    }

    private var scanWifiApsJob: Job? = null

    @ExperimentalCoroutinesApi
    fun getWifiApsForDevice(){
        cancelScanWifiApsJob()
        _listScanDeviceWifi.clear()
        _wifiScanListUiStateFlow.value = WifiScanListUiState.LIST_LOADING
        Timber.d("scanDeviceWifi about to start...")

        scanWifiApsJob = viewModelScope.launch(Dispatchers.IO) {
            val timedJob = withTimeoutOrNull(15000){
                getNetworkListFromDevice()
                    .apply {
                        when(this) {
                            is RequestResult.Success -> {
                                _listScanDeviceWifi.clear()
                                //_listScanDeviceWifi = mutableListOf<WiFiAccessPoint>()
                                this.data?.let { _listScanDeviceWifi.addAll(it) }
                                _wifiScanListUiStateFlow.value = WifiScanListUiState.LIST_FOUND
                                Timber.d("scanDeviceWifi successful...")
                                cancelScanWifiApsJob()
                            }
                            is RequestResult.Error -> {
                                _listScanDeviceWifi.clear()
                                _wifiScanListUiStateFlow.value = WifiScanListUiState.LIST_FOUND_ERROR
                                _wifiScanListUiEventFlow.emit(
                                    WifiScanListUIEvent.ShowSnackbarError(this.message?:"Unknown error scanning network")
                                )
                                Timber.d("scanDeviceWfifi error...")
                                cancelScanWifiApsJob()
                            }
                            is RequestResult.Loading -> {
                                //this should never be invoked
                            }
                        }
                    }
            }
            if (timedJob == null)
                _wifiScanListUiEventFlow.emit(
                    WifiScanListUIEvent.ShowSnackbarError("Scan timed out. Please try again...")
                )
        }
    }

    private fun cancelScanWifiApsJob() {
        scanWifiApsJob?.cancel()
    }

    /************** start of provisioning -- not able to get list of wifi networks to be provisioned   ************/

    private val _wifiConfigUiStateFlow = MutableStateFlow(WifiConfigUiState.IDLE_STATE)
    val wifiConfigUiStateFlow = _wifiConfigUiStateFlow.asStateFlow()

    /****************** provisioning fragment **********************/

    private val _provisioningUiStateFlow = MutableStateFlow<ProvisioningState>(ProvisioningState())
    val provisioningUiStateFlow = _provisioningUiStateFlow.asStateFlow()

    private var _provisionFailureReason: ESPConstants.ProvisionFailureReason ?= null
    val provisionFailureReason get() = _provisionFailureReason

    private var doProvisioningJob: Job ?= null

    private var _isProvisioningSuccessful = false
    val isProvisioningSuccessful get() = _isProvisioningSuccessful

    fun isProvisioningUnderWay():Boolean = doProvisioningJob?.isActive?:false

    fun resetProvisioningFragment(){
        _provisioningUiStateFlow.value = ProvisioningState()
        _provisionFailureReason = null
        _isProvisioningSuccessful = false
        cancelDoProvisioningJob()
        disconnectEspDevice()
        unregisterEventBus()
    }

    fun resetProvisioningFragmentStateOnly() {
        _provisioningUiStateFlow.value = ProvisioningState()
        _provisionFailureReason = null
        _isProvisioningSuccessful = false
        cancelDoProvisioningJob()
    }

    @ExperimentalCoroutinesApi
    fun startProvisioning() {
        cancelDoProvisioningJob()
        if (selectedNetwork == null) return
        if (selectedNetwork!!.ssid.isBlank()) return
        if (selectedNetwork!!.pwd.isNullOrBlank())
            setSelectedNetwork(ProvisionNetwork(selectedNetwork!!.ssid, ""))
        Timber.d("selected Network ssid: ${selectedNetwork!!.ssid},& pwd: ${selectedNetwork!!.pwd}")
        _provisioningUiStateFlow.update { it.copy(createSession = ProvisioningStateTracker.UNDERWAY,
                                                      sendWifiConfig = ProvisioningStateTracker.UNDERWAY) }
        doProvisioningJob = viewModelScope.launch(Dispatchers.IO) {
            doProvisioning(selectedNetwork!!.ssid, selectedNetwork!!.pwd!!)
                .buffer()
                .cancellable()
                .onEach { result ->
                    Timber.d("result: $result")
                    when(result){
                        is ProvisionResult.CreateSessionFailed -> {
                            Timber.e(result.e, "error creating session!")
                            _provisioningUiStateFlow
                                .update { it.copy(createSession = ProvisioningStateTracker.FAILURE) }
                            cancelDoProvisioningJob()
                        }
                        is ProvisionResult.WifiConfigSent -> {
                            Timber.d("wifiConfig sent!")
                            _provisioningUiStateFlow
                                .update { it.copy(createSession = ProvisioningStateTracker.SUCCESS,
                                    sendWifiConfig = ProvisioningStateTracker.SUCCESS,
                                    wifiConfigApplied = ProvisioningStateTracker.UNDERWAY ) }
                        }
                        is ProvisionResult.WifiConfigFailed -> {
                            Timber.e(result.e, "error sending wifi-config!")
                            _provisioningUiStateFlow
                                .update { it.copy(createSession = ProvisioningStateTracker.SUCCESS,
                                    sendWifiConfig = ProvisioningStateTracker.FAILURE,
                                    wifiConfigApplied = ProvisioningStateTracker.INIT) }  //last one not required, probably redundant
                            cancelDoProvisioningJob()
                        }
                        is ProvisionResult.WifiConfigApplied -> {
                            Timber.d( "wifiConfig applied!")
                            _provisioningUiStateFlow
                                .update { it.copy(sendWifiConfig = ProvisioningStateTracker.SUCCESS,   // probably redundant
                                    wifiConfigApplied = ProvisioningStateTracker.SUCCESS,
                                    provisioningStatus = ProvisioningStateTracker.UNDERWAY) }
                        }
                        is ProvisionResult.WifiConfigApplyFailed -> {
                            Timber.e(result.e, "error applying wifi-config!")
                            _provisioningUiStateFlow
                                .update { it.copy(sendWifiConfig = ProvisioningStateTracker.SUCCESS,   // probably redundant
                                    wifiConfigApplied = ProvisioningStateTracker.FAILURE,
                                    provisioningStatus = ProvisioningStateTracker.INIT ) }  // probably redundant
                            cancelDoProvisioningJob()
                        }
                        is ProvisionResult.DeviceProvisioningSuccess -> {
                            Timber.d("provisioning success!")
                            _provisioningUiStateFlow
                                .update { it.copy(wifiConfigApplied = ProvisioningStateTracker.SUCCESS,  //redundant probably
                                    provisioningStatus = ProvisioningStateTracker.SUCCESS) }
                            _isProvisioningSuccessful = true
                            //cancelDoProvisioningJob()
                        }
                        is ProvisionResult.ProvisioningFailedFromDevice -> {
                            Timber.e(result.e, "provisioning failed from device side!")
                            _provisioningUiStateFlow
                                .update { it.copy(wifiConfigApplied = ProvisioningStateTracker.SUCCESS,  //redundant probably
                                    provisioningStatus = ProvisioningStateTracker.FAILURE) }

                            when (result.data) {
                                ESPConstants.ProvisionFailureReason.AUTH_FAILED -> {
                                    _provisionFailureReason = ESPConstants.ProvisionFailureReason.AUTH_FAILED
                                }
                                ESPConstants.ProvisionFailureReason.NETWORK_NOT_FOUND -> {
                                    _provisionFailureReason = ESPConstants.ProvisionFailureReason.NETWORK_NOT_FOUND
                                }
                                ESPConstants.ProvisionFailureReason.DEVICE_DISCONNECTED -> {
                                    _provisionFailureReason = ESPConstants.ProvisionFailureReason.DEVICE_DISCONNECTED
                                }
                                ESPConstants.ProvisionFailureReason.UNKNOWN -> {
                                    _provisionFailureReason = ESPConstants.ProvisionFailureReason.UNKNOWN
                                }
                                else -> {}
                            }
                            cancelDoProvisioningJob()
                        }
                        is ProvisionResult.OnProvisioningFailed -> {
                            Timber.e(result.e, "provisioning failed!")
                            _provisioningUiStateFlow
                                .update { it.copy(wifiConfigApplied = ProvisioningStateTracker.SUCCESS,  //redundant probably
                                    provisioningStatus = ProvisioningStateTracker.FAILURE) }
                            cancelDoProvisioningJob()
                        }
                    }
                }.launchIn(this)
        }
    }


    private fun cancelDoProvisioningJob() {
        doProvisioningJob?.cancel()
    }


    fun endSessionAndCloseConnection() {
        provisionManager.espDevice.closeSessionAndDisableWifiNetwork()
    }


}