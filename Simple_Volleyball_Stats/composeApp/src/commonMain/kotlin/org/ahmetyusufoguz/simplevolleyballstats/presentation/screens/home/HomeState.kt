package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.home

import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Team

data class TeamWithStats(
    val team: Team,
    val playerCount: Int = 0,
    val matchCount: Int = 0
)

data class HomeState(
    val teams: List<TeamWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false
)

sealed interface HomeIntent {
    object LoadTeams : HomeIntent
    object ShowCreateDialog : HomeIntent
    object HideCreateDialog : HomeIntent
    data class CreateTeam(val name: String) : HomeIntent
    data class DeleteTeam(val teamId: String) : HomeIntent
}