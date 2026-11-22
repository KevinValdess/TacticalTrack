package cl.duoc.tacticaltrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaltrack.data.repository.AuthRepository
import cl.duoc.tacticaltrack.data.repository.UserPreferencesRepository
import cl.duoc.tacticaltrack.model.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val user: AuthUser? = null,
    val busy: Boolean = true,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val error: String? = null
)

class LoginViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val auth: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            auth.user.collect { user ->
                _uiState.update { state -> state.copy(user = user, error =
                    if (state.user != null && user == null) "La sesión terminó. Vuelve a ingresar." else state.error) }
            }
        }
        viewModelScope.launch {
            val remembered = preferencesRepository.lastUsername.first()
            if (_uiState.value.username.isBlank()) _uiState.update { it.copy(username = remembered) }
            restoreSessionInternal()
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, usernameError = null, passwordError = null, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, error = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.busy) return
        val usernameError = if (state.username.isBlank()) "Ingresa tu correo." else null
        val passwordError = if (state.password.isBlank()) "Ingresa tu contraseña." else null
        _uiState.update { it.copy(usernameError = usernameError, passwordError = passwordError, error = null) }
        if (usernameError != null || passwordError != null) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val user = auth.login(state.username, state.password)
                _uiState.update { it.copy(username = user.email, password = "", user = user) }
                // Recordar el correo es opcional y no debe invalidar un login correcto.
                try { preferencesRepository.saveLastUsername(user.email) }
                catch (error: CancellationException) { throw error }
                catch (_: IOException) { }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _uiState.update { it.copy(error = errorMessage(error)) } }
            finally { _uiState.update { it.copy(busy = false) } }
        }
    }

    fun restoreSession() {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch { restoreSessionInternal() }
    }

    private suspend fun restoreSessionInternal() {
        try { auth.restore() }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { _uiState.update { it.copy(error = errorMessage(error)) } }
        finally { _uiState.update { it.copy(busy = false) } }
    }

    fun logout(beforeLogout: suspend () -> Unit = {}) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { beforeLogout() }
            finally {
                auth.logout()
                _uiState.update { it.copy(user = null, password = "", busy = false, error = null) }
            }
        }
    }

    private fun errorMessage(error: Exception): String = when (error) {
        is HttpException -> when (error.code()) {
            401 -> "Correo o contraseña incorrectos, o sesión vencida. Vuelve a ingresar."
            403 -> "Tu cuenta no tiene permiso para ingresar."
            400 -> "Revisa el correo y la contraseña ingresados."
            else -> "El servidor no pudo completar la solicitud (HTTP ${error.code()})."
        }
        is IOException -> "No se pudo conectar con la API. Revisa Internet y la conexión de Tailscale."
        else -> "No se pudo validar la sesión. Inténtalo nuevamente."
    }

    class Factory(private val preferences: UserPreferencesRepository, private val auth: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LoginViewModel(preferences, auth) as T
    }
}
