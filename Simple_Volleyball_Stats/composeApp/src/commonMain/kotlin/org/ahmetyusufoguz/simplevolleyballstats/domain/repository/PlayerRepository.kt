package org.ahmetyusufoguz.simplevolleyballstats.domain.repository

import kotlinx.coroutines.flow.Flow
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Player

interface PlayerRepository {
    suspend fun addPlayer(player: Player)
    suspend fun getPlayersByTeam(teamId: String): Flow<List<Player>>
    suspend fun getPlayerById(id: String): Player?
    suspend fun updatePlayer(player: Player)
    suspend fun deletePlayer(id: String)
    suspend fun getPlayerCount(teamId: String): Int
}