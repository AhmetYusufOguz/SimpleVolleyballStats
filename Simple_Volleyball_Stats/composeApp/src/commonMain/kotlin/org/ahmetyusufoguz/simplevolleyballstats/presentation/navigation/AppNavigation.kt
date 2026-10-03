package org.ahmetyusufoguz.simplevolleyballstats.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.AppLanguage
import org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.home.HomeScreen
import org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.home.TeamsListScreen
import org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.match.DualTeamMatchScreen
import org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.match.MatchScreen
import org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.report.MatchReportRoute
import org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.settings.SettingsScreen
import org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.team.AddPlayerScreen
import org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.team.TeamDetailScreen
import org.ahmetyusufoguz.simplevolleyballstats.presentation.theme.AppTheme

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onThemeChanged: (AppTheme) -> Unit = {},
    onLanguageChanged: (AppLanguage) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.path
    ) {
        // ─── Ana Sayfa ───
        composable(Route.Home.path) {
            HomeScreen(
                onTeamClick = { teamId ->
                    navController.navigate(Route.TeamDetail.createPath(teamId))
                },
                onTeamsListClick = {
                    navController.navigate(Route.TeamsList.path)
                },
                onStartSingleMatch = { teamId ->
                    navController.navigate(Route.Match.createPath(teamId))
                },
                onStartDualMatch = { homeTeamId, awayTeamId ->
                    navController.navigate(Route.DualTeamMatch.createPath(homeTeamId, awayTeamId))
                },
                onSettingsClick = {
                    navController.navigate(Route.Settings.path)
                }
            )
        }

        // ─── Takım Listesi ───
        composable(Route.TeamsList.path) {
            TeamsListScreen(
                onTeamClick = { teamId ->
                    navController.navigate(Route.TeamDetail.createPath(teamId))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── Takım Detayı ───
        composable(
            route = Route.TeamDetail.path,
            arguments = listOf(navArgument("teamId") { type = NavType.StringType })
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: return@composable
            TeamDetailScreen(
                teamId = teamId,
                onBackClick = { navController.popBackStack() },
                onAddPlayerClick = {
                    navController.navigate(Route.AddPlayer.createPath(teamId))
                },
                onStartMatchClick = {
                    navController.navigate(Route.Match.createPath(teamId))
                },
                onContinueMatchClick = { matchId ->
                    navController.navigate(Route.Match.createPath(teamId, matchId))
                },
                onMatchReportClick = { matchId ->
                    navController.navigate(Route.MatchReport.createPath(matchId))
                }
            )
        }

        // ─── Oyuncu Ekleme ───
        composable(
            route = Route.AddPlayer.path,
            arguments = listOf(navArgument("teamId") { type = NavType.StringType })
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: return@composable
            AddPlayerScreen(
                teamId = teamId,
                onBackClick = { navController.popBackStack() },
                onPlayerAdded = { navController.popBackStack() }
            )
        }

        // ─── Tek Takım Maçı ───
        composable(
            route = Route.Match.path,
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("matchId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: return@composable
            val matchId = backStackEntry.arguments?.getString("matchId")
            MatchScreen(
                teamId = teamId,
                matchId = matchId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── Çift Takım Maçı ───
        composable(
            route = Route.DualTeamMatch.path,
            arguments = listOf(
                navArgument("homeTeamId") { type = NavType.StringType },
                navArgument("awayTeamId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val homeTeamId = backStackEntry.arguments?.getString("homeTeamId") ?: return@composable
            val awayTeamId = backStackEntry.arguments?.getString("awayTeamId") ?: return@composable
            DualTeamMatchScreen(
                homeTeamId = homeTeamId,
                awayTeamId = awayTeamId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── Maç Raporu ───
        composable(
            route = Route.MatchReport.path,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: return@composable
            val scope = rememberCoroutineScope()
            val shareManager: org.ahmetyusufoguz.simplevolleyballstats.domain.service.ShareManager = org.koin.compose.koinInject()
            
            MatchReportRoute(
                matchId = matchId,
                onBackClick = { navController.popBackStack() },
                onShareReport = { report ->
                    scope.launch {
                        try {
                            val formattedText = org.ahmetyusufoguz.simplevolleyballstats.domain.service.ReportFormatter.formatToShareableText(report)
                            shareManager.shareText(formattedText)
                        } catch (e: Exception) {
                            println("Share Error: ${e.message}")
                        }
                    }
                }
            )
        }

        // ─── Ayarlar ───
        composable(Route.Settings.path) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onThemeChanged = onThemeChanged,
                onLanguageChanged = onLanguageChanged
            )
        }
    }
}