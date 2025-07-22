package `in`.co.xyon.application.android.module.deviceconfig.presentation.uievents

sealed class WifiScanListUIEvent{
    data class ShowSnackbarError(val msg: String): WifiScanListUIEvent()
}
