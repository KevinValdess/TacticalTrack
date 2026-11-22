package cl.duoc.tacticaltrack

import cl.duoc.tacticaltrack.data.remote.*
import cl.duoc.tacticaltrack.data.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthHttpTest {
    @Test fun retrofitMatchesDeployedLoginContractAndBearerHeaders() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi::class.java)
            val repository = AuthRepository(api, MemoryTokenStore())
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""
                {"accessToken":"random-opaque-token","tokenType":"Bearer","expiresAt":"2099-01-01T00:00:00Z",
                 "user":{"id":1,"email":"ad@duoc.cl","name":"Kevin","role":"SUPERVISOR","enabled":true,"supervisorId":"SUP-001"}}
            """.trimIndent()))
            assertEquals("Kevin", repository.login("ad@duoc.cl", "Prueba").name)
            val login = server.takeRequest()
            assertEquals("/api/auth/login", login.path)
            assertEquals("POST", login.method)
            assertTrue(login.body.readUtf8().contains("\"email\":\"ad@duoc.cl\""))
            assertNull(login.getHeader("Authorization"))
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""
                {"id":1,"email":"ad@duoc.cl","name":"Kevin actualizado","role":"SUPERVISOR","enabled":true,"supervisorId":"SUP-001"}
            """.trimIndent()))
            assertEquals("Kevin actualizado", repository.restore()?.name)
            assertEquals("Bearer random-opaque-token", server.takeRequest().getHeader("Authorization"))
            server.enqueue(MockResponse().setResponseCode(204))
            repository.logout()
            val logout = server.takeRequest()
            assertEquals("/api/auth/logout", logout.path)
            assertEquals("Bearer random-opaque-token", logout.getHeader("Authorization"))
        } finally { server.shutdown() }
    }
    @Test fun interceptorAddsTokenAndOnlyInvalidatesMatchingUnauthorizedSession() {
        val server = MockWebServer()
        server.start()
        try {
            var rejected: String? = null
            val client = OkHttpClient.Builder().addInterceptor(BearerSessionInterceptor({ "saved-token" }, { rejected = it })).build()
            server.enqueue(MockResponse().setResponseCode(403))
            client.newCall(Request.Builder().url(server.url("/api/users")).build()).execute().close()
            assertEquals("Bearer saved-token", server.takeRequest().getHeader("Authorization"))
            assertNull(rejected)
            server.enqueue(MockResponse().setResponseCode(401))
            client.newCall(Request.Builder().url(server.url("/api/visits")).build()).execute().close()
            assertEquals("saved-token", rejected)
            server.takeRequest()
            rejected = null
            server.enqueue(MockResponse().setResponseCode(401))
            client.newCall(Request.Builder().url(server.url("/api/auth/login")).build()).execute().close()
            assertNull(server.takeRequest().getHeader("Authorization"))
            assertNull(rejected)
        } finally { server.shutdown() }
    }
}
