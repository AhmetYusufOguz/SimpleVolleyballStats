package org.ahmetyusufoguz.simplevolleyballstats.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ahmetyusufoguz.simplevolleyballstats.database.VolleyballDatabase
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Action
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.ActionResult
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.ActionType
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.BallState
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.CourtPosition
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Match
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.MatchStatus
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Rotation
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.ServingTeam
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.SetScore
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.ActionStat
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.MatchRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerActionStat
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.SetResult

class MatchRepositoryImpl(
    private val database: VolleyballDatabase
) : MatchRepository {

    private val queries = database.volleyballDatabaseQueries

    // ==================== MATCH ====================

    override suspend fun createMatch(match: Match) = withContext(Dispatchers.IO) {
        println("📝 Maç kaydediliyor: ${match.id}")
        queries.insertMatch(
            id = match.id,
            homeTeamId = match.homeTeamId,
            homeTeamName = match.homeTeamName,
            awayTeamId = match.awayTeamId,
            awayTeamName = match.awayTeamName,
            homeScore = match.homeScore.toLong(),
            awayScore = match.awayScore.toLong(),
            homeSetsWon = match.homeSetsWon.toLong(),
            awaySetsWon = match.awaySetsWon.toLong(),
            currentSet = match.currentSet.toLong(),
            status = match.status.name,
            homeServing = if (match.servingTeam == ServingTeam.HOME) 1L else 0L,
            ballState = match.ballState.name,
            courtPositionsJson = match.courtPositionsJson,
            liberoId = match.liberoId,
            liberoOnCourt = if (match.liberoOnCourt) 1L else 0L,
            liberoReplacedPlayerId = match.liberoReplacedPlayerId,
            startedAt = match.createdAt,
            endedAt = match.endedAt,
            notes = match.notes
        )
        println("✅ Maç kaydedildi: ${match.homeTeamName} vs ${match.awayTeamName}")
    }

    override suspend fun getMatchById(id: String): Match? = withContext(Dispatchers.IO) {
        val matchEntity = queries.selectMatchById(id).executeAsOneOrNull() ?: return@withContext null
        val setResults = getSetResults(id)
        matchEntity.toMatch(setResults)
    }

    override suspend fun getMatchesByTeam(teamId: String): Flow<List<Match>> {
        return queries.selectMatchesByTeam(teamId, teamId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    val setResults = withContext(Dispatchers.IO) {
                        queries.selectSetsByMatch(entity.id).executeAsList().map {
                            SetResult(it.setNumber.toInt(), it.homeScore.toInt(), it.awayScore.toInt())
                        }
                    }
                    entity.toMatch(setResults)
                }
            }
    }

    override suspend fun getAllMatches(): Flow<List<Match>> {
        return queries.selectAllMatches()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    val setResults = withContext(Dispatchers.IO) {
                        queries.selectSetsByMatch(entity.id).executeAsList().map {
                            SetResult(it.setNumber.toInt(), it.homeScore.toInt(), it.awayScore.toInt())
                        }
                    }
                    entity.toMatch(setResults)
                }
            }
    }

    override suspend fun getMatchCountByTeam(teamId: String): Long = withContext(Dispatchers.IO) {
        queries.getMatchCountByTeam(teamId, teamId).executeAsOne()
    }

    override suspend fun updateMatch(match: Match) = withContext(Dispatchers.IO) {
        queries.updateMatch(
            homeScore = match.homeScore.toLong(),
            awayScore = match.awayScore.toLong(),
            homeSetsWon = match.homeSetsWon.toLong(),
            awaySetsWon = match.awaySetsWon.toLong(),
            currentSet = match.currentSet.toLong(),
            status = match.status.name,
            homeServing = if (match.servingTeam == ServingTeam.HOME) 1L else 0L,
            ballState = match.ballState.name,
            courtPositionsJson = match.courtPositionsJson,
            liberoId = match.liberoId,
            liberoOnCourt = if (match.liberoOnCourt) 1L else 0L,
            liberoReplacedPlayerId = match.liberoReplacedPlayerId,
            endedAt = match.endedAt,
            notes = match.notes,
            id = match.id
        )
    }

    override suspend fun updateMatchState(
        match: Match,
        courtPositionsJson: String,
        liberoId: String?,
        liberoOnCourt: Boolean,
        liberoReplacedPlayerId: String?
    ) = withContext(Dispatchers.IO) {
        queries.updateMatchState(
            homeScore = match.homeScore.toLong(),
            awayScore = match.awayScore.toLong(),
            homeServing = if (match.servingTeam == ServingTeam.HOME) 1L else 0L,
            ballState = match.ballState.name,
            courtPositionsJson = courtPositionsJson,
            liberoOnCourt = if (liberoOnCourt) 1L else 0L,
            liberoReplacedPlayerId = liberoReplacedPlayerId,
            id = match.id
        )
    }

    override suspend fun deleteMatch(id: String) = withContext(Dispatchers.IO) {
        queries.deleteMatch(id)
    }

    override suspend fun updateMatchTeamInfo(
        matchId: String,
        awayTeamId: String?,
        awayTeamName: String,
        homeTeamName: String
    ) = withContext(Dispatchers.IO) {
        queries.updateMatchTeamInfo(
            awayTeamId   = awayTeamId,
            awayTeamName = awayTeamName,
            homeTeamName = homeTeamName,
            id           = matchId
        )
    }

    // ==================== SET RESULTS ====================

    override suspend fun saveSetResult(
        matchId: String,
        setNumber: Int,
        homeScore: Int,
        awayScore: Int
    ) = withContext(Dispatchers.IO) {
        val id = "set_${matchId}_$setNumber"
        queries.insertSetResult(
            id = id,
            matchId = matchId,
            setNumber = setNumber.toLong(),
            homeScore = homeScore.toLong(),
            awayScore = awayScore.toLong()
        )
    }

    override suspend fun getSetResults(matchId: String): List<SetResult> = withContext(Dispatchers.IO) {
        queries.selectSetsByMatch(matchId).executeAsList().map {
            SetResult(
                setNumber = it.setNumber.toInt(),
                homeScore = it.homeScore.toInt(),
                awayScore = it.awayScore.toInt()
            )
        }
    }

    // ==================== ACTIONS ====================

    override suspend fun addAction(action: Action) = withContext(Dispatchers.IO) {
        println("📝 Aksiyon kaydediliyor: ${action.actionType.name} - ${action.result.name}")
        queries.insertAction(
            id = action.id,
            matchId = action.matchId,
            playerId = action.playerId,
            teamId = action.teamId,
            setNumber = action.setNumber.toLong(),
            homeScore = action.homeScore.toLong(),
            awayScore = action.awayScore.toLong(),
            courtPosition = action.courtPosition.name,
            actionType = action.actionType.name,
            result = action.result.name,
            timestamp = action.timestamp,
            notes = action.notes
        )
    }

    override suspend fun getActionsByMatch(matchId: String): Flow<List<Action>> {
        return queries.selectActionsByMatch(matchId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toAction() } }
    }

    override suspend fun getActionsByMatchAndSet(matchId: String, setNumber: Int): List<Action> =
        withContext(Dispatchers.IO) {
            queries.selectActionsByMatchAndSet(matchId, setNumber.toLong())
                .executeAsList()
                .map { it.toAction() }
        }

    override suspend fun getActionsByPlayer(playerId: String): Flow<List<Action>> {
        return queries.selectActionsByPlayer(playerId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toAction() } }
    }

    override suspend fun deleteAction(actionId: String) = withContext(Dispatchers.IO) {
        queries.deleteAction(actionId)
    }

    override suspend fun deleteLastAction(matchId: String) = withContext(Dispatchers.IO) {
        queries.deleteLastAction(matchId)
    }

    // ==================== STATISTICS ====================

    override suspend fun getActionStatsByPlayer(playerId: String): List<ActionStat> =
        withContext(Dispatchers.IO) {
            queries.selectActionStatsByPlayer(playerId).executeAsList().map {
                ActionStat(
                    actionType = it.actionType,
                    result = it.result,
                    count = it.count_.toInt()
                )
            }
        }

    override suspend fun getActionStatsByMatch(matchId: String): List<PlayerActionStat> =
        withContext(Dispatchers.IO) {
            queries.selectActionStatsByMatch(matchId).executeAsList().map {
                PlayerActionStat(
                    playerId = it.playerId,
                    actionType = it.actionType,
                    result = it.result,
                    count = it.count_.toInt()
                )
            }
        }

    // ==================== MAPPERS ====================

    private fun org.ahmetyusufoguz.simplevolleyballstats.database.Match.toMatch(
        setResults: List<SetResult>
    ): Match {
        val setScores = if (setResults.isEmpty()) {
            listOf(SetScore(homeScore.toInt(), awayScore.toInt()))
        } else {
            setResults.map { SetScore(it.homeScore, it.awayScore) }
        }

        return Match(
            id = id,
            homeTeamId = homeTeamId,
            awayTeamId = awayTeamId,
            homeTeamName = homeTeamName,
            awayTeamName = awayTeamName,
            status = MatchStatus.valueOf(status),
            currentSet = currentSet.toInt(),
            homeSetsWon = homeSetsWon.toInt(),
            awaySetsWon = awaySetsWon.toInt(),
            setScores = setScores,
            homeRotation = Rotation.empty(),
            awayRotation = Rotation.empty(),
            servingTeam = if (homeServing == 1L) ServingTeam.HOME else ServingTeam.AWAY,
            ballState = try { BallState.valueOf(ballState) } catch (e: Exception) { BallState.HOME_SERVE },
            courtPositionsJson = courtPositionsJson,
            liberoId = liberoId,
            liberoOnCourt = liberoOnCourt == 1L,
            liberoReplacedPlayerId = liberoReplacedPlayerId,
            createdAt = startedAt,
            endedAt = endedAt,
            notes = notes
        )
    }

    private fun org.ahmetyusufoguz.simplevolleyballstats.database.Action.toAction(): Action {
        return Action(
            id = id,
            matchId = matchId,
            playerId = playerId,
            teamId = teamId,
            setNumber = setNumber.toInt(),
            homeScore = homeScore.toInt(),
            awayScore = awayScore.toInt(),
            courtPosition = CourtPosition.valueOf(courtPosition),
            actionType = ActionType.valueOf(actionType),
            result = parseActionResult(result),
            timestamp = timestamp,
            notes = notes
        )
    }

    /**
     * ActionResult parse — eski enum isimlerini yeni DataVolley isimlerine dönüştürür.
     * DB'de eski kayıtlar (ACE, KILL vb.) varsa çökmemesi için fallback mapping.
     */
    private fun parseActionResult(value: String): ActionResult {
        return try {
            ActionResult.valueOf(value)
        } catch (e: IllegalArgumentException) {
            // Eski isimlerden yeni isimlere dönüşüm
            when (value) {
                "ACE" -> ActionResult.SERVE_PERFECT
                "SERVICE_IN_PLAY" -> ActionResult.SERVE_NEUTRAL
                "SERVICE_ERROR" -> ActionResult.SERVE_ERROR
                "RECEIVE_PERFECT" -> ActionResult.RECEIVE_PERFECT
                "RECEIVE_GOOD" -> ActionResult.RECEIVE_POSITIVE
                "RECEIVE_ERROR" -> ActionResult.RECEIVE_ERROR
                "KILL" -> ActionResult.ATTACK_PERFECT
                "SPIKE_DEFENDED" -> ActionResult.ATTACK_NEGATIVE
                "SPIKE_BLOCKED" -> ActionResult.ATTACK_BLOCKED
                "SPIKE_ERROR" -> ActionResult.ATTACK_ERROR
                "BLOCK_KILL" -> ActionResult.BLOCK_PERFECT
                "BLOCK_CONTROL" -> ActionResult.BLOCK_POSITIVE
                "BLOCK_TOUCH" -> ActionResult.BLOCK_NEUTRAL
                "BLOCK_ERROR" -> ActionResult.BLOCK_ERROR
                "DIG_PERFECT" -> ActionResult.DIG_PERFECT
                "DIG_PLAYABLE" -> ActionResult.DIG_POSITIVE
                "DIG_ERROR" -> ActionResult.DIG_ERROR
                "SET_PERFECT" -> ActionResult.SET_PERFECT
                "SET_GOOD" -> ActionResult.SET_POSITIVE
                "SET_ERROR" -> ActionResult.SET_ERROR
                else -> {
                    println("⚠️ Bilinmeyen ActionResult: $value — varsayılan SERVE_NEUTRAL kullanılıyor")
                    ActionResult.SERVE_NEUTRAL
                }
            }
        }
    }
}