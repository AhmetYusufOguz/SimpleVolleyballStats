package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.match

import org.ahmetyusufoguz.simplevolleyballstats.domain.model.*
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.BallState

/** Maç takip modu */
enum class MatchMode {
    SINGLE_TEAM,  // Sadece ev sahibi takım aksiyonları kaydedilir
    DUAL_TEAM     // Her iki takımın aksiyonları kaydedilir
}

data class MatchState(
    // Maç bilgileri
    val match: Match? = null,
    val homeTeam: Team? = null,
    val opponentName: String = "Rakip",
    val players: List<Player> = emptyList(),
    val matchMode: MatchMode = MatchMode.SINGLE_TEAM,

    // Saha dizilişi
    val courtPositions: Map<CourtPosition, Player> = emptyMap(),
    val benchPlayers: List<Player> = emptyList(),

    // Libero sistemi
    val libero: Player? = null,
    val liberoOnCourt: Boolean = false,
    val liberoReplacedPlayer: Player? = null,
    val liberoReplacedPosition: CourtPosition? = null,

    // Mevcut durum
    val currentSet: Int = 1,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val homeServing: Boolean = true,
    val ballState: BallState = BallState.HOME_SERVE,
    val setsWon: Pair<Int, Int> = 0 to 0,
    val setScores: List<Pair<Int, Int>> = emptyList(),
    // Seti kimin servis atarak başlattığını takip eder — set geçişinde servis fliplemek için
    val setStartedWithHomeServing: Boolean = true,

    // Seçili oyuncu
    val selectedPlayer: Player? = null,
    val selectedPosition: CourtPosition? = null,

    // Aksiyonlar
    val actionHistory: List<Action> = emptyList(),

    val actions: List<Action> = emptyList(),
    val lastAction: Action? = null,

    // Rally log
    val showRallyLogDialog: Boolean = false,

    // UI durumu
    val isLoading: Boolean = false,
    val isPaused: Boolean = false,
    val showSubstitutionDialog: Boolean = false,
    val showActionResultDialog: Boolean = false,
    val pendingActionType: ActionType? = null,
    val showMatchEndDialog: Boolean = false,
    val showExitConfirmDialog: Boolean = false,
    val showServeChoiceDialog: Boolean = true,
    val showEditOpponentDialog: Boolean = false,
    val showStartingLineupDialog: Boolean = true,
    val isMatchFinished: Boolean = false,
    val shouldExit: Boolean = false,
    val isResumingMatch: Boolean = false,

    // Genel sayı dialog
    val showGeneralPointDialog: Boolean = false,


    /**
     * Slave VM için: master VM (homeVM) maç ID'si.
     * Aksiyon kaydedilirken bu ID kullanılır; slave kendi maçını oluşturmaz.
     */
    val masterMatchId: String? = null,

    val error: String? = null
)


sealed interface MatchIntent {
    // Maç kontrolü
    object StartMatch : MatchIntent
    object PauseMatch : MatchIntent
    object ResumeMatch : MatchIntent
    object EndSet : MatchIntent
    object EndMatch : MatchIntent
    data class SetOpponentName(val name: String) : MatchIntent
    data class SetInitialServe(val homeServes: Boolean) : MatchIntent
    object ShowEditOpponentDialog : MatchIntent
    object HideEditOpponentDialog : MatchIntent

    // Başlangıç dizilişi
    data class SetStartingLineup(val positions: Map<CourtPosition, Player>, val libero: Player?) : MatchIntent
    object HideStartingLineupDialog : MatchIntent

    // Skor
    object AddHomePoint : MatchIntent
    object AddAwayPoint : MatchIntent

    // Oyuncu seçimi
    data class SelectPlayer(val position: CourtPosition) : MatchIntent
    object ClearSelection : MatchIntent

    // Aksiyonlar
    data class StartAction(val actionType: ActionType) : MatchIntent
    data class RecordAction(val result: ActionResult) : MatchIntent
    object CancelAction : MatchIntent

    // Rally log
    object ShowRallyLog : MatchIntent
    object HideRallyLog : MatchIntent

    // Rotasyon & Oyuncu değişikliği
    object Rotate : MatchIntent
    object ShowSubstitutionDialog : MatchIntent
    object HideSubstitutionDialog : MatchIntent
    data class SubstitutePlayer(val outPlayer: Player, val inPlayer: Player) : MatchIntent

    // Libero
    object ToggleLibero : MatchIntent

    // Geri alma
    object UndoLastAction : MatchIntent

    // Dialog kontrolü
    object ShowExitConfirm : MatchIntent
    object HideExitConfirm : MatchIntent
    object ConfirmExit : MatchIntent

    // Genel sayı
    object ShowGeneralPointDialog : MatchIntent
    object HideGeneralPointDialog : MatchIntent
    data class RecordGeneralPoint(val reason: GeneralPointReason) : MatchIntent

    // Set/Maç bitti dialog
    object DismissMatchEndDialog : MatchIntent

    /**
     * DualTeam slave modunda master VM'den gelen durum senkronizasyonu.
     * masterBallState de dahil edildi — IN_PLAY_HOME ↔ IN_PLAY_AWAY koordinat dönüşmü yapılır.
     */
    data class SyncPeerState(
        val masterHomeScore: Int,
        val masterAwayScore: Int,
        val masterHomeServing: Boolean,
        val masterCurrentSet: Int,
        val masterHomeSetsWon: Int,
        val masterAwaySetsWon: Int,
        val masterIsMatchFinished: Boolean,
        val masterBallState: BallState        // ← Yeni: top hakimiyeti de senkronize edilir
    ) : MatchIntent

    /**
     * Top hakimiyeti güncellemesi: slave aksiyon (karşılama, pas...) sonucunda
     * puan olmayan top geçişi DualTeamMatchScreen tarafından master VM'e iletilir.
     */
    data class UpdateBallState(val ballState: BallState) : MatchIntent

    /**
     * DualTeam modunda ev sahibi VM'deki maç kaydına deplasman bilgilerini yazar.
     * SQL updateMatchTeamInfo sorgusu çağrılır — placeholder bug'u önler.
     */
    data class SetAwayTeamInfo(val awayTeamId: String, val awayTeamName: String) : MatchIntent

    /**
     * Slave VM'nin kayıt için kullanacağı master maç ID'sini set eder.
     * Slave kendi maçını oluşturmaz; tüm aksiyonlar bu ID altında kaydedilir.
     */
    data class SetMasterMatchId(val matchId: String) : MatchIntent
}