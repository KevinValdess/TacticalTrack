package cl.duoc.tacticaltrack.data.remote

import cl.duoc.tacticaltrack.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiFactory {
    @Volatile var sessionToken: () -> String? = { null }
    @Volatile var onUnauthorized: (String) -> Unit = {}

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val backendClient = client.newBuilder()
        .addInterceptor(BearerSessionInterceptor({ sessionToken() }, { onUnauthorized(it) })).build()
    private fun retrofit(url: String, http: OkHttpClient = backendClient): Retrofit = Retrofit.Builder()
        .baseUrl(url).client(http).addConverterFactory(GsonConverterFactory.create()).build()
    val auth: AuthApi by lazy { retrofit(BuildConfig.BACKEND_URL).create(AuthApi::class.java) }
    val catalog: CatalogApi by lazy { retrofit(BuildConfig.BACKEND_URL).create(CatalogApi::class.java) }
    val devices: DeviceApi by lazy { retrofit(BuildConfig.BACKEND_URL).create(DeviceApi::class.java) }
    val visits: VisitApi by lazy { retrofit(BuildConfig.BACKEND_URL).create(VisitApi::class.java) }
    val weather: WeatherApi by lazy { retrofit("https://api.open-meteo.com/", client).create(WeatherApi::class.java) }
}
