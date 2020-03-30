package com.altaureum.covid.tracking.realm.data

import android.os.Parcel
import android.os.Parcelable
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class LocationCovidContact:Parcelable, RealmObject {

    @PrimaryKey
    var uuid: String = UUID.randomUUID().toString()
    var date: Date? = null
    var latitude = 0.0
    var longitude = 0.0

    var contactLatitude = 0.0
    var contactLongitude = 0.0

    var calculatedDistance = 0.0
    var rssi =0
    var txPower =0


    constructor() {
        date = Date()
    }

    constructor(covidContact: LocationCovidContact):this() {
        this.date = if(covidContact.date!=null) Date(covidContact.date!!.time) else null
        this.latitude = covidContact.latitude
        this.longitude = covidContact.longitude
        this.contactLatitude = covidContact.contactLatitude
        this.contactLongitude = covidContact.contactLongitude
        this.calculatedDistance = covidContact.calculatedDistance
        this.rssi = covidContact.rssi
        this.txPower = covidContact.txPower
    }

    constructor(input: Parcel):this() {
        this.uuid = if (input.readInt() == 1) input.readString()!! else UUID.randomUUID().toString()
        this.date = if (input.readInt() == 1) Date(input.readLong()) else Date()
        this.latitude = input.readDouble()
        this.longitude = input.readDouble()
        this.contactLatitude = input.readDouble()
        this.contactLongitude = input.readDouble()
        this.calculatedDistance = input.readDouble()
        this.rssi = input.readInt()
        this.txPower = input.readInt()

    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(if (this.uuid != null) 1 else 0)
        if (uuid != null) {
            dest.writeString(uuid)
        }

        dest.writeInt(if (this.date != null) 1 else 0)
        if (date != null) {
            dest.writeLong(date!!.time)
        }

        dest.writeDouble(latitude)
        dest.writeDouble(longitude)
        dest.writeDouble(contactLatitude)
        dest.writeDouble(contactLongitude)
        dest.writeDouble(calculatedDistance)

        dest.writeInt(rssi)
        dest.writeInt(txPower)
    }



    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<LocationCovidContact> {
            override fun createFromParcel(pc: Parcel): LocationCovidContact {
                return LocationCovidContact(pc)
            }

            override fun newArray(size: Int): Array<LocationCovidContact?> {
                return arrayOfNulls(size)
            }
        }
    }

}