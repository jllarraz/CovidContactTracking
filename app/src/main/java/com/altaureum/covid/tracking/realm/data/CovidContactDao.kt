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
                fromContactDate: Date? = null,
                toContactDate: Date?=null,
                fromLastContactDate: Date? = null,
                toLastContactDate: Date?=null,
                minimumContactTimeInSeconds:Long=-1,
                fieldNames: Array<String>? = arrayOf("firstContactDate", "covidId"),
                sortOrders: Array<Sort>? = arrayOf(Sort.DESCENDING, Sort.DESCENDING)): RealmQuery<CovidContact> {
        val query = realm.where(CovidContact::class.java)

        if(numberOfResults>0) {
            query.limit(numberOfResults)
        }
       // query.beginGroup()
        if (fromContactDate!=null) {
            query.and()
            query.greaterThanOrEqualTo("firstContactDate", fromContactDate)
        }
        if (toContactDate!=null) {
            query.and()
            query.lessThanOrEqualTo("firstContactDate", toContactDate)
        }

        if (fromLastContactDate!=null) {
            query.and()
            query.greaterThanOrEqualTo("lastContactDate", fromLastContactDate)
        }
        if (toLastContactDate!=null) {
            query.and()
            query.lessThanOrEqualTo("lastContactDate", toLastContactDate)
        }

        if (covidId!=null) {
            query.and().equalTo("covidId", covidId, Case.INSENSITIVE)
        }

        if(minimumContactTimeInSeconds>-1){
            query.and()
            query.greaterThanOrEqualTo("contactTimeInSeconds", minimumContactTimeInSeconds)
        }
        //query.endGroup()
        return query.sort(fieldNames, sortOrders)
    }

    private fun getBaseSync(realm: Realm = this.realm,
                            covidIds: Array<String>?=null,
                            numberOfResults:Long=-1,
                            fromContactDate: Date? = null,
                            toContactDate: Date?=null,
                            fromLastContactDate: Date? = null,
                            toLastContactDate: Date?=null,
                            minimumContactTimeInSeconds:Long=-1,
                            fieldNames: Array<String>? = arrayOf("firstContactDate", "covidId"),
                            sortOrders: Array<Sort>? = arrayOf(Sort.DESCENDING, Sort.DESCENDING)): RealmQuery<CovidContact> {
        val query = realm.where(CovidContact::class.java)


       // query.beginGroup()
        if (fromContactDate!=null) {
            query.and()
            query.greaterThanOrEqualTo("firstContactDate", fromContactDate)
        }
        if (toContactDate!=null) {
            query.and()
            query.lessThanOrEqualTo("firstContactDate", toContactDate)
        }

        if (fromLastContactDate!=null) {
            query.and()
            query.greaterThanOrEqualTo("lastContactDate", fromLastContactDate)
        }
        if (toLastContactDate!=null) {
            query.and()
            query.lessThanOrEqualTo("lastContactDate", toLastContactDate)
        }

        if (covidIds!=null) {
            query.and().`in`("covidId", covidIds, Case.INSENSITIVE)
        }

        if(minimumContactTimeInSeconds>-1){
            query.and()
            query.greaterThanOrEqualTo("contactTimeInSeconds", minimumContactTimeInSeconds)
        }
        //query.endGroup()

        if(numberOfResults>0) {
            query.limit(numberOfResults)
        }

        return query.sort(fieldNames, sortOrders)
    }


    fun getSync(realm: Realm = this.realm,
                covidId: String?=null,
                numberOfResults:Long=-1,
                fromContactDate: Date? = null,
                toContactDate: Date?=null,
                fromLastContactDate: Date? = null,
                toLastContactDate: Date?=null,
                minimumContactTimeInSeconds:Long=-1,
                fieldNames: Array<String>? = arrayOf("firstContactDate", "covidId"),
                sortOrders: Array<Sort>? = arrayOf(Sort.DESCENDING, Sort.DESCENDING)): RealmResults<CovidContact> {
        return getBaseSync(realm, covidId, numberOfResults, fromContactDate, toContactDate, fromLastContactDate, toLastContactDate, minimumContactTimeInSeconds, fieldNames, sortOrders).findAll()
    }

    fun getSync(realm: Realm = this.realm,
                covidIds: Array<String>?=null,
                numberOfResults:Long=-1,
                fromContactDate: Date? = null,
                toContactDate: Date?=null,
                fromLastContactDate: Date? = null,
                toLastContactDate: Date?=null,
                minimumContactTimeInSeconds:Long=-1,
                fieldNames: Array<String>? = arrayOf("firstContactDate", "covidId"),
                sortOrders: Array<Sort>? = arrayOf(Sort.DESCENDING, Sort.DESCENDING)): RealmResults<CovidContact> {
        return getBaseSync(realm, covidIds, numberOfResults, fromContactDate, toContactDate, fromLastContactDate, toLastContactDate, minimumContactTimeInSeconds, fieldNames, sortOrders).findAll()
    }

    fun getAsync(realm: Realm = this.realm,
                     covidIds: Array<String>?=null,
                     numberOfResults:Long=-1,
                     fromContactDate: Date? = null,
                     toContactDate: Date?=null,
                     fromLastContactDate: Date? = null,
                     toLastContactDate: Date?=null,
                    minimumContactTimeInSeconds:Long=-1,
                     fieldNames: Array<String>? = arrayOf("firstContactDate", "covidId"),
                     sortOrders: Array<Sort>? = arrayOf(Sort.DESCENDING, Sort.DESCENDING)): RealmResults<CovidContact> {
        return getBaseSync(realm, covidIds, numberOfResults, fromContactDate, toContactDate, fromLastContactDate, toLastContactDate, minimumContactTimeInSeconds, fieldNames, sortOrders).findAllAsync()
    }

    fun getAsync(realm: Realm = this.realm,
                covidId: String?=null,
                numberOfResults:Long=-1,
                 fromContactDate: Date? = null,
                 toContactDate: Date?=null,
                 fromLastContactDate: Date? = null,
                 toLastContactDate: Date?=null,
                 minimumContactTimeInSeconds:Long=-1,
                 fieldNames: Array<String>? = arrayOf("firstContactDate", "covidId"),
                 sortOrders: Array<Sort>? = arrayOf(Sort.DESCENDING, Sort.DESCENDING)): RealmResults<CovidContact> {
        return getBaseSync(realm, covidId, numberOfResults, fromContactDate, toContactDate, fromLastContactDate, toLastContactDate, minimumContactTimeInSeconds, fieldNames, sortOrders).findAllAsync()
    }

    fun getAsyncAsLiveData(realm: Realm = this.realm,
                 covidIds: Array<String>?=null,
                 numberOfResults:Long=-1,
                   fromContactDate: Date? = null,
                   toContactDate: Date?=null,
                   fromLastContactDate: Date? = null,
                   toLastContactDate: Date?=null,
                   minimumContactTimeInSeconds:Long=-1,
                 fieldNames: Array<String>? = arrayOf("firstContactDate", "covidId"),
                 sortOrders: Array<Sort>? = arrayOf(Sort.DESCENDING, Sort.DESCENDING)): LiveRealmData<CovidContact> {
        return getAsync(realm, covidIds, numberOfResults, fromContactDate, toContactDate, fromLastContactDate, toLastContactDate, minimumContactTimeInSeconds, fieldNames, sortOrders).asLiveData()
    }

    fun getAsyncAsLiveData(realm: Realm = this.realm,
                 covidId: String?=null,
                 numberOfResults:Long=-1,
                   fromContactDate: Date? = null,
                   toContactDate: Date?=null,
                   fromLastContactDate: Date? = null,
                   toLastContactDate: Date?=null,
                   minimumContactTimeInSeconds:Long=-1,
                   fieldNames: Array<String>? = arrayOf("firstContactDate", "covidId"),
                   sortOrders: Array<Sort>? = arrayOf(Sort.DESCENDING, Sort.DESCENDING)): LiveRealmData<CovidContact> {
        return getAsync(realm, covidId, numberOfResults, fromContactDate, toContactDate, fromLastContactDate, toLastContactDate, minimumContactTimeInSeconds, fieldNames, sortOrders).asLiveData()
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

    //TODO Improve update
    fun updateSync(covidContact: CovidContact) {
        realm.executeTransaction {
            val realmResults = it.where(CovidContact::class.java).equalTo("uuid", covidContact.uuid, Case.INSENSITIVE).findAll()
            realmResults.deleteAllFromRealm()
            it.insertOrUpdate(covidContact)
        }
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
                    .lessThanOrEqualTo("firstContactDate", date)
                    .findAll()
            results.deleteAllFromRealm()

        }
    }

    fun search(search: String, fieldNames: Array<String> = arrayOf("covidId"), sortOrders: Array<Sort> = arrayOf(Sort.DESCENDING)): RealmResults<CovidContact> {
        val split = search.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val query = realm.where(CovidContact::class.java)

        query.beginGroup()
        for ((index, fieldName) in fieldNames.withIndex()) {
            query.beginGroup()
            for (term in split) {
                query.and().contains(fieldName, term, Case.INSENSITIVE)
            }
            query.endGroup()
            if(index<fieldNames.size-1){
                query.or()
            }
        }
        query.endGroup()

        return query.sort(fieldNames, sortOrders).findAllAsync()
    }
}