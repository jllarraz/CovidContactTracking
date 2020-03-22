package com.altaureum.covid.tracking.ui.activities.server

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity

import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.util.BluetoothUtils
import com.altaureum.covid.tracking.util.ByteUtils
import com.altaureum.covid.tracking.util.StringUtils
import kotlinx.android.synthetic.main.activity_server.*
import kotlinx.android.synthetic.main.view_log.*
import java.util.*

class ServerActivity : AppCompatActivity() {
    private var mHandler: Handler? = null
    private var mLogHandler: Handler? = null
    private var mDevices: MutableList<BluetoothDevice>? = null
    private var mGattServer: BluetoothGattServer? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        mHandler = Handler()
        mLogHandler = Handler(Looper.getMainLooper())
        mDevices = ArrayList()
        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter

        restart_server_button.setOnClickListener { v: View? -> restartServer() }
        clear_log_button.setOnClickListener { v: View? -> clearLogs() }
    }

    override fun onResume() {
        super.onResume()
        // Check if bluetooth is enabled
        if (mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) { // Request user to enable it
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
            finish()
            return
        }
        // Check low energy support
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) { // Get a newer device
            log("No LE Support.")
            finish()
            return
        }
        // Check advertising
        if (!mBluetoothAdapter!!.isMultipleAdvertisementSupported) { // Unable to run the server on this device, get a better device
            log("No Advertising Support.")
            finish()
            return
        }
        mBluetoothLeAdvertiser = mBluetoothAdapter!!.bluetoothLeAdvertiser
        val gattServerCallback = GattServerCallback()
        mGattServer = mBluetoothManager!!.openGattServer(this, gattServerCallback)
        @SuppressLint("HardwareIds") val deviceInfo = "Device Info" + "\nName: " + mBluetoothAdapter!!.name + "\nAddress: " + mBluetoothAdapter!!.address
        server_device_info_text_view.text = deviceInfo
        setupServer()
        startAdvertising()
    }

    override fun onPause() {
        super.onPause()
        stopAdvertising()
        stopServer()
    }

    // GattServer
    private fun setupServer() {
        val service = BluetoothGattService(
            Constants.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY)
        // Write characteristic
        val writeCharacteristic = BluetoothGattCharacteristic(Constants.CHARACTERISTIC_ECHO_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,  // Somehow this is not necessary, the client can still enable notifications
//                        | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE)
        service.addCharacteristic(writeCharacteristic)
        mGattServer!!.addService(service)
    }

    private fun stopServer() {
        if (mGattServer != null) {
            mGattServer!!.close()
        }
    }

    private fun restartServer() {
        stopAdvertising()
        stopServer()
        setupServer()
        startAdvertising()
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
        val parcelUuid = ParcelUuid(Constants.SERVICE_UUID)
        val data = AdvertiseData.Builder()
                .addServiceUuid(parcelUuid)
                .build()
        mBluetoothLeAdvertiser!!.startAdvertising(settings, data, mAdvertiseCallback)
    }

    private fun stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
        }
    }

    private val mAdvertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG,"Peripheral advertising started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.d(TAG,"Peripheral advertising failed: $errorCode")
        }
    }

    // Notifications
    private fun notifyCharacteristic(value: ByteArray, uuid: UUID) {
        mHandler!!.post {
            val service = mGattServer!!.getService(Constants.SERVICE_UUID)
            val characteristic = service.getCharacteristic(uuid)
            log("Notifying characteristic " + characteristic.uuid.toString()
                    + ", new value: " + StringUtils.byteArrayInHexFormat(value))
            characteristic.value = value
            val confirm = BluetoothUtils.requiresConfirmation(characteristic)
            for (device in mDevices!!) {
                mGattServer!!.notifyCharacteristicChanged(device, characteristic, confirm)
            }
        }
    }

    // Logging
    fun log(msg: String) {
        Log.d(TAG, msg)
        mLogHandler!!.post {
            log_text_view.append(msg + "\n")
            log_scroll_view.post {log_scroll_view.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun clearLogs() {
        mLogHandler!!.post { log_text_view.text = "" }
    }

    // Gatt Server Action Listener
    fun addDevice(device: BluetoothDevice) {
        log("Deviced added: " + device.address)
        mHandler!!.post { mDevices!!.add(device) }
    }

    fun removeDevice(device: BluetoothDevice) {
        log("Deviced removed: " + device.address)
        mHandler!!.post { mDevices!!.remove(device) }
    }

    fun sendResponse(device: BluetoothDevice?, requestId: Int, status: Int, offset: Int, value: ByteArray?) {
        mHandler!!.post { mGattServer!!.sendResponse(device, requestId, status, 0, null) }
    }

    private fun sendReverseMessage(message: ByteArray) {
        mHandler!!.post {
            // Reverse message to differentiate original message & response
            val response = ByteUtils.reverse(message)
            log("Sending: " + StringUtils.byteArrayInHexFormat(response))
            notifyCharacteristicEcho(response)
        }
    }

    fun notifyCharacteristicEcho(value: ByteArray) {
        notifyCharacteristic(value, Constants.CHARACTERISTIC_ECHO_UUID)
    }

    // Gatt Callback
    private inner class GattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            log("onConnectionStateChange " + device.address
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
            log("onCharacteristicReadRequest "
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
            log("onCharacteristicWriteRequest" + characteristic.uuid.toString()
                    + "\nReceived: " + StringUtils.byteArrayInHexFormat(value))
            if (Constants.CHARACTERISTIC_ECHO_UUID == characteristic.uuid) {
                sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                sendReverseMessage(value)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            super.onNotificationSent(device, status)
            log("onNotificationSent")
        }
    }

    companion object {
        private const val TAG = "ServerActivity"
    }
}