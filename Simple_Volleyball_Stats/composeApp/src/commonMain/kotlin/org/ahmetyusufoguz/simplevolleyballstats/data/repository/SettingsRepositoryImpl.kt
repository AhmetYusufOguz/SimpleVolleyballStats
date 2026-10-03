package org.ahmetyusufoguz.simplevolleyballstats.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.AppLanguage
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.SettingsRepository
import org.ahmetyusufoguz.simplevolleyballstats.presentation.theme.AppTheme

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme")
        private val LANGUAGE_KEY = stringPreferencesKey("app_language")
    }

    override fun getTheme(): Flow<AppTheme> {
        return dataStore.data.map { preferences ->
            val themeName = preferences[THEME_KEY] ?: AppTheme.LIGHT.name
            AppTheme.valueOf(themeName)
        }
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    override fun getLanguage(): Flow<AppLanguage> {
        return dataStore.data.map { preferences ->
            val langCode = preferences[LANGUAGE_KEY] ?: AppLanguage.TURKISH.code
            AppLanguage.fromCode(langCode)
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }
    }
}