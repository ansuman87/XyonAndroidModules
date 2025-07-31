package `in`.co.xyon.application.android.module.deviceconfig.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.Manifest
import android.os.Build
import kotlinx.coroutines.flow.FlowCollector

fun <T> Fragment.collectLatestLifecycleFlow(flow: Flow<T>, action: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest(action)
        }
    }
}

//fun <T> Fragment.collectLifecycleFlow(flow: Flow<T>, action: suspend (T) -> Unit) {
//    lifecycleScope.launch {
//        repeatOnLifecycle(Lifecycle.State.STARTED) {
//            flow.collect(action)
//        }
//    }
//}

fun <T> Fragment.collectLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch { // Use viewLifecycleOwner for Fragments
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { value ->
                collect(value) // Call your provided lambda here
            }
        }
    }
}

fun getQRScanningPermissions(): List<String> {
    return listOf(Manifest.permission.CAMERA)
}

fun getWifiScanPermissions(): List<String> {
    return when{
        Build.VERSION.SDK_INT in 29..30 -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        Build.VERSION.SDK_INT >= 31 -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        else -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}

fun getNetworkConnectPermissions(): List<String> {
    return when{
        Build.VERSION.SDK_INT < 29 -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        else -> emptyList<String>()
    }
}

