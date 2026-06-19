package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Sponsor(
    val id: String? = null,
    val name: String,
    val logo_url: String? = null,
    val website: String? = null,
    val level: String = "Gold" // Gold, Silver, Bronze
) : java.io.Serializable
