package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Team
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.MatchRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.TeamRepository

class HomeViewModel(
    private val teamRepository: TeamRepository,
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        handleIntent(HomeIntent.LoadTeams)
    }

    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadTeams -> loadTeams()
            is HomeIntent.ShowCreateDialog -> showCreateDialog()
            is HomeIntent.HideCreateDialog -> hideCreateDialog()
            is HomeIntent.CreateTeam -> createTeam(intent.name)
            is HomeIntent.DeleteTeam -> deleteTeam(intent.teamId)
        }
    }

    private fun loadTeams() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            teamRepository.getAllTeams()
                .catch { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
                .collect { teams ->
                    val teamsWithStats = teams.map { team ->
                        val playerCount = playerRepository.getPlayerCount(team.id)
                        // Maç sayısını veritabanından oku
                        val matchCount = try {
                            matchRepository.getMatchCountByTeam(team.id).toInt()
                        } catch (e: Exception) { 0 }
                        TeamWithStats(
                            team = team,
                            playerCount = playerCount,
                            matchCount = matchCount
                        )
                    }
                    _state.update { it.copy(teams = teamsWithStats, isLoading = false) }
                }
        }
    }

    private fun showCreateDialog() {
        _state.update { it.copy(showCreateDialog = true) }
    }

    private fun hideCreateDialog() {
        _state.update { it.copy(showCreateDialog = false) }
    }

    private fun createTeam(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            val team = Team.create(name)
            teamRepository.createTeam(team)
            hideCreateDialog()
        }
    }

    private fun deleteTeam(teamId: String) {
        viewModelScope.launch {
            teamRepository.deleteTeam(teamId)
        }
    }
}