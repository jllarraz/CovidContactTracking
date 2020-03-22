package com.altaureum.covid.tracking.ui.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable


class GattServerViewModel(private val mBluetoothDevice: BluetoothDevice?) : BaseObservable() {
    @get:Bindable
    val serverName: String
        get() = if (mBluetoothDevice == null) {
            ""
        } else mBluetoothDevice.address

}