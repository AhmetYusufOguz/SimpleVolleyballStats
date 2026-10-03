package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Match
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.MatchStatus
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Player
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.MatchRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.TeamRepository
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    teamId: String,
    onBackClick: () -> Unit,
    onAddPlayerClick: () -> Unit,
    onStartMatchClick: () -> Unit,
    onContinueMatchClick: (String) -> Unit,
    onMatchReportClick: (String) -> Unit = {}
) {
    val teamRepository: TeamRepository = koinInject()
    val playerRepository: PlayerRepository = koinInject()
    val matchRepository: MatchRepository = koinInject()

    val viewModel = remember(teamId) {
        TeamDetailViewModel(teamId, teamRepository, playerRepository, matchRepository)
    }

    val state by viewModel.state.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.team?.name ?: "Takım",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.players.size} oyuncu • ${state.matches.size} maç",
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
                    IconButton(onClick = onAddPlayerClick) {
                        Icon(Icons.Default.Add, contentDescription = "Oyuncu Ekle")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (state.players.size >= 6) {
                        onStartMatchClick()
                    }
                },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                text = {
                    Text(
                        if (state.players.size >= 6) "Maç Başlat"
                        else "Maç Başlat (${state.players.size}/6)"
                    )
                },
                containerColor = if (state.players.size >= 6)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
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
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Oyuncular (${state.players.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Maçlar (${state.matches.size})") }
                )
            }

            // İçerik
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                selectedTab == 0 -> {
                    // Oyuncular
                    if (state.players.isEmpty()) {
                        EmptyPlayersState(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center),
                            onAddClick = onAddPlayerClick
                        )
                    } else {
                        PlayersList(
                            players = state.players,
                            onDeleteClick = { viewModel.deletePlayer(it) }
                        )
                    }
                }

                selectedTab == 1 -> {
                    // Maçlar
                    if (state.matches.isEmpty()) {
                        EmptyMatchesState(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center)
                        )
                    } else {
                        MatchesList(
                            matches = state.matches,
                            // Sadece tamamlanmış maçlar tıklanabilir (rapor görüntüleme)
                            // "Maça devam et" özelliği UI'dan gizlendi
                            onMatchClick = { matchId -> onMatchReportClick(matchId) },
                            onDeleteClick = { viewModel.deleteMatch(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPlayersState(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "👥",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Henüz Oyuncu Yok",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Maç başlatmak için en az\n6 oyuncu eklemelisiniz",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Oyuncu Ekle")
        }
    }
}

@Composable
private fun EmptyMatchesState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🏐",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Henüz Maç Yok",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Oynanan maçlar burada\ngörünecek",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlayersList(
    players: List<Player>,
    onDeleteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(players, key = { it.id }) { player ->
            PlayerCard(
                player = player,
                onDeleteClick = { onDeleteClick(player.id) }
            )
        }
    }
}

@Composable
private fun MatchesList(
    matches: List<Match>,
    onMatchClick: (String) -> Unit,  // 👈 Eklendi
    onDeleteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(matches, key = { it.id }) { match ->
            MatchCard(
                match = match,
                onClick = { onMatchClick(match.id) },  // 👈 Eklendi
                onDeleteClick = { onDeleteClick(match.id) }
            )
        }
    }
}

@Composable
private fun MatchCard(
    match: Match,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val statusColor = when (match.status) {
        MatchStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        MatchStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
        MatchStatus.FINISHED -> MaterialTheme.colorScheme.secondary
        MatchStatus.NOT_STARTED -> MaterialTheme.colorScheme.outline
    }

    val statusText = when (match.status) {
        MatchStatus.IN_PROGRESS -> "Devam Ediyor"
        MatchStatus.PAUSED -> "Duraklatıldı"
        MatchStatus.FINISHED -> "Tamamlandı"
        MatchStatus.NOT_STARTED -> "Başlamadı"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Üst: Takımlar ve skor
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: Voleybol ikonu
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SportsVolleyball,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Orta: Takım isimleri ve skor
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${match.homeTeamName} vs ${match.awayTeamName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Set skoru
                        Text(
                            text = "Set: ${match.homeSetsWon}-${match.awaySetsWon}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.width(12.dp))

                        // Mevcut set skoru
                        Text(
                            text = "(${match.homeScore}-${match.awayScore})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sağ: Sil butonu
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Alt: Durum ve tarih
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Durum chip
                    Surface(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Rapor görüntüleme göstergesi (sadece tamamlanmış maçlar)
                    if (match.status == MatchStatus.FINISHED) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "📊 Rapor Görüntüle",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Tarih
                Text(
                    text = formatDate(match.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    // Çok basit: sadece "X gün önce" formatı
    val now = org.ahmetyusufoguz.simplevolleyballstats.utils.currentTimeMillis()
    val diffMs = now - timestamp
    val diffMinutes = diffMs / 60000
    val diffHours = diffMs / 3600000
    val diffDays = diffMs / 86400000

    return when {
        diffMinutes < 1 -> "Az önce"
        diffMinutes < 60 -> "$diffMinutes dk önce"
        diffHours < 24 -> "$diffHours saat önce"
        diffDays < 7 -> "$diffDays gün önce"
        else -> "${diffDays / 7} hafta önce"
    }
}

@Composable
private fun PlayerCard(
    player: Player,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${player.number}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (player.position != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = player.position.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (player.position != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = player.position.shortName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}