package fr.geobert.radis.data

import android.os.Parcel
import kotlin.properties.getValue
import kotlin.properties.setValue


public class ExportCol(s: String = "") : ImplParcelable {
    override val parcels = hashMapOf<String, Any?>()

    var label: String by parcels
    var toExport: Boolean by parcels

    constructor(p: Parcel) : this() {
        readFromParcel(p)
    }

    init {
        label = s
        toExport = true
    }
}
