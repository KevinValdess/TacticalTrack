package cl.duoc.tacticaltrack

import cl.duoc.tacticaltrack.data.remote.AuthApi
import cl.duoc.tacticaltrack.data.repository.*
import cl.duoc.tacticaltrack.viewmodel.LoginViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = mockk<AuthApi>()
    private val tokens = MemoryTokenStore()
    private val auth = AuthRepository(api, tokens)
    private val preferences = mockk<UserPreferencesRepository>(relaxed = true)
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { preferences.lastUsername } returns flowOf("op@duoc.cl")
    }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun emptyFieldsDoNotCallApi() = runTest(dispatcher) {
        val vm = LoginViewModel(preferences, auth); advanceUntilIdle()
        assertEquals("op@duoc.cl", vm.uiState.value.username)
        vm.onUsernameChange(""); vm.login(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.usernameError)
        assertNotNull(vm.uiState.value.passwordError)
        coVerify(exactly = 0) { api.login(any()) }
    }
    @Test fun serverControlsNameAndRoleAndDoubleTapSendsOnlyOnce() = runTest(dispatcher) {
        coEvery { api.login(any()) } coAnswers { delay(20); loginResponse(authUser("Kevin", "SUPERVISOR")) }
        val vm = LoginViewModel(preferences, auth); advanceUntilIdle()
        vm.onUsernameChange("nuevo@duoc.cl"); vm.onPasswordChange("Prueba")
        vm.login(); vm.login(); advanceUntilIdle()
        assertEquals("Kevin", vm.uiState.value.user?.name)
        assertEquals("Supervisor", vm.uiState.value.user?.displayRole)
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.busy)
        coVerify(exactly = 1) { api.login(any()); preferences.saveLastUsername("nuevo@duoc.cl") }
        coEvery { api.logout(any()) } just Runs
        var disabledPush = false
        vm.logout { disabledPush = true }; advanceUntilIdle()
        assertTrue(disabledPush)
        assertNull(vm.uiState.value.user)
    }
    @Test fun networkAndHttpErrorsRemainOnLoginAndAllowRetry() = runTest(dispatcher) {
        val vm = LoginViewModel(preferences, auth); advanceUntilIdle()
        vm.onPasswordChange("Prueba")
        for (error in listOf(IOException(), httpError(401), httpError(403), httpError(400), httpError(500), IllegalArgumentException())) {
            coEvery { api.login(any()) } throws error
            vm.login(); advanceUntilIdle()
            assertNull(vm.uiState.value.user)
            assertNotNull(vm.uiState.value.error)
            assertFalse(vm.uiState.value.busy)
        }
        coEvery { api.login(any()) } returns loginResponse()
        vm.login(); advanceUntilIdle()
        assertEquals("Jonathan", vm.uiState.value.user?.name)
    }
    @Test fun restoresSessionAndRefreshesChangedName() = runTest(dispatcher) {
        coEvery { api.me(any()) } returns authUser("Victor")
        val restored = AuthRepository(api, MemoryTokenStore("saved-token"))
        val vm = LoginViewModel(preferences, restored); advanceUntilIdle()
        assertEquals("Victor", vm.uiState.value.user?.name)
        coEvery { api.me(any()) } returns authUser("Nombre actualizado")
        vm.restoreSession(); advanceUntilIdle()
        assertEquals("Nombre actualizado", vm.uiState.value.user?.name)
        restored.invalidate("saved-token"); advanceUntilIdle()
        assertNull(vm.uiState.value.user)
        assertNotNull(vm.uiState.value.error)
    }
    @Test fun startupWithoutConnectionCanRetrySavedSession() = runTest(dispatcher) {
        coEvery { api.me(any()) } throws IOException()
        val restored = AuthRepository(api, MemoryTokenStore("saved-token"))
        val vm = LoginViewModel(preferences, restored); advanceUntilIdle()
        assertNull(vm.uiState.value.user)
        assertNotNull(vm.uiState.value.error)
        coEvery { api.me(any()) } returns authUser()
        vm.restoreSession(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.user)
    }
}
