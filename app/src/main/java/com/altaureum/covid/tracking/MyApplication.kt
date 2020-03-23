package com.altaureum.covid.tracking

import android.app.Application
import android.content.Context
import io.realm.Realm

class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        Realm.init(applicationContext)

    }

    companion object {
        private val TAG = MyApplication::class.java.simpleName
        var context: Context? = null
            private set
    }
}