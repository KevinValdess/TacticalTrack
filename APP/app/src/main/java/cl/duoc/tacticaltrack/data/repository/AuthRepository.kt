package cl.duoc.tacticaltrack.data.repository

import cl.duoc.tacticaltrack.data.remote.AuthApi
import cl.duoc.tacticaltrack.data.remote.LoginRequest
import cl.duoc.tacticaltrack.model.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.time.Instant
import java.util.Locale

class AuthRepository(private val api: AuthApi, private val tokens: SessionTokenStore) {
    private val mutex = Mutex()
    private val _user = MutableStateFlow<AuthUser?>(null)
    val user = _user.asStateFlow()
    @Volatile private var currentToken: String? = tokens.read()

    fun token(): String? = currentToken

    suspend fun login(email: String, password: String): AuthUser = mutex.withLock {
        val response = api.login(LoginRequest(email.trim().lowercase(Locale.ROOT), password))
        val user = response.user.validated()
        require(response.tokenType.equals("Bearer", ignoreCase = true) && response.accessToken.isNotBlank())
        require(Instant.parse(response.expiresAt).isAfter(Instant.now())) { "La sesión recibida ya venció." }
        tokens.write(response.accessToken)
        currentToken = response.accessToken
        _user.value = user
        user
    }

    suspend fun restore(): AuthUser? = mutex.withLock {
        val token = currentToken ?: return@withLock null
        try {
            val user = api.me("Bearer $token").validated()
            if (currentToken == token) _user.value = user
            _user.value
        } catch (error: HttpException) {
            if (error.code() == 401) invalidate(token)
            throw error
        } catch (error: IllegalArgumentException) {
            invalidate(token)
            throw error
        }
    }

    suspend fun logout() = mutex.withLock {
        try {
            currentToken?.let { api.logout("Bearer $it") }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Sin conexión también se elimina la sesión de este dispositivo.
        } finally {
            clear()
        }
    }

    @Synchronized fun invalidate(rejectedToken: String) {
        if (currentToken == rejectedToken) clear()
    }

    @Synchronized private fun clear() {
        currentToken = null
        tokens.clear()
        _user.value = null
    }
}
