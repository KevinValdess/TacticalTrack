package cl.duoc.tacticaltrack

import cl.duoc.tacticaltrack.data.local.*
import cl.duoc.tacticaltrack.data.remote.*
import cl.duoc.tacticaltrack.data.repository.VisitRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

fun sampleVisit() = VisitEntity(1, "Kevin", "Operario", "PV-001", "Demo", "ACT-001", "FRZ-001", "Freezer",
    "Mantenimiento", "Operativo", "", "content://photo", -33.5, -70.7, 5f, 10f, true, 123L)
fun sampleDto() = VisitDto.fromEntity(sampleVisit(), "aW1hZ2Vu", "test-request")

class VisitRepositoryTest {
    private val dao = mockk<VisitDao>(relaxed = true)
    private val api = mockk<VisitApi>()
    private val evidence = mockk<EvidenceStore>()
    private lateinit var repository: VisitRepository
    @Before fun setup() {
        every { dao.observeAll() } returns flowOf(listOf(sampleVisit()))
        every { dao.observeById(1) } returns flowOf(sampleVisit())
        coEvery { evidence.encode(any()) } returns "aW1hZ2Vu"
        coEvery { evidence.persist(any(), any()) } returns "file:///photo.jpg"
        coEvery { evidence.remove(any()) } just Runs
        repository = VisitRepository(dao, api, evidence)
    }
    @Test fun refreshReplacesCacheAfterSuccess() = runBlocking {
        coEvery { api.all() } returns listOf(sampleDto())
        repository.refresh()
        coVerify { dao.replaceSnapshot(match { it.single().synced && it.single().photoUri == "file:///photo.jpg" }) }
    }
    @Test fun refreshErrorKeepsCache() = runBlocking {
        coEvery { api.all() } throws IOException()
        try { repository.refresh(); fail() } catch (_: IOException) { }
        coVerify(exactly = 0) { dao.replaceSnapshot(any()) }
    }
    @Test fun saveUsesServerIdAndIdempotencyKey() = runBlocking {
        coEvery { api.create(any()) } returns sampleDto().copy(id = 55)
        assertEquals(55L, repository.saveVisit(sampleVisit(), "stable-key"))
        coVerify { api.create(match { it.clientRequestId == "stable-key" }) }
        coVerify { dao.insert(match { it.id == 55L && it.synced }) }
    }
    @Test fun failedSaveDoesNotInsert() = runBlocking {
        coEvery { api.create(any()) } throws IOException()
        try { repository.saveVisit(sampleVisit(), "key"); fail() } catch (_: IOException) { }
        coVerify(exactly = 0) { dao.insert(any()) }
    }
    @Test fun editTrimsAndCachesResponse() = runBlocking {
        coEvery { api.edit(1, any()) } returns sampleDto().copy(observations = "Motor detenido")
        repository.editVisit(1, "Con falla", " Motor detenido ")
        coVerify { api.edit(1, EditVisitRequest("Con falla", "Motor detenido")) }
        coVerify { dao.insert(match { it.observations == "Motor detenido" }) }
    }
    @Test fun invalidEditDoesNotCallServer() = runBlocking {
        try { repository.editVisit(1, "Con falla", "x"); fail() } catch (_: IllegalArgumentException) { }
        coVerify(exactly = 0) { api.edit(any(), any()) }
    }
    @Test fun supervisorStatusPersistsRemotely() = runBlocking {
        coEvery { api.status(1, SupervisorRequest("Aprobada")) } returns sampleDto().copy(supervisorStatus = "Aprobada")
        repository.updateSupervisorStatus(1, "Aprobada")
        coVerify { dao.insert(match { it.supervisorStatus == "Aprobada" }) }
        try { repository.updateSupervisorStatus(1, "otro"); fail() } catch (_: IllegalArgumentException) { }
    }
    @Test fun deleteServerBeforeCache() = runBlocking {
        coEvery { api.delete(1) } just Runs
        repository.deleteVisit(1)
        coVerifyOrder { api.delete(1); dao.delete(1); evidence.remove(1) }
    }
    @Test fun failedDeleteKeepsCache() = runBlocking {
        coEvery { api.delete(1) } throws IOException()
        try { repository.deleteVisit(1); fail() } catch (_: IOException) { }
        coVerify(exactly = 0) { dao.delete(any()) }
    }
    @Test fun observeReturnsDaoFlow() {
        assertNotNull(repository.visits)
        assertNotNull(repository.observeVisit(1))
    }
}
