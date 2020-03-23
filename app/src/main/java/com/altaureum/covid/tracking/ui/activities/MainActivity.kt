package com.altaureum.covid.tracking.ui.activities

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.altaureum.covid.tracking.MyApplication
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.common.IntentData
import com.altaureum.covid.tracking.common.Preferences
import com.altaureum.covid.tracking.ui.activities.client.ClientActivity
import com.altaureum.covid.tracking.ui.activities.server.ServerActivity
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector

import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.util.*
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector {

    val uuid = Constants.SERVICE_UUID

    @Inject
    open lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any>? {
        return fragmentDispatchingAndroidInjector
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        AndroidInjection.inject(this)


        val defaultSharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val covidId = defaultSharedPreferences.getString(Preferences.KEY_COVID_ID, null)

        if(covidId==null){
            val intentRequest = Intent(this, RegistryActivity::class.java)
            startActivityForResult(intentRequest, REQUEST_REGISTRY)
        } else{
            val intentRequest = Intent(this, ContactListActivity::class.java)
            startActivityForResult(intentRequest, REQUEST_CONTACT_LIST)
        }
        //val covidId = defaultSharedPreferences.getString(Preferences.KEY_COVID_ID, "")!!
    }



    override fun onDestroy() {
        super.onDestroy()
    }

    fun openContactList(){
        val intentRequest = Intent(this, ContactListActivity::class.java)
        startActivityForResult(intentRequest, REQUEST_CONTACT_LIST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode){
            REQUEST_REGISTRY->{
                when(resultCode){
                    Activity.RESULT_OK->{
                        //We start the server as we have an Id
                        (applicationContext as MyApplication).startServer()
                        (applicationContext as MyApplication).startClient()
                        openContactList()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)

    }

    companion object{

        val REQUEST_REGISTRY=1
        val REQUEST_CONTACT_LIST=2
        val TAG =MainActivity::class.java.simpleName
    }

}