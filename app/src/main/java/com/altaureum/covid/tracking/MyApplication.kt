package com.altaureum.covid.tracking

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.common.IntentData
import com.altaureum.covid.tracking.common.Preferences
import com.altaureum.covid.tracking.di.AppInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.realm.Realm
import java.lang.Exception
import javax.inject.Inject

class MyApplication: Application(), HasAndroidInjector {

    val uuid = Constants.SERVICE_UUID

    @Inject
    lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        AppInjector.init(this)
        Realm.init(applicationContext)

        val defaultSharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val covidId = defaultSharedPreferences.getString(Preferences.KEY_COVID_ID, null)

        if(covidId!=null){
            startServer()
            startClient()
        }

    }

    override fun onTerminate() {
        stopServer()
        stopClient()
        super.onTerminate()
    }

    fun startServer(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_START_BLE_SERVER
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e: Exception){
        }
    }

    fun stopServer(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_STOP_BLE_SERVER
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e: Exception){
        }
    }

    fun startClient(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_START_BLE_CLIENT
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e: Exception){
        }
    }

    fun stopClient(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_STOP_BLE_CLIENT
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e: Exception){
        }
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