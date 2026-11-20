package cl.duoc.tacticaltrack.data.repository

import cl.duoc.tacticaltrack.data.local.VisitDao
import cl.duoc.tacticaltrack.data.local.VisitEntity
import cl.duoc.tacticaltrack.data.remote.*
import cl.duoc.tacticaltrack.domain.VisitRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Actualiza el caché local después de cada operación confirmada por la API. */
class VisitRepository(
    private val visitDao: VisitDao,
    private val api: VisitApi,
    private val evidence: EvidenceStore
) {
    private val mutex = Mutex()
    val visits: Flow<List<VisitEntity>> = visitDao.observeAll()
    fun observeVisit(id: Long): Flow<VisitEntity?> = visitDao.observeById(id)

    suspend fun fetchVisit(id: Long) = mutex.withLock {
        val visit=api.get(id)
        visitDao.insert(visit.toEntity(evidence.persist(id,visit.evidenceBase64)))
        Unit
    }
    suspend fun refresh() = mutex.withLock {
        val remote = api.all()
        val snapshot = remote.map { it.toEntity(evidence.persist(it.id, it.evidenceBase64)) }
        visitDao.replaceSnapshot(snapshot)
    }
    suspend fun saveVisit(visit: VisitEntity, requestId: String): Long = mutex.withLock {
        val dto = VisitDto.fromEntity(visit, evidence.encode(visit.photoUri), requestId)
        val created = api.create(dto)
        visitDao.insert(created.toEntity(evidence.persist(created.id, created.evidenceBase64)))
        created.id
    }
    suspend fun editVisit(id: Long, status: String, observations: String) = mutex.withLock {
        require(VisitRules.editError(status, observations) == null) { "Datos de edición inválidos" }
        val updated = api.edit(id, EditVisitRequest(status, observations.trim()))
        visitDao.insert(updated.toEntity(evidence.persist(id, updated.evidenceBase64)))
        Unit
    }
    suspend fun updateSupervisorStatus(id: Long, status: String) = mutex.withLock {
        require(status in listOf("Pendiente", "Aprobada", "Rechazada"))
        val updated = api.status(id, SupervisorRequest(status))
        visitDao.insert(updated.toEntity(evidence.persist(id, updated.evidenceBase64)))
        Unit
    }
    suspend fun deleteVisit(id: Long) = mutex.withLock {
        api.delete(id)
        visitDao.delete(id)
        evidence.remove(id)
    }
}
