package com.altaureum.covid.tracking.realm.data

import android.os.Parcel
import android.os.Parcelable
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*
import kotlin.collections.ArrayList

open class CovidContact:Parcelable, RealmObject {

    @PrimaryKey
    var uuid: String = UUID.randomUUID().toString()
    var firstContactDate: Date? = null
    var lastContactDate: Date? = null
    var locations:RealmList<LocationCovidContact>?=null

    var covidId: String? = null
    var uploadStatus: String = Upload_Status.NOT_UPLOADED.name

    var keyword: String? = null
    var uploadingAttemptDate: Date? = null
    //Jus for convenience in DB
    var contactTimeInSeconds:Long=0

    val uploadStatusEnum:Upload_Status
        get() {
            try {
                return Upload_Status.valueOf(uploadStatus!!)
            } catch (e: Exception) {
                return Upload_Status.NOT_UPLOADED
            }
        }

    constructor() {
        firstContactDate = Date()
        lastContactDate = Date()
        uploadStatus = Upload_Status.NOT_UPLOADED.name
        locations = RealmList()
        contactTimeInSeconds=0
    }


    constructor(covidId: String):this() {
        this.covidId = covidId
    }

    constructor(covidContact: CovidContact):this() {
        this.firstContactDate = if(covidContact.firstContactDate!=null) Date(covidContact.firstContactDate!!.time) else null
        this.lastContactDate = if(covidContact.lastContactDate!=null) Date(covidContact.lastContactDate!!.time) else null
        if(covidContact.locations!=null){
            for (location in covidContact.locations!!){
                this.locations!!.add(LocationCovidContact(location))
            }
        }
        this.covidId = covidContact.covidId

        this.uploadStatus = covidContact.uploadStatus
        this.keyword = covidContact.keyword
        this.uploadingAttemptDate = if(covidContact.uploadingAttemptDate!=null) Date(covidContact.uploadingAttemptDate!!.time) else null
        this.contactTimeInSeconds = covidContact.contactTimeInSeconds
    }
    enum class Upload_Status{
        NOT_UPLOADED,
        UPLOADING,
        UPLOADED,
    }

    constructor(input: Parcel):this() {
        this.uuid = if (input.readInt() == 1) input.readString()!! else UUID.randomUUID().toString()
        this.firstContactDate = if (input.readInt() == 1) Date(input.readLong()) else Date()
        this.lastContactDate = if (input.readInt() == 1) Date(input.readLong()) else Date()

        if(input.readInt() ==  1){
            val temp = input.readArrayList(LocationCovidContact::class.java.classLoader)
            (locations as RealmList<LocationCovidContact>).addAll((temp as ArrayList<LocationCovidContact>).toTypedArray())
        }
        this.covidId = if (input.readInt() == 1) input.readString() else null
        this.uploadStatus = if (input.readInt() == 1) input.readString()!! else Upload_Status.NOT_UPLOADED.name
        this.keyword = if (input.readInt() == 1) input.readString() else null
        this.uploadingAttemptDate = if (input.readInt() == 1) Date(input.readLong()) else Date()
        this.contactTimeInSeconds=input.readLong()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(if (this.uuid != null) 1 else 0)
        if (uuid != null) {
            dest.writeString(uuid)
        }

        dest.writeInt(if (this.firstContactDate != null) 1 else 0)
        if (firstContactDate != null) {
            dest.writeLong(firstContactDate!!.time)
        }

        dest.writeInt(if (this.lastContactDate != null) 1 else 0)
        if (lastContactDate != null) {
            dest.writeLong(lastContactDate!!.time)
        }

        dest.writeInt(if (this.locations != null) 1 else 0)
        if (locations != null) {
            dest.writeList(locations as List<LocationCovidContact>)
        }

        dest.writeInt(if (this.covidId != null) 1 else 0)
        if (covidId != null) {
            dest.writeString(covidId)
        }


        dest.writeInt(if (this.uploadStatus != null) 1 else 0)
        if (uploadStatus != null) {
            dest.writeString(uploadStatus)
        }

        dest.writeInt(if (this.keyword != null) 1 else 0)
        if (keyword != null) {
            dest.writeString(keyword)
        }

        dest.writeInt(if (this.uploadingAttemptDate != null) 1 else 0)
        if (uploadingAttemptDate != null) {
            dest.writeLong(uploadingAttemptDate!!.time)
        }

        dest.writeLong(contactTimeInSeconds)
    }



    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<CovidContact> {
            override fun createFromParcel(pc: Parcel): CovidContact {
                return CovidContact(pc)
            }

            override fun newArray(size: Int): Array<CovidContact?> {
                return arrayOfNulls(size)
            }
        }
    }

}