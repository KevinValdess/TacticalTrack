package cl.duoc.tacticaltrack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.tacticaltrack.viewmodel.WeatherState
import cl.duoc.tacticaltrack.viewmodel.WeatherViewModel

@Composable
fun WeatherCard(latitude: Double, longitude: Double) {
    val vm: WeatherViewModel = viewModel(factory = WeatherViewModel.Factory())
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(latitude, longitude) { vm.load(latitude, longitude) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Clima actual en el lugar de la visita", style = MaterialTheme.typography.titleMedium)
            when (val current = state) {
                WeatherState.Loading -> Text("Consultando clima…")
                WeatherState.Error -> {
                    Text("No se pudo consultar el clima. Puedes continuar con la visita.")
                    TextButton(onClick = { vm.load(latitude, longitude) }) { Text("Reintentar") }
                }
                is WeatherState.Ready -> {
                    Text("${current.weather.temperature} °C · Código WMO ${current.weather.code}")
                    Text("Hora del dato: ${current.weather.time}")
                }
            }
            Text("Fuente: Open-Meteo · Datos actuales de referencia", style = MaterialTheme.typography.bodySmall)
        }
    }
}
