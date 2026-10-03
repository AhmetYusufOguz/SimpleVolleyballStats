package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.report

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Match
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.MatchRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.service.MatchReport
import org.ahmetyusufoguz.simplevolleyballstats.domain.service.MatchReportGenerator

import org.koin.compose.koinInject

/**
 * Maç raporu wrapper ekranı — matchId'den rapor verisini yükler,
 * sonra MatchReportScreen'e aktarır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchReportRoute(
    matchId: String,
    onBackClick: () -> Unit,
    onShareReport: (MatchReport) -> Unit = {}
) {
    val matchRepository: MatchRepository = koinInject()
    val playerRepository: PlayerRepository = koinInject()
    val reportGenerator: MatchReportGenerator = koinInject()
    val scope = rememberCoroutineScope()

    var report by remember { mutableStateOf<MatchReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(matchId) {
        isLoading = true
        errorMessage = null
        try {
            val loadedMatch = matchRepository.getMatchById(matchId)

            if (loadedMatch != null) {
                // Aksiyonları yükle
                val actions = matchRepository.getActionsByMatch(matchId).firstOrNull() ?: emptyList()

                // Oyuncuları yükle
                val players = playerRepository.getPlayersByTeam(loadedMatch.homeTeamId).firstOrNull() ?: emptyList()

                // Set skorlarını maç verisinden oluştur
                val setScores = loadedMatch.setScores.map { it.homeScore to it.awayScore }

                report = reportGenerator.generateReport(
                    match = loadedMatch,
                    actions = actions,
                    players = players,
                    setScores = setScores
                )
            } else {
                errorMessage = "Maç bulunamadı"
            }
        } catch (e: Exception) {
            errorMessage = "Rapor yüklenemedi: ${e.message}"
        }
        isLoading = false
    }

    when {
        isLoading -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Rapor Yükleniyor...") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        report != null -> {
            val currentReport = report!!
            MatchReportScreen(
                report = currentReport,
                onBackClick = onBackClick,
                onExportPdf = {
                    onShareReport(currentReport)
                }
            )
        }

        else -> {
            // Maç bulunamadı veya hata
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Rapor") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😕", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "Maç bulunamadı",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onBackClick) {
                            Text("Geri Dön")
                        }
                    }
                }
            }
        }
    }
}
