package org.ahmetyusufoguz.simplevolleyballstats.domain.model

import org.ahmetyusufoguz.simplevolleyballstats.utils.currentTimeMillis

data class Match(
    val id: String,
    val homeTeamId: String,
    val awayTeamId: String?,
    val homeTeamName: String,
    val awayTeamName: String,
    val status: MatchStatus,
    val currentSet: Int,
    val homeSetsWon: Int,
    val awaySetsWon: Int,
    val setScores: List<SetScore>,
    val homeRotation: Rotation,
    val awayRotation: Rotation,
    val servingTeam: ServingTeam,
    val ballState: BallState = BallState.HOME_SERVE,
    val courtPositionsJson: String? = null,
    val liberoId: String? = null,
    val liberoOnCourt: Boolean = false,
    val liberoReplacedPlayerId: String? = null,
    val createdAt: Long,
    val endedAt: Long? = null,
    val notes: String? = null
) {
    val isFinished: Boolean get() = status == MatchStatus.FINISHED

    val currentSetScore: SetScore? get() = setScores.getOrNull(currentSet - 1)

    val homeScore: Int get() = currentSetScore?.homeScore ?: 0
    val awayScore: Int get() = currentSetScore?.awayScore ?: 0

    companion object {
        fun create(
            homeTeamId: String,
            homeTeamName: String,
            awayTeamId: String? = null,
            awayTeamName: String = "Rakip Takım"
        ): Match {
            val now = currentTimeMillis()
            return Match(
                id = "match_${now}_${(0..9999).random()}",
                homeTeamId = homeTeamId,
                awayTeamId = awayTeamId,
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                status = MatchStatus.NOT_STARTED,
                currentSet = 1,
                homeSetsWon = 0,
                awaySetsWon = 0,
                setScores = listOf(SetScore()),
                homeRotation = Rotation.empty(),
                awayRotation = Rotation.empty(),
                servingTeam = ServingTeam.HOME,
                ballState = BallState.HOME_SERVE,
                createdAt = now
            )
        }
    }
}

data class SetScore(
    val homeScore: Int = 0,
    val awayScore: Int = 0
) {
    fun incrementHome() = copy(homeScore = homeScore + 1)
    fun incrementAway() = copy(awayScore = awayScore + 1)
}

enum class MatchStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    FINISHED
}

enum class ServingTeam {
    HOME, AWAY;

    fun switch(): ServingTeam = when (this) {
        HOME -> AWAY
        AWAY -> HOME
    }
}

enum class BallState {
    HOME_SERVE,      // Bizim servisimiz
    AWAY_SERVE,      // Rakip servisi
    HOME_RECEIVING,  // Rakip servis attı, bizim manşet almamız ZORUNLU
    AWAY_RECEIVING,  // Biz servis attık, rakibin manşet alması ZORUNLU
    IN_PLAY_HOME,    // Top bizde (ralli içinde, biz atak yapacağız)
    IN_PLAY_AWAY     // Top rakipte (ralli içinde, onlar atak yapacak)
}