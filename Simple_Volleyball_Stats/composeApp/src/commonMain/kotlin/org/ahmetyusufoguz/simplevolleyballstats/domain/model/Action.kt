package org.ahmetyusufoguz.simplevolleyballstats.domain.model

import org.ahmetyusufoguz.simplevolleyballstats.utils.currentTimeMillis

data class Action(
    val id: String,
    val matchId: String,
    val playerId: String,
    val teamId: String,
    val setNumber: Int,
    val homeScore: Int,
    val awayScore: Int,
    val courtPosition: CourtPosition,
    val actionType: ActionType,
    val result: ActionResult,
    val timestamp: Long,
    val notes: String? = null
) {
    /** DataVolley kodu: ör. "S#" (Serve Ace), "A=" (Attack Error) */
    val dataVolleyCode: String
        get() = "${actionType.dvCode}${result.dataVolleySymbol}"

    companion object {
        fun create(
            matchId: String,
            playerId: String,
            teamId: String,
            setNumber: Int,
            homeScore: Int,
            awayScore: Int,
            courtPosition: CourtPosition,
            actionType: ActionType,
            result: ActionResult,
            notes: String? = null
        ): Action {
            val now = currentTimeMillis()
            return Action(
                id = "action_${now}_${(0..9999).random()}",
                matchId = matchId,
                playerId = playerId,
                teamId = teamId,
                setNumber = setNumber,
                homeScore = homeScore,
                awayScore = awayScore,
                courtPosition = courtPosition,
                actionType = actionType,
                result = result,
                timestamp = now,
                notes = notes
            )
        }
    }
}

enum class ActionType(
    val displayName: String,
    val emoji: String,
    /** DataVolley standart kodu: S=Serve, R=Reception, A=Attack, B=Block, D=Dig, E=Set */
    val dvCode: String
) {
    SERVE("Servis", "🏐", "S"),
    RECEIVE("S. Karşılama", "🙌", "R"),
    SPIKE("Smaç", "🔥", "A"),
    BLOCK("Blok", "🛡️", "B"),
    DIG("Savunma", "💪", "D"),
    SET("Pas", "🎯", "E"),
    GENERAL_POINT("Genel Sayı", "±", "G");

    fun getAvailableResults(): List<ActionResult> = when (this) {
        SERVE -> ActionResult.forServe()
        RECEIVE -> ActionResult.forReceive()
        SPIKE -> ActionResult.forSpike()
        BLOCK -> ActionResult.forBlock()
        DIG -> ActionResult.forDig()
        SET -> ActionResult.forSet()
        GENERAL_POINT -> ActionResult.forGeneralPoint()
    }
}

enum class BallStateChange {
    NONE,           // Değişiklik yok
    STAY,           // Aynı kalır
    TO_HOME,        // Top bize geçer (IN_PLAY_HOME)
    TO_AWAY,        // Top rakibe geçer (IN_PLAY_AWAY)
    SERVE_TO_AWAY,  // Biz servis attık, rakip karşılamak zorunda (AWAY_RECEIVING)
    SERVE_TO_HOME,  // Rakip servis attı, biz karşılamak zorundayız (HOME_RECEIVING)
    POINT_HOME,     // Bize sayı (HOME_SERVE)
    POINT_AWAY      // Rakibe sayı (AWAY_SERVE)
}

