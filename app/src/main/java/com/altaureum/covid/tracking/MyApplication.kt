package com.altaureum.covid.tracking

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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

    var isServerStarted=false
    var isClientStarted=false

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        AppInjector.init(this)
        Realm.init(applicationContext)


        val intentFilter = IntentFilter()
        intentFilter.addAction(Actions.ACTION_START_TRACKER)
        intentFilter.addAction(Actions.ACTION_STOP_TRACKER)
        intentFilter.addAction(Actions.ACTION_TRACKER_STATUS_REQUEST)
        intentFilter.addAction(Actions.ACTION_BLE_SERVER_CHECK_STATUS_RESPONSE)
        intentFilter.addAction(Actions.ACTION_BLE_SERVER_STARTED)
        intentFilter.addAction(Actions.ACTION_BLE_SERVER_STOPED)
        intentFilter.addAction(Actions.ACTION_BLE_CLIENT_STARTED)
        intentFilter.addAction(Actions.ACTION_BLE_CLIENT_STOPED)

        val localBroadcastManager = LocalBroadcastManager.getInstance(context!!.applicationContext)
        localBroadcastManager.registerReceiver(bleServerRegister, intentFilter)



        val defaultSharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val covidId = defaultSharedPreferences.getString(Preferences.KEY_COVID_ID, null)

        if(covidId!=null){
            startServer()
            startClient()
        }

    }

    val bleServerRegister = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                Actions.ACTION_START_TRACKER->{
                    startTracker()
                }
                Actions.ACTION_STOP_TRACKER->{
                    stopTracker()
                }
                Actions.ACTION_TRACKER_STATUS_REQUEST->{
                    //checkStatusServer()
                    sendResponseCheckStatus(isServerStarted && isClientStarted)
                }

                Actions.ACTION_BLE_SERVER_CHECK_STATUS_RESPONSE->{
                   // sendResponseCheckStatus(intent.getBooleanExtra(IntentData.KEY_DATA, false))
                }
                Actions.ACTION_BLE_SERVER_STARTED->{
                    isServerStarted = true
                    sendTrackerStatus(isServerStarted, isClientStarted)
                }
                Actions.ACTION_BLE_SERVER_STOPED->{
                    isServerStarted = false
                    sendTrackerStatus(isServerStarted, isClientStarted)
                }
                Actions.ACTION_BLE_CLIENT_STARTED->{
                    isClientStarted = true
                    sendTrackerStatus(isServerStarted, isClientStarted)
                }
                Actions.ACTION_BLE_CLIENT_STOPED->{
                    isClientStarted = false
                    sendTrackerStatus(isServerStarted, isClientStarted)
                }
            }

        }
    }

    override fun onTerminate() {
        stopTracker()
        val localBroadcastManager = LocalBroadcastManager.getInstance(context!!.applicationContext)
        localBroadcastManager.unregisterReceiver(bleServerRegister)

        super.onTerminate()
    }

    fun startTracker(){
        if(!isServerStarted) {
            startServer()
        }
        if(!isClientStarted) {
            startClient()
        }
        if(isClientStarted && isClientStarted){
            sendResponseTrackerStarted()
        }
    }

    fun stopTracker(){
        if(isServerStarted) {
            stopServer()
        }
        if(isClientStarted) {
            stopClient()
        }
        if(!isClientStarted && !isClientStarted){
            sendResponseTrackerStopped()
        }
    }

    private fun sendTrackerStatus(isServerStarted: Boolean, isClientStarted:Boolean){
        if(isClientStarted && isServerStarted){
            sendResponseTrackerStarted()
        } else if(!isClientStarted && !isServerStarted){
            sendResponseTrackerStopped()
        }
    }

    private fun startServer(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_START_BLE_SERVER
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e: Exception){
        }
    }

    private fun stopServer(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_STOP_BLE_SERVER
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e: Exception){
        }
    }

    private fun startClient(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_START_BLE_CLIENT
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e: Exception){
        }
    }

    private fun stopClient(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_STOP_BLE_CLIENT
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e: Exception){
        }
    }

    private fun checkStatusServer(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_BLE_SERVER_CHECK_STATUS
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e: Exception){
        }
    }
    private fun sendResponseCheckStatus(isServerStarted:Boolean){
        try {
            val localBroadcastManager = LocalBroadcastManager.getInstance(this)
            val intentRequest = Intent(Actions.ACTION_TRACKER_STATUS_RESPONSE)
            intentRequest.putExtra(IntentData.KEY_DATA, isServerStarted)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun sendResponseTrackerStarted(){
        try {
            val localBroadcastManager = LocalBroadcastManager.getInstance(this)
            val intentRequest = Intent(Actions.ACTION_TRACKER_STARTED)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun sendResponseTrackerStopped(){
        try {
            val localBroadcastManager = LocalBroadcastManager.getInstance(this)
            val intentRequest = Intent(Actions.ACTION_TRACKER_STOPPED)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
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