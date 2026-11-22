package cl.duoc.tacticaltrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.tacticaltrack.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TacticalTrack", style = MaterialTheme.typography.displaySmall)
            Text(
                "Trazabilidad de visitas en terreno",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Correo electrónico") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !state.busy,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = state.usernameError != null,
                trailingIcon = {
                    if (state.usernameError != null) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "Error de correo")
                    }
                },
                supportingText = {
                    state.usernameError?.let { Text(it) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Contraseña") },
                singleLine = true,
                enabled = !state.busy,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = state.passwordError != null,
                trailingIcon = {
                    if (state.passwordError != null) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "Error de clave")
                    }
                },
                supportingText = {
                    state.passwordError?.let { Text(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Button(
                onClick = viewModel::login,
                enabled = !state.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                if (state.busy) CircularProgressIndicator() else Text("Ingresar")
            }

            state.error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                TextButton(onClick = viewModel::restoreSession, enabled = !state.busy) {
                    Text("Reintentar conexión")
                }
            }

            Text(
                "Ingresa con el correo registrado en TacticalTrack.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

