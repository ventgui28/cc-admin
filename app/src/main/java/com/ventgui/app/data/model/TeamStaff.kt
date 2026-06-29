package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamStaff(
    val id: String? = null,
    val name: String,
    val role: String, // Treinador, Diretor Desportivo, Mecânico, Massagista
    val coach_level: String? = null, // Grau I, Grau II, Grau III
    val license_number: String? = null,
    val is_federated: Boolean = false,
    val phone: String? = null,
    val email: String? = null,
    val created_at: String? = null
) : java.io.Serializable
