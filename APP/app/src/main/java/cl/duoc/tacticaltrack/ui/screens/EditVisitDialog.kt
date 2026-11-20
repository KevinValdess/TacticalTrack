package cl.duoc.tacticaltrack.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import cl.duoc.tacticaltrack.domain.VisitRules

@Composable
fun EditVisitDialog(initialStatus: String, initialObservations: String, busy: Boolean,
                    serverError: String?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var status by remember { mutableStateOf(initialStatus) }
    var observations by remember { mutableStateOf(initialObservations) }
    val error = VisitRules.editError(status, observations)
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Editar visita") },
        text = { Column {
            VisitRules.equipmentStatuses.forEach { option ->
                TextButton(enabled = !busy, onClick = { status = option }) {
                    Text(if (status == option) "✓ $option" else option)
                }
            }
            OutlinedTextField(value = observations, onValueChange = { observations = it },
                enabled = !busy, label = { Text("Observaciones") }, isError = error != null)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            serverError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !busy && error == null,
            onClick = { onSave(status, observations) }) { Text("Guardar") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancelar") } }
    )
}
