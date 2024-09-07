package `in`.co.xyon.application.android.module.deviceconfig.domain.model

import `in`.co.xyon.application.android.module.deviceconfig.utils.Constants.PROVISION_MODE_BLE
import `in`.co.xyon.application.android.module.deviceconfig.utils.Constants.PROVISION_MODE_WIFI

enum class ProvisionMode(val printableName: String?) {
    WIFI(PROVISION_MODE_WIFI),
    BLUETOOTH(PROVISION_MODE_BLE),
    NONE(null)
}