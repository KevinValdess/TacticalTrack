package cl.duoc.tacticaltrack.data.remote

import cl.duoc.tacticaltrack.model.AuthUser
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresAt: String,
    val user: AuthUser
)

interface AuthApi {
    @POST("api/auth/login") suspend fun login(@Body request: LoginRequest): LoginResponse
    @GET("api/auth/me") suspend fun me(@Header("Authorization") authorization: String): AuthUser
    @POST("api/auth/logout") suspend fun logout(@Header("Authorization") authorization: String)
}
