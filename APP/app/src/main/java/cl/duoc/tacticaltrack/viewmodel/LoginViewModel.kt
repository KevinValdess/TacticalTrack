package cl.duoc.tacticaltrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaltrack.data.repository.UserPreferencesRepository
import cl.duoc.tacticaltrack.data.repository.DemoAccountsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val role: String = "",
    val displayName: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null
)

class LoginViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val accountsRepository: DemoAccountsRepository = DemoAccountsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val remembered = preferencesRepository.lastUsername.first()
            if (remembered.isNotBlank() && _uiState.value.username.isBlank()) {
                _uiState.update { it.copy(username = remembered) }
            }
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, usernameError = null, passwordError = null, role = "", displayName = "") }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, role = "", displayName = "") }
    }

    fun validateAndRemember(): Boolean {
        val state = _uiState.value
        val email = state.username.trim()
        val userError = if (email.isBlank()) "Ingresa tu correo." else null
        val passError = if (state.password.isBlank()) "Ingresa tu contraseña." else null
        if (userError != null || passError != null) {
            _uiState.update {
                it.copy(usernameError = userError, passwordError = passError, role = "", displayName = "")
            }
            return false
        }

        val account = accountsRepository.authenticate(email, state.password)
        if (account == null) {
            _uiState.update {
                it.copy(passwordError = "Correo o contraseña incorrectos.", role = "", displayName = "")
            }
            return false
        }

        _uiState.update {
            it.copy(username = account.email, password = "", role = account.role,
                displayName = account.name, usernameError = null, passwordError = null)
        }
        viewModelScope.launch { preferencesRepository.saveLastUsername(account.email) }
        return true
    }

    class Factory(
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(preferencesRepository) as T
        }
    }
}

