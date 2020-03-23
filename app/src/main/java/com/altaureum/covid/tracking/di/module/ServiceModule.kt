package com.altaureum.covid.tracking.di.module

import com.altaureum.covid.tracking.services.client.BLEClientService
import dagger.Module
import dagger.android.ContributesAndroidInjector


@Module
abstract class ServiceModule {
    @ContributesAndroidInjector
    internal abstract fun contributeBLEClientService(): BLEClientService
}
