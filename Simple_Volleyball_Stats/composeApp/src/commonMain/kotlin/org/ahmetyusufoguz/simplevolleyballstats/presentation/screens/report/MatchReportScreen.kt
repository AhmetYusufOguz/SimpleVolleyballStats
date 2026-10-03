package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ahmetyusufoguz.simplevolleyballstats.domain.service.*

/**
 * Maç raporu ekranı — DataVolley standardında istatistik tablosu.
 *
 * Sekmeler:
 * 1. Genel Özet (set skorları, toplam puanlar)
 * 2. Oyuncu İstatistikleri (DataVolley tablosu)
 * 3. Takım İstatistikleri
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchReportScreen(
    report: MatchReport,
    onBackClick: () -> Unit,
    onExportPdf: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Genel Özet", "Oyuncu İstatistikleri", "Takım İstatistikleri")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${report.homeTeamName} vs ${report.awayTeamName}",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Set: ${report.homeSetsWon}-${report.awaySetsWon}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = onExportPdf) {
                        Icon(Icons.Default.Share, contentDescription = "PDF Dışa Aktar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab seçimi
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            // İçerik
            when (selectedTab) {
                0 -> OverviewTab(report)
                1 -> PlayerStatsTab(report.playerStats)
                2 -> TeamStatsTab(report.teamStats)
            }
        }
    }
}

// ─── Genel Özet Sekmesi ───

@Composable
private fun OverviewTab(report: MatchReport) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Set skorları
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Skorları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        report.setScores.forEachIndexed { index, (home, away) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Set ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$home-$away",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Toplam istatistikler
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Takım Özeti",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBox("Toplam Sayı", "${report.teamStats.totalPoints}")
                        StatBox("Toplam Hata", "${report.teamStats.totalErrors}")
                        StatBox("Toplam Aksiyon", "${report.totalActions}")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Oyuncu İstatistikleri Sekmesi ───

@Composable
private fun PlayerStatsTab(playerStats: List<PlayerMatchStats>) {
    if (playerStats.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Henüz istatistik yok")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(playerStats) { stats ->
            PlayerStatsCard(stats)
        }
    }
}

@Composable
private fun PlayerStatsCard(stats: PlayerMatchStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Oyuncu başlığı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${stats.player.number}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stats.player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${stats.totalPoints}P / ${stats.totalErrors}E",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            // DataVolley tablo
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                DataVolleyTable(stats)
            }
        }
    }
}

@Composable
private fun DataVolleyTable(stats: PlayerMatchStats) {
    val rows = listOf(
        "Servis" to stats.serve,
        "Karşılama" to stats.reception,
        "Smaç" to stats.attack,
        "Blok" to stats.block,
        "Savunma" to stats.dig,
        "Pas" to stats.set
    ).filter { it.second.total > 0 }

    Column {
        // Başlık satırı
        Row(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            TableCell("Aksiyon", 80.dp, FontWeight.Bold)
            TableCell("Toplam", 50.dp, FontWeight.Bold)
            TableCell("#", 35.dp, FontWeight.Bold)
            TableCell("+", 35.dp, FontWeight.Bold)
            TableCell("!", 35.dp, FontWeight.Bold)
            TableCell("-", 35.dp, FontWeight.Bold)
            TableCell("/", 35.dp, FontWeight.Bold)
            TableCell("=", 35.dp, FontWeight.Bold)
            TableCell("Etk%", 50.dp, FontWeight.Bold)
            TableCell("Ver%", 50.dp, FontWeight.Bold)
        }

        // Veri satırları
        rows.forEach { (name, actionStats) ->
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                TableCell(name, 80.dp)
                TableCell("${actionStats.total}", 50.dp)
                TableCell("${actionStats.perfect}", 35.dp)
                TableCell("${actionStats.positive}", 35.dp)
                TableCell("${actionStats.neutral}", 35.dp)
                TableCell("${actionStats.negative}", 35.dp)
                TableCell("${actionStats.overpass}", 35.dp)
                TableCell("${actionStats.error}", 35.dp)
                TableCell("${actionStats.efficiency.toInt()}%", 50.dp)
                TableCell("${actionStats.positivity.toInt()}%", 50.dp)
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        fontSize = 11.sp
    )
}

// ─── Takım İstatistikleri Sekmesi ───

@Composable
private fun TeamStatsTab(teamStats: TeamMatchStats) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val statsItems = listOf(
            "🏐 Servis" to teamStats.serve,
            "🙌 Karşılama" to teamStats.reception,
            "🔥 Smaç" to teamStats.attack,
            "🛡️ Blok" to teamStats.block,
            "💪 Savunma" to teamStats.dig,
            "🎯 Pas" to teamStats.set
        )

        items(statsItems) { (name, actionStats) ->
            TeamActionCard(name, actionStats)
        }
    }
}

@Composable
private fun TeamActionCard(name: String, stats: ActionStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("Toplam", "${stats.total}")
                MiniStat("#", "${stats.perfect}")
                MiniStat("+", "${stats.positive}")
                MiniStat("=", "${stats.error}")
                MiniStat("Etk%", "${stats.efficiency.toInt()}%")
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
