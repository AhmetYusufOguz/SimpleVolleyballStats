package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.match

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.*
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.MatchRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.TeamRepository
import org.koin.compose.koinInject

/**
 * Çift Takım Maç Ekranı
 *
 * Mimari:
 *  - homeViewModel (MASTER): Skor, servis, set geçişi yönetiminin tek kaynağı.
 *  - awayViewModel  (SLAVE) : Sadece aksiyon/istatistik kaydeder. Skor/servis master'dan
 *                             SyncPeerState intent'iyle senkronize edilir.
 *
 * Veri akışı:
 *  1. homeVM action → homeVM state değişir → LaunchedEffect → awayVM.SyncPeerState
 *  2. awayVM action (slave, sadece lastAction günceller) → LaunchedEffect →
 *     POINT_HOME/AWAY → homeVM.AddHomePoint/AddAwayPoint → homeVM state değişir → (1)'e döner
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualTeamMatchScreen(
    homeTeamId: String,
    awayTeamId: String,
    onBackClick: () -> Unit
) {
    val teamRepository: TeamRepository = koinInject()
    val playerRepository: PlayerRepository = koinInject()
    val matchRepository: MatchRepository = koinInject()

    // Master VM: Score, substitution vb. yönetir
    val homeViewModel: MatchViewModel = viewModel(key = "home_$homeTeamId") {
        MatchViewModel(homeTeamId, null, teamRepository, playerRepository, matchRepository, false)
    }
    // awayVM = SLAVE (isDualTeamSlave=true: sadece aksiyon kaydeder)
    val awayViewModel: MatchViewModel = viewModel(key = "away_$awayTeamId") {
        MatchViewModel(awayTeamId, null, teamRepository, playerRepository, matchRepository, true)
    }

    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val awayState  by awayViewModel.state.collectAsStateWithLifecycle()

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

    // Aktif takım: son dokunulan taraf
    var activeTeam by remember { mutableStateOf(ActiveTeam.HOME) }
    val activeViewModel = if (activeTeam == ActiveTeam.HOME) homeViewModel else awayViewModel
    val activeState    = if (activeTeam == ActiveTeam.HOME) homeState   else awayState

    // ─── shouldExit ───
    LaunchedEffect(homeState.shouldExit) { if (homeState.shouldExit) handleExit() }
    LaunchedEffect(awayState.shouldExit) { if (awayState.shouldExit) handleExit() }

    // ─── FIX 1 & 3: Takım bilgileri bağlama ───
    // Her iki takım yüklenince:
    //  a) Karşılıklı rakip adı state'e yaz (opponent name)
    //  b) homeVM maç kaydına awayTeamId + awayTeamName kaydet (DB'de tek satır)
    val homeTeamName = homeState.homeTeam?.name
    val awayTeamName  = awayState.homeTeam?.name
    LaunchedEffect(homeTeamName, awayTeamName) {
        if (homeTeamName != null && awayTeamName != null) {
            homeViewModel.handleIntent(MatchIntent.SetOpponentName(awayTeamName))
            awayViewModel.handleIntent(MatchIntent.SetOpponentName(homeTeamName))
            // Madde 3: homeVM'in DB satırında awayTeamId ve awayTeamName doğru dolsun
            homeViewModel.handleIntent(MatchIntent.SetAwayTeamInfo(awayTeamId, awayTeamName))
        }
    }

    // ─── FIX 3: Slave VM → Master maç ID'sine bağla ───
    // homeVM maçını oluşturur; awayVM (slave) bu ID altında aksiyon kaydeder.
    // Tek bir maç satırı oluşur (homeTeamId + awayTeamId dolu).
    val masterMatchId = homeState.match?.id
    LaunchedEffect(masterMatchId) {
        if (masterMatchId != null) {
            awayViewModel.handleIntent(MatchIntent.SetMasterMatchId(masterMatchId))
        }
    }

    // ─── Master → Slave durum senkronizasyonu ───
    // homeVM (master) her değiştiğinde awayVM (slave) koordinat-swap ile güncellenir.
    // ballState de dahil: karşılama sonrası hücum aksiyonları aktifleşir (Madde 1).
    LaunchedEffect(
        homeState.homeScore,
        homeState.awayScore,
        homeState.homeServing,
        homeState.currentSet,
        homeState.setsWon,
        homeState.isMatchFinished,
        homeState.ballState          // ← Yeni: top geçişi de slave'e aktar
    ) {
        awayViewModel.handleIntent(
            MatchIntent.SyncPeerState(
                masterHomeScore       = homeState.homeScore,
                masterAwayScore       = homeState.awayScore,
                masterHomeServing     = homeState.homeServing,
                masterCurrentSet      = homeState.currentSet,
                masterHomeSetsWon     = homeState.setsWon.first,
                masterAwaySetsWon     = homeState.setsWon.second,
                masterIsMatchFinished = homeState.isMatchFinished,
                masterBallState       = homeState.ballState      // ← Koordinat-swap ile slave'e
            )
        )
    }

    // ─── FIX 1 & 2: Away takım aksiyonlarını master'a çapraz-yayımlama ───
    // awayVM aksiyon kaydeder (slave = skor güncellemez).
    // Sonuç POINT_HOME/AWAY ise homeVM puanı işler.
    // Sonuç TO_HOME/TO_AWAY ise top hakimiyeti homeVM'e aktarılır (Madde 1 düzeltmesi).
    val lastAwayAction = awayState.lastAction
    LaunchedEffect(lastAwayAction?.id) {
        val action = lastAwayAction ?: return@LaunchedEffect
        when (action.result.ballStateChange) {
            BallStateChange.POINT_AWAY ->
                // awayVM'de POINT_AWAY = fiziksel ev sahibi (master) puan alır
                homeViewModel.handleIntent(MatchIntent.AddHomePoint)

            BallStateChange.POINT_HOME ->
                // awayVM'de POINT_HOME = fiziksel deplasman (slave) puan alır
                homeViewModel.handleIntent(MatchIntent.AddAwayPoint)

            BallStateChange.TO_HOME ->
                // Slave'de TO_HOME = top fiziksel dep takımında = master için IN_PLAY_AWAY
                homeViewModel.handleIntent(MatchIntent.UpdateBallState(BallState.IN_PLAY_AWAY))

            BallStateChange.TO_AWAY ->
                // Slave'de TO_AWAY = top fiziksel ev sahibinde = master için IN_PLAY_HOME
                homeViewModel.handleIntent(MatchIntent.UpdateBallState(BallState.IN_PLAY_HOME))

            else -> {} // STAY veya NONE — değişiklik yok
        }
    }

    // ─── ADIM 1: Ortak Servis Seçimi ───
    val needsServeChoice = homeState.showServeChoiceDialog || awayState.showServeChoiceDialog
    if (needsServeChoice) {
        AlertDialog(
            onDismissRequest = { /* zorunlu seçim */ },
            title = { Text("İlk Servis", fontWeight = FontWeight.Bold) },
            text = { Text("İlk servisi hangi takım atacak?") },
            confirmButton = {
                Button(onClick = {
                    homeViewModel.handleIntent(MatchIntent.SetInitialServe(true))
                    awayViewModel.handleIntent(MatchIntent.SetInitialServe(false))
                }) { Text(homeState.homeTeam?.name ?: "Ev Sahibi") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    homeViewModel.handleIntent(MatchIntent.SetInitialServe(false))
                    awayViewModel.handleIntent(MatchIntent.SetInitialServe(true))
                }) { Text(awayState.homeTeam?.name ?: "Deplasman") }
            }
        )
        return
    }

    // ─── ADIM 2: Başlangıç Dizilişi Dialogları (sıralı) ───
    if (homeState.showStartingLineupDialog) {
        StartingLineupDialog(
            players = homeState.players,
            homeServing = homeState.homeServing,
            onConfirm = { positions, libero ->
                homeViewModel.handleIntent(MatchIntent.SetStartingLineup(positions, libero))
            },
            onDismiss = { homeViewModel.handleIntent(MatchIntent.HideStartingLineupDialog) }
        )
        return
    }

    if (awayState.showStartingLineupDialog) {
        StartingLineupDialog(
            players = awayState.players,
            homeServing = awayState.homeServing,
            onConfirm = { positions, libero ->
                awayViewModel.handleIntent(MatchIntent.SetStartingLineup(positions, libero))
            },
            onDismiss = { awayViewModel.handleIntent(MatchIntent.HideStartingLineupDialog) }
        )
        return
    }

    // ─── Aksiyon Sonucu Dialog ───
    if (activeState.showActionResultDialog && activeState.pendingActionType != null) {
        ActionResultDialog(
            actionType = activeState.pendingActionType!!,
            playerName = activeState.selectedPlayer?.name ?: "",
            onResultSelected = { result ->
                activeViewModel.handleIntent(MatchIntent.RecordAction(result))
            },
            onDismiss = { activeViewModel.handleIntent(MatchIntent.CancelAction) }
        )
    }

    // ─── Oyuncu Değişikliği Dialog ───
    if (activeState.showSubstitutionDialog) {
        val activeCourtPlayers = activeState.courtPositions.values.toList()
        val activeBenchPlayers = activeState.players.filter { it !in activeCourtPlayers }
        SubstitutionDialog(
            courtPlayers = activeCourtPlayers,
            benchPlayers = activeBenchPlayers,
            onDismiss = { activeViewModel.handleIntent(MatchIntent.HideSubstitutionDialog) },
            onSubstitute = { outPlayer, inPlayer ->
                activeViewModel.handleIntent(MatchIntent.SubstitutePlayer(outPlayer, inPlayer))
            }
        )
    }

    // ─── Aksiyon Listesi (Rally Log) Dialog ───
    if (activeState.showRallyLogDialog) {
        val playersMap = activeState.players.associateBy { it.id }
        RallyLogDialog(
            currentSet = activeState.currentSet,
            homeScore = activeState.homeScore,
            awayScore = activeState.awayScore,
            homeName = activeState.homeTeam?.name ?: "Ev Sahibi",
            awayName = activeState.opponentName,
            rallies = activeViewModel.getRallies(),
            players = playersMap,
            onDismiss = { activeViewModel.handleIntent(MatchIntent.HideRallyLog) }
        )
    }

    // ─── Set / Maç Bitti Dialog ───
    // Sadece homeVM (master) set/maç bitişini tetikler
    if (homeState.showMatchEndDialog) {
        AlertDialog(
            onDismissRequest = {
                homeViewModel.handleIntent(MatchIntent.DismissMatchEndDialog)
                awayViewModel.handleIntent(MatchIntent.DismissMatchEndDialog)
            },
            icon = {
                Text(
                    if (homeState.isMatchFinished) "🏆" else "🏐",
                    style = MaterialTheme.typography.displayMedium
                )
            },
            title = {
                Text(
                    if (homeState.isMatchFinished) "Maç Bitti!" else "Set Bitti!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Set Durumu: ${homeState.setsWon.first}-${homeState.setsWon.second}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!homeState.isMatchFinished) {
                        Text(
                            "Yeni sette ilk servis: ${if (homeState.homeServing) homeState.homeTeam?.name else awayState.homeTeam?.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    homeViewModel.handleIntent(MatchIntent.DismissMatchEndDialog)
                    awayViewModel.handleIntent(MatchIntent.DismissMatchEndDialog)
                    if (homeState.isMatchFinished) handleExit()
                }) {
                    Text(if (homeState.isMatchFinished) "Bitir" else "Devam Et")
                }
            }
        )
    }

    // ─── Çıkış Dialog ───
    if (homeState.showExitConfirmDialog || awayState.showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                homeViewModel.handleIntent(MatchIntent.HideExitConfirm)
                awayViewModel.handleIntent(MatchIntent.HideExitConfirm)
            },
            title = { Text("Maçtan Çık") },
            text = { Text("Maçtan çıkmak istediğinize emin misiniz? Maç tamamlanmış olarak kaydedilecektir.") },
            confirmButton = {
                Button(onClick = {
                    homeViewModel.handleIntent(MatchIntent.ConfirmExit)
                    awayViewModel.handleIntent(MatchIntent.ConfirmExit)
                }) { Text("Kaydet ve Çık") }
            },
            dismissButton = {
                TextButton(onClick = {
                    homeViewModel.handleIntent(MatchIntent.HideExitConfirm)
                    awayViewModel.handleIntent(MatchIntent.HideExitConfirm)
                }) { Text("İptal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${homeState.homeTeam?.name ?: "Ev Sahibi"} vs ${awayState.homeTeam?.name ?: "Deplasman"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Set: ${homeState.setsWon.first}-${homeState.setsWon.second} • Set ${homeState.currentSet}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        homeViewModel.handleIntent(MatchIntent.ShowExitConfirm)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ─── Sol: Ev Sahibi Sahası ───
            TeamCourtPanel(
                state = homeState,
                isActive = activeTeam == ActiveTeam.HOME,
                teamColor = MaterialTheme.colorScheme.primary,
                onActivate = { activeTeam = ActiveTeam.HOME },
                onPositionClick = { pos ->
                    activeTeam = ActiveTeam.HOME
                    homeViewModel.handleIntent(MatchIntent.SelectPlayer(pos))
                },
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
            )

            // ─── Orta: Skor + Aksiyon ───
            Column(
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Skor — her iki skor da homeVM'den (master) okunur
                ScoreBoard(
                    homeScore = homeState.homeScore,
                    awayScore = homeState.awayScore,
                    homeServing = homeState.homeServing,
                    homeName = homeState.homeTeam?.name ?: "Ev",
                    awayName = awayState.homeTeam?.name ?: "Dep",
                    onHomeScoreClick = { homeViewModel.handleIntent(MatchIntent.AddHomePoint) },
                    onAwayScoreClick = { homeViewModel.handleIntent(MatchIntent.AddAwayPoint) }
                )

                // Aksiyon paneli — aktif takıma göre
                ActionPanel(
                    selectedPlayer = activeState.selectedPlayer,
                    ballState = activeState.ballState,
                    isActionEnabled = { activeViewModel.isActionEnabledForSelected(it) },
                    onActionClick = { actionType ->
                        activeViewModel.handleIntent(MatchIntent.StartAction(actionType))
                    },
                    onClearSelection = { activeViewModel.handleIntent(MatchIntent.ClearSelection) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                // Alt butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val libero = activeState.match?.liberoId
                    val liberoOnCourt = activeState.match?.liberoOnCourt == true

                    // Log
                    FilledTonalIconButton(
                        onClick = { activeViewModel.handleIntent(MatchIntent.ShowRallyLog) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Aksiyon Listesi", modifier = Modifier.size(20.dp))
                    }

                    // Libero
                    FilledTonalIconButton(
                        onClick = { activeViewModel.handleIntent(MatchIntent.ToggleLibero) },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (liberoOnCourt) Color(0xFFFFD700) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("L", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Rotate
                    FilledTonalIconButton(
                        onClick = { activeViewModel.handleIntent(MatchIntent.Rotate) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rotasyon", modifier = Modifier.size(20.dp))
                    }

                    // Undo
                    FilledTonalIconButton(
                        onClick = { activeViewModel.handleIntent(MatchIntent.UndoLastAction) },
                        enabled = activeState.lastAction != null,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Geri Al", modifier = Modifier.size(20.dp))
                    }

                    // Sub
                    FilledTonalIconButton(
                        onClick = { activeViewModel.handleIntent(MatchIntent.ShowSubstitutionDialog) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Değişiklik", modifier = Modifier.size(20.dp))
                    }
                }

            }

            // ─── Sağ: Deplasman Sahası ───
            TeamCourtPanel(
                state = awayState,
                isActive = activeTeam == ActiveTeam.AWAY,
                teamColor = MaterialTheme.colorScheme.error,
                onActivate = { activeTeam = ActiveTeam.AWAY },
                onPositionClick = { pos ->
                    activeTeam = ActiveTeam.AWAY
                    awayViewModel.handleIntent(MatchIntent.SelectPlayer(pos))
                },
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
            )
        }
    }
}

/** Aktif takım seçimi */
enum class ActiveTeam { HOME, AWAY }

@Composable
private fun TeamCourtPanel(
    state: MatchState,
    isActive: Boolean,
    teamColor: androidx.compose.ui.graphics.Color,
    onActivate: () -> Unit,
    onPositionClick: (CourtPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.then(
            if (isActive) Modifier.border(2.dp, teamColor, RoundedCornerShape(12.dp))
            else Modifier
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp),
        onClick = onActivate
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = state.homeTeam?.name ?: "Yükleniyor...",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = teamColor
            )

            if (state.isLoading) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                VolleyballCourt(
                    courtPositions = state.courtPositions,
                    selectedPosition = state.selectedPosition,
                    onPositionClick = onPositionClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Set: ${state.setsWon.first}-${state.setsWon.second}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
