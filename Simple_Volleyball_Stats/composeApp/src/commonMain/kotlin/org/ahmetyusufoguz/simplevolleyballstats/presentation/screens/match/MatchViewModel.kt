package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.*
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.MatchRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.TeamRepository
import org.ahmetyusufoguz.simplevolleyballstats.utils.currentTimeMillis

class MatchViewModel(
    private val teamId: String,
    private val matchId: String?,
    private val teamRepository: TeamRepository,
    private val playerRepository: PlayerRepository,
    private val matchRepository: MatchRepository,
    /**
     * DualTeam modu: slave VM (deplasman) skor/servis/set bitişişini yönetmez.
     * Sadece aksiyon kaydeder. Tüm durum master VM (ev sahibi) tarafından senkronize edilir.
     */
    private val isDualTeamSlave: Boolean = false
) : ViewModel() {

    private val _state = MutableStateFlow(MatchState())
    val state: StateFlow<MatchState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val team = teamRepository.getTeamById(teamId)
            val players = playerRepository.getPlayersByTeam(teamId).first()

            if (matchId != null) {
                // Mevcut maçı yükle
                loadExistingMatch(team, players)
            } else {
                // Yeni maç oluştur
                createNewMatch(team, players)
            }
        }
    }

    private suspend fun loadExistingMatch(team: Team?, players: List<Player>) {
        val match = matchRepository.getMatchById(matchId!!)

        if (match == null) {
            createNewMatch(team, players)
            return
        }

        // Court positions'ı JSON'dan çöz
        val courtPositions = mutableMapOf<CourtPosition, Player>()
        match.courtPositionsJson?.split(";")?.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val position = try { CourtPosition.valueOf(parts[0]) } catch (e: Exception) { null }
                val player = players.find { it.id == parts[1] }
                if (position != null && player != null) {
                    courtPositions[position] = player
                }
            }
        }

        // Libero ve replaced player'ı bul
        val libero = players.find { it.id == match.liberoId }
        val liberoReplacedPlayer = players.find { it.id == match.liberoReplacedPlayerId }

        // Bench players hesapla
        val onCourt = courtPositions.values.toList()
        val benchPlayers = players.filter { player ->
            player !in onCourt && player != libero
        }

        // Aksiyonları yükle
        val actions = matchRepository.getActionsByMatch(matchId).first()

        _state.update {
            it.copy(
                match = match,
                homeTeam = team,
                players = players,
                benchPlayers = benchPlayers,
                courtPositions = courtPositions,
                homeScore = match.homeScore,
                awayScore = match.awayScore,
                currentSet = match.currentSet,
                setsWon = match.homeSetsWon to match.awaySetsWon,
                opponentName = match.awayTeamName,
                homeServing = match.servingTeam == ServingTeam.HOME,
                ballState = match.ballState,
                libero = libero,
                liberoOnCourt = match.liberoOnCourt,
                liberoReplacedPlayer = liberoReplacedPlayer,
                actions = actions,
                lastAction = actions.lastOrNull(),
                isLoading = false,
                showStartingLineupDialog = courtPositions.isEmpty(),  // Pozisyon yoksa sor
                isResumingMatch = courtPositions.isNotEmpty()
            )
        }
    }

    private suspend fun createNewMatch(team: Team?, players: List<Player>) {
        if (isDualTeamSlave) {
            // Slave VM: kendi maçını oluşturma, master'dan SetMasterMatchId gelecek
            _state.update {
                it.copy(
                    homeTeam = team,
                    players = players,
                    benchPlayers = players,
                    isLoading = false,
                    showStartingLineupDialog = true
                )
            }
            return
        }

        val match = Match.create(
            homeTeamId = teamId,
            homeTeamName = team?.name ?: "Ev Sahibi"
        )

        // Maçı DB'ye kaydet
        matchRepository.createMatch(match)

        _state.update {
            it.copy(
                match = match,
                homeTeam = team,
                players = players,
                benchPlayers = players,
                isLoading = false,
                showStartingLineupDialog = true
            )
        }
    }


    fun handleIntent(intent: MatchIntent) {
        when (intent) {
            is MatchIntent.SetOpponentName -> setOpponentName(intent.name)
            is MatchIntent.SetInitialServe -> setInitialServe(intent.homeServes)
            is MatchIntent.ShowEditOpponentDialog -> _state.update { it.copy(showEditOpponentDialog = true) }
            is MatchIntent.HideEditOpponentDialog -> _state.update { it.copy(showEditOpponentDialog = false) }

            is MatchIntent.SetStartingLineup -> setStartingLineup(intent.positions, intent.libero)
            is MatchIntent.HideStartingLineupDialog -> _state.update { it.copy(showStartingLineupDialog = false) }

            is MatchIntent.AddHomePoint -> addHomePoint()
            is MatchIntent.AddAwayPoint -> addAwayPoint()

            is MatchIntent.SelectPlayer -> selectPlayer(intent.position)
            is MatchIntent.ClearSelection -> clearSelection()

            is MatchIntent.StartAction -> startAction(intent.actionType)
            is MatchIntent.RecordAction -> recordAction(intent.result)
            is MatchIntent.CancelAction -> cancelAction()
            is MatchIntent.UndoLastAction -> undoLastAction()

            is MatchIntent.ShowRallyLog -> _state.update { it.copy(showRallyLogDialog = true) }
            is MatchIntent.HideRallyLog -> _state.update { it.copy(showRallyLogDialog = false) }

            is MatchIntent.Rotate -> rotate()

            is MatchIntent.ShowSubstitutionDialog -> _state.update { it.copy(showSubstitutionDialog = true) }
            is MatchIntent.HideSubstitutionDialog -> _state.update { it.copy(showSubstitutionDialog = false) }
            is MatchIntent.SubstitutePlayer -> substitutePlayer(intent.outPlayer, intent.inPlayer)

            is MatchIntent.ToggleLibero -> toggleLibero()

            is MatchIntent.PauseMatch -> _state.update { it.copy(isPaused = true) }
            is MatchIntent.ResumeMatch -> _state.update { it.copy(isPaused = false) }
            is MatchIntent.EndSet -> endSet()
            is MatchIntent.EndMatch -> endMatch()

            is MatchIntent.ShowExitConfirm -> _state.update { it.copy(showExitConfirmDialog = true) }
            is MatchIntent.HideExitConfirm -> _state.update { it.copy(showExitConfirmDialog = false) }
            is MatchIntent.ConfirmExit -> confirmExit()

            is MatchIntent.ShowGeneralPointDialog -> _state.update { it.copy(showGeneralPointDialog = true) }
            is MatchIntent.HideGeneralPointDialog -> _state.update { it.copy(showGeneralPointDialog = false) }
            is MatchIntent.RecordGeneralPoint -> recordGeneralPoint(intent.reason)

            is MatchIntent.DismissMatchEndDialog -> _state.update { it.copy(showMatchEndDialog = false) }

            is MatchIntent.SyncPeerState    -> syncPeerState(intent)
            is MatchIntent.UpdateBallState  -> _state.update { it.copy(ballState = intent.ballState) }
            is MatchIntent.SetAwayTeamInfo  -> setAwayTeamInfo(intent.awayTeamId, intent.awayTeamName)
            is MatchIntent.SetMasterMatchId -> _state.update { it.copy(masterMatchId = intent.matchId) }

            else -> {}
        }
    }

    private fun setOpponentName(name: String) {
        val currentState = _state.value
        _state.update { state ->
            val updatedMatch = state.match?.copy(awayTeamName = name)
            state.copy(
                opponentName = name,
                match = updatedMatch,
                showEditOpponentDialog = false
            )
        }
        // DB'ye isim güncellemesi — updateMatchTeamInfo ile kalıcı yaz
        viewModelScope.launch {
            val matchId = currentState.match?.id ?: return@launch
            val homeTeamName = currentState.homeTeam?.name ?: currentState.match?.homeTeamName ?: ""
            matchRepository.updateMatchTeamInfo(
                matchId      = matchId,
                awayTeamId   = null,   // awayTeamId SetAwayTeamInfo ile ayrıca set edilir
                awayTeamName = name,
                homeTeamName = homeTeamName
            )
        }
    }

    /**
     * DualTeam modunda ev sahibi VM'e deplasman bilgilerini yaz.
     * Ték maç satırında homeTeamId ve awayTeamId doğru dolar.
     */
    private fun setAwayTeamInfo(awayTeamId: String, awayTeamName: String) {
        val currentState = _state.value
        val match = currentState.match ?: return
        val updatedMatch = match.copy(awayTeamId = awayTeamId, awayTeamName = awayTeamName)
        _state.update { it.copy(match = updatedMatch, opponentName = awayTeamName) }
        viewModelScope.launch {
            matchRepository.updateMatchTeamInfo(
                matchId      = match.id,
                awayTeamId   = awayTeamId,
                awayTeamName = awayTeamName,
                homeTeamName = match.homeTeamName
            )
        }
    }

    private fun setInitialServe(homeServes: Boolean) {
        val ballState = if (homeServes) BallState.HOME_SERVE else BallState.AWAY_SERVE
        val selectedPos = if (homeServes) CourtPosition.POSITION_1 else null
        val selectedPlayer = if (homeServes) _state.value.courtPositions[CourtPosition.POSITION_1] else null

        _state.update {
            it.copy(
                homeServing = homeServes,
                ballState = ballState,
                showServeChoiceDialog = false,
                setStartedWithHomeServing = homeServes,
                selectedPosition = selectedPos,
                selectedPlayer = selectedPlayer
            )
        }
    }

    private fun setStartingLineup(positions: Map<CourtPosition, Player>, libero: Player?) {
        val onCourt = positions.values.toList()
        val onBench = _state.value.players.filter { player ->
            player !in onCourt && player != libero
        }

        _state.update { state ->
            // Maç durumunu IN_PROGRESS yap
            val updatedMatch = state.match?.copy(
                status = MatchStatus.IN_PROGRESS
            )
            state.copy(
                match = updatedMatch,
                courtPositions = positions,
                benchPlayers = onBench,
                libero = libero,
                showStartingLineupDialog = false
            )
        }
        // DB güncelle
        saveMatchToDb()

        saveMatchState()
    }

    private fun addHomePoint() {
        val wasAwayServing = !_state.value.homeServing

        _state.update {
            it.copy(
                homeScore = it.homeScore + 1,
                homeServing = true,
                ballState = BallState.HOME_SERVE
            )
        }

        if (wasAwayServing) {
            rotate()
        }

        checkSetEnd()
    }

    private fun addAwayPoint() {
        _state.update {
            it.copy(
                awayScore = it.awayScore + 1,
                homeServing = false,
                ballState = BallState.AWAY_SERVE
            )
        }
        checkSetEnd()
    }

    private fun checkLiberoAfterRotation() {
        val currentState = _state.value
        val libero = currentState.libero ?: currentState.players.find { it.position == PlayerPosition.LIBERO } ?: return

        if (currentState.liberoOnCourt) {
            val liberoPosition = currentState.courtPositions.entries
                .find { it.value.id == libero.id }?.key ?: return

            if (liberoPosition.isFrontRow()) {
                val replacedPlayer = currentState.liberoReplacedPlayer ?: return

                val newPositions = currentState.courtPositions.toMutableMap()
                newPositions[liberoPosition] = replacedPlayer

                _state.update {
                    it.copy(
                        courtPositions = newPositions,
                        liberoOnCourt = false,
                        liberoReplacedPlayer = null,
                        liberoReplacedPosition = null
                    )
                }
            }
        }
    }

    private fun toggleLibero() {
        val currentState = _state.value
        val libero = currentState.libero ?: currentState.players.find { it.position == PlayerPosition.LIBERO } ?: return

        if (currentState.liberoOnCourt) {
            val liberoPosition = currentState.courtPositions.entries
                .find { it.value.id == libero.id }?.key ?: return
            val replacedPlayer = currentState.liberoReplacedPlayer ?: return

            val newPositions = currentState.courtPositions.toMutableMap()
            newPositions[liberoPosition] = replacedPlayer

            _state.update {
                it.copy(
                    courtPositions = newPositions,
                    liberoOnCourt = false,
                    liberoReplacedPlayer = null,
                    liberoReplacedPosition = null
                )
            }
        } else {
            val backRowMiddleBlocker = findBackRowMiddleBlocker()
            if (backRowMiddleBlocker != null) {
                val (position, player) = backRowMiddleBlocker

                val newPositions = currentState.courtPositions.toMutableMap()
                newPositions[position] = libero

                _state.update {
                    it.copy(
                        courtPositions = newPositions,
                        liberoOnCourt = true,
                        liberoReplacedPlayer = player,
                        liberoReplacedPosition = position
                    )
                }
            }
        }
    }

    private fun findBackRowMiddleBlocker(): Pair<CourtPosition, Player>? {
        val currentState = _state.value
        val backRowPositions = listOf(
            CourtPosition.POSITION_1,
            CourtPosition.POSITION_5,
            CourtPosition.POSITION_6
        )

        for (position in backRowPositions) {
            val player = currentState.courtPositions[position]
            if (player?.position == PlayerPosition.MIDDLE_BLOCKER) {
                return position to player
            }
        }
        return null
    }

    private fun selectPlayer(position: CourtPosition) {
        val player = _state.value.courtPositions[position]
        _state.update {
            it.copy(
                selectedPlayer = player,
                selectedPosition = position
            )
        }
    }

    private fun clearSelection() {
        _state.update {
            it.copy(
                selectedPlayer = null,
                selectedPosition = null,
                pendingActionType = null,
                showActionResultDialog = false
            )
        }
    }

    private fun startAction(actionType: ActionType) {
        if (_state.value.selectedPlayer == null) return

        _state.update {
            it.copy(
                pendingActionType = actionType,
                showActionResultDialog = true
            )
        }
    }

    private fun recordAction(result: ActionResult) {
        val currentState = _state.value
        val player = currentState.selectedPlayer ?: return
        val position = currentState.selectedPosition ?: return
        val actionType = currentState.pendingActionType ?: return

        val wasAwayServing = !currentState.homeServing

        val action = Action.create(
            // Slave: masterMatchId kullan; master: kendi maç ID'si
            matchId = if (isDualTeamSlave) currentState.masterMatchId ?: "" else currentState.match?.id ?: "",
            playerId = player.id,
            teamId = teamId,
            setNumber = currentState.currentSet,
            homeScore = currentState.homeScore,
            awayScore = currentState.awayScore,
            courtPosition = position,
            actionType = actionType,
            result = result
        )

        // DB'ye aksiyonu kaydet
        viewModelScope.launch {
            matchRepository.addAction(action)
        }

        // ─── SLAVE MODU: Sadece aksiyonu kaydet, skoru master yönetir ───
        if (isDualTeamSlave) {
            _state.update { state ->
                state.copy(
                    actions = state.actions + action,
                    lastAction = action,           // Master'daki LaunchedEffect bunu dinler
                    selectedPlayer = null,
                    selectedPosition = null,
                    pendingActionType = null,
                    showActionResultDialog = false
                )
            }
            // Skor güncelleme, rotasyon, set bitişi — master VM halleder
            return
        }

        // ─── NORMAL MOD: Tam skor + rotasyon + set-bitiş mantığı ───
        _state.update { state ->
            var newHomeScore = state.homeScore
            var newAwayScore = state.awayScore
            var newHomeServing = state.homeServing

            // Top durumu değişimi
            val newBallState = when (result.ballStateChange) {
                BallStateChange.POINT_HOME -> {
                    newHomeScore++
                    newHomeServing = true
                    BallState.HOME_SERVE
                }
                BallStateChange.POINT_AWAY -> {
                    newAwayScore++
                    newHomeServing = false
                    BallState.AWAY_SERVE
                }
                BallStateChange.TO_HOME -> BallState.IN_PLAY_HOME
                BallStateChange.TO_AWAY -> BallState.IN_PLAY_AWAY
                BallStateChange.SERVE_TO_AWAY -> BallState.AWAY_RECEIVING
                BallStateChange.SERVE_TO_HOME -> BallState.HOME_RECEIVING
                BallStateChange.STAY -> state.ballState
                BallStateChange.NONE -> state.ballState
            }

            // Auto-select server if entering HOME_SERVE
            var newSelectedPlayer = state.selectedPlayer
            var newSelectedPosition = state.selectedPosition
            
            if (newBallState == BallState.HOME_SERVE) {
                newSelectedPosition = CourtPosition.POSITION_1
                newSelectedPlayer = state.courtPositions[CourtPosition.POSITION_1]
            } else {
                newSelectedPlayer = null
                newSelectedPosition = null
            }

            state.copy(
                actions = state.actions + action,
                lastAction = action,
                homeScore = newHomeScore,
                awayScore = newAwayScore,
                homeServing = newHomeServing,
                ballState = newBallState,
                selectedPlayer = newSelectedPlayer,
                selectedPosition = newSelectedPosition,
                pendingActionType = null,
                showActionResultDialog = false
            )
        }

        // Servis kazanınca rotasyon
        if (result.pointValue > 0 && wasAwayServing) {
            rotate()
        }

        saveMatchState()
        checkSetEnd()
    }

    private fun cancelAction() {
        _state.update {
            it.copy(
                pendingActionType = null,
                showActionResultDialog = false
            )
        }
    }

    private fun undoLastAction() {
        val currentState = _state.value
        val lastAction = currentState.lastAction ?: return

        // DB'den aksiyonu sil
        viewModelScope.launch {
            matchRepository.deleteAction(lastAction.id)
        }

        _state.update { state ->
            var newHomeScore = state.homeScore
            var newAwayScore = state.awayScore

            when {
                lastAction.result.pointValue > 0 -> newHomeScore--
                lastAction.result.pointValue < 0 -> newAwayScore--
            }

            val newActions = state.actions.dropLast(1)
            val previousAction = newActions.lastOrNull()

            // Önceki aksiyonun durumuna göre ball state belirle
            val newBallState = if (previousAction == null) {
                if (state.homeServing) BallState.HOME_SERVE else BallState.AWAY_SERVE
            } else {
                when (previousAction.result.ballStateChange) {
                    BallStateChange.POINT_HOME -> BallState.HOME_SERVE
                    BallStateChange.POINT_AWAY -> BallState.AWAY_SERVE
                    BallStateChange.TO_HOME -> BallState.IN_PLAY_HOME
                    BallStateChange.TO_AWAY -> BallState.IN_PLAY_AWAY
                    else -> if (state.homeServing) BallState.HOME_SERVE else BallState.AWAY_SERVE
                }
            }

            state.copy(
                actions = newActions,
                lastAction = previousAction,
                homeScore = newHomeScore,
                awayScore = newAwayScore,
                ballState = newBallState
            )
        }
    }

    private fun rotate() {
        _state.update { state ->
            val newPositions = state.courtPositions.mapKeys { (pos, _) ->
                pos.rotateClockwise()
            }
            state.copy(courtPositions = newPositions)
        }
        checkLiberoAfterRotation()

        saveMatchState()
    }

    private fun substitutePlayer(outPlayer: Player, inPlayer: Player) {
        _state.update { state ->
            val position = state.courtPositions.entries
                .find { it.value.id == outPlayer.id }?.key
                ?: return@update state

            val newCourtPositions = state.courtPositions.toMutableMap()
            newCourtPositions[position] = inPlayer

            val newBench = state.benchPlayers.toMutableList()
            newBench.remove(inPlayer)
            newBench.add(outPlayer)

            val libero = state.libero ?: state.players.find { it.position == PlayerPosition.LIBERO }
            val isSubbingInLibero = libero != null && inPlayer.id == libero.id
            val isSubbingOutLibero = libero != null && outPlayer.id == libero.id

            state.copy(
                courtPositions = newCourtPositions,
                benchPlayers = newBench,
                showSubstitutionDialog = false,
                liberoOnCourt = if (isSubbingInLibero) true else if (isSubbingOutLibero) false else state.liberoOnCourt,
                liberoReplacedPlayer = if (isSubbingInLibero) outPlayer else if (isSubbingOutLibero) null else state.liberoReplacedPlayer,
                liberoReplacedPosition = if (isSubbingInLibero) position else if (isSubbingOutLibero) null else state.liberoReplacedPosition
            )
        }
        saveMatchState()
    }

    /**
     * DualTeam slave modu: master VM'den gelen durum güncellemesini uygula.
     *
     * Koordinat dönüşmü (master → slave perspektifi):
     *   score : slave.homeScore = master.awayScore, slave.awayScore = master.homeScore
     *   serve : slave.homeServing = !master.homeServing
     *   ball  : HOME_SERVE ↔ AWAY_SERVE, IN_PLAY_HOME ↔ IN_PLAY_AWAY
     */
    private fun syncPeerState(intent: MatchIntent.SyncPeerState) {
        val currentState = _state.value
        val wasServing = currentState.homeServing
        val setChanged = intent.masterCurrentSet != currentState.currentSet

        val myNewScore  = intent.masterAwayScore   // Master'dan: bize karsı skoru = bizim skorumuz
        val oppNewScore = intent.masterHomeScore    // Master'dan: kendi skoru   = rakibimizin skoru
        val nowServing  = !intent.masterHomeServing

        // Top hakimiyeti koordinat dönüşmü:
        //   Master'da HOME_SERVE   → Slave'de AWAY_SERVE  (master servis atıyor = biz alıyoruz)
        //   Master'da AWAY_SERVE   → Slave'de HOME_SERVE  (master alıyor    = biz servis atıyoruz)
        //   Master'da IN_PLAY_HOME → Slave'de IN_PLAY_AWAY (top master'da   = bizim rakibimizde)
        //   Master'da IN_PLAY_AWAY → Slave'de IN_PLAY_HOME (top master'da dep = bizde!)  ← KRITIK
        val slaveBallState = when (intent.masterBallState) {
            BallState.HOME_SERVE   -> BallState.AWAY_SERVE
            BallState.AWAY_SERVE   -> BallState.HOME_SERVE
            BallState.HOME_RECEIVING -> BallState.AWAY_RECEIVING
            BallState.AWAY_RECEIVING -> BallState.HOME_RECEIVING
            BallState.IN_PLAY_HOME -> BallState.IN_PLAY_AWAY
            BallState.IN_PLAY_AWAY -> BallState.IN_PLAY_HOME  // Top bize geçti!
        }

        // Auto-select server for slave
        val newSelectedPos = if (slaveBallState == BallState.HOME_SERVE && currentState.ballState != BallState.HOME_SERVE) CourtPosition.POSITION_1 else currentState.selectedPosition
        val newSelectedPlayer = if (slaveBallState == BallState.HOME_SERVE && currentState.ballState != BallState.HOME_SERVE) currentState.courtPositions[CourtPosition.POSITION_1] else currentState.selectedPlayer

        _state.update {
            it.copy(
                homeScore          = myNewScore,
                awayScore          = oppNewScore,
                homeServing        = nowServing,
                currentSet         = intent.masterCurrentSet,
                setsWon            = intent.masterAwaySetsWon to intent.masterHomeSetsWon,
                ballState          = slaveBallState,
                selectedPosition   = newSelectedPos,
                selectedPlayer     = newSelectedPlayer,
                isMatchFinished    = intent.masterIsMatchFinished,
                showMatchEndDialog = if (intent.masterIsMatchFinished) true else it.showMatchEndDialog,
                setStartedWithHomeServing = !intent.masterHomeServing
            )
        }

        // Side-out rotasyon: biz servis kazandıysak (alıcı → servisci) rota yap
        if (!wasServing && nowServing && !setChanged) {
            rotate()
        }
    }

    private fun checkSetEnd() {
        val state = _state.value
        val home = state.homeScore
        val away = state.awayScore

        // Tie-break (3. set) 15 sayıda biter, normal setler 25
        val targetScore = if (state.currentSet == 3) 15 else 25
        val isSetOver = (home >= targetScore || away >= targetScore) &&
                kotlin.math.abs(home - away) >= 2

        if (isSetOver) {
            // Otomatik olarak seti bitir
            endSet()
        }
    }

    private fun endSet() {
        val state = _state.value
        val homeWon = state.homeScore > state.awayScore

        val newSetsWon = if (homeWon) {
            state.setsWon.first + 1 to state.setsWon.second
        } else {
            state.setsWon.first to state.setsWon.second + 1
        }

        val newSetScores = state.setScores + (state.homeScore to state.awayScore)

        // Set skorunu DB'ye kaydet
        viewModelScope.launch {
            matchRepository.saveSetResult(
                matchId = state.match?.id ?: "",
                setNumber = state.currentSet,
                homeScore = state.homeScore,
                awayScore = state.awayScore
            )
        }

        // Maç bitti mi kontrol — 2 set kazanan maçı alır
        if (newSetsWon.first == 2 || newSetsWon.second == 2) {
            _state.update {
                it.copy(
                    setsWon = newSetsWon,
                    setScores = newSetScores,
                    showMatchEndDialog = true,
                    isMatchFinished = true
                )
            }
            finalizeMatch()
            return
        }

        // Yeni set için: önceki sette ALICI olan takım yeni sette servis atar
        // Kural: setStartedWithHomeServing'i flip et
        val newHomeServing = !state.setStartedWithHomeServing
        val newPositions = state.courtPositions

        _state.update {
            val newSelectedPos = if (newHomeServing) CourtPosition.POSITION_1 else null
            val newSelectedPlayer = if (newHomeServing) newPositions[CourtPosition.POSITION_1] else null

            it.copy(
                homeScore = 0,
                awayScore = 0,
                currentSet = it.currentSet + 1,
                setsWon = newSetsWon,
                homeServing = newHomeServing,
                ballState = if (newHomeServing) BallState.HOME_SERVE else BallState.AWAY_SERVE,
                courtPositions = newPositions,
                selectedPosition = newSelectedPos,
                selectedPlayer = newSelectedPlayer,
                showMatchEndDialog = true,
                setStartedWithHomeServing = newHomeServing
            )
        }

        // Maç durumunu güncelle
        saveMatchToDb()
    }

    private fun endMatch() {
        _state.update { it.copy(showMatchEndDialog = true) }
    }

    private fun finalizeMatch() {
        val state = _state.value

        viewModelScope.launch {
            val updatedMatch = state.match?.copy(
                status = MatchStatus.FINISHED,
                homeSetsWon = state.setsWon.first,
                awaySetsWon = state.setsWon.second,
                endedAt = currentTimeMillis()
            )

            if (updatedMatch != null) {
                matchRepository.updateMatch(updatedMatch)
            }
        }
    }

    private fun confirmExit() {
        // Maçı "Tamamlandı" olarak kaydet — devam ettirme özelliği kaldırıldı
        viewModelScope.launch {
            val state = _state.value
            val match = state.match ?: return@launch

            // Son set skorunu kaydet (eğer devam eden set varsa)
            if (state.homeScore > 0 || state.awayScore > 0) {
                matchRepository.saveSetResult(
                    matchId = match.id,
                    setNumber = state.currentSet,
                    homeScore = state.homeScore,
                    awayScore = state.awayScore
                )
            }

            // Maçı FINISHED olarak işaretle — kullanıcı devam edemez
            val updatedMatch = match.copy(
                status = MatchStatus.FINISHED,
                homeSetsWon = state.setsWon.first,
                awaySetsWon = state.setsWon.second,
                endedAt = currentTimeMillis()
            )
            matchRepository.updateMatch(updatedMatch)
        }

        _state.update { it.copy(showExitConfirmDialog = false, shouldExit = true) }
    }

    private fun saveMatchToDb() {
        viewModelScope.launch {
            val state = _state.value
            val match = state.match ?: return@launch

            val updatedMatch = match.copy(
                awayTeamName = state.opponentName,
                homeSetsWon = state.setsWon.first,
                awaySetsWon = state.setsWon.second,
                currentSet = state.currentSet
            )
            matchRepository.updateMatch(updatedMatch)
        }
    }

    /**
     * Aksiyon aktif mi kontrolü - Oyuncu ve pozisyon bazlı
     */
    fun isActionEnabled(actionType: ActionType, player: Player?, position: CourtPosition?): Boolean {
        if (player == null || position == null) return false

        val state = _state.value
        val isLibero = player.position == PlayerPosition.LIBERO
        val isFrontRow = position.isFrontRow()

        return when (state.ballState) {
            BallState.HOME_SERVE -> {
                actionType == ActionType.SERVE &&
                        position == CourtPosition.POSITION_1 &&
                        !isLibero
            }

            BallState.AWAY_SERVE, BallState.HOME_RECEIVING -> {
                actionType == ActionType.RECEIVE
            }

            BallState.IN_PLAY_HOME -> {
                when (actionType) {
                    ActionType.SET -> true
                    ActionType.SPIKE -> !isLibero
                    ActionType.SERVE -> false
                    ActionType.RECEIVE -> false
                    ActionType.BLOCK -> false
                    ActionType.DIG -> true
                    ActionType.GENERAL_POINT -> false
                }
            }

            BallState.AWAY_RECEIVING, BallState.IN_PLAY_AWAY -> {
                when (actionType) {
                    ActionType.BLOCK -> isFrontRow && !isLibero
                    ActionType.DIG -> true
                    ActionType.SET -> true
                    ActionType.SPIKE -> !isLibero
                    ActionType.SERVE -> false
                    ActionType.RECEIVE -> false
                    ActionType.GENERAL_POINT -> false
                }
            }
        }
    }

    fun isActionEnabledForSelected(actionType: ActionType): Boolean {
        val state = _state.value
        return isActionEnabled(actionType, state.selectedPlayer, state.selectedPosition)
    }

    private fun recordGeneralPoint(reason: GeneralPointReason) {
        val currentState = _state.value
        val wasAwayServing = !currentState.homeServing

        val result = ActionResult.fromGeneralPointReason(reason)

        val action = Action.create(
            matchId = currentState.match?.id ?: "",
            playerId = "TEAM",
            teamId = teamId,
            setNumber = currentState.currentSet,
            homeScore = currentState.homeScore,
            awayScore = currentState.awayScore,
            courtPosition = CourtPosition.POSITION_6,
            actionType = ActionType.GENERAL_POINT,
            result = result
        )

        // DB'ye kaydet
        viewModelScope.launch {
            matchRepository.addAction(action)
        }

        _state.update { state ->
            val newHomeScore: Int
            val newAwayScore: Int
            val newHomeServing: Boolean
            val newBallState: BallState

            if (reason.forUs) {
                newHomeScore = state.homeScore + 1
                newAwayScore = state.awayScore
                newHomeServing = true
                newBallState = BallState.HOME_SERVE
            } else {
                newHomeScore = state.homeScore
                newAwayScore = state.awayScore + 1
                newHomeServing = false
                newBallState = BallState.AWAY_SERVE
            }

            state.copy(
                actions = state.actions + action,
                lastAction = action,
                homeScore = newHomeScore,
                awayScore = newAwayScore,
                homeServing = newHomeServing,
                ballState = newBallState,
                showGeneralPointDialog = false
            )
        }

        if (reason.forUs && wasAwayServing) {
            rotate()
        }

        checkSetEnd()
    }

    fun isGeneralPointReasonEnabled(reason: GeneralPointReason): Boolean {
        val ballState = _state.value.ballState

        return when (ballState) {
            BallState.HOME_SERVE -> {
                when (reason) {
                    GeneralPointReason.OPPONENT_ROTATION_ERROR -> true
                    GeneralPointReason.OPPONENT_OTHER -> true
                    GeneralPointReason.TEAM_ROTATION_ERROR -> true
                    GeneralPointReason.TEAM_FOOT_FAULT -> true
                    GeneralPointReason.TEAM_OTHER -> true
                    else -> false
                }
            }

            BallState.AWAY_SERVE, BallState.HOME_RECEIVING -> {
                when (reason) {
                    GeneralPointReason.OPPONENT_SERVE_ERROR -> true
                    GeneralPointReason.OPPONENT_ROTATION_ERROR -> true
                    GeneralPointReason.OPPONENT_FOOT_FAULT -> true
                    GeneralPointReason.OPPONENT_OTHER -> true
                    GeneralPointReason.TEAM_ROTATION_ERROR -> true
                    GeneralPointReason.TEAM_BALL_LANDED -> true
                    GeneralPointReason.TEAM_OTHER -> true
                    else -> false
                }
            }

            BallState.IN_PLAY_HOME -> {
                when (reason) {
                    GeneralPointReason.OPPONENT_BLOCK_ERROR -> true
                    GeneralPointReason.OPPONENT_NET_TOUCH -> true
                    GeneralPointReason.OPPONENT_OTHER -> true
                    GeneralPointReason.TEAM_DOUBLE_TOUCH -> true
                    GeneralPointReason.TEAM_FOUR_TOUCH -> true
                    GeneralPointReason.TEAM_NET_TOUCH -> true
                    GeneralPointReason.TEAM_FOOT_FAULT -> true
                    GeneralPointReason.TEAM_OTHER -> true
                    else -> false
                }
            }

            BallState.AWAY_RECEIVING, BallState.IN_PLAY_AWAY -> {
                when (reason) {
                    GeneralPointReason.OPPONENT_SPIKE_ERROR -> true
                    GeneralPointReason.OPPONENT_DOUBLE_TOUCH -> true
                    GeneralPointReason.OPPONENT_FOUR_TOUCH -> true
                    GeneralPointReason.OPPONENT_NET_TOUCH -> true
                    GeneralPointReason.OPPONENT_FOOT_FAULT -> true
                    GeneralPointReason.OPPONENT_BALL_DROPPED -> true
                    GeneralPointReason.OPPONENT_OTHER -> true
                    GeneralPointReason.TEAM_DOUBLE_TOUCH -> true
                    GeneralPointReason.TEAM_NET_TOUCH -> true
                    GeneralPointReason.TEAM_BALL_LANDED -> true
                    GeneralPointReason.TEAM_OTHER -> true
                    else -> false
                }
            }
        }
    }

    fun getRallies(): List<Rally> {
        val actions = _state.value.actions
        val rallies = mutableListOf<Rally>()
        var currentRallyActions = mutableListOf<Action>()
        var rallyStartScore = 0 to 0

        actions.forEach { action ->
            if (currentRallyActions.isEmpty()) {
                rallyStartScore = action.homeScore to action.awayScore
            }

            currentRallyActions.add(action)

            if (action.result.pointValue != 0) {
                val endScore = if (action.result.pointValue > 0) {
                    (action.homeScore + 1) to action.awayScore
                } else {
                    action.homeScore to (action.awayScore + 1)
                }

                rallies.add(Rally(
                    number = rallies.size + 1,
                    startScore = rallyStartScore,
                    endScore = endScore,
                    actions = currentRallyActions.toList(),
                    weScored = action.result.pointValue > 0
                ))
                currentRallyActions = mutableListOf()
            }
        }

        if (currentRallyActions.isNotEmpty()) {
            rallies.add(Rally(
                number = rallies.size + 1,
                startScore = rallyStartScore,
                endScore = _state.value.homeScore to _state.value.awayScore,
                actions = currentRallyActions.toList(),
                weScored = null
            ))
        }

        return rallies
    }

    private fun saveMatchState() {
        viewModelScope.launch {
            val state = _state.value
            val match = state.match ?: return@launch

            // Court positions'ı JSON'a çevir
            val positionsJson = state.courtPositions.entries.joinToString(";") { (pos, player) ->
                "${pos.name}:${player.id}"
            }

            val updatedMatch = match.copy(
                awayTeamName = state.opponentName,
                homeSetsWon = state.setsWon.first,
                awaySetsWon = state.setsWon.second,
                currentSet = state.currentSet,
                servingTeam = if (state.homeServing) ServingTeam.HOME else ServingTeam.AWAY,
                ballState = state.ballState,
                courtPositionsJson = positionsJson,
                liberoId = state.libero?.id,
                liberoOnCourt = state.liberoOnCourt,
                liberoReplacedPlayerId = state.liberoReplacedPlayer?.id,
                status = MatchStatus.IN_PROGRESS
            )

            matchRepository.updateMatch(updatedMatch)
        }
    }

    data class Rally(
        val number: Int,
        val startScore: Pair<Int, Int>,
        val endScore: Pair<Int, Int>,
        val actions: List<Action>,
        val weScored: Boolean?
    )
}