package com.lakelobster.trainerspin

import android.os.Parcel
import android.os.Parcelable

class BtScanHolder : Parcelable {
    val Address: String
    val FriendlyName: String

    constructor(
        address: String,
        friendlyName: String
    ) {
        Address = address
        FriendlyName = friendlyName

    }

    constructor(parcel: Parcel) {
        Address = parcel.readString()!!
        FriendlyName = parcel.readString()!!

    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(Address)
        dest.writeString(FriendlyName)

    }

    val DisplayName: String
        get() {
            return String.format("%s - %s", FriendlyName, Address.replace(":", ""))
        }


    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BtScanHolder> {
        override fun createFromParcel(parcel: Parcel): BtScanHolder {
            return BtScanHolder(parcel)
        }

        override fun newArray(size: Int): Array<BtScanHolder?> {
            return arrayOfNulls(size)
        }
    }
}