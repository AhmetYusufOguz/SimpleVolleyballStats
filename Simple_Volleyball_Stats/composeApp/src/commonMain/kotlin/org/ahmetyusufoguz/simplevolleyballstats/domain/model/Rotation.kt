package org.ahmetyusufoguz.simplevolleyballstats.domain.model

data class Rotation(
    val positions: Map<CourtPosition, String> = emptyMap()
) {
    fun rotate(): Rotation {
        val newPositions = positions.mapKeys { (pos, _) ->
            pos.rotateClockwise()
        }
        return copy(positions = newPositions)
    }

    fun getPlayerAt(position: CourtPosition): String? = positions[position]

    fun getPositionOf(playerId: String): CourtPosition? {
        return positions.entries.find { it.value == playerId }?.key
    }

    fun setPlayer(position: CourtPosition, playerId: String): Rotation {
        return copy(positions = positions + (position to playerId))
    }

    fun removePlayer(playerId: String): Rotation {
        return copy(positions = positions.filterValues { it != playerId })
    }

    fun swapPlayers(pos1: CourtPosition, pos2: CourtPosition): Rotation {
        val player1 = positions[pos1]
        val player2 = positions[pos2]

        val newPositions = positions.toMutableMap()
        if (player1 != null) newPositions[pos2] = player1
        if (player2 != null) newPositions[pos1] = player2

        return copy(positions = newPositions)
    }

    fun isComplete(): Boolean = positions.size == 6

    companion object {
        fun empty() = Rotation()

        fun fromPlayerList(players: List<Pair<CourtPosition, String>>): Rotation {
            return Rotation(players.toMap())
        }
    }
}