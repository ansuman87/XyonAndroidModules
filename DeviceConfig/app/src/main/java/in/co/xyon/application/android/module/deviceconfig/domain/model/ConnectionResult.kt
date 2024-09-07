package `in`.co.xyon.application.android.module.deviceconfig.domain.model

sealed class ConnectionResult<T>(val data: T? = null, val message: String? = null) {
    class Connected<T>(data: T) : ConnectionResult<T>(data)
    class Disconnected<T>(message: String? = null, data: T? = null) : ConnectionResult<T>(data, message)
    class Connecting<T>(data: T? = null) : ConnectionResult<T>(data)
    class ErrorConnecting<T>(message: String? = null, data: T? = null) : ConnectionResult<T>(data, message)
    //class TimeOut<T>(message: String? = null, data: T? = null) : ConnectionResult<T>(data, message)
}
