package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SocialPost(
    val id: String? = null,
    val title: String,
    val content: String,
    val image_url: String? = null,
    val tags: List<String> = emptyList(),
    val created_at: String? = null
) : java.io.Serializable
