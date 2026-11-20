package cl.duoc.tacticaltrack.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherResponse(val current: CurrentWeather?)
data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("weather_code") val code: Int,
    val time: String
)
interface WeatherApi {
    @GET("v1/forecast")
    suspend fun current(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") fields: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}
