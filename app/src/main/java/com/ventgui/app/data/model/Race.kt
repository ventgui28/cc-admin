package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

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
    val link: String? = null
) : java.io.Serializable
