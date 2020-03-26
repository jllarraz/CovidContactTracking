package com.altaureum.covid.tracking.services.client

import android.bluetooth.BluetoothDevice
import com.altaureum.covid.tracking.services.data.DeviceSignal
import java.util.*

class DiscoveredDevice {

    var device: BluetoothDevice? = null
    var lastUpdated: Date? = null
    var isTryingToConnect = false
    var isConnected = false

    var deviceSignal:DeviceSignal?=null


}