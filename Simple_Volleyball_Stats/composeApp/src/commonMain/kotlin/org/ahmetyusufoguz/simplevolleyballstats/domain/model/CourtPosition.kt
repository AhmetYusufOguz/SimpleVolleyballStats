package org.ahmetyusufoguz.simplevolleyballstats.domain.model

/**
 * Voleybol sahasındaki 6 pozisyon (rotasyon sistemi)
 *
 * Saha görünümü:
 * ---------------- ← File
 *     4   3   2    ← Ön sıra
 *     5   6   1    ← Arka sıra
 */
enum class CourtPosition(val number: Int) {
    POSITION_1(1),
    POSITION_2(2),
    POSITION_3(3),
    POSITION_4(4),
    POSITION_5(5),
    POSITION_6(6);

    fun rotateClockwise(): CourtPosition = when (this) {
        POSITION_1 -> POSITION_6
        POSITION_6 -> POSITION_5
        POSITION_5 -> POSITION_4
        POSITION_4 -> POSITION_3
        POSITION_3 -> POSITION_2
        POSITION_2 -> POSITION_1
    }

    fun isBackRow(): Boolean = this in listOf(POSITION_1, POSITION_5, POSITION_6)
    fun isFrontRow(): Boolean = !isBackRow()
    fun isServingPosition(): Boolean = this == POSITION_1

    companion object {
        fun fromNumber(number: Int): CourtPosition? {
            return entries.find { it.number == number }
        }
    }
}