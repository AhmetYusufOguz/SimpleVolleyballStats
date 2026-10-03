package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.AppLanguage
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.SettingsRepository
import org.ahmetyusufoguz.simplevolleyballstats.presentation.theme.AppTheme
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    val settingsRepository: SettingsRepository = koinInject()
    val viewModel = remember { SettingsViewModel(settingsRepository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        // Her açılışta güncel ayarları yükle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tema Bölümü
            item {
                SettingsSection(title = "Görünüm") {
                    ThemeSelector(
                        currentTheme = state.currentTheme,
                        onThemeSelected = { theme ->
                            viewModel.setTheme(theme)
                            onThemeChanged(theme)
                        }
                    )
                }
            }

            // Dil Bölümü
            item {
                SettingsSection(title = "Dil") {
                    LanguageSelector(
                        currentLanguage = state.currentLanguage,
                        onLanguageSelected = { language ->
                            viewModel.setLanguage(language)
                            onLanguageChanged(language)
                        }
                    )
                }
            }

            // Uygulama Bilgisi
            item {
                SettingsSection(title = "Hakkında") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Simple Volleyball Stats",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Versiyon 0.0.1",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun ThemeSelector(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            AppTheme.entries.forEach { theme ->
                ThemeOption(
                    theme = theme,
                    isSelected = currentTheme == theme,
                    onClick = { onThemeSelected(theme) }
                )
                if (theme != AppTheme.entries.last()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (emoji, name) = when (theme) {
        AppTheme.LIGHT -> "☀️" to "Açık Tema"
        AppTheme.DARK -> "🌙" to "Koyu Tema"
        AppTheme.COURT_GREEN -> "🏐" to "Saha Yeşili"
        AppTheme.OCEAN_BLUE -> "🌊" to "Okyanus Mavisi"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(16.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Seçili",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LanguageSelector(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            AppLanguage.entries.forEach { language ->
                LanguageOption(
                    language = language,
                    isSelected = currentLanguage == language,
                    onClick = { onLanguageSelected(language) }
                )
                if (language != AppLanguage.entries.last()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val emoji = when (language) {
        AppLanguage.TURKISH -> "🇹🇷"
        AppLanguage.ENGLISH -> "🇬🇧"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(16.dp))
        Text(
            text = language.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Seçili",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
