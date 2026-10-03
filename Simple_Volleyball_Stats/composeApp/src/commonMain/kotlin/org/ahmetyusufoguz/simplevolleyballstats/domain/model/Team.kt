package org.ahmetyusufoguz.simplevolleyballstats.domain.model

import org.ahmetyusufoguz.simplevolleyballstats.utils.currentTimeMillis

data class Team(
    val id: String,
    val name: String,
    val createdAt: Long
) {
    companion object {
        fun create(name: String): Team {
            val now = currentTimeMillis()
            return Team(
                id = "team_${now}_${(0..9999).random()}",
                name = name,
                createdAt = now
            )
        }
    }
}