package com.altaureum.covid.tracking.services.client

import android.Manifest
import android.app.IntentService
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.*
import 	androidx.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.common.IntentData
import com.altaureum.covid.tracking.common.Preferences
import com.altaureum.covid.tracking.util.BluetoothUtils
import com.altaureum.covid.tracking.util.StringUtils
import java.util.*
import java.util.concurrent.TimeUnit

class BLEClientService: IntentService(BLEClientService::class.java.simpleName) {

    private var mAutoScanning = false
    private var mScanning = false
    private var mHandler: Handler? = null
    private var mLogHandler: Handler? = null
    private var mScanResults: MutableMap<String, DiscoveredDevice>? = null
    //private var mConnected = false
    //private var mEchoInitialized = false
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mScanCallback: ScanCallback? = null
    //private var mGatt: BluetoothGatt? = null
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var serviceUUID: UUID?=null

    private val mBinder = BLEClientServiceBinder()

    inner class BLEClientServiceBinder: Binder(){
        val service: BLEClientService
            get() = this@BLEClientService

    }

    override fun onCreate() {
        super.onCreate()
        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
        mHandler = Handler()
        mLogHandler = Handler(Looper.getMainLooper())
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onHandleIntent(intent: Intent?) {
        when(intent?.action){
            Actions.ACTION_START_BLE_CLIENT->{
                mAutoScanning = true
                serviceUUID = UUID.fromString(intent.getStringExtra(IntentData.KEY_SERVICE_UUID))
                startScan()
            }
            Actions.ACTION_STOP_BLE_CLIENT->{
                mAutoScanning = false
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
        Log.d(TAG, "Started scanning.")
    }



    // Gat Client Actions


    // Callbacks
    private inner class BtleScanCallback internal constructor(private val mScanResults: MutableMap<String, DiscoveredDevice>) : ScanCallback() {
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
        }

        private fun addScanResult(result: ScanResult) {
            val device = result.device
            val deviceAddress = device.address
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val calculateAccuracy = calculateAccuracy(result.txPower, result.rssi)
                Log.d(TAG, "Distance:"+ calculateAccuracy)

                //Toast.makeText(this@ClientActivity, "Distance: "+ calculateAccuracy, Toast.LENGTH_SHORT).show()
            }

            if(mScanResults.containsKey(deviceAddress)){
                val bluetoothDevice = mScanResults[deviceAddress]!!
                var minutesSinceLastUpdate:Int
                try {
                    minutesSinceLastUpdate = TimeUnit.MILLISECONDS.toMinutes(Date().time - bluetoothDevice.lastUpdated?.time!!).toInt()
                }catch (e:java.lang.Exception){
                    minutesSinceLastUpdate=-1
                }
                // If we know this device we try to connec if is not trying to connect and  we didnt connect in the last 5 minutes or never
                if(!bluetoothDevice.isTryingToConnect && (minutesSinceLastUpdate == -1 || minutesSinceLastUpdate>5)){
                    connectDevice(bluetoothDevice.device)
                }

            }else{
                val discoveredDevice = DiscoveredDevice()
                discoveredDevice.device = device
                mScanResults[deviceAddress] = discoveredDevice
                connectDevice(discoveredDevice.device)
            }
            //mScanResults[deviceAddress] = device
        }

    }

    protected fun calculateAccuracy(txPower: Int, rssi: Int): Double {
        if (rssi == 0) {
            return -1.0 // if we cannot determine accuracy, return -1.
        }
        return Math.pow(10.0, (txPower.toDouble() - rssi) / (10 * 2))
    }

    private inner class GattClientCallback : BluetoothGattCallback() {
        var mConnected = false
        var mEchoInitialized = false
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.d(TAG, "onConnectionStateChange newState: $newState")
            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.e(TAG, "Connection Gatt failure status $status")
                disconnectGattServer(gatt)
                return
            } else if (status != BluetoothGatt.GATT_SUCCESS) { // handle anything not SUCCESS as failure
                Log.e(TAG, "Connection not GATT sucess status $status")
                disconnectGattServer(gatt)
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to device " + gatt.device.address)
                mConnected = true
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from device")
                disconnectGattServer(gatt)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Device service discovery unsuccessful, status $status")
                return
            }
            val matchingCharacteristics = BluetoothUtils.findCharacteristics(gatt)
            if (matchingCharacteristics.isEmpty()) {
                Log.e(TAG, "Unable to find characteristics.")
                return
            }
            Log.d(TAG, "Initializing: setting write type and enabling notification")
            for (characteristic in matchingCharacteristics) {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                enableCharacteristicNotification(gatt, characteristic)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully")
            } else {
                Log.e(TAG, "Characteristic write unsuccessful, status: $status")
                disconnectGattServer(gatt)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read successfully")
                readCharacteristic(characteristic, gatt)
            } else {
                Log.e(TAG, "Characteristic read unsuccessful, status: $status")
                // Trying to read from the Time Characteristic? It doesnt have the property or permissions
// set to allow this. Normally this would be an error and you would want to:
// disconnectGattServer();
                disconnectGattServer(gatt)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "Characteristic changed, " + characteristic.uuid.toString())
            readCharacteristic(characteristic, gatt)
        }

