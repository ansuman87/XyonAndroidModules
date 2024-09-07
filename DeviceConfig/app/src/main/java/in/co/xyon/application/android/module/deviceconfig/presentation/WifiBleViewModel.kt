package `in`.co.xyon.application.android.module.deviceconfig.presentation

import `in`.co.xyon.application.android.module.deviceconfig.domain.model.ProvisionMode
import `in`.co.xyon.application.android.module.deviceconfig.domain.usecase.GetQRScanResult
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WifiBleViewModel @Inject constructor(
    private val getQRScanResult: GetQRScanResult,
    application: Application
) : AndroidViewModel(application){

    private val _retrievedPopStateFlow = MutableStateFlow("")
    val retrievedPopStateFlow = _retrievedPopStateFlow.asStateFlow()

    private val _provisionModeStateFlow = MutableStateFlow(ProvisionMode.NONE)
    val provisionModeStateFlow = _provisionModeStateFlow.asStateFlow()


}