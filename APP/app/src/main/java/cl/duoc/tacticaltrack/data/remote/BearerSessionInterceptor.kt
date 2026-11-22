package cl.duoc.tacticaltrack.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class BearerSessionInterceptor(
    private val token: () -> String?,
    private val onUnauthorized: (String) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // El login es público y nunca debe recibir una sesión anterior.
        if (request.url.encodedPath == "/api/auth/login") return chain.proceed(request)
        val sessionToken = request.header("Authorization")?.removePrefix("Bearer ") ?: token()
        val authenticated = if (sessionToken.isNullOrBlank()) request else request.newBuilder()
            .header("Authorization", "Bearer $sessionToken").build()
        val response = chain.proceed(authenticated)
        if (response.code == 401 && !sessionToken.isNullOrBlank()) onUnauthorized(sessionToken)
        return response
    }
}
