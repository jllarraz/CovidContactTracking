package com.altaureum.covid.tracking.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.ui.activities.client.ClientActivity
import com.altaureum.covid.tracking.ui.activities.server.ServerActivity

import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        launch_server_button.setOnClickListener { v: View? ->
            startActivity(Intent(this@MainActivity,
                    ServerActivity::class.java))
        }
        launch_client_button.setOnClickListener { v: View? ->
            startActivity(Intent(this@MainActivity,
                    ClientActivity::class.java))
        }

        launch_server_background_button.setOnClickListener {
            try{
                val intentRequest = Intent()
                intentRequest.setPackage(this.getPackageName());
                intentRequest.action = Actions.ACTION_START_BLE_SERVER
                startService(intentRequest)
            }catch (e:Exception){
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(Actions.ACTION_BLE_SERVER_STARTED)
        intentFilter.addAction(Actions.ACTION_BLE_SERVER_STOPED)
        intentFilter.addAction(Actions.ACTION_ERROR_BLE_ADVERTISMENT_FAILED)
        intentFilter.addAction(Actions.ACTION_ERROR_BLE_ADVERTISMENT_NOT_SUPPORTED)
        intentFilter.addAction(Actions.ACTION_ERROR_BLE_NOT_SUPPORTED)
        intentFilter.addAction(Actions.ACTION_ERROR_BLE_ADVERTISMENT_SUCCESS)
        intentFilter.addAction(Actions.ACTION_REQUEST_BLE_PERMISSIONS)

        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(bleServerRegister, intentFilter)
    }

    override fun onDestroy() {
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.unregisterReceiver(bleServerRegister)
        super.onDestroy()
    }

    val bleServerRegister = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                Actions.ACTION_BLE_SERVER_STARTED->{

                }
                Actions.ACTION_BLE_SERVER_STOPED->{

                }
            }
            Toast.makeText(this@MainActivity, intent.action,Toast.LENGTH_SHORT).show()
        }
    }

}