package cl.duoc.tacticaltrack

import cl.duoc.tacticaltrack.data.remote.*
import cl.duoc.tacticaltrack.data.repository.WeatherRepository
import cl.duoc.tacticaltrack.domain.VisitRules
import cl.duoc.tacticaltrack.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherTest {
    private val api = mockk<WeatherApi>()
    private val weather = CurrentWeather(18.2, 0, "2026-11-12T12:00")
    @Test fun repositoryValidatesAndReadsApi() = runTest {
        coEvery { api.current(any(), any(), any(), any()) } returns WeatherResponse(weather)
        val repo = WeatherRepository(api)
        assertEquals(weather, repo.current(-33.5, -70.7))
        listOf(91.0 to 0.0, 0.0 to 181.0, Double.NaN to 0.0).forEach { (lat, lon) ->
            try { repo.current(lat, lon); fail() } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun missingCurrentIsError() = runTest {
        coEvery { api.current(any(), any(), any(), any()) } returns WeatherResponse(null)
        try { WeatherRepository(api).current(0.0, 0.0); fail() } catch (_: IllegalArgumentException) { }
    }
    @Test fun viewModelSupportsSuccessErrorAndRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            coEvery { api.current(any(), any(), any(), any()) } returns WeatherResponse(weather)
            val vm = WeatherViewModel(WeatherRepository(api))
            vm.load(0.0, 0.0); advanceUntilIdle()
            assertTrue(vm.state.value is WeatherState.Ready)
            coEvery { api.current(any(), any(), any(), any()) } throws IOException()
            vm.load(0.0, 0.0); advanceUntilIdle()
            assertEquals(WeatherState.Error, vm.state.value)
            coEvery { api.current(any(), any(), any(), any()) } returns WeatherResponse(weather)
            vm.load(0.0, 0.0); advanceUntilIdle()
            assertTrue(vm.state.value is WeatherState.Ready)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun staleRequestCannotOverrideNewCoordinates() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            coEvery { api.current(1.0, any(), any(), any()) } coAnswers { delay(1000); WeatherResponse(weather) }
            coEvery { api.current(2.0, any(), any(), any()) } returns WeatherResponse(weather.copy(temperature = 30.0))
            val vm = WeatherViewModel(WeatherRepository(api))
            vm.load(1.0, 0.0); runCurrent(); vm.load(2.0, 0.0); advanceUntilIdle()
            assertEquals(30.0, (vm.state.value as WeatherState.Ready).weather.temperature, 0.01)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun editRulesCoverBoundaries() {
        assertNull(VisitRules.editError("Operativo", ""))
        assertNull(VisitRules.editError("Con falla", "12345"))
        assertNull(VisitRules.editError("Operativo con observaciones", "Motor revisado"))
        assertNotNull(VisitRules.editError("Con falla", "1234"))
        assertNotNull(VisitRules.editError("Otro", "texto"))
        assertNotNull(VisitRules.editError("Operativo", "x".repeat(2001)))
        assertNull(VisitRules.editError("Operativo", "x".repeat(2000)))
    }
}
