package org.ahmetyusufoguz.simplevolleyballstats

import androidx.compose.runtime.*
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.AppLanguage
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.SettingsRepository
import org.ahmetyusufoguz.simplevolleyballstats.presentation.navigation.AppNavigation
import org.ahmetyusufoguz.simplevolleyballstats.presentation.theme.AppTheme
import org.ahmetyusufoguz.simplevolleyballstats.presentation.theme.SimpleVolleyballStatsTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val settingsRepository: SettingsRepository = koinInject()

    var currentTheme by remember { mutableStateOf(AppTheme.LIGHT) }
    var currentLanguage by remember { mutableStateOf(AppLanguage.TURKISH) }

    // Kayıtlı ayarları yükle
    LaunchedEffect(Unit) {
        settingsRepository.getTheme().collect { theme ->
            currentTheme = theme
        }
    }

    LaunchedEffect(Unit) {
        settingsRepository.getLanguage().collect { language ->
            currentLanguage = language
        }
    }

    SimpleVolleyballStatsTheme(appTheme = currentTheme) {
        AppNavigation(
            onThemeChanged = { currentTheme = it },
            onLanguageChanged = { currentLanguage = it }
        )
    }
}