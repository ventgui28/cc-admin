package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String? = null,
    val full_name: String? = null,
    val avatar_url: String? = null,
    val role: String? = "editor",
    val phone: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val birth_date: String? = null,
    val gender: String? = null,
    val updated_at: String? = null,
    val created_at: String? = null,
    val language: String? = null,
    val theme: String? = null
) : java.io.Serializable
