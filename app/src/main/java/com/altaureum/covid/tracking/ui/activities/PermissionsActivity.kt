package com.altaureum.covid.tracking.ui.activities

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.altaureum.covid.tracking.MyApplication
import com.altaureum.covid.tracking.MyApplication.Companion.context
import com.altaureum.covid.tracking.R
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class PermissionsActivity: AppCompatActivity(), HasAndroidInjector {

    private var mBluetoothAdapter: BluetoothAdapter? = null

    @Inject
    open lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any>? {
        return fragmentDispatchingAndroidInjector
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
    }


    override fun onResume() {
        super.onResume()
        // Check low energy support
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) { // Get a newer device
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show()
            finish()
        } else{
            val hasPermissions = hasPermissions()
            if(hasPermissions){
                onPermissionsGranted()
            }
        }
    }

    override fun onDestroy() {

        super.onDestroy()
    }

    private fun hasPermissions(): Boolean {
        if (mBluetoothAdapter == null || !mBluetoothAdapter!!.isEnabled) {
            requestBluetoothEnable()
            return false
        } else {
            return checkPermissions()
        }
        return true
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //   Permissions
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    private fun requestBluetoothEnable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        Log.d(TAG, "Requested user enables Bluetooth. Try starting the scan again.")
    }

    private fun checkPermissions(): Boolean {
        //request permission

        val permissions= java.util.ArrayList<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasPermissionBackgroundLocation = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermissionBackgroundLocation) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

            }
        }

        val hasPermissionLocation = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermissionLocation) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val hasPermissionCoarseLocation = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermissionCoarseLocation) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissions.isNotEmpty()) {
                val arrayOf = permissions.toTypedArray()
                requestPermissions(arrayOf, REQUEST_PERMISSIONS)
                return false
            } else {
                return true
            }
        } else {
            return true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSIONS ->{
                val permissionsDenied = java.util.ArrayList<String>()
                val permissionsGranted = java.util.ArrayList<String>()
                permissions.forEachIndexed { index, element ->
                    if(grantResults[index] != PackageManager.PERMISSION_GRANTED){
                        permissionsDenied.add(element)
                    } else {
                        permissionsGranted.add(element)
                    }
                }
                for(permission in permissionsDenied){
                    when (permission){
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION->{
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle(getString(R.string.permission_title))
                                .setMessage(R.string.permission_message_after_deny)
                                .setPositiveButton(android.R.string.ok) { dialog, which ->
                                    dialog.dismiss()
                                }
                                .show()
                        }
                    }
                }

                if(permissionsGranted.isNotEmpty()){
                    onPermissionsGranted()
                }
            }
            else ->{
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }


    fun onPermissionsGranted(){
        (applicationContext as MyApplication).startServer()
        (applicationContext as MyApplication).startClient()
        finish()
    }


    companion object{
        val TAG =PermissionsActivity::class.java.simpleName
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_PERMISSIONS = 2
    }
}