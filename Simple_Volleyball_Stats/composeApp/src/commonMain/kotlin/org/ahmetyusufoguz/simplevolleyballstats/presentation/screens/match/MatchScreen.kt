package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.*
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.MatchRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.TeamRepository
import org.koin.compose.koinInject
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    teamId: String,
    matchId: String? = null,
    onBackClick: () -> Unit
) {
    val teamRepository: TeamRepository = koinInject()
    val playerRepository: PlayerRepository = koinInject()
    val matchRepository: MatchRepository = koinInject()

    val viewModel: MatchViewModel = viewModel(key = teamId) {
        MatchViewModel(teamId, matchId, teamRepository, playerRepository, matchRepository, false)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    val handleExit = {
        coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                onBackClick()
            }
        }
    }

    // Android'in fiziksel geri tuşunu ele alıyoruz
    BackHandler(enabled = true) {
        handleExit()
    }

    // shouldExit kontrolü - state'den sonra olmalı
    LaunchedEffect(state.shouldExit) {
        if (state.shouldExit) {
            handleExit()
        }
    }

    var showOpponentDialog by remember { mutableStateOf(false) }
    var opponentName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        showOpponentDialog = true
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Rakip ismi dialog (ilk açılış)
    if (showOpponentDialog) {
        OpponentNameDialog(
            initialName = "",
            onConfirm = { name ->
                opponentName = name.ifBlank { "Rakip" }
                viewModel.handleIntent(MatchIntent.SetOpponentName(opponentName))
                showOpponentDialog = false
            },
            onDismiss = {
                opponentName = "Rakip"
                viewModel.handleIntent(MatchIntent.SetOpponentName(opponentName))
                showOpponentDialog = false
            }
        )
    }

    // Servis seçim dialog
    if (state.showServeChoiceDialog && !showOpponentDialog) {
        ServeChoiceDialog(
            homeName = state.homeTeam?.name ?: "Biz",
            awayName = state.opponentName,
            onChoice = { homeServes ->
                viewModel.handleIntent(MatchIntent.SetInitialServe(homeServes))
            }
        )
    }

    // Rakip ismi düzenleme dialog
    if (state.showEditOpponentDialog) {
        OpponentNameDialog(
            initialName = state.opponentName,
            onConfirm = { name ->
                viewModel.handleIntent(MatchIntent.SetOpponentName(name.ifBlank { "Rakip" }))
            },
            onDismiss = {
                viewModel.handleIntent(MatchIntent.HideEditOpponentDialog)
            }
        )
    }

    // Başlangıç dizilişi dialog
    if (state.showStartingLineupDialog && !showOpponentDialog && !state.showServeChoiceDialog) {
        StartingLineupDialog(
            players = state.players,
            homeServing = state.homeServing,
            onConfirm = { positions, libero ->
                viewModel.handleIntent(MatchIntent.SetStartingLineup(positions, libero))
            },
            onDismiss = {
                // İptal edilirse varsayılan diziliş
                val defaultPositions = CourtPosition.entries.take(6)
                    .zip(state.players.take(6)).toMap()
                viewModel.handleIntent(MatchIntent.SetStartingLineup(defaultPositions, null))
            }
        )
    }

    // Rally Log dialog
    if (state.showRallyLogDialog) {
        val playersMap = state.players.associateBy { it.id }
        RallyLogDialog(
            currentSet = state.currentSet,
            homeScore = state.homeScore,
            awayScore = state.awayScore,
            homeName = state.homeTeam?.name ?: "Biz",
            awayName = state.opponentName,
            rallies = viewModel.getRallies(),
            players = playersMap,
            onDismiss = { viewModel.handleIntent(MatchIntent.HideRallyLog) }
        )
    }

    // Genel sayı dialog
    if (state.showGeneralPointDialog) {
        GeneralPointDialog(
            ballState = state.ballState,
            isReasonEnabled = { viewModel.isGeneralPointReasonEnabled(it) },
            onReasonSelected = { reason ->
                viewModel.handleIntent(MatchIntent.RecordGeneralPoint(reason))
            },
            onDismiss = { viewModel.handleIntent(MatchIntent.HideGeneralPointDialog) }
        )
    }

    Scaffold(
        topBar = {
            MatchTopBar(
                teamName = state.homeTeam?.name ?: "",
                opponentName = state.opponentName,
                currentSet = state.currentSet,
                setsWon = state.setsWon,
                onBackClick = { viewModel.handleIntent(MatchIntent.ShowExitConfirm) },
                onPauseClick = {
                    if (state.isPaused) viewModel.handleIntent(MatchIntent.ResumeMatch)
                    else viewModel.handleIntent(MatchIntent.PauseMatch)
                },
                onEditOpponentClick = { viewModel.handleIntent(MatchIntent.ShowEditOpponentDialog) },
                isPaused = state.isPaused
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Sol: Saha görünümü
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                ) {
                    // Skor
                    ScoreBoard(
                        homeScore = state.homeScore,
                        awayScore = state.awayScore,
                        homeServing = state.homeServing,
                        homeName = state.homeTeam?.name ?: "BİZ",
                        awayName = state.opponentName,
                        onHomeScoreClick = { viewModel.handleIntent(MatchIntent.AddHomePoint) },
                        onAwayScoreClick = { viewModel.handleIntent(MatchIntent.AddAwayPoint) }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Voleybol Sahası
                    VolleyballCourt(
                        courtPositions = state.courtPositions,
                        selectedPosition = state.selectedPosition,
                        onPositionClick = { position ->
                            viewModel.handleIntent(MatchIntent.SelectPlayer(position))
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.height(8.dp))

                    // Alt kontroller
                    BottomControls(
                        lastAction = state.lastAction,
                        libero = state.libero,
                        liberoOnCourt = state.liberoOnCourt,
                        onRotateClick = { viewModel.handleIntent(MatchIntent.Rotate) },
                        onUndoClick = { viewModel.handleIntent(MatchIntent.UndoLastAction) },
                        onSubClick = { viewModel.handleIntent(MatchIntent.ShowSubstitutionDialog) },
                        onLiberoClick = { viewModel.handleIntent(MatchIntent.ToggleLibero) },
                        onRallyLogClick = { viewModel.handleIntent(MatchIntent.ShowRallyLog) },
                        onGeneralPointClick = { viewModel.handleIntent(MatchIntent.ShowGeneralPointDialog) }
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Sağ: Aksiyon butonları
                ActionPanel(
                    selectedPlayer = state.selectedPlayer,
                    ballState = state.ballState,
                    isActionEnabled = { viewModel.isActionEnabledForSelected(it) },
                    onActionClick = { actionType ->
                        viewModel.handleIntent(MatchIntent.StartAction(actionType))
                    },
                    onClearSelection = { viewModel.handleIntent(MatchIntent.ClearSelection) },
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                )
            }
        }
    }

    // Aksiyon sonucu dialog
    if (state.showActionResultDialog && state.pendingActionType != null) {
        ActionResultDialog(
            actionType = state.pendingActionType!!,
            playerName = state.selectedPlayer?.name ?: "",
            onResultSelected = { result ->
                viewModel.handleIntent(MatchIntent.RecordAction(result))
            },
            onDismiss = { viewModel.handleIntent(MatchIntent.CancelAction) }
        )
    }

    // Set Bitti / Maç Bitti dialog
    if (state.showMatchEndDialog) {
        val isMatchOver = state.isMatchFinished
        val homeWon = state.setsWon.first > state.setsWon.second
        val winnerName = if (homeWon) (state.homeTeam?.name ?: "Ev Sahibi") else state.opponentName
        
        AlertDialog(
            onDismissRequest = { 
                viewModel.handleIntent(MatchIntent.DismissMatchEndDialog)
            },
            icon = {
                Text(
                    text = if (isMatchOver) "🏆" else "🏐",
                    style = MaterialTheme.typography.displayMedium
                )
            },
            title = {
                Text(
                    text = if (isMatchOver) "Maç Bitti!" else "Set Bitti!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (isMatchOver) {
                        Text(
                            text = "$winnerName kazandı!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Set: ${state.setsWon.first}-${state.setsWon.second}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        // Set detayları
                        state.setScores.forEachIndexed { index, (home, away) ->
                            Text(
                                text = "  Set ${index + 1}: $home-$away",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val lastSet = state.setScores.lastOrNull()
                        Text(
                            text = "Set ${state.currentSet - 1}: ${lastSet?.first ?: 0}-${lastSet?.second ?: 0}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Genel Durum: ${state.setsWon.first}-${state.setsWon.second}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sonraki set başlıyor...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (isMatchOver) {
                    Button(
                        onClick = {
                            viewModel.handleIntent(MatchIntent.DismissMatchEndDialog)
                            handleExit()
                        }
                    ) {
                        Text("📊 Rapor Görüntüle")
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.handleIntent(MatchIntent.DismissMatchEndDialog)
                        }
                    ) {
                        Text("Devam Et")
                    }
                }
            },
            dismissButton = {
                if (isMatchOver) {
                    TextButton(
                        onClick = {
                            viewModel.handleIntent(MatchIntent.DismissMatchEndDialog)
                            handleExit()
                        }
                    ) {
                        Text("Kapat")
                    }
                }
            }
        )
    }

    // Çıkış onay dialog
    if (state.showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.handleIntent(MatchIntent.HideExitConfirm) },
            title = { Text("Maçtan Çık") },
            text = { Text("Maçtan çıkmak istediğinize emin misiniz? Maç kaydedilecek ve tamamlanmış olarak işaretlenecektir.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleIntent(MatchIntent.ConfirmExit)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Kaydet ve Çık")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleIntent(MatchIntent.HideExitConfirm) }) {
                    Text("İptal")
                }
            }
        )
    }

    // Oyuncu değişikliği dialog
    if (state.showSubstitutionDialog) {
        SubstitutionDialog(
            courtPlayers = state.courtPositions.values.toList(),
            benchPlayers = state.benchPlayers,
            onSubstitute = { out, inPlayer ->
                viewModel.handleIntent(MatchIntent.SubstitutePlayer(out, inPlayer))
            },
            onDismiss = { viewModel.handleIntent(MatchIntent.HideSubstitutionDialog) }
        )
    }

    // Duraklama overlay
    if (state.isPaused) {
        PauseOverlay(
            onResume = { viewModel.handleIntent(MatchIntent.ResumeMatch) }
        )
    }
}

@Composable
private fun ServeChoiceDialog(
    homeName: String,
    awayName: String,
    onChoice: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("İlk Servis") },
        text = { Text("İlk servisi kim atacak?") },
        confirmButton = {
            Button(onClick = { onChoice(true) }) {
                Text(homeName)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onChoice(false) }) {
                Text(awayName)
            }
        }
    )
}

