package com.altaureum.covid.tracking.services.client

import android.Manifest
import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import 	androidx.preference.PreferenceManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.altaureum.covid.tracking.MyApplication
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.common.IntentData
import com.altaureum.covid.tracking.common.Preferences
import com.altaureum.covid.tracking.services.data.ChunkHeader
import com.altaureum.covid.tracking.services.data.CovidMessage
import com.altaureum.covid.tracking.services.data.DeviceSignal
import com.altaureum.covid.tracking.services.notification.NotificationFactory
import com.altaureum.covid.tracking.services.server.BLEServerService
import com.altaureum.covid.tracking.util.BluetoothUtils
import com.altaureum.covid.tracking.util.BluetoothUtils.calculateAccuracy
import com.altaureum.covid.tracking.util.StringUtils
import com.google.gson.Gson
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class BLEClientService: Service() {

    private var mAutoScanning = false
    private var mScanning = false
    //private var mHandler: Handler? = null
    //private var mLogHandler: Handler? = null
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

    @Volatile
    private var mHandlerThread: HandlerThread? = null
    private var mServiceHandler: ServiceHandler? = null

    // Define how the handler will process messages
    inner class ServiceHandler(looper: Looper?) : Handler(looper) {
        // Define how to handle any incoming messages here
        override fun handleMessage(message: Message?) {
            when(message?.what) {
                MESSAGE_ACTION_START_BLE_CLIENT -> {
                    mAutoScanning = true
                    serviceUUID =
                        UUID.fromString(message.data.getString(IntentData.KEY_SERVICE_UUID))
                    try {
                        val intentRequest = Intent(Actions.ACTION_BLE_CLIENT_STARTED)
                        localBroadcastManager.sendBroadcast(intentRequest)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    startScan()
                }
                MESSAGE_ACTION_STOP_BLE_CLIENT -> {
                    mAutoScanning = false
                    stopScan()
                    try {
                        val intentRequest = Intent(Actions.ACTION_BLE_CLIENT_STOPED)
                        localBroadcastManager.sendBroadcast(intentRequest)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    stopSelf()
                }
                MESSAGE_ACTION_START_BLE_CLIENT_SCAN->{
                    startScan()
                }
                MESSAGE_ACTION_STOP_BLE_CLIENT_SCAN->{
                    stopScan()
                }
            }
            // ...
            // When needed, stop the service with
            // stopSelf();
        }
    }

    override fun onCreate() {
        super.onCreate()
        // An Android handler thread internally operates on a looper.
        mHandlerThread = HandlerThread("BLEClientService.HandlerThread")
        mHandlerThread?.start();
        // An Android service handler is a handler running on a specific background thread.
        mServiceHandler = ServiceHandler(mHandlerThread!!.getLooper());

        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)

        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundWithNotification()
        }
    }

    override fun onDestroy() {
        mHandlerThread?.quit();

        val notificationManager = MyApplication.context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationFactory.NOTIFICATION_ID)
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startForegroundWithNotification(){
        startForeground(NotificationFactory.NOTIFICATION_ID, NotificationFactory.getNotification(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        onHandleIntent(intent)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            Actions.ACTION_START_BLE_CLIENT -> {
                val message =
                    mServiceHandler?.obtainMessage(MESSAGE_ACTION_START_BLE_CLIENT)
                message?.data = intent.extras
                mServiceHandler?.sendMessage(message)
            }
            Actions.ACTION_STOP_BLE_CLIENT -> {
                val message =
                    mServiceHandler?.obtainMessage(MESSAGE_ACTION_STOP_BLE_CLIENT)
                message?.data = intent.extras
                mServiceHandler?.sendMessage(message)
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

        mServiceHandler?.postDelayed({ stopScan() }, Constants.SCAN_PERIOD)
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

            val deviceSignal = DeviceSignal()
            deviceSignal.rssi = result.rssi
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //if txpower is not provided we use the default one
                Log.d(TAG, "txPower Received:"+result.txPower)
                deviceSignal.txPower = if(result.txPower!=ScanResult.TX_POWER_NOT_PRESENT) result.txPower else -59
            } else{
                deviceSignal.txPower = -59
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val calculateAccuracy = calculateAccuracy(deviceSignal.txPower, deviceSignal.rssi)
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
                discoveredDevice.deviceSignal = deviceSignal
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
        var bufferPackets = ArrayList<ByteArray>()
        var packetInteration = 0
        fun sendData(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, data: ByteArray):Int{
            val chunksize = 20; //20 byte chunk
            packetSize = Math.ceil(data.size/chunksize.toDouble()).toInt()
            val chunkPackage = ChunkHeader()
            chunkPackage.packets=packetSize
            characteristic.setValue(Gson().toJson(chunkPackage).toByteArray());

            gatt.writeCharacteristic(characteristic)
            gatt.executeReliableWrite()


            bufferPackets = ArrayList<ByteArray>()
            packetInteration = 0
            var start = 0
            for (packet in 0..packetSize-1){
                var end = start +chunksize
                if(end>data.size){
                    end=data.size
                }
                val copyOfRange = data.copyOfRange(start, end)
                bufferPackets.add(copyOfRange)
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

                if(packetInteration < packetSize && bufferPackets.size>0){
                    val bytesToSend = bufferPackets[packetInteration]
                    Log.d(TAG, "Sending Information: "+String(bytesToSend))
                    characteristic.setValue(bytesToSend);
                    gatt.writeCharacteristic(characteristic);
                    packetInteration++;
                } else{
                    // we finish
                    //disconnecting becasue we finish sending info
                    Log.d(TAG, "Disconnecting as we send the info")
                    disconnectGattServer(gatt)
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
            Handler(Looper.myLooper()).postDelayed(Runnable {
                val success = gatt!!.writeCharacteristic(characteristic)
                if (success) {
                    Log.d(TAG, "Wrote: " + StringUtils.byteArrayInHexFormat(messageBytes))
                } else {
                    Log.e(TAG, "Failed to write data")
                }
            }, 100)

            return true
        }

        fun onReceivedMessage(message: ByteArray, gatt: BluetoothGatt){
            Log.d(TAG, "Received message: "+String(message)+ "From: "+gatt.device.address)
        }
    }

    fun onScanCompleted(){
        Log.d(TAG, "Stopped scanning.")
        if(mAutoScanning){
            val message = mServiceHandler?.obtainMessage(MESSAGE_ACTION_START_BLE_CLIENT_SCAN)
            mServiceHandler?.sendMessage(message)
            //startScan()
        }

    }



    // Gatt connection
    private fun connectDevice(device: BluetoothDevice?) {
        Log.d(TAG, "Connecting to " + device!!.address)
        val discoveredDevice = mScanResults?.get(device.address)
        discoveredDevice?.let {
            if(!it.isTryingToConnect && !it.isConnected) {
                it.isTryingToConnect = true
                val gattClientCallback = GattClientCallback()
                Handler(Looper.myLooper()).postDelayed(Runnable {
                    val connectGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        device.connectGatt(this, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE)
                    } else {
                        device.connectGatt(this, true, gattClientCallback)
                    }
                }, 100)

            }
        }
    }

    override fun unbindService(conn: ServiceConnection) {
        super.unbindService(conn)
    }



    private fun hasLocationPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }
    companion object{
        private val TAG = BLEClientService::class.java.simpleName
        private val TIME_TO_CONNECT_SECONDS=20

        private val MESSAGE_ACTION_START_BLE_CLIENT=0
        private val MESSAGE_ACTION_STOP_BLE_CLIENT=1
        private val MESSAGE_ACTION_START_BLE_CLIENT_SCAN=2
        private val MESSAGE_ACTION_STOP_BLE_CLIENT_SCAN=3
    }
}