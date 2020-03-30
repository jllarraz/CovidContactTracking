package com.altaureum.covid.tracking.common

import java.util.*

object Constants {
    var SERVICE_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE93E"
    var SERVICE_UUID = UUID.fromString(SERVICE_STRING)
    var CHARACTERISTIC_ECHO_STRING = "7D2EBAAD-F7BD-485A-BD9D-92AD6ECFE93E"
    var CHARACTERISTIC_ECHO_UUID = UUID.fromString(CHARACTERISTIC_ECHO_STRING)
    const val SCAN_PERIOD: Long = 29*60*1000 // 29 minutes
}