@Composable
private fun OpponentNameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rakip Takım") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Rakip Takım Adı") },
                placeholder = { Text("Örn: Fenerbahçe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) {
                Text("Tamam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Atla")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchTopBar(
    teamName: String,
    opponentName: String,
    currentSet: Int,
    setsWon: Pair<Int, Int>,
    onBackClick: () -> Unit,
    onPauseClick: () -> Unit,
    onEditOpponentClick: () -> Unit,
    isPaused: Boolean
) {
    TopAppBar(
        title = {
            Column(
                modifier = Modifier.clickable(onClick = onEditOpponentClick)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$teamName vs $opponentName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Set $currentSet • ${setsWon.first}-${setsWon.second}",
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
            IconButton(onClick = onPauseClick) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Devam" else "Duraklat"
                )
            }
        }
    )
}

@Composable
private fun PauseOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onResume),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Pause,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "DURAKLATILDI",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Devam etmek için dokun",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
internal fun ScoreBoard(
    homeScore: Int,
    awayScore: Int,
    homeServing: Boolean,
    homeName: String,
    awayName: String,
    onHomeScoreClick: () -> Unit,
    onAwayScoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScoreButton(
            score = homeScore,
            label = homeName,
            onClick = onHomeScoreClick,
            color = MaterialTheme.colorScheme.primary
        )

        // Ortada servis göstergesi - top tire'nin yanında
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Sol top (biz servis atıyorsak görünür)
            Text(
                text = "🏐",
                fontSize = 14.sp,
                modifier = Modifier.alpha(if (homeServing) 1f else 0f)
            )

            Spacer(Modifier.width(4.dp))

            Text(
                text = "-",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.width(4.dp))

            // Sağ top (rakip servis atıyorsa görünür)
            Text(
                text = "🏐",
                fontSize = 14.sp,
                modifier = Modifier.alpha(if (!homeServing) 1f else 0f)
            )
        }

        ScoreButton(
            score = awayScore,
            label = awayName,
            onClick = onAwayScoreClick,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
internal fun ScoreButton(
    score: Int,
    label: String,
    onClick: () -> Unit,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 80.dp)
        )
        Text(
            text = "$score",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
internal fun VolleyballCourt(
    courtPositions: Map<CourtPosition, Player>,
    selectedPosition: CourtPosition?,
    onPositionClick: (CourtPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2E7D32))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // File - EN ÜSTTE
            FileIndicator()

            // Ön sıra (4, 3, 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    CourtPosition.POSITION_4,
                    CourtPosition.POSITION_3,
                    CourtPosition.POSITION_2
                ).forEach { position ->
                    PlayerSlot(
                        player = courtPositions[position],
                        isSelected = selectedPosition == position,
                        onClick = { onPositionClick(position) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Arka sıra (5, 6, 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    CourtPosition.POSITION_5,
                    CourtPosition.POSITION_6,
                    CourtPosition.POSITION_1
                ).forEach { position ->
                    PlayerSlot(
                        player = courtPositions[position],
                        isSelected = selectedPosition == position,
                        onClick = { onPositionClick(position) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun FileIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "── FİLE ──",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .padding(horizontal = 16.dp)
                .background(Color.White)
        )
    }
}

@Composable
internal fun PlayerSlot(
    player: Player?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLibero = player?.position == PlayerPosition.LIBERO
    val backgroundColor = when {
        isSelected -> Color.Yellow.copy(alpha = 0.9f)
        isLibero -> Color(0xFFFFD700)
        else -> Color.White.copy(alpha = 0.95f)
    }

    Box(
        modifier = modifier.padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (isSelected) Modifier.border(2.dp, Color.Yellow, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .clickable(onClick = onClick)
                .background(backgroundColor)
                .padding(6.dp)
                .widthIn(min = 50.dp, max = 70.dp)
        ) {
            // Oyuncu numarası
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isLibero) Color(0xFFB8860B) else MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${player?.number ?: "?"}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(2.dp))

            // Oyuncu adı
            Text(
                text = player?.name?.split(" ")?.firstOrNull() ?: "Boş",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Pozisyon kısaltması
            if (player?.position != null) {
                Text(
                    text = player.position.shortName,
                    fontSize = 8.sp,
                    color = if (isLibero) Color(0xFF8B4513) else Color.Gray
                )
            }
        }
    }
}

@Composable
internal fun ActionPanel(
    selectedPlayer: Player?,
    ballState: BallState,
    isActionEnabled: (ActionType) -> Boolean,
    onActionClick: (ActionType) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Seçili oyuncu
            if (selectedPlayer != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${selectedPlayer.number}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = selectedPlayer.name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Temizle",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "👆 Sahadan oyuncu seçin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Top durumu göstergesi
            val ballStateText = when (ballState) {
                BallState.HOME_SERVE -> "🏐 Servis Bizde"
                BallState.AWAY_SERVE -> "⏳ Rakip Servisi"
                BallState.HOME_RECEIVING, BallState.IN_PLAY_HOME -> "🔥 Top Bizde"
                BallState.AWAY_RECEIVING, BallState.IN_PLAY_AWAY -> "🛡️ Top Rakipte"
            }
            Text(
                text = ballStateText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(2.dp))

            // Aksiyon butonları - 2 sütunlu grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(ActionType.entries) { actionType ->
                    val enabled = selectedPlayer != null && isActionEnabled(actionType)
                    ActionButton(
                        actionType = actionType,
                        enabled = enabled,
                        onClick = { onActionClick(actionType) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ActionButton(
    actionType: ActionType,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = actionType.emoji,
                fontSize = 14.sp
            )
            Text(
                text = actionType.displayName,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BottomControls(
    lastAction: Action?,
    libero: Player?,
    liberoOnCourt: Boolean,
    onRotateClick: () -> Unit,
    onUndoClick: () -> Unit,
    onSubClick: () -> Unit,
    onLiberoClick: () -> Unit,
    onRallyLogClick: () -> Unit,
    onGeneralPointClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Son aksiyon
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = if (lastAction != null) {
                    "Son: ${lastAction.actionType.emoji} ${lastAction.result.displayName}"
                } else {
                    "Henüz aksiyon yok"
                },
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Genel Sayı butonu
        FilledTonalIconButton(
            onClick = onGeneralPointClick,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Text("±", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        // Rally Log butonu
        FilledTonalIconButton(
            onClick = onRallyLogClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = "Aksiyon Listesi",
                modifier = Modifier.size(20.dp)
            )
        }

        // Libero butonu
        FilledTonalIconButton(
            onClick = onLiberoClick,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (liberoOnCourt) Color(0xFFFFD700)
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("L", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        FilledTonalIconButton(onClick = onRotateClick, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = "Rotasyon", modifier = Modifier.size(20.dp))
        }

        FilledTonalIconButton(
            onClick = onUndoClick,
            enabled = lastAction != null,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Default.Undo, contentDescription = "Geri Al", modifier = Modifier.size(20.dp))
        }

        FilledTonalIconButton(onClick = onSubClick, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Değişiklik", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun ActionResultDialog(
    actionType: ActionType,
    playerName: String,
    onResultSelected: (ActionResult) -> Unit,
    onDismiss: () -> Unit
) {
    val results = actionType.getAvailableResults()
    // 6 sonuçlu aksiyonlar için 3 sütun, diğerleri için 2 sütun
    val columns = if (results.size >= 6) 3 else 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${actionType.emoji} ${actionType.displayName}")
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                items(results) { result ->
                    DataVolleyResultChip(
                        result = result,
                        onClick = { onResultSelected(result) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
internal fun DataVolleyResultChip(
    result: ActionResult,
    onClick: () -> Unit
) {
    // Renge göre arka plan — DataVolley sembolüne göre
    val (bgColor, textColor) = when (result.dataVolleySymbol) {
        "#" -> Color(0xFF1B5E20) to Color.White            // Mükemmel — koyu yeşil
        "+" -> Color(0xFF2E7D32) to Color.White            // Pozitif — yeşil
        "!" -> Color(0xFFF57F17) to Color.White            // Nötr — amber
        "-" -> Color(0xFFE65100) to Color.White            // Negatif — turuncu
        "/" -> Color(0xFFC62828) to Color.White            // Overpass — kırmızı
        "=" -> Color(0xFF4E342E) to Color.White            // Hata — koyu kahve
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sembol — büyük ve belirgin
            Text(
                text = result.dataVolleySymbol,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            Spacer(Modifier.height(2.dp))
            // Açıklama
            Text(
                text = result.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontSize = 9.sp
            )
            // Puan etkisi
            if (result.pointValue != 0) {
                Text(
                    text = if (result.pointValue > 0) "🏆 Sayı" else "❌ Kayıp",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubstitutionDialog(
    courtPlayers: List<Player>,
    benchPlayers: List<Player>,
    onSubstitute: (Player, Player) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOut by remember { mutableStateOf<Player?>(null) }
    var selectedIn by remember { mutableStateOf<Player?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oyuncu Değişikliği") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Çıkacak oyuncu:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    courtPlayers.forEach { player ->
                        FilterChip(
                            selected = selectedOut == player,
                            onClick = { selectedOut = player },
                            label = { Text("#${player.number} ${player.name}", fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Girecek oyuncu:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                if (benchPlayers.isEmpty()) {
                    Text(
                        "Yedek oyuncu yok",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        benchPlayers.forEach { player ->
                            FilterChip(
                                selected = selectedIn == player,
                                onClick = { selectedIn = player },
                                label = { Text("#${player.number} ${player.name}", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedOut != null && selectedIn != null) {
                        onSubstitute(selectedOut!!, selectedIn!!)
                    }
                },
                enabled = selectedOut != null && selectedIn != null
            ) {
                Text("Değiştir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
internal fun RallyLogDialog(
    currentSet: Int,
    homeScore: Int,
    awayScore: Int,
    homeName: String,
    awayName: String,
    rallies: List<MatchViewModel.Rally>,
    players: Map<String, Player>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Set $currentSet Aksiyonları")
                Text(
                    text = "$homeName $homeScore - $awayScore $awayName",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            if (rallies.isEmpty()) {
                Text(
                    text = "Henüz aksiyon yok",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 400.dp),
                    reverseLayout = true
                ) {
                    items(rallies) { rally ->
                        RallyCard(
                            rally = rally,
                            players = players
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@Composable
internal fun RallyCard(
    rally: MatchViewModel.Rally,
    players: Map<String, Player>
) {
    val bgColor = when (rally.weScored) {
        true -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        false -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        null -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Rally başlığı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rally #${rally.number}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${rally.startScore.first}-${rally.startScore.second}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${rally.endScore.first}-${rally.endScore.second}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (rally.weScored) {
                            true -> MaterialTheme.colorScheme.primary
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (rally.weScored != null) {
                        Text(
                            text = if (rally.weScored) " ✓" else " ✗",
                            fontWeight = FontWeight.Bold,
                            color = if (rally.weScored) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (rally.actions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // Aksiyonlar
                rally.actions.forEach { action ->
                    val player = players[action.playerId]
                    ActionLogItem(action = action, player = player)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionLogItem(
    action: Action,
    player: Player?
) {
    val resultColor = when {
        action.result.pointValue > 0 -> MaterialTheme.colorScheme.primary
        action.result.pointValue < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isTeamAction = action.playerId == "TEAM"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Oyuncu veya Takım
        Text(
            text = if (isTeamAction) "⚑" else "#${player?.number ?: "?"}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )

        // Aksiyon tipi
        Text(
            text = action.actionType.emoji,
            fontSize = 14.sp
        )

        Spacer(Modifier.width(4.dp))

        // Aksiyon adı
        Text(
            text = if (isTeamAction) "Genel" else action.actionType.displayName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )

        // Sonuç
        Text(
            text = action.result.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = resultColor,
            fontWeight = if (action.result.pointValue != 0) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StartingLineupDialog(
    players: List<Player>,
    homeServing: Boolean,
    onConfirm: (Map<CourtPosition, Player>, Player?) -> Unit,
    onDismiss: () -> Unit
) {
    val setters = players.filter { it.position == PlayerPosition.SETTER }
    val opposites = players.filter { it.position == PlayerPosition.OPPOSITE }
    val outsideHitters = players.filter { it.position == PlayerPosition.OUTSIDE_HITTER }
    val middleBlockers = players.filter { it.position == PlayerPosition.MIDDLE_BLOCKER }
    val liberos = players.filter { it.position == PlayerPosition.LIBERO }
    val unassigned = players.filter { it.position == null }

    var selectedPositions by remember { mutableStateOf<Map<CourtPosition, Player>>(emptyMap()) }
    var selectedLibero by remember { mutableStateOf<Player?>(null) }

    val suggestedLineup = remember(players, homeServing) {
        buildSuggestedLineup(setters, opposites, outsideHitters, middleBlockers, unassigned, homeServing)
    }

    LaunchedEffect(suggestedLineup) {
        selectedPositions = suggestedLineup
        selectedLibero = liberos.firstOrNull()
    }

    // Çapraz değiştirme fonksiyonu
    fun swapDiagonals(pos1: CourtPosition, pos2: CourtPosition) {
        val player1 = selectedPositions[pos1]
        val player2 = selectedPositions[pos2]
        if (player1 != null && player2 != null) {
            selectedPositions = selectedPositions + (pos1 to player2) + (pos2 to player1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Başlangıç Dizilişi") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (homeServing) "5-1 Sistemi • Servis Bizde (P→1)"
                    else "5-1 Sistemi • Servis Rakipte (P→2)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Çaprazlar: P↔PÇ • KS↔KS • OO↔OO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                MiniCourtPreview(selectedPositions)

                Spacer(Modifier.height(12.dp))

                // Çapraz değiştirme butonları
                Text("Çaprazları Değiştir:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // KS çaprazları (1-4 veya servis durumuna göre)
                    OutlinedButton(
                        onClick = {
                            if (homeServing) {
                                swapDiagonals(CourtPosition.POSITION_2, CourtPosition.POSITION_5)
                            } else {
                                swapDiagonals(CourtPosition.POSITION_3, CourtPosition.POSITION_6)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("KS ↔ KS", fontSize = 12.sp)
                    }

                    // OO çaprazları (2-5 veya 3-6)
                    OutlinedButton(
                        onClick = {
                            if (homeServing) {
                                swapDiagonals(CourtPosition.POSITION_3, CourtPosition.POSITION_6)
                            } else {
                                swapDiagonals(CourtPosition.POSITION_4, CourtPosition.POSITION_1)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("OO ↔ OO", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Pozisyonlar:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                val positionLabels = mapOf(
                    CourtPosition.POSITION_4 to "4 (Ön Sol)",
                    CourtPosition.POSITION_3 to "3 (Ön Orta)",
                    CourtPosition.POSITION_2 to "2 (Ön Sağ)",
                    CourtPosition.POSITION_5 to "5 (Arka Sol)",
                    CourtPosition.POSITION_6 to "6 (Arka Orta)",
                    CourtPosition.POSITION_1 to "1 (Arka Sağ)"
                )

                val positionOrder = listOf(
                    CourtPosition.POSITION_4,
                    CourtPosition.POSITION_3,
                    CourtPosition.POSITION_2,
                    CourtPosition.POSITION_5,
                    CourtPosition.POSITION_6,
                    CourtPosition.POSITION_1
                )

                positionOrder.forEach { position ->
                    PositionSelector(
                        label = positionLabels[position] ?: "",
                        selectedPlayer = selectedPositions[position],
                        availablePlayers = players.filter { player ->
                            player !in selectedPositions.values &&
                                    player != selectedLibero &&
                                    player.position != PlayerPosition.LIBERO
                        },
                        onPlayerSelected = { player ->
                            selectedPositions = if (player != null) {
                                selectedPositions + (position to player)
                            } else {
                                selectedPositions - position
                            }
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(12.dp))

                Text("Libero:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedLibero == null,
                        onClick = { selectedLibero = null },
                        label = { Text("Yok") }
                    )

                    liberos.forEach { player ->
                        FilterChip(
                            selected = selectedLibero == player,
                            onClick = { selectedLibero = player },
                            label = { Text("#${player.number} ${player.name}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD700)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedPositions, selectedLibero) },
                enabled = selectedPositions.size == 6
            ) {
                Text("Başla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

private fun buildSuggestedLineup(
    setters: List<Player>,
    opposites: List<Player>,
    outsideHitters: List<Player>,
    middleBlockers: List<Player>,
    unassigned: List<Player>,
    homeServing: Boolean
): Map<CourtPosition, Player> {
    val lineup = mutableMapOf<CourtPosition, Player>()
    val used = mutableSetOf<Player>()

    /**
     * 5-1 Sistemi Çapraz Eşleşmeler:
     * - Pozisyon 1 ↔ 4: KS - KS (Köşe Smaçörleri)
     * - Pozisyon 2 ↔ 5: OO - OO (Orta Oyuncular)
     * - Pozisyon 3 ↔ 6: PÇ - P  (Pasör Çaprazı - Pasör)
     *
     * Servis durumuna göre pasör pozisyonu:
     * - Servis bizde: Pasör 1'de başlar
     * - Servis rakipte: Pasör 2'de başlar
     */

    val setter = setters.firstOrNull()
    val opposite = opposites.firstOrNull()
    val oh1 = outsideHitters.getOrNull(0)
    val oh2 = outsideHitters.getOrNull(1)
    val mb1 = middleBlockers.getOrNull(0)
    val mb2 = middleBlockers.getOrNull(1)

    if (homeServing) {
        // Servis bizde - Pasör 1'de
        // Diziliş: 4-KS, 3-PÇ, 2-OO, 5-OO, 6-P, 1-KS (servis atan)
        setter?.let { lineup[CourtPosition.POSITION_1] = it; used.add(it) }
        opposite?.let { lineup[CourtPosition.POSITION_4] = it; used.add(it) } // P çaprazı = 4
        oh1?.let { lineup[CourtPosition.POSITION_2] = it; used.add(it) }
        oh2?.let { lineup[CourtPosition.POSITION_5] = it; used.add(it) } // KS çaprazları
        mb1?.let { lineup[CourtPosition.POSITION_3] = it; used.add(it) }
        mb2?.let { lineup[CourtPosition.POSITION_6] = it; used.add(it) } // OO çaprazları
    } else {
        // Servis rakipte - Pasör 2'de (ön sağ)
        // Diziliş: 4-OO, 3-KS, 2-P, 5-KS, 6-OO, 1-PÇ
        setter?.let { lineup[CourtPosition.POSITION_2] = it; used.add(it) }
        opposite?.let { lineup[CourtPosition.POSITION_5] = it; used.add(it) } // P çaprazı = 5
        oh1?.let { lineup[CourtPosition.POSITION_3] = it; used.add(it) }
        oh2?.let { lineup[CourtPosition.POSITION_6] = it; used.add(it) } // KS çaprazları = 3-6
        mb1?.let { lineup[CourtPosition.POSITION_4] = it; used.add(it) }
        mb2?.let { lineup[CourtPosition.POSITION_1] = it; used.add(it) } // OO çaprazları = 4-1
    }

    // Kalan pozisyonları doldur
    val remainingPositions = CourtPosition.entries.filter { it !in lineup.keys }
    val remainingPlayers = (setters + opposites + outsideHitters + middleBlockers + unassigned)
        .filter { it !in used }

    remainingPositions.zip(remainingPlayers).forEach { (pos, player) ->
        lineup[pos] = player
    }

    return lineup
}

@Composable
private fun MiniCourtPreview(positions: Map<CourtPosition, Player>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2E7D32))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // File
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White)
            )

            // Ön sıra
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(CourtPosition.POSITION_4, CourtPosition.POSITION_3, CourtPosition.POSITION_2)
                    .forEach { pos ->
                        MiniPlayerSlot(positions[pos])
                    }
            }

            // Arka sıra
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(CourtPosition.POSITION_5, CourtPosition.POSITION_6, CourtPosition.POSITION_1)
                    .forEach { pos ->
                        MiniPlayerSlot(positions[pos])
                    }
            }
        }
    }
}

@Composable
private fun MiniPlayerSlot(player: Player?) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = player?.number?.toString() ?: "?",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PositionSelector(
    label: String,
    selectedPlayer: Player?,
    availablePlayers: List<Player>,
    onPlayerSelected: (Player?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.4f)
        )

        Box(modifier = Modifier.weight(0.6f)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = selectedPlayer?.let { "#${it.number} ${it.name}" } ?: "Seç",
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (selectedPlayer != null) {
                    DropdownMenuItem(
                        text = { Text("Kaldır", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onPlayerSelected(null)
                            expanded = false
                        }
                    )
                    HorizontalDivider()
                }

                availablePlayers.forEach { player ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "#${player.number} ${player.name} ${player.position?.shortName ?: ""}",
                                fontSize = 13.sp
                            )
                        },
                        onClick = {
                            onPlayerSelected(player)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralPointDialog(
    ballState: BallState,
    isReasonEnabled: (GeneralPointReason) -> Boolean,
    onReasonSelected: (GeneralPointReason) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    val opponentErrors = GeneralPointReason.entries.filter { it.forUs }
    val teamErrors = GeneralPointReason.entries.filter { !it.forUs }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Genel Sayı") },
        text = {
            Column {
                // Tab seçimi
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Rakip Hatası (+1)", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Bizim Hata (-1)", fontSize = 12.sp) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Seçenekler
                val reasons = if (selectedTab == 0) opponentErrors else teamErrors

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(reasons) { reason ->
                        val enabled = isReasonEnabled(reason)
                        GeneralPointChip(
                            reason = reason,
                            enabled = enabled,
                            onClick = { if (enabled) onReasonSelected(reason) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun GeneralPointChip(
    reason: GeneralPointReason,
    enabled: Boolean,  // 👈 Yeni parametre
    onClick: () -> Unit
) {
    val bgColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        reason.forUs -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }

    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        reason.forUs -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = reason.emoji,
                fontSize = 20.sp,
                modifier = Modifier.alpha(if (enabled) 1f else 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = reason.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontSize = 10.sp
            )
        }
    }
}
