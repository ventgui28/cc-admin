package com.ventgui.app.ui.screens.races

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.Race
import com.ventgui.app.data.model.RaceResult
import com.ventgui.app.data.model.RaceResultUpdate
import com.ventgui.app.data.repository.RacesRepository
import com.ventgui.app.data.utils.UserLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RacesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val races: List<Race> = emptyList(),
    val allAthletes: List<Athlete> = emptyList(),
    val selectedRaceResults: List<Pair<RaceResult, Athlete>> = emptyList(),
    val raceWeatherTemp: String = "18°",
    val raceWeatherDesc: String = "A carregar...",
    val isFetchingDetails: Boolean = false,
    val error: String? = null
)

class RacesViewModel(
    private val repository: RacesRepository = RacesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RacesUiState())
    val uiState: StateFlow<RacesUiState> = _uiState.asStateFlow()

    suspend fun getSelectedAthleteIdsForRace(raceId: String): Set<String> {
        return try {
            repository.getRaceResults(raceId).map { it.athlete_id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun loadRacesAndAthletes(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            try {
                val racesList = repository.getRaces()
                val athletesList = repository.getAthletes()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    races = racesList,
                    allAthletes = athletesList
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

    fun fetchDetailsForRace(race: Race) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingDetails = true)
            try {
                val results = repository.getRaceResults(race.id!!)
                val athletes = repository.getAthletes()
                val weather = repository.fetchWeatherForDate(race.date)

                val mappedResults = results.mapNotNull { res ->
                    val athlete = athletes.find { it.id == res.athlete_id }
                    if (athlete != null) res to athlete else null
                }.sortedWith(compareBy {
                    val pos = it.first.position
                    val status = it.first.time
                    when {
                        pos != null && pos > 0 -> pos
                        status == "OTL" -> 10000
                        status == "DSQ" -> 10001
                        status == "DNS" -> 10002
                        status == "DNF" -> 10003
                        else -> 999999
                    }
                })

                _uiState.value = _uiState.value.copy(
                    isFetchingDetails = false,
                    selectedRaceResults = mappedResults,
                    raceWeatherTemp = weather.first,
                    raceWeatherDesc = weather.second
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFetchingDetails = false,
                    error = e.localizedMessage ?: e.message ?: e.toString()
                )
            }
        }
    }

    suspend fun handleCreateRace(race: Race, selectedAthleteIds: Set<String>): Boolean {
        require(race.id == null) { "Para criar uma prova, o id deve ser nulo" }
        return try {
            repository.createRaceWithAthletes(race, selectedAthleteIds)
            UserLogger.log("Criou a prova ${race.title}", "Categoria: ${race.category}")
            loadRacesAndAthletes()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun handleUpdateRace(race: Race, selectedAthleteIds: Set<String>): Boolean {
        require(race.id != null) { "Para atualizar uma prova, o id não deve ser nulo" }
        return try {
            repository.updateRaceWithAthletes(race, selectedAthleteIds)
            UserLogger.log("Atualizou a prova ${race.title}", "Categoria: ${race.category}")
            loadRacesAndAthletes()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun disassociateAthletes(raceId: String, athleteIds: List<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteRaceResults(raceId, athleteIds)
                val currentRace = _uiState.value.races.find { it.id == raceId }
                if (currentRace != null) {
                    fetchDetailsForRace(currentRace)
                }
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun keepAthletes(raceId: String, athleteIdsToKeep: List<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val currentResults = repository.getRaceResults(raceId)
                val allRegisteredAthletes = currentResults.map { it.athlete_id }
                val athletesToDelete = allRegisteredAthletes.filter { !athleteIdsToKeep.contains(it) }
                if (athletesToDelete.isNotEmpty()) {
                    repository.deleteRaceResults(raceId, athletesToDelete)
                }
                val currentRace = _uiState.value.races.find { it.id == raceId }
                if (currentRace != null) {
                    fetchDetailsForRace(currentRace)
                }
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun finishRace(race: Race, updatedResults: List<RaceResult>, teamClassification: Int?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.upsertRaceResults(updatedResults)
                val updatedRace = race.copy(status = "Concluída", team_classification = teamClassification)
                repository.updateRace(updatedRace)
                UserLogger.log("Concluiu a prova ${race.title}", "Resultados e classificações registados")
                loadRacesAndAthletes()
                fetchDetailsForRace(updatedRace)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteRace(race: Race, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteRace(race.id!!)
                UserLogger.log("Eliminou a prova ${race.title}")
                loadRacesAndAthletes()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun reactivateRace(race: Race, results: List<Pair<RaceResult, Athlete>>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val updatedRace = race.copy(status = "Agendada")
                repository.updateRace(updatedRace)

                // Clear positions
                results.forEach { (res, _) ->
                    repository.updateRaceResult(race.id!!, res.athlete_id, RaceResultUpdate(position = null, time = null))
                }

                UserLogger.log("Reativou a prova ${race.title}")
                loadRacesAndAthletes()
                fetchDetailsForRace(updatedRace)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}
