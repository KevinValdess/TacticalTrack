package cl.duoc.tacticaltrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaltrack.data.remote.ApiFactory
import cl.duoc.tacticaltrack.data.remote.CurrentWeather
import cl.duoc.tacticaltrack.data.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WeatherState {
    data object Loading : WeatherState
    data class Ready(val weather: CurrentWeather) : WeatherState
    data object Error : WeatherState
}
class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
    private val _state = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val state = _state.asStateFlow()
    private var job: Job? = null
    fun load(latitude: Double, longitude: Double) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = WeatherState.Loading
            try { _state.value = WeatherState.Ready(repository.current(latitude, longitude)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.value = WeatherState.Error }
        }
    }
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeatherViewModel(WeatherRepository(ApiFactory.weather)) as T
    }
}
