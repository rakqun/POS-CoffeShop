package com.example.dayeat.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemMove (
    @SerialName("DetailID")
    var detailID: Int? = null,
    @SerialName("SalesID")
    var salesID: Int? = null,
    @SerialName("User")
    var user: String? = null
)