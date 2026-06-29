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
    val status: String = "active",
    val encarregado_educacao_nome: String? = null,
    val encarregado_educacao_contacto: String? = null,
    val termo_responsabilidade_assinado: Boolean? = false,
    val termo_responsabilidade_url: String? = null,
    val emd_validade: String? = null
) : java.io.Serializable
