package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.PlayerPosition
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPlayerScreen(
    teamId: String,
    onBackClick: () -> Unit,
    onPlayerAdded: () -> Unit
) {
    val playerRepository: PlayerRepository = koinInject()

    val viewModel = remember(teamId) {
        AddPlayerViewModel(teamId, playerRepository)
    }

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Oyuncu Ekle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.savePlayer(onPlayerAdded) },
                        enabled = state.isValid && !state.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Kaydet")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Forma Numarası
            OutlinedTextField(
                value = state.number,
                onValueChange = { viewModel.updateNumber(it) },
                label = { Text("Forma Numarası") },
                placeholder = { Text("1-99") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // İsim
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Oyuncu Adı") },
                placeholder = { Text("Örn: Ahmet Yılmaz") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Pozisyon Seçimi
            Text(
                text = "Pozisyon (Opsiyonel)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerPosition.entries.forEach { position ->
                    FilterChip(
                        selected = state.position == position,
                        onClick = {
                            viewModel.updatePosition(
                                if (state.position == position) null else position
                            )
                        },
                        label = { Text(position.displayName) }
                    )
                }
            }

            // Hata mesajı
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.weight(1f))

            // Kaydet butonu
            Button(
                onClick = { viewModel.savePlayer(onPlayerAdded) },
                enabled = state.isValid && !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Oyuncu Ekle")
                }
            }
        }
    }
}