enum class ActionResult(
    val displayName: String,
    val isPositive: Boolean,
    val pointValue: Int,
    val ballStateChange: BallStateChange,
    /** DataVolley kalite sembolü: #=Mükemmel, +=Pozitif, !=Nötr, -=Negatif, /=Overpass, ==Hata */
    val dataVolleySymbol: String
) {
    // ─── Servis sonuçları (DataVolley: #, +, !, -, =) ───
    SERVE_PERFECT("Mükemmel (Ace)", true, 1, BallStateChange.POINT_HOME, "#"),
    SERVE_POSITIVE("Pozitif", true, 0, BallStateChange.SERVE_TO_AWAY, "+"),
    SERVE_NEUTRAL("Nötr", true, 0, BallStateChange.SERVE_TO_AWAY, "!"),
    SERVE_NEGATIVE("Negatif", false, 0, BallStateChange.SERVE_TO_AWAY, "-"),
    SERVE_ERROR("Hata", false, -1, BallStateChange.POINT_AWAY, "="),

    // ─── Karşılama sonuçları (DataVolley: #, +, !, -, /, =) ───
    RECEIVE_PERFECT("Mükemmel", true, 0, BallStateChange.TO_HOME, "#"),
    RECEIVE_POSITIVE("Pozitif", true, 0, BallStateChange.TO_HOME, "+"),
    RECEIVE_NEUTRAL("Nötr", true, 0, BallStateChange.TO_HOME, "!"),
    RECEIVE_NEGATIVE("Negatif", false, 0, BallStateChange.TO_HOME, "-"),
    RECEIVE_OVERPASS("Karşıya Geçen Top", false, 0, BallStateChange.TO_AWAY, "/"),
    RECEIVE_ERROR("Hata (Ace yendi)", false, -1, BallStateChange.POINT_AWAY, "="),

    // ─── Smaç / Hücum sonuçları (DataVolley: #, +, !, -, /, =) ───
    ATTACK_PERFECT("Mükemmel (Kill)", true, 1, BallStateChange.POINT_HOME, "#"),
    ATTACK_POSITIVE("Pozitif", true, 0, BallStateChange.TO_AWAY, "+"),
    ATTACK_NEUTRAL("Nötr (Blok sekme)", true, 0, BallStateChange.TO_HOME, "!"),
    ATTACK_NEGATIVE("Negatif", false, 0, BallStateChange.TO_AWAY, "-"),
    ATTACK_BLOCKED("Bloklandı", false, -1, BallStateChange.POINT_AWAY, "/"),
    ATTACK_ERROR("Hata", false, -1, BallStateChange.POINT_AWAY, "="),

    // ─── Blok sonuçları (DataVolley: #, +, !, -, =) ───
    BLOCK_PERFECT("Mükemmel (Stuff)", true, 1, BallStateChange.POINT_HOME, "#"),
    BLOCK_POSITIVE("Pozitif", true, 0, BallStateChange.TO_AWAY, "+"),
    BLOCK_NEUTRAL("Nötr (Rebound)", true, 0, BallStateChange.TO_HOME, "!"),
    BLOCK_NEGATIVE("Negatif (Block-out)", false, -1, BallStateChange.POINT_AWAY, "-"),
    BLOCK_ERROR("Hata", false, -1, BallStateChange.POINT_AWAY, "="),

    // ─── Savunma sonuçları (DataVolley: #, +, -, /, =) ───
    DIG_PERFECT("Mükemmel", true, 0, BallStateChange.TO_HOME, "#"),
    DIG_POSITIVE("Pozitif", true, 0, BallStateChange.TO_HOME, "+"),
    DIG_NEGATIVE("Negatif", false, 0, BallStateChange.TO_HOME, "-"),
    DIG_OVERPASS("Karşıya Geçen Top", false, 0, BallStateChange.TO_AWAY, "/"),
    DIG_ERROR("Hata", false, -1, BallStateChange.POINT_AWAY, "="),

    // ─── Pas sonuçları (DataVolley: #, +, -, =) ───
    SET_PERFECT("Mükemmel", true, 0, BallStateChange.STAY, "#"),
    SET_POSITIVE("Pozitif", true, 0, BallStateChange.STAY, "+"),
    SET_NEGATIVE("Negatif", false, 0, BallStateChange.STAY, "-"),
    SET_ERROR("Hata", false, -1, BallStateChange.POINT_AWAY, "="),

    // ─── Genel Sayı sonuçları (Rakip Hataları) ───
    OPPONENT_SERVE_ERROR("Rakip Servis Hatası", true, 1, BallStateChange.POINT_HOME, "#"),
    OPPONENT_SPIKE_ERROR("Rakip Smaç Hatası", true, 1, BallStateChange.POINT_HOME, "#"),
    OPPONENT_BLOCK_ERROR("Rakip Blok Hatası", true, 1, BallStateChange.POINT_HOME, "#"),
    OPPONENT_DOUBLE_TOUCH("Rakip Çift Top", true, 1, BallStateChange.POINT_HOME, "#"),
    OPPONENT_FOUR_TOUCH("Rakip 4 Top", true, 1, BallStateChange.POINT_HOME, "#"),
    OPPONENT_NET_TOUCH("Rakip File Hatası", true, 1, BallStateChange.POINT_HOME, "#"),
    OPPONENT_ROTATION_ERROR("Rakip Pozisyon Hatası", true, 1, BallStateChange.POINT_HOME, "#"),
    OPPONENT_FOOT_FAULT("Rakip Ayak Hatası", true, 1, BallStateChange.POINT_HOME, "#"),
    OPPONENT_OTHER("Rakip Diğer Hata", true, 1, BallStateChange.POINT_HOME, "#"),
    OPPONENT_BALL_DROPPED("Rakip Top Düşürdü", true, 1, BallStateChange.POINT_HOME, "#"),

    // ─── Genel Sayı sonuçları (Bizim Hatalar) ───
    TEAM_DOUBLE_TOUCH("Çift Top", false, -1, BallStateChange.POINT_AWAY, "="),
    TEAM_FOUR_TOUCH("4 Top", false, -1, BallStateChange.POINT_AWAY, "="),
    TEAM_NET_TOUCH("File Hatası", false, -1, BallStateChange.POINT_AWAY, "="),
    TEAM_ROTATION_ERROR("Pozisyon Hatası", false, -1, BallStateChange.POINT_AWAY, "="),
    TEAM_FOOT_FAULT("Ayak Hatası", false, -1, BallStateChange.POINT_AWAY, "="),
    TEAM_BALL_LANDED("Top Yere Düştü", false, -1, BallStateChange.POINT_AWAY, "="),
    TEAM_OTHER("Diğer Hata", false, -1, BallStateChange.POINT_AWAY, "=");

    companion object {
        fun forServe() = listOf(SERVE_PERFECT, SERVE_POSITIVE, SERVE_NEUTRAL, SERVE_NEGATIVE, SERVE_ERROR)
        fun forReceive() = listOf(RECEIVE_PERFECT, RECEIVE_POSITIVE, RECEIVE_NEUTRAL, RECEIVE_NEGATIVE, RECEIVE_OVERPASS, RECEIVE_ERROR)
        fun forSpike() = listOf(ATTACK_PERFECT, ATTACK_POSITIVE, ATTACK_NEUTRAL, ATTACK_NEGATIVE, ATTACK_BLOCKED, ATTACK_ERROR)
        fun forBlock() = listOf(BLOCK_PERFECT, BLOCK_POSITIVE, BLOCK_NEUTRAL, BLOCK_NEGATIVE, BLOCK_ERROR)
        fun forDig() = listOf(DIG_PERFECT, DIG_POSITIVE, DIG_NEGATIVE, DIG_OVERPASS, DIG_ERROR)
        fun forSet() = listOf(SET_PERFECT, SET_POSITIVE, SET_NEGATIVE, SET_ERROR)
        fun forGeneralPoint() = listOf(
            OPPONENT_SERVE_ERROR, OPPONENT_SPIKE_ERROR, OPPONENT_BLOCK_ERROR,
            OPPONENT_DOUBLE_TOUCH, OPPONENT_FOUR_TOUCH, OPPONENT_NET_TOUCH,
            OPPONENT_ROTATION_ERROR, OPPONENT_FOOT_FAULT, OPPONENT_OTHER,
            TEAM_DOUBLE_TOUCH, TEAM_FOUR_TOUCH, TEAM_NET_TOUCH,
            TEAM_ROTATION_ERROR, TEAM_FOOT_FAULT, TEAM_BALL_LANDED, TEAM_OTHER
        )

        // GeneralPointReason'dan ActionResult'a dönüşüm
        fun fromGeneralPointReason(reason: GeneralPointReason): ActionResult {
            return when (reason) {
                GeneralPointReason.OPPONENT_SERVE_ERROR -> OPPONENT_SERVE_ERROR
                GeneralPointReason.OPPONENT_SPIKE_ERROR -> OPPONENT_SPIKE_ERROR
                GeneralPointReason.OPPONENT_BLOCK_ERROR -> OPPONENT_BLOCK_ERROR
                GeneralPointReason.OPPONENT_DOUBLE_TOUCH -> OPPONENT_DOUBLE_TOUCH
                GeneralPointReason.OPPONENT_FOUR_TOUCH -> OPPONENT_FOUR_TOUCH
                GeneralPointReason.OPPONENT_NET_TOUCH -> OPPONENT_NET_TOUCH
                GeneralPointReason.OPPONENT_ROTATION_ERROR -> OPPONENT_ROTATION_ERROR
                GeneralPointReason.OPPONENT_FOOT_FAULT -> OPPONENT_FOOT_FAULT
                GeneralPointReason.OPPONENT_OTHER -> OPPONENT_OTHER
                GeneralPointReason.OPPONENT_BALL_DROPPED -> OPPONENT_BALL_DROPPED
                GeneralPointReason.TEAM_DOUBLE_TOUCH -> TEAM_DOUBLE_TOUCH
                GeneralPointReason.TEAM_FOUR_TOUCH -> TEAM_FOUR_TOUCH
                GeneralPointReason.TEAM_NET_TOUCH -> TEAM_NET_TOUCH
                GeneralPointReason.TEAM_ROTATION_ERROR -> TEAM_ROTATION_ERROR
                GeneralPointReason.TEAM_FOOT_FAULT -> TEAM_FOOT_FAULT
                GeneralPointReason.TEAM_BALL_LANDED -> TEAM_BALL_LANDED
                GeneralPointReason.TEAM_OTHER -> TEAM_OTHER
            }
        }
    }
}

