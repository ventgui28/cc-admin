package com.ventgui.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BirthdayNotificationLog(
    val id: String? = null,
    val athlete_id: String,
    val notification_year: Int,
    val days_before: Int,
    val sent_at: String? = null
)
