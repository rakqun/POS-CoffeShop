package com.example.dayeat.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Parcelize
@Serializable
data class Product (
    @SerialName("id")
    var id: Int? = null,
    @SerialName("uid")
    var uid: String? = null,
    @SerialName("MenuId")
    var menuId: String? = null,
    @SerialName("DetailId")
    var detailId: Int? = null,
    @SerialName("MenuName")
    var nama: String? = null,
    @SerialName("Qt")
    var quanty: Int? = null,
    @SerialName("Price")
    var harga: Int? = null,
    @SerialName("Note")
    var catatan: String? = null,
    @SerialName("Tax")
    var tax: Int? = null,
    @SerialName("ServiceChg")
    var chg: Int? = null,
    @SerialName("Total")
    var total: Int? = null
):Parcelable