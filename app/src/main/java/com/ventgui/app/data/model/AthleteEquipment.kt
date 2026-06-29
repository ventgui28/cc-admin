package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AthleteEquipment(
    val id: String? = null,
    val athlete_id: String,
    val discipline: String, // Estrada, Pista, BTT
    val wheel_size: String? = null, // "24", "26", "27.5", "29", "700c"
    val front_chainring: Int, // e.g., 46
    val rear_cog: Int, // e.g., 14
    val carbon_wheels: Boolean = false,
    val rim_profile_over_65: Boolean = false,
    val disc_wheels: Boolean = false,
    val tt_handlebars: Boolean = false,
    val is_validated: Boolean = false,
    val created_at: String? = null
) : java.io.Serializable
