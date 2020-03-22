package com.altaureum.covid.tracking.ui.activities.client

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.common.Constants
import com.altaureum.covid.tracking.services.client.DiscoveredDevice
import com.altaureum.covid.tracking.ui.viewmodel.GattServerViewModel
import com.altaureum.covid.tracking.util.BluetoothUtils
import com.altaureum.covid.tracking.util.StringUtils
import kotlinx.android.synthetic.main.activity_client.*
import kotlinx.android.synthetic.main.view_log.*

import java.util.*

class ClientActivity : AppCompatActivity() {
    private var mScanning = false
    private var mHandler: Handler? = null
    private var mLogHandler: Handler? = null
    private var mScanResults: MutableMap<String, BluetoothDevice>? = null
    private var mConnected = false
    private var mEchoInitialized = false
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mScanCallback: ScanCallback? = null
    private var mGatt: BluetoothGatt? = null
    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)
        mLogHandler = Handler(Looper.getMainLooper())
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
       // mBinding = DataBindingUtil.setContentView(this, R.layout.activity_client)
        @SuppressLint("HardwareIds") val deviceInfo = ("Device Info"
                + "\nName: " + mBluetoothAdapter!!.getName()
                + "\nAddress: " + mBluetoothAdapter!!.getAddress())
        client_device_info_text_view.text = deviceInfo
        start_scanning_button.setOnClickListener { v: View? -> startScan() }
        stop_scanning_button.setOnClickListener { v: View? -> stopScan() }
        send_message_button.setOnClickListener { v: View? -> sendMessage() }
        disconnect_button.setOnClickListener { v: View? -> disconnectGattServer() }
        clear_log_button.setOnClickListener { v: View? -> clearLogs() }
    }

    override fun onResume() {
        super.onResume()
        // Check low energy support
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) { // Get a newer device
            logError("No LE Support.")
            finish()
        }
    }

    // Scanning
    private fun startScan() {
        if (!hasPermissions() || mScanning) {
            return
        }
        disconnectGattServer()
        server_list_container.removeAllViews()
        mScanResults = HashMap()
        mScanCallback = BtleScanCallback(mScanResults!!)
        mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
        // Note: Filtering does not work the same (or at all) on most devices. It also is unable to
// search for a mask or anything less than a full UUID.
// Unless the full UUID of the server is known, manual filtering may be necessary.
// For example, when looking for a brand of device that contains a char sequence in the UUID
        val scanFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(Constants.SERVICE_UUID))
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
        log("Started scanning.")
    }

    private fun stopScan() {
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter!!.isEnabled && mBluetoothLeScanner != null) {
            mBluetoothLeScanner!!.stopScan(mScanCallback)
            scanComplete()
        }
        mScanCallback = null
        mScanning = false
        mHandler = null
        log("Stopped scanning.")
    }

    private fun scanComplete() {
        if (mScanResults!!.isEmpty()) {
            return
        }
        for (deviceAddress in mScanResults!!.keys) {
            val device = mScanResults!![deviceAddress]
            val viewModel = GattServerViewModel(device)
            val layoutInflater = LayoutInflater.from(this)
            val binding = layoutInflater.inflate(
                    R.layout.view_gatt_server,
                    server_list_container,
                    true)
            binding.findViewById<TextView>(R.id.gatt_server_name_text_view).text = viewModel.serverName
            binding.findViewById<Button>(R.id.connect_gatt_server_button).setOnClickListener { v: View? -> connectDevice(device) }
        }
    }

    private fun hasPermissions(): Boolean {
        if (mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) {
            requestBluetoothEnable()
            return false
        } else if (!hasLocationPermissions()) {
            requestLocationPermission()
            return false
        }
        return true
    }

    private fun requestBluetoothEnable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        log("Requested user enables Bluetooth. Try starting the scan again.")
    }

    private fun hasLocationPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }

    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "Requested user enable Location. Try starting the scan again.")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
        }

    }

    // Gatt connection
    private fun connectDevice(device: BluetoothDevice?) {
        log("Connecting to " + device!!.address)
        val gattClientCallback = GattClientCallback()
        mGatt = device.connectGatt(this, false, gattClientCallback)
    }

    // Messaging
    private fun sendMessage() {
        if (!mConnected || !mEchoInitialized) {
            return
        }
        val characteristic = BluetoothUtils.findEchoCharacteristic(mGatt!!)
        if (characteristic == null) {
            logError("Unable to find echo characteristic.")
            disconnectGattServer()
            return
        }
        val message = message_edit_text.text.toString()
        log("Sending message: $message")
        val messageBytes = StringUtils.bytesFromString(message)
        if (messageBytes.size == 0) {
            logError("Unable to convert message to bytes")
            return
        }
        characteristic.value = messageBytes
        val success = mGatt!!.writeCharacteristic(characteristic)
        if (success) {
            log("Wrote: " + StringUtils.byteArrayInHexFormat(messageBytes))
        } else {
            logError("Failed to write data")
        }
    }

    // Logging
    private fun clearLogs() {
        mLogHandler!!.post { log_text_view.text = "" }
    }

    fun log(msg: String) {
        Log.d(TAG, msg)
        mLogHandler!!.post {
            log_text_view.append(msg + "\n")
            log_scroll_view.post { log_scroll_view.fullScroll(View.FOCUS_DOWN) }
        }
    }

    fun logError(msg: String) {
        log("Error: $msg")
    }

    // Gat Client Actions
    fun setConnected(connected: Boolean) {
        mConnected = connected
    }

    fun initializeEcho() {
        mEchoInitialized = true
    }

    fun disconnectGattServer() {
        log("Closing Gatt connection")
        clearLogs()
        mConnected = false
        mEchoInitialized = false
        if (mGatt != null) {
            mGatt!!.disconnect()
            mGatt!!.close()
        }
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
            logError("BLE Scan Failed with code $errorCode")
        }

        private fun addScanResult(result: ScanResult) {
            val device = result.device
            val deviceAddress = device.address
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val calculateAccuracy = calculateAccuracy(result.txPower, result.rssi)
                Log.d(TAG, "Distance:"+ calculateAccuracy)

                Toast.makeText(this@ClientActivity, "Distance: "+ calculateAccuracy, Toast.LENGTH_SHORT).show()
            }





            mScanResults[deviceAddress] = device
        }

    }

    protected fun calculateAccuracy(txPower: Int, rssi: Int): Double {
        if (rssi == 0) {
            return -1.0 // if we cannot determine accuracy, return -1.
        }
        return Math.pow(10.0, (txPower.toDouble() - rssi) / (10 * 2))
    }

    private inner class GattClientCallback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            log("onConnectionStateChange newState: $newState")
            if (status == BluetoothGatt.GATT_FAILURE) {
                logError("Connection Gatt failure status $status")
                disconnectGattServer()
                return
            } else if (status != BluetoothGatt.GATT_SUCCESS) { // handle anything not SUCCESS as failure
                logError("Connection not GATT sucess status $status")
                disconnectGattServer()
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                log("Connected to device " + gatt.device.address)
                setConnected(true)
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                log("Disconnected from device")
                disconnectGattServer()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log("Device service discovery unsuccessful, status $status")
                return
            }
            val matchingCharacteristics = BluetoothUtils.findCharacteristics(gatt)
            if (matchingCharacteristics.isEmpty()) {
                logError("Unable to find characteristics.")
                return
            }
            log("Initializing: setting write type and enabling notification")
            for (characteristic in matchingCharacteristics) {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                enableCharacteristicNotification(gatt, characteristic)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log("Characteristic written successfully")
            } else {
                logError("Characteristic write unsuccessful, status: $status")
                disconnectGattServer()
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log("Characteristic read successfully")
                readCharacteristic(characteristic)
            } else {
                logError("Characteristic read unsuccessful, status: $status")
                // Trying to read from the Time Characteristic? It doesnt have the property or permissions
// set to allow this. Normally this would be an error and you would want to:
// disconnectGattServer();
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            log("Characteristic changed, " + characteristic.uuid.toString())
            readCharacteristic(characteristic)
        }

        private fun enableCharacteristicNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val characteristicWriteSuccess = gatt.setCharacteristicNotification(characteristic, true)
            if (characteristicWriteSuccess) {
                log("Characteristic notification set successfully for " + characteristic.uuid.toString())
                if (BluetoothUtils.isEchoCharacteristic(characteristic)) {
                    initializeEcho()
                }
            } else {
                logError("Characteristic notification set failure for " + characteristic.uuid.toString())
            }
        }

        private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
            val messageBytes = characteristic.value
            log("Read: " + StringUtils.byteArrayInHexFormat(messageBytes))
            val message = StringUtils.stringFromBytes(messageBytes)
            if (message == null) {
                logError("Unable to convert bytes to string")
                return
            }
            log("Received message: $message")
        }
    }

    companion object {
        private const val TAG = "ClientActivity"
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_FINE_LOCATION = 2
    }
}