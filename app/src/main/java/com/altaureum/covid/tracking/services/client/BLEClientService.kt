package com.altaureum.covid.tracking.services.client

import android.Manifest
import android.app.IntentService
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import 	androidx.preference.PreferenceManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.common.IntentData
import com.altaureum.covid.tracking.common.Preferences
import com.altaureum.covid.tracking.services.data.ChunkHeader
import com.altaureum.covid.tracking.services.data.CovidMessage
import com.altaureum.covid.tracking.services.data.DeviceSignal
import com.altaureum.covid.tracking.util.BluetoothUtils
import com.altaureum.covid.tracking.util.BluetoothUtils.calculateAccuracy
import com.altaureum.covid.tracking.util.StringUtils
import com.google.gson.Gson
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

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
                try {
                    val intentRequest = Intent(Actions.ACTION_BLE_CLIENT_STARTED)
                    localBroadcastManager.sendBroadcast(intentRequest)
                }catch (e: java.lang.Exception){
                    e.printStackTrace()
                }
                startScan()
            }
            Actions.ACTION_STOP_BLE_CLIENT->{
                mAutoScanning = false
                stopScan()
                try {
                    val intentRequest = Intent(Actions.ACTION_BLE_CLIENT_STOPED)
                    localBroadcastManager.sendBroadcast(intentRequest)
                }catch (e: java.lang.Exception){
                    e.printStackTrace()
                }
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
                intentRequest.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intentRequest)
                //localBroadcastManager.sendBroadcast(intentRequest)
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
                intentRequest.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intentRequest)
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
        val settings = ScanSettings.Builder().setReportDelay(0)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        mBluetoothLeScanner?.startScan(filters, settings, mScanCallback)



        mHandler = Handler(Looper.getMainLooper())
        mHandler!!.postDelayed({ stopScan() }, Constants.SCAN_PERIOD)
        mScanning = true



        try {
            val intentRequest = Intent(Actions.ACTION_BLE_CLIENT_SCAN_STARTED)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
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
            try {
                val intentRequest = Intent(Actions.ACTION_BLE_CLIENT_SCAN_FAILED)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e: java.lang.Exception){
                e.printStackTrace()
            }
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
            try {
                val intentRequest = Intent(Actions.ACTION_BLE_CLIENT_DEVICE_ADDED)
                intentRequest.putExtra(IntentData.KEY_DATA, device)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }

            if(mScanResults.containsKey(deviceAddress)){
                val bluetoothDevice = mScanResults[deviceAddress]!!
                val deviceSignal = DeviceSignal()
                deviceSignal.rssi = result.rssi
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    deviceSignal.txPower = result.txPower
                } else{
                    deviceSignal.txPower = -59
                }
                bluetoothDevice.deviceSignal = deviceSignal
                var minutesSinceLastUpdate:Int
                try {
                    minutesSinceLastUpdate = TimeUnit.MILLISECONDS.toSeconds(Date().time - bluetoothDevice.lastUpdated?.time!!).toInt()
                }catch (e:java.lang.Exception){
                    minutesSinceLastUpdate=-1
                }
                // If we know this device we try to connec if is not trying to connect and  we didnt connect in the last 5 minutes or never
                if(!bluetoothDevice.isTryingToConnect && !bluetoothDevice.isConnected){//&& (minutesSinceLastUpdate == -1 || minutesSinceLastUpdate>TIME_TO_CONNECT_SECONDS)){
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




    private inner class GattClientCallback : BluetoothGattCallback() {
        var mConnected = false
        var mEchoInitialized = false




        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                val address = gatt!!.device.address!!
                if(mScanResults!!.containsKey(address)){
                    mScanResults!!.get(address)?.deviceSignal?.rssi = rssi
                }
            }
            super.onReadRemoteRssi(gatt, rssi, status)
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){
            }
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){
            }
            super.onPhyRead(gatt, txPhy, rxPhy, status)
        }

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
                mScanResults?.get(gatt.device.address)?.isTryingToConnect=false
                mScanResults?.get(gatt.device.address)?.isConnected = true
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

        var packetSize:Int=0
        var packets = ArrayList<ByteArray>()
        var packetInteration = 0
        fun sendData(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, data: ByteArray):Int{
            val chunksize = 20; //20 byte chunk
            packetSize = Math.ceil(data.size/chunksize.toDouble()).toInt()
            val chunkPackage = ChunkHeader()
            chunkPackage.packets=packetSize

            characteristic.setValue(Gson().toJson(chunkPackage).toByteArray());
            Handler(Looper.getMainLooper())?.postDelayed(Runnable {
                gatt.writeCharacteristic(characteristic)
                gatt.executeReliableWrite()
            }, 100)

            packets = ArrayList<ByteArray>()
            packetInteration = 0
            var start = 0
            for (packet in 0..packetSize-1){
                var end = start +chunksize
                if(end>data.size){
                    end=data.size
                }
                val copyOfRange = data.copyOfRange(start, end)
                packets.add(copyOfRange)
                //packets.set(packet, copyOfRange)
                //packets[packet] = copyOfRange
                start += chunksize
            }
            return packetSize
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully")
                if(packetInteration < packetSize && packets.size>0){
                    Handler(Looper.getMainLooper())?.postDelayed(Runnable {
                        characteristic.setValue(packets[packetInteration]);
                        gatt.writeCharacteristic(characteristic);
                        packetInteration++;
                    }, 100)


                }

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

                    val covidMessage = CovidMessage()
                    covidMessage.covidId=covidId
                    val address = gatt.device.address
                    if(mScanResults!!.containsKey(address)) {
                        val deviceSignal = mScanResults!!.get(address)?.deviceSignal
                        covidMessage.deviceSignal = deviceSignal
                    }


                    val covidJsonMessage = Gson().toJson(covidMessage)

                    val numberOfPackets = sendData(
                        gatt,
                        characteristic,
                        covidJsonMessage.toByteArray()
                    )



                    if(numberOfPackets>0) {
                        val discoveredDevice = mScanResults?.get(address)
                        discoveredDevice?.lastUpdated = Date()
                    }


                    //disconnectGattServer(gatt)
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
            val address = gatt.device.address
            val discoveredDevice = mScanResults?.get(address)
            discoveredDevice?.isConnected = false
            discoveredDevice?.isTryingToConnect =false
            mConnected = false
            mEchoInitialized = false
            try {
                val intentRequest = Intent(Actions.ACTION_BLE_CLIENT_DEVICE_REMOVED)
                intentRequest.putExtra(IntentData.KEY_DATA, gatt.device)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }

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
        val connectGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(this, false, gattClientCallback)
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
        val TAG = BLEClientService::class.java.simpleName
        val TIME_TO_CONNECT_SECONDS=20
    }
}