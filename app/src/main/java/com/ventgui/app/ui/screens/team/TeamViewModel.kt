package com.ventgui.app.ui.screens.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.repository.TeamRepository
import com.ventgui.app.data.utils.UserLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeamUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val athletes: List<Athlete> = emptyList(),
    val error: String? = null
)

class TeamViewModel(
    private val repository: TeamRepository = TeamRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    fun loadAthletes(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            try {
                val list = repository.getAthletes()
                _uiState.value = TeamUiState(
                    isLoading = false,
                    isRefreshing = false,
                    athletes = list
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

    fun saveAthlete(athlete: Athlete, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (athlete.id == null) {
                    repository.insertAthlete(athlete)
                    UserLogger.log("Adicionou o atleta ${athlete.name}", "Categoria: ${athlete.category}")
                } else {
                    repository.updateAthlete(athlete)
                    UserLogger.log("Atualizou o atleta ${athlete.name}", "Categoria: ${athlete.category}")
                }
                loadAthletes()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteAthlete(athlete: Athlete, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteAthlete(athlete.id!!)
                UserLogger.log("Eliminou o atleta ${athlete.name}")
                loadAthletes()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}
