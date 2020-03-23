package com.altaureum.covid.tracking.ui.activities

import android.Manifest
import android.app.Activity
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
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.realm.data.CovidContact
import com.altaureum.covid.tracking.ui.activities.client.ClientActivity
import com.altaureum.covid.tracking.ui.fragments.FragmentContacts
import com.altaureum.covid.tracking.ui.fragments.FragmentRegistry
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector

import javax.inject.Inject

class RegistryActivity : AppCompatActivity(), HasAndroidInjector, FragmentRegistry.OnListFragmentInteractionListener {


    private var mBluetoothAdapter: BluetoothAdapter? = null

    @Inject
    open lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any>? {
        return fragmentDispatchingAndroidInjector
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_list)
        AndroidInjection.inject(this)
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        if(savedInstanceState==null){
            selectFragmentRegistry()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check low energy support
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) { // Get a newer device
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show()
            finish()
        } else{
            hasPermissions()
        }
    }

    override fun onDestroy() {

        super.onDestroy()
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
        startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT)
        Log.d(TAG, "Requested user enables Bluetooth. Try starting the scan again.")
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
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FINE_LOCATION
            )
        }

    }


    override fun onContinue() {
        val intentResponse = Intent()
        setResult(Activity.RESULT_OK, intentResponse)
        finish()
    }


    protected fun selectFragmentRegistry() {
        val fragmentManager = supportFragmentManager
        val ft = fragmentManager.beginTransaction()

        val fragment = FragmentRegistry.newInstance()
        ft.replace(R.id.fragment, fragment, FragmentRegistry::class.java.simpleName)
        ft.commit()
    }

    companion object{
        val TAG =RegistryActivity::class.java.simpleName
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_FINE_LOCATION = 2
    }




}