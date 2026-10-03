package org.ahmetyusufoguz.simplevolleyballstats.domain.repository

import kotlinx.coroutines.flow.Flow
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Team

interface TeamRepository {
    suspend fun createTeam(team: Team)
    suspend fun getAllTeams(): Flow<List<Team>>
    suspend fun getTeamById(id: String): Team?
    suspend fun deleteTeam(id: String)
}