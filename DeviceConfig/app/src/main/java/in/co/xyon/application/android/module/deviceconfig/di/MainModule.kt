package `in`.co.xyon.application.android.module.deviceconfig.di

import `in`.co.xyon.application.android.module.deviceconfig.domain.network.ProvisioningHandler
import `in`.co.xyon.application.android.module.deviceconfig.domain.usecase.GetQRScanResult
import `in`.co.xyon.application.android.module.deviceconfig.domain.network.WifiConnectorHandler
import `in`.co.xyon.application.android.module.deviceconfig.domain.network.WifiScanHandler
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Provides
    @Singleton
    fun providesGetPopUseCase(): GetQRScanResult {
        return GetQRScanResult()
    }

    @Provides
    @Singleton
    fun providesWifiScanHandler(@ApplicationContext appContext: Context) : WifiScanHandler {
        return WifiScanHandler(appContext)
    }

    @Provides
    @Singleton
    fun providesWifiConnectorHandler(@ApplicationContext appContext: Context) : WifiConnectorHandler {
        return WifiConnectorHandler(appContext)
    }

    @Provides
    @Singleton
    fun providesProvisioningHandler(@ApplicationContext appContext: Context) : ProvisioningHandler {
        return ProvisioningHandler(appContext)
    }
}