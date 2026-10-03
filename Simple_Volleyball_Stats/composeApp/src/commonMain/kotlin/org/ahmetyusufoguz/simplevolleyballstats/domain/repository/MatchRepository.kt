package org.ahmetyusufoguz.simplevolleyballstats.domain.repository

import kotlinx.coroutines.flow.Flow
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Action
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Match

interface MatchRepository {
    // Match operations
    suspend fun createMatch(match: Match)
    suspend fun getMatchById(id: String): Match?
    suspend fun getMatchesByTeam(teamId: String): Flow<List<Match>>
    suspend fun getAllMatches(): Flow<List<Match>>
    suspend fun getMatchCountByTeam(teamId: String): Long
    suspend fun updateMatch(match: Match)
    suspend fun updateMatchState(match: Match, courtPositionsJson: String, liberoId: String?, liberoOnCourt: Boolean, liberoReplacedPlayerId: String?)
    /** DualTeam modunda takım adı ve ID'lerini günceller (awayTeamId boş gelir, sonradan set edilir) */
    suspend fun updateMatchTeamInfo(matchId: String, awayTeamId: String?, awayTeamName: String, homeTeamName: String)
    suspend fun deleteMatch(id: String)

    // Set results
    suspend fun saveSetResult(matchId: String, setNumber: Int, homeScore: Int, awayScore: Int)
    suspend fun getSetResults(matchId: String): List<SetResult>

    // Action operations
    suspend fun addAction(action: Action)
    suspend fun getActionsByMatch(matchId: String): Flow<List<Action>>
    suspend fun getActionsByMatchAndSet(matchId: String, setNumber: Int): List<Action>
    suspend fun getActionsByPlayer(playerId: String): Flow<List<Action>>
    suspend fun deleteAction(actionId: String)
    suspend fun deleteLastAction(matchId: String)

    // Statistics
    suspend fun getActionStatsByPlayer(playerId: String): List<ActionStat>
    suspend fun getActionStatsByMatch(matchId: String): List<PlayerActionStat>
}

data class SetResult(
    val setNumber: Int,
    val homeScore: Int,
    val awayScore: Int
)

data class ActionStat(
    val actionType: String,
    val result: String,
    val count: Int
)

data class PlayerActionStat(
    val playerId: String,
    val actionType: String,
    val result: String,
    val count: Int
)