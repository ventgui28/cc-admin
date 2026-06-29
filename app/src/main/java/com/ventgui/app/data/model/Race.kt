package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RaceSubStage(
    val id: String? = null,
    val race_id: String,
    val name: String,
    val stage_type: String, // e.g., "gincana", "linha", "xcc", "cronometro"
    val distance_km: Double? = null,
    val duration_minutes: Int? = null,
    val created_at: String? = null
) : java.io.Serializable

@Serializable
data class Race(
    val id: String? = null,
    val title: String,
    val date: String,
    val category: String, // e.g., Estrada, MTB
    val status: String = "Aberto", // e.g., Aberto, Pendente, Planeado
    val location: String? = null,
    val description: String? = null,
    val gender: String? = "Misto", // Masculino, Feminino, Misto
    val sub_categories: List<String> = emptyList(), // Sub-17, Elite, etc.
    val team_classification: Int? = null,
    val start_time: String? = null,
    val link: String? = null,
    val race_format: String = "Estrada", // e.g., "Encontro de Escolas", "Estrada", "BTT XCO"
    val sub_stages: List<RaceSubStage>? = null,
    val distance_km: Double? = null,
    val duration_minutes: Int? = null
) : java.io.Serializable
