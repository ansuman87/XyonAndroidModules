package `in`.co.xyon.application.android.module.deviceconfig.presentation.states

enum class ConnectDeviceUiState{
    //TODO: add additional states like HAS_SCANNED, HAS_DOWNLOADED (may be ERROR_DOWNLOADING, ERROR_SCANNING as well)
    // probably not...they won't be mutually exclusive states
    IS_DOWNLOADING,
    IS_SCANNING,
    IS_CONNECTING,
    IS_CONNECTED,
    HAS_DISCONNECTED,
    SHOW_WIFI_BLE_DIALOG,
    SHOW_DOWNLOADING_FAILED_DIALOG,
    /*ESP_IS_CONNECTING,
    ESP_IS_CONNECTED,
    ESP_HAS_DISCONNECTED,
    ESP_ERROR_CONNECTING,*/
    IDLE_STATE
}
