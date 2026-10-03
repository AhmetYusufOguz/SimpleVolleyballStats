package org.ahmetyusufoguz.simplevolleyballstats.domain.model

import org.ahmetyusufoguz.simplevolleyballstats.utils.currentTimeMillis

data class Player(
    val id: String,
    val teamId: String,
    val number: Int,
    val name: String,
    val position: PlayerPosition? = null,
    val height: Int? = null,
    val weight: Int? = null,
    val notes: String? = null,
    val createdAt: Long
) {
    companion object {
        fun create(
            teamId: String,
            number: Int,
            name: String,
            position: PlayerPosition? = null,
            height: Int? = null,
            weight: Int? = null,
            notes: String? = null
        ): Player {
            val now = currentTimeMillis()
            return Player(
                id = "player_${now}_${(0..9999).random()}",
                teamId = teamId,
                number = number,
                name = name,
                position = position,
                height = height,
                weight = weight,
                notes = notes,
                createdAt = now
            )
        }
    }
}

enum class PlayerPosition(val displayName: String, val shortName: String) {
    SETTER("Pasör", "P"),
    OPPOSITE("Pasör Çaprazı", "PÇ"),
    OUTSIDE_HITTER("Köşe Smaçörü", "KS"),
    MIDDLE_BLOCKER("Orta Oyuncu", "OO"),
    LIBERO("Libero", "L");

    companion object {
        fun fromString(value: String?): PlayerPosition? {
            return entries.find { it.name == value }
        }
    }
}