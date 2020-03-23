package com.altaureum.covid.tracking.realm.utils

import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.realm.data.CovidContact
import java.io.File


import io.realm.Realm
import io.realm.RealmConfiguration


object RealmUtils {

    private val TAG = RealmUtils::class.java.simpleName
    val SCHEMA_VERSION:Long = 4

    private fun getRealmConfig(context: Context):RealmConfiguration{
        val configurationBuilder = getRealmConfigurationBuilder(context.filesDir, context.getString(
            R.string.pref_default_realm_file_name))
        return configurationBuilder.build()
    }


    @Throws(SecurityException::class)
    private fun getRealmConfigurationBuilder(folder: File, filename: String): RealmConfiguration.Builder {

        //Realm is not currently supporting inter process encryption

        return RealmConfiguration.Builder()
                .directory(folder)
                .name(filename)
                .modules(Realm.getDefaultModule(), CovidContact())
                .schemaVersion(SCHEMA_VERSION)
                .deleteRealmIfMigrationNeeded()


    }


    fun getInstance(context: Context):Realm?{
        return Realm.getInstance(getRealmConfig(context))
    }
}