enum class GeneralPointReason(
    val displayName: String,
    val emoji: String,
    val forUs: Boolean  // true = bize sayı, false = rakibe sayı
) {
    // Rakip hataları (bize sayı)
    OPPONENT_SERVE_ERROR("Rakip Servis Hatası", "🏐", true),
    OPPONENT_SPIKE_ERROR("Rakip Smaç Hatası", "🔥", true),
    OPPONENT_BLOCK_ERROR("Rakip Blok Hatası", "🛡️", true),
    OPPONENT_DOUBLE_TOUCH("Rakip Çift Top", "✌️", true),
    OPPONENT_FOUR_TOUCH("Rakip 4 Top", "4️⃣", true),
    OPPONENT_NET_TOUCH("Rakip File Hatası", "🕸️", true),
    OPPONENT_ROTATION_ERROR("Rakip Pozisyon Hatası", "🔄", true),
    OPPONENT_FOOT_FAULT("Rakip Ayak Hatası", "👟", true),
    OPPONENT_BALL_DROPPED("Rakip Top Düşürdü", "⬇️", true),
    OPPONENT_OTHER("Rakip Diğer Hata", "❓", true),

    // Bizim takım hataları (rakibe sayı) - kişiye özel değil
    TEAM_DOUBLE_TOUCH("Çift Top", "✌️", false),
    TEAM_FOUR_TOUCH("4 Top", "4️⃣", false),
    TEAM_NET_TOUCH("File Hatası", "🕸️", false),
    TEAM_ROTATION_ERROR("Pozisyon Hatası", "🔄", false),
    TEAM_FOOT_FAULT("Ayak Hatası", "👟", false),
    TEAM_BALL_LANDED("Top Yere Düştü", "⬇️", false),
    TEAM_OTHER("Diğer Hata", "❓", false)
}