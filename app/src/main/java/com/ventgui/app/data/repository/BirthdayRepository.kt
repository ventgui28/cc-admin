package com.ventgui.app.data.repository

import com.ventgui.app.data.model.BirthdayNotificationLog
import com.ventgui.app.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BirthdayRepository {

    suspend fun wasAlreadySent(athleteId: String, year: Int, daysBefore: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = SupabaseClient.client.postgrest.from("birthday_notification_log")
                .select {
                    filter {
                        eq("athlete_id", athleteId)
                        eq("notification_year", year)
                        eq("days_before", daysBefore)
                    }
                }.decodeList<BirthdayNotificationLog>()
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAsSent(athleteId: String, year: Int, daysBefore: Int) = withContext(Dispatchers.IO) {
        try {
            val entry = BirthdayNotificationLog(
                athlete_id = athleteId,
                notification_year = year,
                days_before = daysBefore
            )
            SupabaseClient.client.postgrest.from("birthday_notification_log").insert(entry)
        } catch (e: Exception) {
            // Fail silently
        }
    }
}
