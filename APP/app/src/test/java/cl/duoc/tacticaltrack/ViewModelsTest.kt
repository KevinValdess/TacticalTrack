package cl.duoc.tacticaltrack

import android.location.Location
import cl.duoc.tacticaltrack.data.repository.*
import cl.duoc.tacticaltrack.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = mockk<VisitRepository>(relaxed = true)
    private lateinit var vm: VisitViewModel
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { repo.visits } returns flowOf(listOf(sampleVisit()))
        every { repo.observeVisit(1) } returns flowOf(sampleVisit())
        every { repo.observeVisit(99) } returns flowOf(null)
        val catalog=mockk<CatalogRepository>()
        coEvery { catalog.load() } returns CatalogSnapshot(cl.duoc.tacticaltrack.model.TestCatalog.points,cl.duoc.tacticaltrack.model.TestCatalog.assets)
        vm = VisitViewModel(repo,catalog)
        dispatcher.scheduler.runCurrent()
    }
    @After fun cleanup() { unmockkAll(); Dispatchers.resetMain() }

    private fun fillForm() {
        vm.setOperator("Kevin", "Operario")
        vm.onPointSelected("PV-001"); vm.onAssetSelected("ACT-001")
        vm.onVisitTypeSelected("Mantenimiento"); vm.onEquipmentStatusSelected("Operativo")
    }
    private fun locate(distance: Float = 10f) {
        mockkStatic(Location::class)
        every { Location.distanceBetween(any(), any(), any(), any(), any()) } answers {
            lastArg<FloatArray>()[0] = distance
        }
        val location = mockk<Location>()
        every { location.latitude } returns -33.5186
        every { location.longitude } returns -70.7159
        every { location.accuracy } returns 5f
        vm.setLocation(location)
    }
    @Test fun formValidationAndFieldChanges() {
        assertFalse(vm.validateBeforeEvidence())
        fillForm(); assertTrue(vm.validateBeforeEvidence())
        vm.onEquipmentStatusSelected("Con falla"); vm.onObservationsChange("x")
        assertFalse(vm.validateBeforeEvidence())
        vm.onObservationsChange("Motor detenido"); assertTrue(vm.validateBeforeEvidence())
        assertEquals(2, vm.assetsForSelectedPoint().size)
        vm.onPointSelected("PV-001"); assertEquals("ACT-001", vm.form.value.assetId)
        vm.onPointSelected("PV-002"); assertEquals("", vm.form.value.assetId)
        vm.setLocating(true); assertTrue(vm.form.value.isLocating)
        vm.setLocationError("Sin permiso"); assertFalse(vm.form.value.isLocating)
        assertEquals("Sin permiso", vm.form.value.errors.location)
    }
    @Test fun presenceHandlesMissingPointInsideAndOutsideRadius() {
        vm.setLocation(mockk())
        assertNotNull(vm.form.value.errors.location)
        fillForm(); locate(); assertTrue(vm.form.value.presenceVerified)
        locate(151f); assertFalse(vm.form.value.presenceVerified)
    }
    @Test fun savingRequiresFormPhotoAndLocation() = runTest(dispatcher) {
        vm.saveVisit { fail() }; advanceUntilIdle()
        fillForm(); vm.saveVisit { fail() }; advanceUntilIdle()
        assertNotNull(vm.form.value.errors.photo)
        vm.setPhoto("content://photo"); vm.saveVisit { fail() }; advanceUntilIdle()
        assertNotNull(vm.form.value.errors.location)
        coVerify(exactly = 0) { repo.saveVisit(any(), any()) }
    }
    @Test fun successfulSaveBlocksDoubleTap() = runTest(dispatcher) {
        fillForm(); locate(); vm.setPhoto("content://photo")
        coEvery { repo.saveVisit(any(), any()) } coAnswers { delay(10); 5L }
        var id = 0L
        vm.saveVisit { id = it }; vm.saveVisit { fail("Doble envío") }; advanceUntilIdle()
        assertEquals(5L, id); assertTrue(vm.form.value.saveSuccess); assertFalse(vm.form.value.isSaving)
        coVerify(exactly = 1) { repo.saveVisit(any(), any()) }
        vm.clearForm(); assertEquals("", vm.form.value.pointId)
    }
    @Test fun saveErrorRetainsRequestKeyForRetry() = runTest(dispatcher) {
        fillForm(); locate(); vm.setPhoto("content://photo")
        val keys = mutableListOf<String>()
        coEvery { repo.saveVisit(any(), capture(keys)) } throws IOException()
        vm.saveVisit { fail() }; advanceUntilIdle()
        assertNotNull(vm.form.value.saveError)
        vm.saveVisit { fail() }; advanceUntilIdle()
        assertEquals(2, keys.size); assertEquals(keys[0], keys[1])
    }
    @Test fun detailsRepresentFoundAndNotFound() = runTest(dispatcher) {
        assertTrue(vm.observeVisitDetail(1).toList().last() is VisitDetailUiState.Found)
        assertEquals(VisitDetailUiState.NotFound, vm.observeVisitDetail(99).toList().last())
        assertNotNull(vm.observeVisit(1))
    }
    @Test fun remoteOperationsSurfaceErrorsAndSuccess() = runTest(dispatcher) {
        advanceUntilIdle()
        vm.approveVisit(1); advanceUntilIdle()
        vm.rejectVisit(1); advanceUntilIdle()
        var edited = false
        vm.editVisit(1, "Operativo", "") { edited = true }; advanceUntilIdle(); assertTrue(edited)
        var deleted = false
        vm.deleteVisit(1) { deleted = true }; advanceUntilIdle(); assertTrue(deleted)
        coVerify { repo.updateSupervisorStatus(1, "Aprobada"); repo.updateSupervisorStatus(1, "Rechazada") }
        coEvery { repo.refresh() } throws IOException()
        vm.refreshRemote(); advanceUntilIdle()
        assertNotNull(vm.remote.value.error); assertFalse(vm.remote.value.busy)
    }
    @Test fun loginRejectsEmptyFieldsWithoutRemembering() = runTest(dispatcher) {
        val prefs = mockk<UserPreferencesRepository>(relaxed = true)
        every { prefs.lastUsername } returns flowOf("")
        val login = LoginViewModel(prefs); advanceUntilIdle()
        assertFalse(login.validateAndRemember())
        assertNotNull(login.uiState.value.usernameError)
        assertNotNull(login.uiState.value.passwordError)
        coVerify(exactly = 0) { prefs.saveLastUsername(any()) }
    }

    @Test fun loginAssignsSupervisorFromAccountAndRemembersEmail() = runTest(dispatcher) {
        val prefs = mockk<UserPreferencesRepository>(relaxed = true)
        every { prefs.lastUsername } returns flowOf("jona@tacticaltrack.example")
        val login = LoginViewModel(prefs); advanceUntilIdle()
        assertEquals("jona@tacticaltrack.example", login.uiState.value.username)
        login.onUsernameChange(" KEVIN@TACTICALTRACK.EXAMPLE ")
        login.onPasswordChange("Tactical123")
        assertTrue(login.validateAndRemember()); advanceUntilIdle()
        assertEquals("Supervisor", login.uiState.value.role)
        assertEquals("Kevin", login.uiState.value.displayName)
        assertEquals("", login.uiState.value.password)
        coVerify(exactly = 1) { prefs.saveLastUsername("kevin@tacticaltrack.example") }
    }

    @Test fun loginRejectsUnknownAccountAndWrongPassword() = runTest(dispatcher) {
        val prefs = mockk<UserPreferencesRepository>(relaxed = true)
        every { prefs.lastUsername } returns flowOf("")
        val login = LoginViewModel(prefs); advanceUntilIdle()
        login.onUsernameChange("otro@tacticaltrack.example"); login.onPasswordChange("Tactical123")
        assertFalse(login.validateAndRemember())
        login.onUsernameChange("kevin@tacticaltrack.example"); login.onPasswordChange("tactical123")
        assertFalse(login.validateAndRemember())
        assertEquals("", login.uiState.value.role)
        assertNotNull(login.uiState.value.passwordError)
        coVerify(exactly = 0) { prefs.saveLastUsername(any()) }
    }

    @Test fun operatorAccountsCannotChooseSupervisorRole() = runTest(dispatcher) {
        val prefs = mockk<UserPreferencesRepository>(relaxed = true)
        every { prefs.lastUsername } returns flowOf("")
        val login = LoginViewModel(prefs); advanceUntilIdle()
        for ((email, name) in listOf("jona" to "Jona", "victor" to "Víctor")) {
            login.onUsernameChange("$email@tacticaltrack.example"); login.onPasswordChange("Tactical123")
            assertTrue(login.validateAndRemember())
            assertEquals("Operario", login.uiState.value.role)
            assertEquals(name, login.uiState.value.displayName)
        }
        login.onPasswordChange("incorrecta")
        assertEquals("", login.uiState.value.role)
        assertFalse(login.validateAndRemember())
    }
}
