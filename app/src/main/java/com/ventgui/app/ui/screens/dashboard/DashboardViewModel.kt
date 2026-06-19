package com.ventgui.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ventgui.app.R
import com.ventgui.app.data.model.*
import com.ventgui.app.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val profile: Profile? = null,
    val nextRace: Race? = null,
    val socialPosts: List<SocialPost> = emptyList(),
    val totalAthletes: Int = 0,
    val totalRaces: Int = 0,
    val totalPodiums: Int = 0,
    val totalVictories: Int = 0,
    val podiumsList: List<DetailedResult> = emptyList(),
    val victoriesList: List<DetailedResult> = emptyList(),
    val temp: String = "18°",
    val weatherDescResId: Int = R.string.dashboard_weather_cloudy,
    val error: String? = null
)

class DashboardViewModel(
    private val repository: DashboardRepository = DashboardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard(userId: String?, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }

            try {
                val profile = userId?.let { repository.fetchProfile(it) }
                val allRaces = repository.fetchRaces()
                val allAthletes = repository.fetchAthletes()
                val allResults = repository.fetchRaceResults()
                val socialPosts = repository.fetchSocialPosts()
                val weather = repository.fetchWeather()

                // Process next race
                val today = java.time.LocalDate.now(ZoneId.systemDefault())
                val nextRace = allRaces
                    .filter { race ->
                        val instant = parseFlexibleDate(race.date)
                        if (instant != null) {
                            val raceDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                            raceDate.isAfter(today) || raceDate.isEqual(today)
                        } else false
                    }
                    .minByOrNull { parseFlexibleDate(it.date) ?: Instant.MAX }

                // Process results
                val processedResults = allResults.mapNotNull { res ->
                    if (res.position != null && res.position > 0) {
                        val athlete = allAthletes.find { it.id == res.athlete_id }
                        val race = allRaces.find { it.id == res.race_id }
                        if (athlete != null && race != null) {
                            DetailedResult(
                                athleteName = athlete.name,
                                athletePhotoUrl = athlete.photo_url,
                                raceTitle = race.title,
                                raceCategory = race.category,
                                raceDate = race.date,
                                raceLocation = race.location,
                                position = res.position,
                                time = res.time
                            )
                        } else null
                    } else null
                }

                val podiumsList = processedResults.filter { it.position in 1..3 }
                    .sortedWith(compareBy({ it.position }, { it.raceDate }))
                val victoriesList = processedResults.filter { it.position == 1 }
                    .sortedByDescending { it.raceDate }

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    isRefreshing = false,
                    profile = profile,
                    nextRace = nextRace,
                    socialPosts = socialPosts,
                    totalAthletes = allAthletes.size,
                    totalRaces = allRaces.size,
                    totalPodiums = podiumsList.size,
                    totalVictories = victoriesList.size,
                    podiumsList = podiumsList,
                    victoriesList = victoriesList,
                    temp = weather.first,
                    weatherDescResId = weather.second
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.localizedMessage ?: e.message ?: e.toString()
                )
            }
        }
    }

    internal fun parseFlexibleDate(dateStr: String): Instant? {
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
