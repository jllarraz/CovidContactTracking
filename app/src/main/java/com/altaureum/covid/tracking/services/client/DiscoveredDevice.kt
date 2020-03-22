package com.altaureum.covid.tracking.services.client

import android.bluetooth.BluetoothDevice
import java.util.*

class DiscoveredDevice {

    var device: BluetoothDevice? = null
    var lastUpdated: Date? = null
    var isTryingToConnect = false

}