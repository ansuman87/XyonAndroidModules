package `in`.co.xyon.application.android.module.deviceconfig.domain.model

import `in`.co.xyon.application.android.module.deviceconfig.utils.Constants.DEVICE_TYPE_FAN
import `in`.co.xyon.application.android.module.deviceconfig.utils.Constants.DEVICE_TYPE_LIGHT
import `in`.co.xyon.application.android.module.deviceconfig.utils.Constants.DEVICE_TYPE_SOCKET
import `in`.co.xyon.application.android.module.deviceconfig.utils.Constants.DEVICE_TYPE_STRIP
import `in`.co.xyon.application.android.module.deviceconfig.utils.Constants.DEVICE_TYPE_UNKNOWN

enum class DeviceType(val printableName: String?) {
    LIGHT(DEVICE_TYPE_LIGHT),
    SOCKET(DEVICE_TYPE_SOCKET),
    STRIP(DEVICE_TYPE_STRIP),
    FAN(DEVICE_TYPE_FAN),
    UNKNOWN(DEVICE_TYPE_UNKNOWN),
    NONE(null)
}