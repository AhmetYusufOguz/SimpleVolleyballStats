package org.ahmetyusufoguz.simplevolleyballstats.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ahmetyusufoguz.simplevolleyballstats.database.VolleyballDatabase
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Team
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.TeamRepository

class TeamRepositoryImpl(
    private val database: VolleyballDatabase
) : TeamRepository {

    private val queries = database.volleyballDatabaseQueries

    override suspend fun createTeam(team: Team) = withContext(Dispatchers.IO) {
        queries.insertTeam(
            id = team.id,
            name = team.name,
            createdAt = team.createdAt
        )
    }

    override suspend fun getAllTeams(): Flow<List<Team>> {
        return queries.selectAllTeams()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    Team(
                        id = entity.id,
                        name = entity.name,
                        createdAt = entity.createdAt
                    )
                }
            }
    }

    override suspend fun getTeamById(id: String): Team? = withContext(Dispatchers.IO) {
        queries.selectTeamById(id).executeAsOneOrNull()?.let { entity ->
            Team(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt
            )
        }
    }

    override suspend fun deleteTeam(id: String) = withContext(Dispatchers.IO) {
        queries.deleteTeam(id)
    }
}