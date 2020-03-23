package com.altaureum.covid.tracking.ui.activities

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
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.common.IntentData
import com.altaureum.covid.tracking.common.Preferences
import com.altaureum.covid.tracking.ui.activities.client.ClientActivity
import com.altaureum.covid.tracking.ui.activities.server.ServerActivity

import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity() {

    val uuid = Constants.SERVICE_UUID

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
            startServer()
        }
        stop_server_background_button.setOnClickListener {
            stopServer()
        }

        launch_client_background_button.setOnClickListener {
            startThreadScan()
        }

        stop_client_background_button.setOnClickListener {
            stopThreadScan()
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(Actions.ACTION_BLE_SERVER_STARTED)
        intentFilter.addAction(Actions.ACTION_BLE_SERVER_STOPED)
        intentFilter.addAction(Actions.ACTION_ERROR_BLE_ADVERTISMENT_FAILED)
        intentFilter.addAction(Actions.ACTION_ERROR_BLE_ADVERTISMENT_NOT_SUPPORTED)
        intentFilter.addAction(Actions.ACTION_ERROR_BLE_NOT_SUPPORTED)
        intentFilter.addAction(Actions.ACTION_ERROR_BLE_ADVERTISMENT_SUCCESS)
        intentFilter.addAction(Actions.ACTION_REQUEST_BLE_PERMISSIONS)
        intentFilter.addAction(Actions.ACTION_BLE_LIGHT_CLIENT_SCAN_STOPED)
        intentFilter.addAction(Actions.ACTION_BLE_LIGHT_CLIENT_SCAN_STARTED)
        intentFilter.addAction(Actions.ACTION_BLE_LIGHT_DEVICE_ADDED)
        intentFilter.addAction(Actions.ACTION_BLE_LIGHT_DEVICE_LIGHT_REMOVED)

        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(bleServerRegister, intentFilter)


        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val name = bluetoothManager.adapter.name
        val defaultSharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val edit = defaultSharedPreferences.edit()
        edit.putString(Preferences.KEY_COVID_ID, name)
        edit.commit()
        //val covidId = defaultSharedPreferences.getString(Preferences.KEY_COVID_ID, "")!!
    }

    override fun onDestroy() {
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.unregisterReceiver(bleServerRegister)

        stopServer()
        stopThreadScan()
        stopLightThreadScan()
        super.onDestroy()
    }

    val bleServerRegister = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                Actions.ACTION_BLE_SERVER_STARTED->{
                    //startThreadScan()
                }
                Actions.ACTION_BLE_SERVER_STOPED->{
                    //stopThreadScan()
                }
                Actions.ACTION_BLE_LIGHT_DEVICE_ADDED->{
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(IntentData.KEY_DATA)
                    Log.d(TAG, "Client Device MAC: "+device.address)
                    Toast.makeText(this@MainActivity, device.address,Toast.LENGTH_SHORT).show()
                }
                Actions.ACTION_BLE_SERVER_DEVICE_ADDED->{
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(IntentData.KEY_DATA)
                    Log.d(TAG, "Server Device MAC: "+device.address)
                    Toast.makeText(this@MainActivity, device.address,Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    fun startServer(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_START_BLE_SERVER
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e:Exception){
        }
    }

    fun stopServer(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_STOP_BLE_SERVER
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e:Exception){
        }
    }

    fun startLightThreadScan(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_START_SCAN_BLE_LIGHT_CLIENT
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e:Exception){
        }
    }

    fun stopLightThreadScan(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_STOP_SCAN_BLE_LIGHT_CLIENT
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e:Exception){
        }
    }

    fun startThreadScan(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_START_BLE_CLIENT
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e:Exception){
        }
    }

    fun stopThreadScan(){
        try{
            val intentRequest = Intent()
            intentRequest.setPackage(this.getPackageName());
            intentRequest.action = Actions.ACTION_STOP_BLE_CLIENT
            intentRequest.putExtra(IntentData.KEY_SERVICE_UUID, uuid.toString())
            startService(intentRequest)
        }catch (e:Exception){
        }
    }

    companion object{
        val TAG =MainActivity::class.java.simpleName
    }

}