package com.altaureum.covid.tracking.di.component

import android.app.Application
import com.altaureum.covid.tracking.MyApplication
import com.altaureum.covid.tracking.di.module.ActivityBindingModule
import com.altaureum.covid.tracking.di.module.AppModule
import com.altaureum.covid.tracking.di.module.ServiceModule
import com.altaureum.covid.tracking.di.module.ViewModelModule

import javax.inject.Singleton

import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule

@Singleton
@Component(modules = [AndroidSupportInjectionModule::class,
    ViewModelModule::class,
    AppModule::class,
    ActivityBindingModule::class,
    ServiceModule::class])
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }

    fun inject(application: MyApplication)
}
