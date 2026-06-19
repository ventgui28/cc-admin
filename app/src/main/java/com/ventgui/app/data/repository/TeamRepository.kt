package com.ventgui.app.data.repository

import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TeamRepository {

    suspend fun getAthletes(): List<Athlete> = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("athletes")
            .select()
            .decodeList<Athlete>()
            .sortedBy { it.name }
    }

    suspend fun insertAthlete(athlete: Athlete) = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("athletes").insert(athlete)
    }

    suspend fun updateAthlete(athlete: Athlete) = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("athletes").update(athlete) {
            filter { eq("id", athlete.id!!) }
        }
    }

    suspend fun deleteAthlete(athleteId: String) = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("athletes").delete {
            filter { eq("id", athleteId) }
        }
    }
}
