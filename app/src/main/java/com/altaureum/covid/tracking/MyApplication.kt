package com.altaureum.covid.tracking

import android.app.Application
import android.content.Context
import com.altaureum.covid.tracking.di.AppInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.realm.Realm
import javax.inject.Inject

class MyApplication: Application(), HasAndroidInjector {

    @Inject
    lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        AppInjector.init(this)
        Realm.init(applicationContext)

    }

    override fun androidInjector(): AndroidInjector<Any>? {
        return dispatchingActivityInjector
    }

    companion object {
        private val TAG = MyApplication::class.java.simpleName
        var context: Context? = null
            private set
    }
}