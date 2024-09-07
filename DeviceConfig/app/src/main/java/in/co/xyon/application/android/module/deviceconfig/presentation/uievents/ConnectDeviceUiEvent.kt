package `in`.co.xyon.application.android.module.deviceconfig.presentation.uievents

sealed class ConnectDeviceUiEvent {
    data class ShowSnackbarError(val msg: String): ConnectDeviceUiEvent()
    data class ShowConfirmationDialog(val msg: String): ConnectDeviceUiEvent()
}
