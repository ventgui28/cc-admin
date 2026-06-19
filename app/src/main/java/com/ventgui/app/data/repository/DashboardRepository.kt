package com.ventgui.app.data.repository

import com.ventgui.app.R
import com.ventgui.app.data.model.*
import com.ventgui.app.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardRepository {

    suspend fun fetchProfile(userId: String): Profile? = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.postgrest.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<Profile>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchRaces(): List<Race> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.postgrest.from("races").select().decodeList<Race>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchAthletes(): List<Athlete> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.postgrest.from("athletes").select().decodeList<Athlete>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchRaceResults(): List<RaceResult> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.postgrest.from("race_results").select().decodeList<RaceResult>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchSocialPosts(): List<SocialPost> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.postgrest.from("social_posts")
                .select { limit(5) }
                .decodeList<SocialPost>()
                .reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchWeather(): Pair<String, Int> = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=40.3467&longitude=-8.5936&current_weather=true")
            val connection = url.openConnection() as java.net.HttpURLConnection
            val data = connection.inputStream.bufferedReader().readText()
            if (data.contains("temperature")) {
                val t = data.substringAfter("\"temperature\":").substringBefore(",").trim()
                val temp = "${t.split(".")[0]}°"
                val code = data.substringAfter("\"weathercode\":").substringBefore(",").substringBefore("}").trim().toInt()
                val weatherDescResId = when(code) {
                    0 -> R.string.dashboard_weather_clear
                    1, 2, 3 -> R.string.dashboard_weather_partly_cloudy
                    45, 48 -> R.string.dashboard_weather_foggy
                    51, 53, 55 -> R.string.dashboard_weather_drizzle
                    61, 63, 65 -> R.string.dashboard_weather_rainy
                    71, 73, 75 -> R.string.dashboard_weather_snowy
                    95, 96, 99 -> R.string.dashboard_weather_stormy
                    else -> R.string.dashboard_weather_cloudy
                }
                Pair(temp, weatherDescResId)
            } else {
                Pair("18°", R.string.dashboard_weather_cloudy)
            }
        } catch (e: Exception) {
            Pair("18°", R.string.dashboard_weather_cloudy)
        }
    }
}
