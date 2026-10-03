package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Team
import org.koin.compose.koinInject

/**
 * Ana Ekran — İki ana buton:
 * 1. 🏐 Maç Başlat → Tekli/Çiftli mod seçimi → Takım seçimi → Maç
 * 2. 📋 Takımlar → Takım listesi sayfası
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinInject(),
    onTeamClick: (String) -> Unit = {},
    onTeamsListClick: () -> Unit = {},
    onStartSingleMatch: (teamId: String) -> Unit = {},
    onStartDualMatch: (homeTeamId: String, awayTeamId: String) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    // Maç başlatma akışı state'leri
    var showMatchModeDialog by remember { mutableStateOf(false) }
    var showTeamSelectDialog by remember { mutableStateOf(false) }
    var showDualTeamSelectDialog by remember { mutableStateOf(false) }
    var selectedMatchMode by remember { mutableStateOf<MatchMode?>(null) }
    var selectedHomeTeamId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(HomeIntent.LoadTeams)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🏐 Simple Volleyball Stats",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Voleybol istatistik takip sistemi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo / İkon alanı
            Text(
                text = "🏐",
                fontSize = 48.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "SVS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Simple Volleyball Stats",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Ana butonlar — yatay düzen (landscape)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🏐 Maç Başlat butonu
                MainActionCard(
                    icon = "🏐",
                    title = "Maç Başlat",
                    subtitle = "",
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = { showMatchModeDialog = true },
                    modifier = Modifier.weight(1f)
                )

                // 📋 Takımlar butonu
                MainActionCard(
                    icon = "📋",
                    title = "Takımlar",
                    subtitle = "",
                    containerColor = MaterialTheme.colorScheme.secondary,
                    onClick = onTeamsListClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Alt bilgi
            if (state.teams.isNotEmpty()) {
                Text(
                    text = "${state.teams.size} takım kayıtlı",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // ─── Dialog 1: Tekli mi Çiftli mi? ───
    if (showMatchModeDialog) {
        MatchModeDialog(
            onSingleMode = {
                selectedMatchMode = MatchMode.SINGLE
                showMatchModeDialog = false
                showTeamSelectDialog = true
            },
            onDualMode = {
                selectedMatchMode = MatchMode.DUAL
                showMatchModeDialog = false
                showTeamSelectDialog = true
            },
            onDismiss = { showMatchModeDialog = false }
        )
    }

    // ─── Dialog 2: Takım Seçimi ───
    if (showTeamSelectDialog) {
        TeamSelectDialog(
            teams = state.teams.map { it.team },
            title = when (selectedMatchMode) {
                MatchMode.SINGLE -> "Takımınızı Seçin"
                MatchMode.DUAL -> "Ev Sahibi Takımı Seçin"
                else -> "Takım Seçin"
            },
            onTeamSelected = { teamId ->
                showTeamSelectDialog = false
                when (selectedMatchMode) {
                    MatchMode.SINGLE -> {
                        onStartSingleMatch(teamId)
                    }
                    MatchMode.DUAL -> {
                        selectedHomeTeamId = teamId
                        showDualTeamSelectDialog = true
                    }
                    else -> {}
                }
            },
            onDismiss = { showTeamSelectDialog = false }
        )
    }

    // ─── Dialog 3: Çift Takım — Deplasman Seçimi ───
    if (showDualTeamSelectDialog && selectedHomeTeamId != null) {
        TeamSelectDialog(
            teams = state.teams.map { it.team }.filter { it.id != selectedHomeTeamId },
            title = "Deplasman Takımını Seçin",
            onTeamSelected = { awayTeamId ->
                showDualTeamSelectDialog = false
                onStartDualMatch(selectedHomeTeamId!!, awayTeamId)
                selectedHomeTeamId = null
            },
            onDismiss = {
                showDualTeamSelectDialog = false
                selectedHomeTeamId = null
            }
        )
    }

    // Takım oluşturma dialog
    if (state.showCreateDialog) {
        CreateTeamDialog(
            onDismiss = { viewModel.handleIntent(HomeIntent.HideCreateDialog) },
            onConfirm = { name ->
                viewModel.handleIntent(HomeIntent.CreateTeam(name))
            }
        )
    }
}

/** Maç modu enum */
private enum class MatchMode { SINGLE, DUAL }

// ─── Ana Aksiyon Kartı ───

@Composable
private fun MainActionCard(
    icon: String,
    title: String,
    subtitle: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .heightIn(min = 80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Maç Modu Seçim Dialog ───

@Composable
private fun MatchModeDialog(
    onSingleMode: () -> Unit,
    onDualMode: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maç Modu", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Hangi modda maç takip etmek istiyorsunuz?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                // Tekli ve Çiftli mod yan yana
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tekli mod
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onSingleMode),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("👤", fontSize = 28.sp)
                            Text(
                                text = "Tek Takım",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Sadece kendi takımınız",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Çiftli mod
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onDualMode),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("👥", fontSize = 28.sp)
                            Text(
                                text = "Çift Takım",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Her iki takım ayrı ayrı",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

// ─── Takım Seçim Dialog ───

@Composable
private fun TeamSelectDialog(
    teams: List<Team>,
    title: String,
    onTeamSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            if (teams.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏐", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Henüz takım yok.\nÖnce Takımlar bölümünden takım oluşturun.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(teams, key = { it.id }) { team ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTeamSelected(team.id) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🏐", fontSize = 20.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = team.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

// ─── Takım Oluşturma Dialog ───

@Composable
private fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var teamName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Yeni Takım Oluştur")
        },
        text = {
            OutlinedTextField(
                value = teamName,
                onValueChange = { teamName = it },
                label = { Text("Takım Adı") },
                placeholder = { Text("Örn: Galatasaray") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (teamName.isNotBlank()) {
                        onConfirm(teamName)
                    }
                },
                enabled = teamName.isNotBlank()
            ) {
                Text("Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}