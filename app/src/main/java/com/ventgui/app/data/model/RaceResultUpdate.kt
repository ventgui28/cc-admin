package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RaceResultUpdate(
    val position: Int? = null,
    val time: String? = null
)
