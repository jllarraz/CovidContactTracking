package com.altaureum.covid.tracking.realm.data


import android.util.Log
import com.altaureum.covid.tracking.realm.utils.LiveRealmData
import com.altaureum.covid.tracking.realm.utils.asLiveData
import io.reactivex.Completable
import io.reactivex.Single
import io.realm.*
import java.util.*

class CovidContactDao(val realm: Realm) {

    private fun getBaseSync(realm: Realm = this.realm,
                covidId: String?=null,
                numberOfResults:Long=-1,
                fromDate: Date? = null,
                toDate: Date?=null,
                fieldNames: Array<String>,
                sortOrders: Array<Sort>): RealmQuery<CovidContact> {
        val query = realm.where(CovidContact::class.java)

        if(numberOfResults>0) {
            query.limit(numberOfResults)
        }
        query.beginGroup()
        if (fromDate!=null) {
            query.and()
            query.lessThanOrEqualTo("contactDate", fromDate)
        }
        if (toDate!=null) {
            query.and()
            query.greaterThanOrEqualTo("contactDate", toDate)
        }

        if (covidId!=null) {
            query.and().equalTo("covidId", covidId, Case.INSENSITIVE)
        }
        query.endGroup()
        return query.sort(fieldNames, sortOrders)
    }

    private fun getBaseSync(realm: Realm = this.realm,
                    covidIds: Array<String>?=null,
                    numberOfResults:Long=-1,
                    fromDate: Date? = null,
                    toDate: Date?=null,
                    fieldNames: Array<String>,
                    sortOrders: Array<Sort>): RealmQuery<CovidContact> {
        val query = realm.where(CovidContact::class.java)

        if(numberOfResults>0) {
            query.limit(numberOfResults)
        }
        query.beginGroup()
        if (fromDate!=null) {
            query.and()
            query.lessThanOrEqualTo("contactDate", fromDate)
        }
        if (toDate!=null) {
            query.and()
            query.greaterThanOrEqualTo("contactDate", toDate)
        }

        if (covidIds!=null) {
            query.and().`in`("covidId", covidIds, Case.INSENSITIVE)
        }
        query.endGroup()
        return query.sort(fieldNames, sortOrders)
    }


    fun getSync(realm: Realm = this.realm,
                covidId: String?=null,
                numberOfResults:Long=-1,
                fromDate: Date? = null,
                toDate: Date?=null,
                fieldNames: Array<String>,
                sortOrders: Array<Sort>): RealmResults<CovidContact> {
        return getBaseSync(realm, covidId, numberOfResults, fromDate, toDate, fieldNames, sortOrders).findAll()
    }

    fun getSync(realm: Realm = this.realm,
                covidIds: Array<String>?=null,
                numberOfResults:Long=-1,
                fromDate: Date? = null,
                toDate: Date?=null,
                fieldNames: Array<String>,
                sortOrders: Array<Sort>): RealmResults<CovidContact> {
        return getBaseSync(realm, covidIds, numberOfResults, fromDate, toDate, fieldNames, sortOrders).findAll()
    }

    fun getAsync(realm: Realm = this.realm,
                     covidIds: Array<String>?=null,
                     numberOfResults:Long=-1,
                     fromDate: Date? = null,
                     toDate: Date?=null,
                     fieldNames: Array<String>,
                     sortOrders: Array<Sort>): RealmResults<CovidContact> {
        return getBaseSync(realm, covidIds, numberOfResults, fromDate, toDate, fieldNames, sortOrders).findAllAsync()
    }

    fun getAsync(realm: Realm = this.realm,
                covidId: String?=null,
                numberOfResults:Long=-1,
                fromDate: Date? = null,
                toDate: Date?=null,
                fieldNames: Array<String>,
                sortOrders: Array<Sort>): RealmResults<CovidContact> {
        return getBaseSync(realm, covidId, numberOfResults, fromDate, toDate, fieldNames, sortOrders).findAllAsync()
    }

    fun getAsyncAsLiveData(realm: Realm = this.realm,
                 covidIds: Array<String>?=null,
                 numberOfResults:Long=-1,
                 fromDate: Date? = null,
                 toDate: Date?=null,
                 fieldNames: Array<String>,
                 sortOrders: Array<Sort>): LiveRealmData<CovidContact> {
        return getAsync(realm, covidIds, numberOfResults, fromDate, toDate, fieldNames, sortOrders).asLiveData()
    }

    fun getAsyncAsLiveData(realm: Realm = this.realm,
                 covidId: String?=null,
                 numberOfResults:Long=-1,
                 fromDate: Date? = null,
                 toDate: Date?=null,
                 fieldNames: Array<String>,
                 sortOrders: Array<Sort>): LiveRealmData<CovidContact> {
        return getAsync(realm, covidId, numberOfResults, fromDate, toDate, fieldNames, sortOrders).asLiveData()
    }





