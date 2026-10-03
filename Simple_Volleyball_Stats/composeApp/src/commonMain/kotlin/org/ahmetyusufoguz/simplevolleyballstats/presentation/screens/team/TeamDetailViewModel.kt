package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Match
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Player
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Team
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.MatchRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.TeamRepository

data class TeamDetailState(
    val team: Team? = null,
    val players: List<Player> = emptyList(),
    val matches: List<Match> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TeamDetailViewModel(
    private val teamId: String,
    private val teamRepository: TeamRepository,
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeamDetailState())
    val state: StateFlow<TeamDetailState> = _state.asStateFlow()

    init {
        loadTeam()
        loadPlayers()
        loadMatches()
    }

    private fun loadTeam() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val team = teamRepository.getTeamById(teamId)
            _state.update { it.copy(team = team, isLoading = false) }
        }
    }

    private fun loadPlayers() {
        viewModelScope.launch {
            playerRepository.getPlayersByTeam(teamId).collect { players ->
                _state.update { it.copy(players = players) }
            }
        }
    }

    private fun loadMatches() {
        viewModelScope.launch {
            matchRepository.getMatchesByTeam(teamId).collect { matches ->
                _state.update { it.copy(matches = matches) }
            }
        }
    }

    fun deletePlayer(playerId: String) {
        viewModelScope.launch {
            playerRepository.deletePlayer(playerId)
        }
    }

    fun deleteMatch(matchId: String) {
        viewModelScope.launch {
            matchRepository.deleteMatch(matchId)
        }
    }
}