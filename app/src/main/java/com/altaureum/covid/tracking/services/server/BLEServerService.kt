package com.altaureum.covid.tracking.services.server

import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.altaureum.covid.tracking.MyApplication.Companion.context
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.common.IntentData
import com.altaureum.covid.tracking.common.Preferences
import com.altaureum.covid.tracking.realm.data.CovidContact
import com.altaureum.covid.tracking.realm.data.LocationCovidContact
import com.altaureum.covid.tracking.realm.utils.RealmUtils
import com.altaureum.covid.tracking.realm.utils.covidContacts
import com.altaureum.covid.tracking.services.data.ChunkHeader
import com.altaureum.covid.tracking.services.data.ChunkMessage
import com.altaureum.covid.tracking.services.data.CovidMessage
import com.altaureum.covid.tracking.services.data.DeviceSignal
import com.altaureum.covid.tracking.services.notification.NotificationFactory
import com.altaureum.covid.tracking.util.BluetoothUtils
import com.altaureum.covid.tracking.util.ByteUtils
import com.altaureum.covid.tracking.util.StringUtils
import com.google.gson.Gson
import io.realm.Sort
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class BLEServerService: Service() {

    /*
    private var mHandler: Handler? = null
    private var mLogHandler: Handler? = null
    */
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


    @Volatile
    private var mHandlerThread: HandlerThread? = null
    private var mServiceHandler: ServiceHandler? = null

    // Define how the handler will process messages
    inner class ServiceHandler(looper: Looper?) : Handler(looper) {
        // Define how to handle any incoming messages here
        override fun handleMessage(message: Message?) {
            when(message?.what){
                MESSAGE_ACTION_START_BLE_SERVER->{
                    serviceUUID =
                        UUID.fromString(message.data.getString(IntentData.KEY_SERVICE_UUID))
                    initServer()
                }
                MESSAGE_ACTION_STOP_BLE_SERVER->{
                    fullStopServer()
                    stopSelf()
                }
                MESSAGE_ACTION_RESTART_BLE_SERVER->{
                    serviceUUID =
                        UUID.fromString(message.data.getString(IntentData.KEY_SERVICE_UUID))
                    if (isServerInitialized) {
                        restartServer()
                    } else {
                        initServer()
                    }
                }
                MESSAGE_ACTION_BLE_SERVER_CHECK_STATUS->{
                    sendResponseCheckStatus()
                }
                MESSAGE_ACTION_SEND_MESSAGE_SERVER->{
                    try {
                        if (message.data.containsKey(IntentData.KEY_DATA)) {
                            sendMessage(message.data.getByteArray(IntentData.KEY_DATA)!!)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // An Android handler thread internally operates on a looper.
        mHandlerThread = HandlerThread("BLEServerService.HandlerThread")
        mHandlerThread?.start();
        // An Android service handler is a handler running on a specific background thread.
        mServiceHandler = ServiceHandler(mHandlerThread!!.getLooper());

        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)

        /*
        mHandler = Handler()
        mLogHandler = Handler(Looper.getMainLooper())
        */

        mDevices = ArrayList()
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundWithNotification()
        }
    }

    override fun onDestroy() {
        mHandlerThread?.quit();
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationFactory.NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startForegroundWithNotification(){
        startForeground(NotificationFactory.NOTIFICATION_ID, NotificationFactory.getNotification(this))

    }

    fun onHandleIntent(intent: Intent?) {
            when (intent?.action) {
                Actions.ACTION_START_BLE_SERVER -> {
                    val message =
                        mServiceHandler?.obtainMessage(MESSAGE_ACTION_START_BLE_SERVER)
                    message?.data = intent.extras
                    mServiceHandler?.sendMessage(message)
                }
                Actions.ACTION_RESTART_BLE_SERVER -> {
                    val message =
                        mServiceHandler?.obtainMessage(MESSAGE_ACTION_RESTART_BLE_SERVER)
                    message?.data = intent.extras
                    mServiceHandler?.sendMessage(message)
                }
                Actions.ACTION_STOP_BLE_SERVER -> {
                    val message =
                        mServiceHandler?.obtainMessage(MESSAGE_ACTION_STOP_BLE_SERVER)
                    message?.data = intent.extras
                    mServiceHandler?.sendMessage(message)
                }
                Actions.ACTION_BLE_SERVER_CHECK_STATUS -> {
                    val message =
                        mServiceHandler?.obtainMessage(MESSAGE_ACTION_BLE_SERVER_CHECK_STATUS)
                    message?.data = intent.extras
                    mServiceHandler?.sendMessage(message)
                }
                Actions.ACTION_SEND_MESSAGE_SERVER -> {
                    val message =
                        mServiceHandler?.obtainMessage(MESSAGE_ACTION_SEND_MESSAGE_SERVER)
                    message?.data = intent.extras
                    mServiceHandler?.sendMessage(message)
                }
            }
    }

    fun sendResponseCheckStatus(){
        try {
            val intentRequest = Intent(Actions.ACTION_BLE_SERVER_CHECK_STATUS_RESPONSE)
            intentRequest.putExtra(IntentData.KEY_DATA, isServerStarted)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        onHandleIntent(intent)
        return START_NOT_STICKY
    }

    override fun unbindService(conn: ServiceConnection) {

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
                intentRequest.setFlags( FLAG_ACTIVITY_NEW_TASK)
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
        try {
            setupGattServer()
            startAdvertising()
            try {
                val intentRequest = Intent(Actions.ACTION_BLE_SERVER_STARTED)
                localBroadcastManager.sendBroadcast(intentRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }catch (e:Exception){
            try {
                val intentRequest = Intent(Actions.ACTION_BLE_SERVER_ERROR)
                intentRequest.putExtra(IntentData.KEY_DATA, e.toString())
                localBroadcastManager.sendBroadcast(intentRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        stopSelf()
    }

    // GattServer
    private fun setupGattServer() {
        val service = BluetoothGattService(
            serviceUUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        // Write characteristic
        val writeCharacteristic = BluetoothGattCharacteristic(
            Constants.CHARACTERISTIC_ECHO_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,  // Somehow this is not necessary, the client can still enable notifications
//                        | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(writeCharacteristic)
        mGattServer?.addService(service)
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
        val data = AdvertiseData.Builder().setIncludeTxPowerLevel(true)
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
        mServiceHandler?.post { mDevices!!.add(device) }
        //mHandler!!.post { mDevices!!.add(device) }
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
        mServiceHandler?.post { mDevices!!.remove(device) }
        //mHandler!!.post { mDevices!!.remove(device) }
    }

    fun sendResponse(device: BluetoothDevice?, requestId: Int, status: Int, offset: Int=0, value: ByteArray?=null) {
        mServiceHandler?.post { mGattServer!!.sendResponse(device, requestId, status, offset, value) }
    }

    private fun sendReverseMessage(message: ByteArray) {
        val response = ByteUtils.reverse(message)
        sendMessage(response)
    }
    private fun sendMessage(message: String) {
        //sendMessage(StringUtils.bytesFromString(message))
    }

    private fun sendMessage(device: BluetoothDevice?, requestId: Int, status: Int, message: String) {
        sendResponse(device, requestId, status, 0, StringUtils.bytesFromString(message))
    }

    private fun sendMessage(message: ByteArray) {
        mServiceHandler?.post {
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
        mServiceHandler?.post {
            val service = mGattServer!!.getService(serviceUUID)
            val characteristic = service.getCharacteristic(uuid)
            Log.d(TAG, "Notifying characteristic " + characteristic.uuid.toString()
                    + ", new value: " + StringUtils.byteArrayInHexFormat(value))
            characteristic.value = value
            val confirm = BluetoothUtils.requiresConfirmation(characteristic)
            for (device in mDevices!!) {
                val notifyCharacteristicChanged =
                    mGattServer!!.notifyCharacteristicChanged(device, characteristic, confirm)

            }
        }
    }

    // Gatt Callback
    private inner class gattServerCallback : BluetoothGattServerCallback() {
        private var mDevicesSignal: MutableMap<String, DeviceSignal> = HashMap()
        private var mChunkMessages: MutableMap<String, ChunkMessage> = HashMap()

        override fun onPhyUpdate(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                val address = device?.address!!
                if(mDevicesSignal.containsKey(address)){
                    mDevicesSignal.get(address)!!.txPower = txPhy
                }else{
                    val deviceSignal = DeviceSignal()
                    deviceSignal.txPower = txPhy
                    mDevicesSignal.put(address, deviceSignal)
                }
            }
        }

        override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                val address = device?.address!!
                if(mDevicesSignal.containsKey(address)){
                    mDevicesSignal.get(address)!!.txPower = txPhy
                }else{
                    val deviceSignal = DeviceSignal()
                    deviceSignal.txPower = txPhy
                    mDevicesSignal.put(address, deviceSignal)
                }
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.d(TAG, "onConnectionStateChange " + device.address
                    + "\nstatus " + status
                    + "\nnewState " + newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                addDevice(device)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mDevicesSignal.remove(device.address)
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
            val message = String(value)
            Log.d(TAG, "onCharacteristicWriteRequest" + characteristic.uuid.toString()
                    + "\nReceived: " + StringUtils.byteArrayInHexFormat(value)+"\nPartial Message: "+ message
            )
            if (Constants.CHARACTERISTIC_ECHO_UUID == characteristic.uuid) {
                sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                var isChunkPackageHeader=false
                val address1 = device.address
                if(message.contains("packets")) isChunkPackageHeader =true else isChunkPackageHeader=false
                if(isChunkPackageHeader){
                    try{
                        val chunkHeader = Gson().fromJson(message, ChunkHeader::class.java)
                        val chunkMessage = ChunkMessage()
                        chunkMessage.packets=chunkHeader.packets
                        mChunkMessages.put(address1, chunkMessage)
                    }catch (e:Exception){
                    }
                    //is the chunk header
                    return
                }

                if(mChunkMessages.containsKey(address1)) {
                    val chunkMessage = mChunkMessages.get(address1)
                    chunkMessage!!.chunks.add(message)
                    if(chunkMessage!!.chunks.size != chunkMessage.packets){
                        //waiting for new messages
                        return
                    }
                }
                val message = mChunkMessages.get(address1).toString()
                mChunkMessages.remove(address1)


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
                //sendMessage(device, requestId, BluetoothGatt.GATT_SUCCESS, covidJsonMessage)
                onMessageReceived(message, device)
            }else{
                sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
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

    fun onMessageReceived(message: String, bluetoothDevice: BluetoothDevice){
        Log.d(TAG, "Full Message Received: "+message)

        var covidMessage:CovidMessage?=null
        try {
             covidMessage = Gson().fromJson(message, CovidMessage::class.java)
        }catch (e:Exception){
            e.printStackTrace()
        }

        if(covidMessage == null){
            return
        }

        val realm = RealmUtils.getInstance(context!!)


        val timeSinceLastTime = Calendar.getInstance()
        timeSinceLastTime.add(Calendar.SECOND, - TIME_TO_UPDATE_SECONDS)
        val time = timeSinceLastTime.time

        val contactsSinceLastTime = realm?.covidContacts()
            ?.getSync(covidId = covidMessage.covidId, fromLastContactDate = time, fieldNames = arrayOf("lastContactDate", "covidId"), sortOrders = arrayOf(
                Sort.DESCENDING, Sort.DESCENDING))



        var covidContact:CovidContact?

        val date = Date()
        if(contactsSinceLastTime.isNullOrEmpty()){
            Log.d(TAG, "No contact in DB for id: "+covidMessage.covidId)
            //There is no contact, so we create one
            covidContact = CovidContact()
            covidContact.covidId = covidMessage.covidId
            covidContact.firstContactDate = date
            covidContact.lastContactDate = date
            covidContact.contactTimeInSeconds = 0

            val locationCovidContact = LocationCovidContact()

            locationCovidContact.latitude = 0.0
            locationCovidContact.longitude = 0.0
            locationCovidContact.date = date

            if(covidMessage.deviceSignal!=null) {
                locationCovidContact.calculatedDistance =
                    BluetoothUtils.calculateAccuracy(
                        covidMessage.deviceSignal!!.txPower,
                        covidMessage.deviceSignal!!.rssi
                    )
                locationCovidContact.rssi = covidMessage.deviceSignal!!.rssi
                locationCovidContact.txPower = covidMessage.deviceSignal!!.txPower
            }
            covidContact.locations?.add(locationCovidContact)
            realm?.covidContacts()?.putAsync(covidContact)
        } else {

            covidContact = realm!!.copyFromRealm(contactsSinceLastTime.first()!!)
            Log.d(TAG, "Contact exist in DB for id: "+covidMessage.covidId)
            val secondsSinceLastUpdate =
                TimeUnit.MILLISECONDS.toSeconds(date.time - covidContact.lastContactDate!!.time)
            Log.d(TAG, "Contact was seen: "+secondsSinceLastUpdate+" seconds ago")

                Log.d(TAG, "We try to update contact: "+covidContact.covidId)
                // we update the contact and add a new location
                covidContact?.lastContactDate = date

                covidContact.contactTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(covidContact.lastContactDate!!.time - covidContact.firstContactDate!!.time)

                val locationCovidContact = LocationCovidContact()
                if(covidMessage.deviceSignal!=null) {
                    locationCovidContact.calculatedDistance =
                        BluetoothUtils.calculateAccuracy(
                            covidMessage.deviceSignal!!.txPower,
                            covidMessage.deviceSignal!!.rssi
                        )
                    locationCovidContact.rssi = covidMessage.deviceSignal!!.rssi
                    locationCovidContact.txPower = covidMessage.deviceSignal!!.txPower
                }
                locationCovidContact.date = date
                covidContact?.locations?.add(locationCovidContact)
                realm?.covidContacts()?.updateSync(covidContact)

        }
        realm?.close()


    }

    companion object{
        private val TAG = BLEServerService::class.java.simpleName
        private val TIME_TO_UPDATE_SECONDS=5*60// 5 minutes
        private val MESSAGE_ACTION_START_BLE_SERVER=0
        private val MESSAGE_ACTION_RESTART_BLE_SERVER=1
        private val MESSAGE_ACTION_STOP_BLE_SERVER=2
        private val MESSAGE_ACTION_SEND_MESSAGE_SERVER=3
        private val MESSAGE_ACTION_BLE_SERVER_CHECK_STATUS=4
    }
}