package com.altaureum.covid.tracking.services.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.altaureum.covid.tracking.MyApplication.Companion.context
import com.altaureum.covid.tracking.common.Actions
import com.altaureum.covid.tracking.common.Preferences

open class BootServiceStarter: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val defaultSharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val covidId = defaultSharedPreferences.getString(Preferences.KEY_COVID_ID, null)
        if(covidId!=null) {
            // only start it if covid Id is not null
            startTracker()
        }
    }

    fun startTracker(){
        try {
            val localBroadcastManager = LocalBroadcastManager.getInstance(context!!)
            val intentRequest = Intent(Actions.ACTION_START_TRACKER)
            localBroadcastManager.sendBroadcast(intentRequest)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}