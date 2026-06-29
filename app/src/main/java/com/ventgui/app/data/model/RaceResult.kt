package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RaceResult(
    val id: String? = null,
    val race_id: String,
    val athlete_id: String,
    val position: Int? = null,
    val time: String? = null,
    val category_at_time: String? = null,
    val created_at: String? = null,
    val penalty_seconds: Int = 0,
    val pedagogical_status: String? = null,
    val team_points: Int? = null,
    val bib_number: String? = null,
    val race_sub_stage_id: String? = null
) : java.io.Serializable

@Serializable
data class JoinedRaceResult(
    val id: String? = null,
    val race_id: String,
    val athlete_id: String,
    val position: Int? = null,
    val time: String? = null,
    val category_at_time: String? = null,
    val created_at: String? = null,
    val races: Race? = null,
    val penalty_seconds: Int = 0,
    val pedagogical_status: String? = null,
    val team_points: Int? = null,
    val bib_number: String? = null,
    val race_sub_stage_id: String? = null
) : java.io.Serializable
