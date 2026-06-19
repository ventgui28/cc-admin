package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DetailedResult(
    val athleteName: String,
    val athletePhotoUrl: String?,
    val raceTitle: String,
    val raceCategory: String,
    val raceDate: String,
    val raceLocation: String?,
    val position: Int,
    val time: String? = null
) : java.io.Serializable
