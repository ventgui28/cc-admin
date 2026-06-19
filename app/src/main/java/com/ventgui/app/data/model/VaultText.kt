package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VaultText(
    val id: String? = null,
    val title: String,
    val content: String,
    val created_at: String? = null
) : java.io.Serializable