        private fun enableCharacteristicNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val characteristicWriteSuccess = gatt.setCharacteristicNotification(characteristic, true)
            if (characteristicWriteSuccess) {
                Log.d(TAG, "Characteristic notification set successfully for " + characteristic.uuid.toString())
                if (BluetoothUtils.isEchoCharacteristic(characteristic)) {
                    mEchoInitialized = true

                    val defaultSharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    val covidId = defaultSharedPreferences.getString(Preferences.KEY_COVID_ID, "")!!

                    val isMessageSent = sendMessage(covidId, gatt)
                    if(isMessageSent) {
                        val discoveredDevice = mScanResults?.get(gatt.device.address)
                        discoveredDevice?.lastUpdated = Date()
                    }
                    disconnectGattServer(gatt)
                }
            } else {
                Log.e(TAG, "Characteristic notification set failure for " + characteristic.uuid.toString())
            }
        }

        private fun readCharacteristic(characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt) {
            val messageBytes = characteristic.value
            Log.d(TAG, "Read: " + StringUtils.byteArrayInHexFormat(messageBytes))
            val message = StringUtils.stringFromBytes(messageBytes)
            if (message == null) {
                Log.e(TAG, "Unable to convert bytes to string")
                return
            }
            onReceivedMessage(messageBytes, gatt)

        }

        fun disconnectGattServer(gatt: BluetoothGatt) {
            Log.d(TAG, "Closing Gatt connection")
            val discoveredDevice = mScanResults?.get(gatt.device.address)
            discoveredDevice?.isTryingToConnect = false
            mConnected = false
            mEchoInitialized = false
            gatt.disconnect()
            gatt.close()
        }

        private fun sendMessage(message:String, gatt: BluetoothGatt):Boolean{
            return sendMessage(StringUtils.bytesFromString(message), gatt)
        }

        // Messaging
        private fun sendMessage(messageBytes:ByteArray, gatt: BluetoothGatt):Boolean {
            if (!mConnected || !mEchoInitialized) {
                return false
            }
            val characteristic = BluetoothUtils.findEchoCharacteristic(gatt!!)
            if (characteristic == null) {
                Log.e(TAG, "Unable to find echo characteristic.")
                disconnectGattServer(gatt)
                return false
            }
            if (messageBytes.size == 0) {
                Log.e(TAG, "Unable to convert message to bytes")
                return false
            }
            characteristic.value = messageBytes
            val success = gatt!!.writeCharacteristic(characteristic)
            if (success) {
                Log.d(TAG, "Wrote: " + StringUtils.byteArrayInHexFormat(messageBytes))
            } else {
                Log.e(TAG, "Failed to write data")
            }
            return true
        }

        fun onReceivedMessage(message: ByteArray, gatt: BluetoothGatt){
            Log.d(TAG, "Received message: "+String(message)+ "From: "+gatt.device.address)
        }
    }

    fun onScanCompleted(){
        Log.d(TAG, "Stopped scanning.")
        if(mAutoScanning){
            startScan()
        }

    }



    // Gatt connection
    private fun connectDevice(device: BluetoothDevice?) {
        Log.d(TAG, "Connecting to " + device!!.address)
        val discoveredDevice = mScanResults?.get(device.address)
        discoveredDevice?.isTryingToConnect = true

        val gattClientCallback = GattClientCallback()
        val connectGatt = device.connectGatt(this, false, gattClientCallback)
    }





    private fun hasLocationPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }
    companion object{
        val TAG = BLEClientService::class.java.simpleName
    }
}