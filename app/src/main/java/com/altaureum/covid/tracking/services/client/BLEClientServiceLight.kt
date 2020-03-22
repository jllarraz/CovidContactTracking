package com.altaureum.covid.tracking.services.client

import android.Manifest
import android.app.IntentService
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.common.IntentData
import java.util.*

class BLEClientServiceLight: IntentService(BLEClientServiceLight::class.java.simpleName) {

    private var mScanning = false
    private var mHandler: Handler? = null
    private var mScanResults: MutableMap<String, BluetoothDevice>? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mScanCallback: ScanCallback? = null
    //private var mGatt: BluetoothGatt? = null
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var serviceUUID: UUID?=null

    private val mBinder = BLEClientServiceBinder()

    inner class BLEClientServiceBinder: Binder(){
        val service: BLEClientServiceLight
            get() = this@BLEClientServiceLight

    }

    override fun onCreate() {
        super.onCreate()
        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
        mHandler = Handler()
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onHandleIntent(intent: Intent?) {
        when(intent?.action){
            Actions.ACTION_START_SCAN_BLE_LIGHT_CLIENT->{
                serviceUUID = UUID.fromString(intent.getStringExtra(IntentData.KEY_SERVICE_UUID))
                startScan()
            }
            Actions.ACTION_STOP_SCAN_BLE_LIGHT_CLIENT->{
                stopScan()
            }
        }
    }

    fun verifyPermissions() :Boolean{
        if (mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) { // Request user to enable it
            /*
             val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
             startActivity(enableBtIntent)
 */
            try {
                val intentRequest = Intent(Actions.ACTION_REQUEST_BLE_ENABLE)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }
            return false
        }
        // Check low energy support
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) { // Get a newer device
            Log.e(TAG, "No LE Support.")
            try {
                val intentRequest = Intent(Actions.ACTION_ERROR_BLE_NOT_SUPPORTED)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }
            return false

        }

        if(!hasLocationPermissions()){
            try {
                val intentRequest =
                    Intent(Actions.ACTION_REQUEST_LOCATION_PERMISSIONS)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }
            return false
        }
        return true
    }

    private fun stopScan() {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled && mBluetoothLeScanner != null) {
            mBluetoothLeScanner!!.stopScan(mScanCallback)
        }
        mScanCallback = null
        mScanning = false
        mHandler = null
        onScanCompleted()
    }

    // Scanning
    private fun startScan() {
        if(!verifyPermissions() || mScanning){
            return
        }
//        disconnectGattServer()
        mScanResults = HashMap()
        mScanCallback = BtleScanCallback(mScanResults!!)
        mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
        // Note: Filtering does not work the same (or at all) on most devices. It also is unable to
        // search for a mask or anything less than a full UUID.
        // Unless the full UUID of the server is known, manual filtering may be necessary.
        // For example, when looking for a brand of device that contains a char sequence in the UUID
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUUID))
            .build()
        val filters: MutableList<ScanFilter> = ArrayList()
        filters.add(scanFilter)
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        mBluetoothLeScanner?.startScan(filters, settings, mScanCallback)
        mHandler = Handler()
        mHandler!!.postDelayed({ stopScan() }, Constants.SCAN_PERIOD)
        mScanning = true
        try {
            val intentRequest = Intent(Actions.ACTION_BLE_LIGHT_CLIENT_SCAN_STARTED)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
        Log.d(TAG, "Started scanning.")
    }


    // Callbacks
    private inner class BtleScanCallback internal constructor(private val mScanResults: MutableMap<String, BluetoothDevice>) : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                addScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan Failed with code $errorCode")
            try {
                val intentRequest = Intent(Actions.ACTION_BLE_LIGHT_CLIENT_SCAN_FAILED)
                intentRequest.putExtra(IntentData.KEY_DATA, errorCode)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }

        private fun addScanResult(result: ScanResult) {
            val device = result.device
            val deviceAddress = device.address
            mScanResults[deviceAddress] = device
            try {
                val intentRequest = Intent(Actions.ACTION_BLE_LIGHT_DEVICE_ADDED)
                intentRequest.putExtra(IntentData.KEY_DATA, device)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }

    }


    fun onScanCompleted(){
        Log.d(TAG, "Stopped scanning.")
        try {
            val intentRequest = Intent(Actions.ACTION_BLE_LIGHT_CLIENT_SCAN_STOPED)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
    private fun hasLocationPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }
    companion object{
        val TAG = BLEClientServiceLight::class.java.simpleName
    }
}