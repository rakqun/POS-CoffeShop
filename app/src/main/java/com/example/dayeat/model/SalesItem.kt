package com.example.dayeat.model

import kotlinx.serialization.SerialName

data class SalesItem (
    var id: Int? = null,
    var uid: String? = null,
    var menuId: String? = null,
    var detailId: Int? = null,
    var nama: String? = null,
    var quanty: Int? = null,
    var harga: Int? = null,
    var catatan: String? = null,
    var tax: Int? = null,
    var chg: Int? = null,
    var total: Int? = null,
    var move: Boolean? = null
)