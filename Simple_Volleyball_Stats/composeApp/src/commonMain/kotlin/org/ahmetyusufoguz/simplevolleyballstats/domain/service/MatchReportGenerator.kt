package org.ahmetyusufoguz.simplevolleyballstats.domain.service

import org.ahmetyusufoguz.simplevolleyballstats.domain.model.*

/**
 * DataVolley standardına uygun maç istatistik raporu oluşturur.
 *
 * Rapor içeriği:
 * - Oyuncu bazlı istatistikler (her aksiyon tipi × kalite)
 * - Takım toplam istatistikleri
 * - Set skorları
 * - Etkinlik ve verimlilik hesaplamaları
 */
class MatchReportGenerator {

    /**
     * Verilen aksiyonlardan tam bir maç raporu üretir.
     */
    fun generateReport(
        match: Match,
        actions: List<Action>,
        players: List<Player>,
        setScores: List<Pair<Int, Int>>
    ): MatchReport {
        val playerStats = generatePlayerStats(actions, players)
        val teamStats = generateTeamStats(actions)

        return MatchReport(
            matchId = match.id,
            homeTeamName = match.homeTeamName,
            awayTeamName = match.awayTeamName,
            homeSetsWon = match.homeSetsWon,
            awaySetsWon = match.awaySetsWon,
            setScores = setScores,
            playerStats = playerStats,
            teamStats = teamStats,
            totalActions = actions.size,
            matchDurationMs = (match.endedAt ?: 0L) - match.createdAt
        )
    }

    /**
     * Her oyuncu için DataVolley formatında istatistik hesaplar.
     */
    private fun generatePlayerStats(
        actions: List<Action>,
        players: List<Player>
    ): List<PlayerMatchStats> {
        return players.map { player ->
            val playerActions = actions.filter { it.playerId == player.id }
            PlayerMatchStats(
                player = player,
                serve = calculateActionStats(playerActions, ActionType.SERVE),
                reception = calculateActionStats(playerActions, ActionType.RECEIVE),
                attack = calculateActionStats(playerActions, ActionType.SPIKE),
                block = calculateActionStats(playerActions, ActionType.BLOCK),
                dig = calculateActionStats(playerActions, ActionType.DIG),
                set = calculateActionStats(playerActions, ActionType.SET),
                totalPoints = playerActions.count { it.result.pointValue > 0 },
                totalErrors = playerActions.count { it.result.pointValue < 0 }
            )
        }.filter { stats ->
            // Hiç aksiyonu olmayan oyuncuları filtrele
            stats.serve.total + stats.reception.total + stats.attack.total +
            stats.block.total + stats.dig.total + stats.set.total > 0
        }
    }

    /**
     * Takım toplamı istatistik hesaplar.
     */
    private fun generateTeamStats(actions: List<Action>): TeamMatchStats {
        // TEAM (genel sayı) aksiyonlarını hariç tut
        val playerActions = actions.filter { it.playerId != "TEAM" }

        return TeamMatchStats(
            serve = calculateActionStats(playerActions, ActionType.SERVE),
            reception = calculateActionStats(playerActions, ActionType.RECEIVE),
            attack = calculateActionStats(playerActions, ActionType.SPIKE),
            block = calculateActionStats(playerActions, ActionType.BLOCK),
            dig = calculateActionStats(playerActions, ActionType.DIG),
            set = calculateActionStats(playerActions, ActionType.SET),
            totalPoints = actions.count { it.result.pointValue > 0 },
            totalErrors = actions.count { it.result.pointValue < 0 }
        )
    }

    /**
     * Tek bir aksiyon tipi için DataVolley sembol bazlı istatistik hesaplar.
     */
    private fun calculateActionStats(
        actions: List<Action>,
        actionType: ActionType
    ): ActionStats {
        val filtered = actions.filter { it.actionType == actionType }
        if (filtered.isEmpty()) return ActionStats()

        val perfect = filtered.count { it.result.dataVolleySymbol == "#" }  // Mükemmel
        val positive = filtered.count { it.result.dataVolleySymbol == "+" } // Pozitif
        val neutral = filtered.count { it.result.dataVolleySymbol == "!" }  // Nötr
        val negative = filtered.count { it.result.dataVolleySymbol == "-" } // Negatif
        val overpass = filtered.count { it.result.dataVolleySymbol == "/" } // Overpass
        val error = filtered.count { it.result.dataVolleySymbol == "=" }    // Hata

        val total = filtered.size

        // Etkinlik (Efficiency): (# + + - = - /) / Total * 100
        // Verimlilik (Positivity): (# + +) / Total * 100
        val efficiency = if (total > 0) {
            ((perfect + positive - error - overpass).toFloat() / total * 100)
        } else 0f

        val positivity = if (total > 0) {
            ((perfect + positive).toFloat() / total * 100)
        } else 0f

        return ActionStats(
            total = total,
            perfect = perfect,
            positive = positive,
            neutral = neutral,
            negative = negative,
            overpass = overpass,
            error = error,
            efficiency = efficiency,
            positivity = positivity
        )
    }
}

// ─── Veri Modelleri ───

/**
 * Tam maç raporu.
 */
data class MatchReport(
    val matchId: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val homeSetsWon: Int,
    val awaySetsWon: Int,
    val setScores: List<Pair<Int, Int>>,
    val playerStats: List<PlayerMatchStats>,
    val teamStats: TeamMatchStats,
    val totalActions: Int,
    val matchDurationMs: Long
)

/**
 * Oyuncu bazlı maç istatistikleri.
 */
data class PlayerMatchStats(
    val player: Player,
    val serve: ActionStats,
    val reception: ActionStats,
    val attack: ActionStats,
    val block: ActionStats,
    val dig: ActionStats,
    val set: ActionStats,
    val totalPoints: Int,
    val totalErrors: Int
)

/**
 * Takım toplamı istatistikleri.
 */
data class TeamMatchStats(
    val serve: ActionStats = ActionStats(),
    val reception: ActionStats = ActionStats(),
    val attack: ActionStats = ActionStats(),
    val block: ActionStats = ActionStats(),
    val dig: ActionStats = ActionStats(),
    val set: ActionStats = ActionStats(),
    val totalPoints: Int = 0,
    val totalErrors: Int = 0
)

/**
 * DataVolley sembol bazlı aksiyon istatistikleri.
 *
 * Her aksiyon tipi için (#, +, !, -, /, =) dağılımı ve
 * etkinlik/verimlilik yüzdeleri.
 */
data class ActionStats(
    val total: Int = 0,
    /** # — Mükemmel (Ace, Kill, Stuff Block, vb.) */
    val perfect: Int = 0,
    /** + — Pozitif */
    val positive: Int = 0,
    /** ! — Nötr */
    val neutral: Int = 0,
    /** - — Negatif */
    val negative: Int = 0,
    /** / — Overpass / Bloklanma */
    val overpass: Int = 0,
    /** = — Hata */
    val error: Int = 0,
    /** Etkinlik: (# + + - = - /) / Total × 100 */
    val efficiency: Float = 0f,
    /** Verimlilik: (# + +) / Total × 100 */
    val positivity: Float = 0f
)
