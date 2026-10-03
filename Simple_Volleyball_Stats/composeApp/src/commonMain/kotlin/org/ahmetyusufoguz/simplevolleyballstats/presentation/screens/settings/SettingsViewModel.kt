package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.AppLanguage
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.SettingsRepository
import org.ahmetyusufoguz.simplevolleyballstats.presentation.theme.AppTheme

data class SettingsState(
    val currentTheme: AppTheme = AppTheme.LIGHT,
    val currentLanguage: AppLanguage = AppLanguage.TURKISH,
    val isLoading: Boolean = true
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getTheme().collect { theme ->
                _state.update { it.copy(currentTheme = theme, isLoading = false) }
            }
        }
        viewModelScope.launch {
            settingsRepository.getLanguage().collect { language ->
                _state.update { it.copy(currentLanguage = language) }
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

}