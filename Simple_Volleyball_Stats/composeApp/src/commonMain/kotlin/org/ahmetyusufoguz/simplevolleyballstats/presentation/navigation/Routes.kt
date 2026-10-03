package org.ahmetyusufoguz.simplevolleyballstats.presentation.navigation

sealed class Route(val path: String) {
    object Home : Route("home")
    object TeamDetail : Route("team/{teamId}") {
        fun createPath(teamId: String) = "team/$teamId"
    }
    object AddPlayer : Route("team/{teamId}/add-player") {
        fun createPath(teamId: String) = "team/$teamId/add-player"
    }
    object Match : Route("match/{teamId}?matchId={matchId}") {
        fun createPath(teamId: String, matchId: String? = null): String {
            return if (matchId != null) {
                "match/$teamId?matchId=$matchId"
            } else {
                "match/$teamId"
            }
        }
    }
    /** Çift takım modu - ayrı bir ekran */
    object DualTeamMatch : Route("dual-match/{homeTeamId}/{awayTeamId}") {
        fun createPath(homeTeamId: String, awayTeamId: String) =
            "dual-match/$homeTeamId/$awayTeamId"
    }
    /** Maç raporu ekranı */
    object MatchReport : Route("report/{matchId}") {
        fun createPath(matchId: String) = "report/$matchId"
    }
    /** Takım listesi sayfası — Ana sayfa'dan erişim */
    object TeamsList : Route("teams")
    object Settings : Route("settings")
}