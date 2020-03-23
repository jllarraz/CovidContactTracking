package com.altaureum.covid.tracking.realm.data

import android.os.Parcel
import android.os.Parcelable
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

class CovidContact:Parcelable, RealmObject {

    @PrimaryKey
    var uuid: String = UUID.randomUUID().toString()
    var contactDate: Date? = null
    var latitude = 0.0
    var longitude = 0.0
    var calculatedDistance = 0.0
    var covidId: String? = null
    var uploadStatus: String = Upload_Status.NOT_UPLOADED.name

    var keyword: String? = null
    var uploadingAttemptDate: Date? = null

    val uploadStatusEnum:Upload_Status
        get() {
            try {
                return Upload_Status.valueOf(uploadStatus!!)
            } catch (e: Exception) {
                return Upload_Status.NOT_UPLOADED
            }
        }

    constructor() {
        contactDate = Date()
        uploadStatus = Upload_Status.NOT_UPLOADED.name
    }


    constructor(covidId: String):this() {
        this.covidId = covidId
    }

    constructor(vehicle: CovidContact):this() {
        this.contactDate = if(vehicle.contactDate!=null) Date(vehicle.contactDate!!.time) else null
        this.latitude = vehicle.latitude
        this.longitude = vehicle.longitude
        this.covidId = vehicle.covidId

        this.uploadStatus = vehicle.uploadStatus
        this.keyword = vehicle.keyword
        this.uploadingAttemptDate = if(vehicle.uploadingAttemptDate!=null) Date(vehicle.uploadingAttemptDate!!.time) else null
    }
    enum class Upload_Status{
        NOT_UPLOADED,
        UPLOADING,
        UPLOADED,
    }

    constructor(input: Parcel):this() {
        this.uuid = if (input.readInt() == 1) input.readString()!! else UUID.randomUUID().toString()
        this.contactDate = if (input.readInt() == 1) Date(input.readLong()) else Date()
        this.latitude = input.readDouble()
        this.longitude = input.readDouble()
        this.longitude = input.readDouble()
        this.covidId = if (input.readInt() == 1) input.readString() else null
        this.uploadStatus = if (input.readInt() == 1) input.readString()!! else Upload_Status.NOT_UPLOADED.name
        this.keyword = if (input.readInt() == 1) input.readString() else null
        this.uploadingAttemptDate = if (input.readInt() == 1) Date(input.readLong()) else Date()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(if (this.uuid != null) 1 else 0)
        if (uuid != null) {
            dest.writeString(uuid)
        }

        dest.writeInt(if (this.contactDate != null) 1 else 0)
        if (contactDate != null) {
            dest.writeLong(contactDate!!.time)
        }

        dest.writeDouble(latitude)
        dest.writeDouble(longitude)
        dest.writeDouble(calculatedDistance)

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