package cl.duoc.tacticaltrack.data.repository

import cl.duoc.tacticaltrack.data.remote.CurrentWeather
import cl.duoc.tacticaltrack.data.remote.WeatherApi

class WeatherRepository(private val api: WeatherApi) {
    suspend fun current(latitude: Double, longitude: Double): CurrentWeather {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        return requireNotNull(api.current(latitude, longitude).current) { "API sin datos de clima" }
    }
}
