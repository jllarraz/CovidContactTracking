package com.altaureum.covid.tracking.realm.utils


import com.altaureum.covid.tracking.realm.data.CovidContactDao
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmResults


fun Realm.covidContacts() : CovidContactDao = CovidContactDao(this)

fun <T: RealmModel> RealmResults<T>.asLiveData() = LiveRealmData<T>(this)
