package com.altaureum.covid.tracking.util

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.altaureum.covid.tracking.common.Constants

import java.util.*

object BluetoothUtils {
    // Characteristics
    fun findCharacteristics(bluetoothGatt: BluetoothGatt): List<BluetoothGattCharacteristic> {
        val matchingCharacteristics: MutableList<BluetoothGattCharacteristic> = ArrayList()
        val serviceList = bluetoothGatt.services
        val service = findService(serviceList) ?: return matchingCharacteristics
        val characteristicList = service.characteristics
        for (characteristic in characteristicList) {
            if (isMatchingCharacteristic(characteristic)) {
                matchingCharacteristics.add(characteristic)
            }
        }
        return matchingCharacteristics
    }

    fun findEchoCharacteristic(bluetoothGatt: BluetoothGatt): BluetoothGattCharacteristic? {
        return findCharacteristic(bluetoothGatt, Constants.CHARACTERISTIC_ECHO_STRING)
    }

    private fun findCharacteristic(bluetoothGatt: BluetoothGatt, uuidString: String): BluetoothGattCharacteristic? {
        val serviceList = bluetoothGatt.services
        val service = findService(serviceList) ?: return null
        val characteristicList = service.characteristics
        for (characteristic in characteristicList) {
            if (characteristicMatches(characteristic, uuidString)) {
                return characteristic
            }
        }
        return null
    }

    fun isEchoCharacteristic(characteristic: BluetoothGattCharacteristic?): Boolean {
        return characteristicMatches(characteristic, Constants.CHARACTERISTIC_ECHO_STRING)
    }

    private fun characteristicMatches(characteristic: BluetoothGattCharacteristic?, uuidString: String): Boolean {
        if (characteristic == null) {
            return false
        }
        val uuid = characteristic.uuid
        return uuidMatches(uuid.toString(), uuidString)
    }

    private fun isMatchingCharacteristic(characteristic: BluetoothGattCharacteristic?): Boolean {
        if (characteristic == null) {
            return false
        }
        val uuid = characteristic.uuid
        return matchesCharacteristicUuidString(uuid.toString())
    }

    private fun matchesCharacteristicUuidString(characteristicIdString: String): Boolean {
        return uuidMatches(characteristicIdString, Constants.CHARACTERISTIC_ECHO_STRING)
    }

    fun requiresResponse(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
    }

    fun requiresConfirmation(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == BluetoothGattCharacteristic.PROPERTY_INDICATE
    }

    // Service
    private fun matchesServiceUuidString(serviceIdString: String): Boolean {
        return uuidMatches(serviceIdString, Constants.SERVICE_STRING)
    }

    private fun findService(serviceList: List<BluetoothGattService>): BluetoothGattService? {
        for (service in serviceList) {
            val serviceIdString = service.uuid
                    .toString()
            if (matchesServiceUuidString(serviceIdString)) {
                return service
            }
        }
        return null
    }

    // String matching
// If manually filtering, substring to match:
// 0000XXXX-0000-0000-0000-000000000000
    private fun uuidMatches(uuidString: String, vararg matches: String): Boolean {
        for (match in matches) {
            if (uuidString.equals(match, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun calculateAccuracy(txPower: Int, rssi: Int): Double {
        if (rssi == 0) {
            return -1.0 // if we cannot determine accuracy, return -1.
        }
        val ratio = rssi*1.0/txPower.toDouble()
        val distance = (0.89976)*Math.pow(ratio, 7.7095)+0.111
        //return Math.pow(10.0, (txPower.toDouble() - rssi) / (10 * 2))
        return distance/1000000
    }
}