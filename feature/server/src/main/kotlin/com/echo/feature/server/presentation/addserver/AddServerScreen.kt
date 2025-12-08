package com.echo.feature.server.presentation.addserver

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    onServerAdded: (serverId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: AddServerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.serverAddedId) {
        state.serverAddedId?.let { serverId ->
            onServerAdded(serverId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conectar a servidor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Text(
                text = "Dirección del servidor",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://echo.ejemplo.com") },
                singleLine = true,
                isError = state.error != null,
                supportingText = state.error?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                trailingIcon = {
                    when {
                        state.isValidating -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        state.isValid -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Válido",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        state.error != null -> Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (state.serverUrl.isNotBlank()) {
                            viewModel.validateServer()
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Examples
            Column {
                Text(
                    text = "Ejemplos:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExampleUrl("https://echo.ejemplo.com")
                ExampleUrl("http://192.168.1.100:3000")
                ExampleUrl("https://mi-servidor.duckdns.org/echo")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Server info if validated
            if (state.serverInfo != null) {
                ServerInfoCard(
                    name = state.serverInfo!!.name,
                    version = state.serverInfo!!.version
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    keyboardController?.hide()
                    if (state.isValid) {
                        viewModel.addServer()
                    } else {
                        viewModel.validateServer()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.serverUrl.isNotBlank() && !state.isValidating && !state.isAdding
            ) {
                if (state.isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (state.isValid) "Continuar" else "Conectar")
                }
            }
        }
    }
}

@Composable
private fun ExampleUrl(url: String) {
    Text(
        text = "• $url",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ServerInfoCard(
    name: String,
    version: String?
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Servidor encontrado",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (version != null) {
                Text(
                    text = "Versión: $version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
