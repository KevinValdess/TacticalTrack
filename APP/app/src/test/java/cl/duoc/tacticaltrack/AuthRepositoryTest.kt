package cl.duoc.tacticaltrack

import cl.duoc.tacticaltrack.data.remote.*
import cl.duoc.tacticaltrack.data.repository.*
import cl.duoc.tacticaltrack.model.AuthUser
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class MemoryTokenStore(var saved: String? = null) : SessionTokenStore {
    override fun read() = saved
    override fun write(token: String) { saved = token }
    override fun clear() { saved = null }
}

fun authUser(name: String = "Jonathan", role: String = "OPERARIO") =
    AuthUser(3, "nuevo@duoc.cl", name, role, true, if (role == "SUPERVISOR") "SUP-002" else null)
fun loginResponse(user: AuthUser = authUser()) = LoginResponse("opaque-session-token", "Bearer", "2099-01-01T00:00:00Z", user)
fun httpError(code: Int) = HttpException(Response.error<Unit>(code, "{}".toResponseBody("application/json".toMediaType())))

class AuthRepositoryTest {
    private val api = mockk<AuthApi>()
    private val tokens = MemoryTokenStore()
    private val repository = AuthRepository(api, tokens)

    @Test fun loginUsesServerAccountAndStoresOnlyToken() = runTest {
        coEvery { api.login(LoginRequest("nuevo@duoc.cl", "Prueba")) } returns loginResponse()
        val user = repository.login(" NUEVO@DUOC.CL ", "Prueba")
        assertEquals("Jonathan", user.name)
        assertEquals("Operario", user.displayRole)
        assertEquals("opaque-session-token", tokens.saved)
        assertEquals(user, repository.user.value)
        coVerify(exactly = 1) { api.login(LoginRequest("nuevo@duoc.cl", "Prueba")) }
    }
    @Test fun failedLoginDoesNotCreateLocalAccount() = runTest {
        coEvery { api.login(any()) } throws httpError(401)
        try { repository.login("ad@duoc.cl", "incorrecta"); fail() } catch (_: HttpException) { }
        assertNull(tokens.saved)
        assertNull(repository.user.value)
    }
    @Test fun restoresFreshNameAndRoleUsingSavedToken() = runTest {
        tokens.saved = "saved-token"
        val restored = AuthRepository(api, tokens)
        coEvery { api.me("Bearer saved-token") } returns authUser("Kevin", "SUPERVISOR")
        assertEquals("Kevin", restored.restore()?.name)
        assertEquals("Supervisor", restored.user.value?.displayRole)
        assertEquals("SUP-002", restored.user.value?.supervisorId)
    }
    @Test fun expiredSessionIsRemovedButConnectionFailureRetainsToken() = runTest {
        tokens.saved = "saved-token"
        val restored = AuthRepository(api, tokens)
        coEvery { api.me(any()) } throws IOException("Sin conexión")
        try { restored.restore(); fail() } catch (_: IOException) { }
        assertEquals("saved-token", tokens.saved)
        assertNull(restored.user.value)
        coEvery { api.me(any()) } throws httpError(401)
        try { restored.restore(); fail() } catch (_: HttpException) { }
        assertNull(tokens.saved)
        assertNull(restored.token())
    }
    @Test fun disabledAndUnknownRolesCannotStartSession() = runTest {
        for (user in listOf(authUser().copy(enabled = false), authUser().copy(role = "ADMIN"), authUser().copy(role = "SUPERVISOR"))) {
            coEvery { api.login(any()) } returns loginResponse(user)
            try { repository.login("nuevo@duoc.cl", "Prueba"); fail() } catch (_: IllegalArgumentException) { }
            assertNull(repository.user.value)
        }
        try { authUser().copy(role = "ADMIN").displayRole; fail() } catch (_: IllegalStateException) { }
    }
    @Test fun expiredOrUnsupportedLoginResponseIsRejected() = runTest {
        for (response in listOf(loginResponse().copy(expiresAt = "2000-01-01T00:00:00Z"), loginResponse().copy(tokenType = "Basic"))) {
            coEvery { api.login(any()) } returns response
            try { repository.login("nuevo@duoc.cl", "Prueba"); fail() } catch (_: IllegalArgumentException) { }
            assertNull(tokens.saved)
        }
    }
    @Test fun logoutCallsServerAndAlwaysClearsDeviceSession() = runTest {
        coEvery { api.login(any()) } returns loginResponse()
        coEvery { api.logout("Bearer opaque-session-token") } throws IOException()
        repository.login("nuevo@duoc.cl", "Prueba")
        repository.logout()
        coVerify(exactly = 1) { api.logout("Bearer opaque-session-token") }
        assertNull(tokens.saved)
        assertNull(repository.user.value)
        assertNull(repository.restore())
    }
    @Test fun staleUnauthorizedResponseCannotClearNewerSession() = runTest {
        coEvery { api.login(any()) } returns loginResponse()
        repository.login("nuevo@duoc.cl", "Prueba")
        repository.invalidate("previous-token")
        assertNotNull(repository.user.value)
        repository.invalidate("opaque-session-token")
        assertNull(repository.user.value)
        assertNull(tokens.saved)
    }
    @Test fun invalidAccountDuringRestoreClearsSession() = runTest {
        val restored = AuthRepository(api, MemoryTokenStore("saved-token"))
        coEvery { api.me(any()) } returns authUser().copy(enabled = false)
        try { restored.restore(); fail() } catch (_: IllegalArgumentException) { }
        assertNull(restored.token())
    }
}
