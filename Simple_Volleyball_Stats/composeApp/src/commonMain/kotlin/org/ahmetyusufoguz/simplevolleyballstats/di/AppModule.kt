package org.ahmetyusufoguz.simplevolleyballstats.di

import org.ahmetyusufoguz.simplevolleyballstats.data.repository.PlayerRepositoryImpl
import org.ahmetyusufoguz.simplevolleyballstats.data.repository.SettingsRepositoryImpl
import org.ahmetyusufoguz.simplevolleyballstats.data.repository.TeamRepositoryImpl
import org.ahmetyusufoguz.simplevolleyballstats.data.repository.MatchRepositoryImpl
import org.ahmetyusufoguz.simplevolleyballstats.database.VolleyballDatabase
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.SettingsRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.TeamRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.MatchRepository
import org.ahmetyusufoguz.simplevolleyballstats.domain.service.MatchReportGenerator
import org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.home.HomeViewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single { VolleyballDatabase(get()) }

    // Repositories
    single<TeamRepository> { TeamRepositoryImpl(get()) }
    single<PlayerRepository> { PlayerRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<MatchRepository> { MatchRepositoryImpl(get()) }

    // Services
    single { MatchReportGenerator() }

    // ViewModels
    single { HomeViewModel(get(), get(), get()) }
    factory { params ->
        org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.match.MatchViewModel(
            teamId = params.get(),
            matchId = params.getOrNull(),
            teamRepository = get(),
            playerRepository = get(),
            matchRepository = get(),
            isDualTeamSlave = params.get()
        )
    }
}