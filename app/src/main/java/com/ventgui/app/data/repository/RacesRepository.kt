package com.ventgui.app.data.repository

import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.Race
import com.ventgui.app.data.model.RaceResult
import com.ventgui.app.data.model.RaceResultUpdate
import com.ventgui.app.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.Instant

class RacesRepository {

    suspend fun getRaces(): List<Race> = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("races").select()
            .decodeList<Race>()
            .sortedByDescending { it.date }
    }

    suspend fun getAthletes(): List<Athlete> = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("athletes").select()
            .decodeList<Athlete>()
            .sortedBy { it.name }
    }

    suspend fun getRaceResults(raceId: String): List<RaceResult> = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("race_results")
            .select { filter { eq("race_id", raceId) } }
            .decodeList<RaceResult>()
    }

    suspend fun insertRace(race: Race) = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("races").insert(race)
    }

    suspend fun createRaceWithAthletes(race: Race, athleteIds: Set<String>): Race = withContext(Dispatchers.IO) {
        val createdRace = SupabaseClient.client.postgrest.from("races")
            .insert(race) { select() }
            .decodeSingle<Race>()
        
        val results = athleteIds.map { athleteId ->
            RaceResult(race_id = createdRace.id!!, athlete_id = athleteId, position = null, time = null)
        }
        if (results.isNotEmpty()) {
            SupabaseClient.client.postgrest.from("race_results").insert(results)
        }
        createdRace
    }

    suspend fun updateRace(race: Race) = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("races").update(race) {
            filter { eq("id", race.id!!) }
        }
    }

    suspend fun updateRaceWithAthletes(race: Race, athleteIds: Set<String>) = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("races").update(race) {
            filter { eq("id", race.id!!) }
        }
        
        SupabaseClient.client.postgrest.from("race_results").delete {
            filter { eq("race_id", race.id!!) }
        }
        
        val results = athleteIds.map { athleteId ->
            RaceResult(race_id = race.id!!, athlete_id = athleteId, position = null, time = null)
        }
        if (results.isNotEmpty()) {
            SupabaseClient.client.postgrest.from("race_results").insert(results)
        }
    }

    suspend fun deleteRace(raceId: String) = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("races").delete {
            filter { eq("id", raceId) }
        }
    }

    suspend fun deleteRaceResults(raceId: String, athleteIds: List<String>) = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("race_results").delete {
            filter {
                eq("race_id", raceId)
                isIn("athlete_id", athleteIds)
            }
        }
    }

    suspend fun updateRaceResult(raceId: String, athleteId: String, update: RaceResultUpdate) = withContext(Dispatchers.IO) {
        SupabaseClient.client.postgrest.from("race_results").update(update) {
            filter {
                eq("race_id", raceId)
                eq("athlete_id", athleteId)
            }
        }
    }

    suspend fun fetchWeatherForDate(dateStr: String): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val instant = parseFlexibleDate(dateStr)
            if (instant != null) {
                val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                val url = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=40.3467&longitude=-8.5936&daily=temperature_2m_max,weathercode&start_date=$date&end_date=$date&timezone=auto")
                val connection = url.openConnection() as java.net.HttpURLConnection
                val data = connection.inputStream.bufferedReader().readText()
                if (data.contains("temperature_2m_max")) {
                    val t = data.substringAfter("\"temperature_2m_max\":[").substringBefore("]").trim()
                    val temp = "${t.split(".")[0]}°"
                    val code = data.substringAfter("\"weathercode\":[").substringBefore("]").trim().toInt()
                    val desc = when(code) {
                        0 -> "Céu Limpo"
                        1, 2, 3 -> "Parcialmente Nublado"
                        45, 48 -> "Nevoeiro"
                        61, 63, 65 -> "Chuvoso"
                        else -> "Nublado"
                    }
                    Pair(temp, desc)
                } else {
                    Pair("18°", "Nublado")
                }
            } else {
                Pair("18°", "Nublado")
            }
        } catch (e: Exception) {
            Pair("18°", "Met. Indisponível")
        }
    }

    private fun parseFlexibleDate(dateStr: String): Instant? {
        if (dateStr.isBlank()) return null
        return try {
            Instant.parse(dateStr)
        } catch (e: Exception) {
            try {
                val parts = dateStr.split(" ")
                if (parts.size >= 3) {
                    val day = parts[0].toInt()
                    val monthName = parts[1].lowercase().replaceFirstChar { it.uppercase() }
                    val year = parts[2].toInt()
                    val month = java.time.Month.valueOf(monthName.uppercase())
                    java.time.LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant()
                } else null
            } catch (e2: Exception) {
                null
            }
        }
    }
}