    fun size(): Int {
        val results = realm.where(CovidContact::class.java)?.findAllAsync()
        if (results?.isEmpty()!!)
            return 0
        else
            return results.size

    }

    fun sizeSync(): Int {
        val results = realm.where(CovidContact::class.java)?.findAll()
        if (results?.isEmpty()!!)
            return 0
        else
            return results.size

    }


    fun updateAsync(uuids: Array<String>, uploadStatus: CovidContact.Upload_Status):Single<Boolean> {
        return Completable.create { emitter ->
            realm.executeTransactionAsync({
                val realmResults = it.where(CovidContact::class.java).`in`("uuid", uuids).findAll()
                realmResults.setValue("uploadStatus", uploadStatus.name)
                if(uploadStatus == CovidContact.Upload_Status.UPLOADING) {
                    realmResults.setValue("uploadingAttemptDate", Date())
                }
            }, { emitter.onComplete() }, { emitter.onError(it) })
        }.toSingleDefault(true)
                .onErrorReturnItem(false)
    }

    fun updateSync(uuids: Array<String>, uploadStatus: CovidContact.Upload_Status) {
        realm.executeTransaction {
            val realmResults = it.where(CovidContact::class.java).`in`("uuid", uuids).findAll()
            realmResults.setValue("uploadStatus", uploadStatus.name)
            if(uploadStatus == CovidContact.Upload_Status.UPLOADING) {
                realmResults.setValue("uploadingAttemptDate", Date())
            }
        }
    }

    fun updateSync(uuids: Array<String>, uploadingAttemptDate:Date?) {
        realm.executeTransaction {
            it.where(CovidContact::class.java).`in`("uuid", uuids).findAll().setValue("uploadingAttemptDate", uploadingAttemptDate)
        }
    }

    fun put(item: List<CovidContact>):Single<Boolean> {
        return Completable.create { emitter ->
            realm.executeTransactionAsync({
                it.insertOrUpdate(item)
            }, { emitter.onComplete() }, { emitter.onError(it) })
        }.toSingleDefault(true)
                .onErrorReturnItem(false)
    }

    fun put(item: CovidContact):Single<Boolean> {
        return Completable.create { emitter ->
                realm?.executeTransactionAsync({
                    item?.let { myItem->
                        try {
                            it?.insertOrUpdate(myItem)
                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                    }
                }, { emitter.onComplete() }, { emitter.onError(it) })
            }.toSingleDefault(true)
            .onErrorReturnItem(false)
    }

    fun putAsync(item: List<CovidContact>){
            realm.executeTransactionAsync({
                it.insertOrUpdate(item)
            })
    }

    fun putAsync(item: CovidContact){

        realm.executeTransactionAsync({
            it.insertOrUpdate(item)
        })
    }

    fun putSync(item: CovidContact) {
        realm?.executeTransaction {
            item?.let {myItem->
                try {
                    it?.insertOrUpdate(myItem)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }
    fun putSync(item: List<CovidContact>) {
        realm?.executeTransaction {
            item?.let {myItem->
                try {
                    it.insertOrUpdate(myItem)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }



    fun putSingle(item: List<CovidContact>):Single<Boolean> {
        return Completable.create { emitter ->
                realm?.executeTransactionAsync({
                    item?.let { myItem->
                        try {
                            it?.insertOrUpdate(myItem)
                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                    }
                }, { emitter.onComplete() }, { emitter.onError(it) })
            }.toSingleDefault(true)
            .onErrorReturnItem(false)
    }

    fun deleteAsync(id: String) {
        realm?.executeTransactionAsync {
            val results = it.where(CovidContact::class.java)
                    .equalTo("uuid", id)
                    .findAll()
            results.deleteAllFromRealm()
            Log.d("REALM", " delete Finish")
        }
    }

    fun deleteSync(id: String) {
        realm.executeTransaction {
            Log.d("REALM", " delete Sync Start")
            val results = it.where(CovidContact::class.java)
                    .equalTo("uuid", id)
                    .findAll()
            results.deleteAllFromRealm()
            Log.d("REALM", " delete Sync Finish")
        }
    }

    fun deleteAllSingle():Single<Boolean> {
        return Completable.create { emitter ->
            realm.executeTransactionAsync({
                val results = it.where(CovidContact::class.java).findAll()
                it.delete(CovidContact::class.java)
            }, { emitter.onComplete() }, { emitter.onError(it) })
        }.toSingleDefault(true)
                .onErrorReturnItem(false)
    }

    fun deleteAllSync() {
        realm.executeTransaction {
            val results = it.where(CovidContact::class.java).findAll()
            results.deleteAllFromRealm()
        }
    }

    fun deleteAllBefore(date: Date) {
        realm.executeTransaction {
            val results = it.where(CovidContact::class.java)
                    .lessThanOrEqualTo("contactDate", date)
                    .findAll()
            results.deleteAllFromRealm()

        }
    }
}