package com.echo.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.echo.core.ui.theme.EchoCoral
import com.echo.core.ui.theme.EchoDarkSurfaceVariant

enum class AdminTab(val title: String, val icon: ImageVector) {
    STATUS("Estado", Icons.Default.Speed),
    USERS("Usuarios", Icons.Default.Group),
    CONFIG("Config", Icons.Default.Settings),
    LOGS("Logs", Icons.Default.Description)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Administración",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = EchoCoral,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = EchoCoral
                    )
                }
            ) {
                AdminTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        selectedContentColor = EchoCoral,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab Content
            when (AdminTab.entries[selectedTab]) {
                AdminTab.STATUS -> ServerStatusTab(state)
                AdminTab.USERS -> UsersTab(state)
                AdminTab.CONFIG -> ConfigTab(state)
                AdminTab.LOGS -> LogsTab(state)
            }
        }
    }
}

@Composable
private fun ServerStatusTab(state: AdminState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Server Status Card
        item {
            StatusCard(
                title = "Estado del Servidor",
                icon = Icons.Default.CheckCircle,
                iconTint = if (state.serverOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.serverOnline) "En línea" else "Desconectado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (state.serverOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = state.serverVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Library Stats
        item {
            StatusCard(
                title = "Biblioteca",
                icon = Icons.Default.MusicNote,
                iconTint = EchoCoral
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatRow("Canciones", state.totalTracks.toString())
                    StatRow("Álbumes", state.totalAlbums.toString())
                    StatRow("Artistas", state.totalArtists.toString())
                    StatRow("Playlists", state.totalPlaylists.toString())
                }
            }
        }

        // System Resources
        item {
            StatusCard(
                title = "Recursos del Sistema",
                icon = Icons.Default.Memory,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResourceBar(
                        label = "CPU",
                        value = state.cpuUsage,
                        color = when {
                            state.cpuUsage > 80 -> MaterialTheme.colorScheme.error
                            state.cpuUsage > 60 -> Color(0xFFFFA726)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                    ResourceBar(
                        label = "RAM",
                        value = state.memoryUsage,
                        color = when {
                            state.memoryUsage > 80 -> MaterialTheme.colorScheme.error
                            state.memoryUsage > 60 -> Color(0xFFFFA726)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                    ResourceBar(
                        label = "Disco",
                        value = state.diskUsage,
                        color = when {
                            state.diskUsage > 90 -> MaterialTheme.colorScheme.error
                            state.diskUsage > 75 -> Color(0xFFFFA726)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                }
            }
        }

        // Active Sessions
        item {
            StatusCard(
                title = "Sesiones Activas",
                icon = Icons.Default.Person,
                iconTint = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = "${state.activeSessions} usuarios conectados",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun UsersTab(state: AdminState) {
    if (state.users.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Cargando usuarios...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.users) { user ->
                UserCard(user = user)
            }
        }
    }
}

@Composable
private fun UserCard(user: AdminUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EchoDarkSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EchoCoral.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = EchoCoral,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (user.isAdmin) "Administrador" else "Usuario",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.isAdmin) EchoCoral else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Online status
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (user.isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun ConfigTab(state: AdminState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StatusCard(
                title = "Configuración del Servidor",
                icon = Icons.Default.Settings,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfigRow("Nombre del servidor", state.serverName)
                    ConfigRow("Puerto", state.serverPort.toString())
                    ConfigRow("Transcodificación", if (state.transcodingEnabled) "Activada" else "Desactivada")
                    ConfigRow("Calidad por defecto", state.defaultQuality)
                }
            }
        }

        item {
            StatusCard(
                title = "Almacenamiento",
                icon = Icons.Default.Storage,
                iconTint = MaterialTheme.colorScheme.secondary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfigRow("Ruta de música", state.musicPath)
                    ConfigRow("Espacio usado", state.storageUsed)
                    ConfigRow("Espacio disponible", state.storageAvailable)
                }
            }
        }
    }
}

@Composable
private fun LogsTab(state: AdminState) {
    if (state.logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay logs recientes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.logs) { log ->
                LogEntry(log = log)
            }
        }
    }
}

@Composable
private fun LogEntry(log: AdminLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EchoDarkSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (log.level) {
                    LogLevel.ERROR -> Icons.Default.Error
                    LogLevel.WARNING -> Icons.Default.Warning
                    LogLevel.INFO -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = when (log.level) {
                    LogLevel.ERROR -> MaterialTheme.colorScheme.error
                    LogLevel.WARNING -> Color(0xFFFFA726)
                    LogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EchoDarkSurfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ConfigRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ResourceBar(
    label: String,
    value: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$value%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
