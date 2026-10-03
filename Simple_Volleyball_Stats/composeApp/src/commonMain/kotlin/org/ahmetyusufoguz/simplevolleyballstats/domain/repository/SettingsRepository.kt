package org.ahmetyusufoguz.simplevolleyballstats.domain.repository

import kotlinx.coroutines.flow.Flow
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.AppLanguage
import org.ahmetyusufoguz.simplevolleyballstats.presentation.theme.AppTheme

interface SettingsRepository {
    fun getTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)

    fun getLanguage(): Flow<AppLanguage>
    suspend fun setLanguage(language: AppLanguage)
}