package com.altaureum.covid.tracking.services.server

import android.app.IntentService
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.altaureum.covid.tracking.MyApplication.Companion.context
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.common.IntentData
import com.altaureum.covid.tracking.common.Preferences
import com.altaureum.covid.tracking.realm.data.CovidContact
import com.altaureum.covid.tracking.realm.utils.RealmUtils
import com.altaureum.covid.tracking.realm.utils.covidContacts
import com.altaureum.covid.tracking.util.BluetoothUtils
import com.altaureum.covid.tracking.util.ByteUtils
import com.altaureum.covid.tracking.util.StringUtils
import java.lang.Exception

import java.util.*

class BLEServerService: IntentService(BLEServerService::class.java.simpleName) {

    private var mHandler: Handler? = null
    private var mLogHandler: Handler? = null
    private var mDevices: MutableList<BluetoothDevice>? = null
    private var mGattServer: BluetoothGattServer? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var isServerStarted=false
    private var isServerAdvertising=false
    private var isServerInitialized=false
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var serviceUUID:UUID?=null

    private val mBinder = ServerServiceBinder()

    inner class ServerServiceBinder:Binder(){
        val service:BLEServerService
        get() = this@BLEServerService

    }

    override fun onCreate() {
        super.onCreate()
        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
        mHandler = Handler()
        mLogHandler = Handler(Looper.getMainLooper())
        mDevices = ArrayList()
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onHandleIntent(intent: Intent?) {
        when(intent?.action){
            Actions.ACTION_START_BLE_SERVER->{
                serviceUUID = UUID.fromString(intent.getStringExtra(IntentData.KEY_SERVICE_UUID))
                initServer()
            }
            Actions.ACTION_RESTART_BLE_SERVER->{
                if(intent.hasExtra(IntentData.KEY_SERVICE_UUID)) {
                    serviceUUID =
                        UUID.fromString(intent.getStringExtra(IntentData.KEY_SERVICE_UUID))
                }

                if(isServerInitialized) {
                    restartServer()
                } else{
                    initServer()
                }
            }
            Actions.ACTION_STOP_BLE_SERVER->{
               fullStopServer()
            }
            Actions.ACTION_SEND_MESSAGE_SERVER->{
                try {
                    if(intent.hasExtra(IntentData.KEY_DATA)) {
                        sendMessage(intent.getByteArrayExtra(IntentData.KEY_DATA)!!)
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun unbindService(conn: ServiceConnection) {
        fullStopServer()
        super.unbindService(conn)
    }

    fun initServer(){
        if (mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) { // Request user to enable it
           /*
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
*/
            try {
                val intentRequest = Intent(Actions.ACTION_REQUEST_BLE_ENABLE)
                startActivity(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }
            return
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
            return

        }
        // Check advertising
        if (!mBluetoothAdapter!!.isMultipleAdvertisementSupported) { // Unable to run the server on this device, get a better device
            try {
                val intentRequest =
                    Intent(Actions.ACTION_ERROR_BLE_ADVERTISMENT_NOT_SUPPORTED)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }
            return
        }
        mBluetoothLeAdvertiser = mBluetoothAdapter!!.bluetoothLeAdvertiser
        val gattServerCallback = gattServerCallback()
        mGattServer = mBluetoothManager!!.openGattServer(this, gattServerCallback)
        isServerInitialized = true
        //@SuppressLint("HardwareIds") val deviceInfo = "Device Info" + "\nName: " + mBluetoothAdapter!!.name + "\nAddress: " + mBluetoothAdapter!!.address
        fullStartSever()
    }

    fun fullStartSever(){
        setupGattServer()
        startAdvertising()
        try {
            val intentRequest = Intent(Actions.ACTION_BLE_SERVER_STARTED)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun fullStopServer(){
        stopAdvertising()
        stopGattServer()
        try {
            val intentRequest = Intent(Actions.ACTION_BLE_SERVER_STOPED)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    // GattServer
    private fun setupGattServer() {
        val service = BluetoothGattService(
            serviceUUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY)
        // Write characteristic
        val writeCharacteristic = BluetoothGattCharacteristic(Constants.CHARACTERISTIC_ECHO_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,  // Somehow this is not necessary, the client can still enable notifications
//                        | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE)
        service.addCharacteristic(writeCharacteristic)
        mGattServer!!.addService(service)
        isServerStarted = true
    }

    private fun stopGattServer() {
        if (mGattServer != null) {
            mGattServer!!.close()
        }
        isServerStarted = false
    }

    private fun restartServer() {
        fullStopServer()
        fullStartSever()
    }

    // Advertising
    private fun startAdvertising() {
        if (mBluetoothLeAdvertiser == null) {
            return
        }
        val settings = AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build()
        val parcelUuid = ParcelUuid(serviceUUID)
        val data = AdvertiseData.Builder()
                .addServiceUuid(parcelUuid)
                .build()
        mBluetoothLeAdvertiser!!.startAdvertising(settings, data, mAdvertiseCallback)
        isServerAdvertising = true
    }

    private fun stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
        }
        isServerAdvertising= false
    }

    private val mAdvertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            try {
                val intentRequest = Intent(Actions.ACTION_ERROR_BLE_ADVERTISMENT_SUCCESS)
                intentRequest.putExtra(IntentData.KEY_DATA, settingsInEffect)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }
            Log.d(TAG, "Peripheral advertising started.")
        }

        override fun onStartFailure(errorCode: Int) {
            try {
                val intentRequest = Intent(Actions.ACTION_ERROR_BLE_ADVERTISMENT_FAILED)
                intentRequest.putExtra(IntentData.KEY_DATA, errorCode)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }
            Log.d(TAG, "Peripheral advertising failed: $errorCode")
        }
    }

    // Gatt Server Action Listener
    fun addDevice(device: BluetoothDevice) {
        Log.d(TAG, "Deviced added: " + device.address)
        try {
            val intentRequest = Intent(Actions.ACTION_BLE_SERVER_DEVICE_ADDED)
            intentRequest.putExtra(IntentData.KEY_DATA, device)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
        mHandler!!.post { mDevices!!.add(device) }
    }

    fun removeDevice(device: BluetoothDevice) {
        Log.d(TAG, "Deviced removed: " + device.address)
        try {
            val intentRequest = Intent(Actions.ACTION_BLE_SERVER_DEVICE_REMOVED)
            intentRequest.putExtra(IntentData.KEY_DATA, device)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
        mHandler!!.post { mDevices!!.remove(device) }
    }

    fun sendResponse(device: BluetoothDevice?, requestId: Int, status: Int, offset: Int, value: ByteArray?) {
        mHandler!!.post { mGattServer!!.sendResponse(device, requestId, status, 0, null) }
    }

    private fun sendReverseMessage(message: ByteArray) {
        val response = ByteUtils.reverse(message)
        sendMessage(response)
    }
    private fun sendMessage(message: String) {
        sendMessage(StringUtils.bytesFromString(message))
    }

    private fun sendMessage(message: ByteArray) {
        mHandler!!.post {
            // Reverse message to differentiate original message & response
            Log.d(TAG, "Message Send: " + StringUtils.byteArrayInHexFormat(message))
            notifyCharacteristicEcho(message)
        }
    }

    fun notifyCharacteristicEcho(value: ByteArray) {
        notifyCharacteristic(value, Constants.CHARACTERISTIC_ECHO_UUID)
    }

    // Notifications
    private fun notifyCharacteristic(value: ByteArray, uuid: UUID) {
        mHandler!!.post {
            val service = mGattServer!!.getService(serviceUUID)
            val characteristic = service.getCharacteristic(uuid)
            Log.d(TAG, "Notifying characteristic " + characteristic.uuid.toString()
                    + ", new value: " + StringUtils.byteArrayInHexFormat(value))
            characteristic.value = value
            val confirm = BluetoothUtils.requiresConfirmation(characteristic)
            for (device in mDevices!!) {
                mGattServer!!.notifyCharacteristicChanged(device, characteristic, confirm)
            }
        }
    }

    // Gatt Callback
    private inner class gattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.d(TAG, "onConnectionStateChange " + device.address
                    + "\nstatus " + status
                    + "\nnewState " + newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                addDevice(device)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                removeDevice(device)
            }
        }

        // The Gatt will reject Characteristic Read requests that do not have the permission set,
// so there is no need to check inside the callback
        override fun onCharacteristicReadRequest(device: BluetoothDevice,
                                                 requestId: Int,
                                                 offset: Int,
                                                 characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Log.d(TAG, "onCharacteristicReadRequest "
                    + characteristic.uuid.toString())
            if (BluetoothUtils.requiresResponse(characteristic)) { // Unknown read characteristic requiring response, send failure
                sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
            }
            // Not one of our characteristics or has NO_RESPONSE property set
        }

        // The Gatt will reject Characteristic Write requests that do not have the permission set,
// so there is no need to check inside the callback
        override fun onCharacteristicWriteRequest(device: BluetoothDevice,
                                                  requestId: Int,
                                                  characteristic: BluetoothGattCharacteristic,
                                                  preparedWrite: Boolean,
                                                  responseNeeded: Boolean,
                                                  offset: Int,
                                                  value: ByteArray) {
            super.onCharacteristicWriteRequest(device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value)
            Log.d(TAG, "onCharacteristicWriteRequest" + characteristic.uuid.toString()
                    + "\nReceived: " + StringUtils.byteArrayInHexFormat(value))
            if (Constants.CHARACTERISTIC_ECHO_UUID == characteristic.uuid) {
                sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                try {
                    val intentRequest = Intent(Actions.ACTION_BLE_SERVER_MESSAGE_RECEIVED)
                    intentRequest.putExtra(IntentData.KEY_DATA, value)
                    localBroadcastManager.sendBroadcast(intentRequest)
                }catch (e:Exception){
                    e.printStackTrace()
                }
                //Send info Back to sender
                val defaultSharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val covidId = defaultSharedPreferences.getString(Preferences.KEY_COVID_ID, "")!!
                //We notify all the clients our COVID ID
                sendMessage(covidId)
                onMessageReceived(value, device)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            super.onNotificationSent(device, status)
            try {
                val intentRequest = Intent(Actions.ACTION_BLE_SERVER_NOTIFICATION_SENT)
                localBroadcastManager.sendBroadcast(intentRequest)
            }catch (e:Exception){
                e.printStackTrace()
            }
            Log.d(TAG, "onNotificationSent")
        }
    }

    fun onMessageReceived(message: ByteArray, bluetoothDevice: BluetoothDevice){
        Log.d(TAG, "Message Received: "+String(message))
        val covidContact = CovidContact()
        covidContact.covidId=String(message)
        covidContact.contactDate=Date()
        val realm = RealmUtils.getInstance(context!!)
        realm!!.covidContacts().putSync(covidContact)
        realm.close()


    }

    companion object{
        val TAG = BLEServerService::class.java.simpleName
    }
}