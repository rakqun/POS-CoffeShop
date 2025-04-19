package com.example.dayeat.model

data class Menu (
    var menuId: Int? = null,
    var menuCode: String? = null,
    var menuName: String? = null,
    var catId: Int? = null,
    var price: Int? = null,
    var netPrice: Int? = null,
    var flag: String? = null,
    var prefs: String? = null,
    var includeTax: Int? = null,
    var includeChg: Int? = null,
    var taxAndChg: Int? = null
)