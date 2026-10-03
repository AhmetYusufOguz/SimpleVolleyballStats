package org.ahmetyusufoguz.simplevolleyballstats.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ahmetyusufoguz.simplevolleyballstats.database.VolleyballDatabase
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Player
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.PlayerPosition
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository

class PlayerRepositoryImpl(
    private val database: VolleyballDatabase
) : PlayerRepository {

    private val queries = database.volleyballDatabaseQueries

    override suspend fun addPlayer(player: Player) = withContext(Dispatchers.IO) {
        queries.insertPlayer(
            id = player.id,
            teamId = player.teamId,
            number = player.number.toLong(),
            name = player.name,
            position = player.position?.name,
            height = player.height?.toLong(),
            weight = player.weight?.toLong(),
            notes = player.notes,
            createdAt = player.createdAt
        )
    }

    override suspend fun getPlayersByTeam(teamId: String): Flow<List<Player>> {
        return queries.selectPlayersByTeam(teamId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toPlayer() }
            }
    }

    override suspend fun getPlayerById(id: String): Player? = withContext(Dispatchers.IO) {
        queries.selectPlayerById(id).executeAsOneOrNull()?.toPlayer()
    }

    override suspend fun updatePlayer(player: Player) = withContext(Dispatchers.IO) {
        queries.updatePlayer(
            number = player.number.toLong(),
            name = player.name,
            position = player.position?.name,
            height = player.height?.toLong(),
            weight = player.weight?.toLong(),
            notes = player.notes,
            id = player.id
        )
    }

    override suspend fun deletePlayer(id: String) = withContext(Dispatchers.IO) {
        queries.deletePlayer(id)
    }

    override suspend fun getPlayerCount(teamId: String): Int = withContext(Dispatchers.IO) {
        queries.countPlayersByTeam(teamId).executeAsOne().toInt()
    }

    private fun org.ahmetyusufoguz.simplevolleyballstats.database.Player.toPlayer(): Player {
        return Player(
            id = id,
            teamId = teamId,
            number = number.toInt(),
            name = name,
            position = PlayerPosition.fromString(position),
            height = height?.toInt(),
            weight = weight?.toInt(),
            notes = notes,
            createdAt = createdAt
        )
    }
}