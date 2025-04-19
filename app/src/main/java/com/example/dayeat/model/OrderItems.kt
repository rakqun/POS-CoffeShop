package com.example.dayeat.model

import java.util.Date

data class OrderItems (
    var id: Int? = null,
    var uid: String? = null,
    var menuId: String? = null,
    var nama: String? = null,
    var quanty: Int? = null,
    var harga: Int? = null,
    var todayDisable: Int? = null,
    var date: String? = null,
    var disabled: Int? = null
)