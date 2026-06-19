package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Athlete(
    val id: String? = null,
    val name: String,
    val category: String,
    val photo_url: String? = null,
    val birth_date: String? = null,
    val license_number: String? = null,
    val phone: String? = null,
    val status: String = "active"
) : java.io.Serializable
