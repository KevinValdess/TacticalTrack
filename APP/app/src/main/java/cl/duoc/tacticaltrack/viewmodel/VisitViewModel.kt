package cl.duoc.tacticaltrack.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaltrack.data.local.VisitEntity
import cl.duoc.tacticaltrack.data.repository.VisitRepository
import cl.duoc.tacticaltrack.model.CoolingAsset
import cl.duoc.tacticaltrack.data.repository.CatalogRepository
import cl.duoc.tacticaltrack.model.PointOfSale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.atomic.AtomicBoolean

data class CatalogUiState(
    val points: List<PointOfSale> = emptyList(),
    val assets: List<CoolingAsset> = emptyList(),
    val busy: Boolean = true,
    val error: String? = null
)

data class RemoteUiState(val busy: Boolean = false, val message: String? = null, val error: String? = null)

sealed interface VisitDetailUiState {
    data object Loading : VisitDetailUiState
    data object NotFound : VisitDetailUiState
    data class Found(val visit: VisitEntity) : VisitDetailUiState
}

data class VisitErrors(
    val point: String? = null,
    val asset: String? = null,
    val visitType: String? = null,
    val equipmentStatus: String? = null,
    val observations: String? = null,
    val photo: String? = null,
    val location: String? = null
)

data class VisitFormUiState(
    val pointId: String = "",
    val assetId: String = "",
    val visitType: String = "",
    val equipmentStatus: String = "",
    val observations: String = "",
    val photoUri: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracyMeters: Float? = null,
    val distanceToPointMeters: Float? = null,
    val presenceVerified: Boolean = false,
    val isLocating: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    val locationMessage: String = "",
    val errors: VisitErrors = VisitErrors()
)

class VisitViewModel(
    private val repository: VisitRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    companion object {
        // Radio usado para verificar la cercanía al punto de venta.
        const val PRESENCE_RADIUS_METERS = 150f
    }

    private val _catalogState = MutableStateFlow(CatalogUiState())
    val catalogState = _catalogState.asStateFlow()
    val points: List<PointOfSale> get() = _catalogState.value.points
    fun pointById(id:String) = points.firstOrNull { it.id==id }
    fun assetById(id:String) = _catalogState.value.assets.firstOrNull { it.id==id }
    private fun refreshCatalog() {
        viewModelScope.launch {
            _catalogState.update { it.copy(busy=true,error=null) }
            try {
                val snapshot=catalogRepository.load()
                _catalogState.value=CatalogUiState(snapshot.points,snapshot.assets,busy=false)
            } catch(e:CancellationException){throw e}
            catch(e:Exception){_catalogState.update {it.copy(busy=false,error="No se pudo cargar el catálogo. Actualiza desde Inicio.")}}
        }
    }
    val visitTypes = listOf("Mantenimiento", "Venta", "Transporte")
    val equipmentStatuses = listOf("Operativo", "Operativo con observaciones", "Con falla")

    private val _form = MutableStateFlow(VisitFormUiState())
    private var requestId = java.util.UUID.randomUUID().toString()
    private val saveInProgress = AtomicBoolean(false)
    val form: StateFlow<VisitFormUiState> = _form.asStateFlow()

    private val _operatorName = MutableStateFlow("Operario")
    val operatorName: StateFlow<String> = _operatorName.asStateFlow()

    private val _operatorRole = MutableStateFlow("Operario")
    val operatorRole: StateFlow<String> = _operatorRole.asStateFlow()

    val visits: StateFlow<List<VisitEntity>> = repository.visits.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _remote = MutableStateFlow(RemoteUiState())
    val remote = _remote.asStateFlow()
    init { refreshRemote() }

    private fun remoteOperation(action: suspend () -> Unit, onSuccess: () -> Unit = {}) {
        if (_remote.value.busy) return
        _remote.value = RemoteUiState(busy = true)
        viewModelScope.launch {
            try {
                action()
                _remote.value = RemoteUiState(message = "Datos actualizados.")
                onSuccess()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _remote.value = RemoteUiState(error = "No se pudo completar la operación. Revisa la conexión y actualiza antes de reintentar.")
            } finally { _remote.update { it.copy(busy = false) } }
        }
    }
    fun refreshRemote() {
        refreshCatalog()
        remoteOperation({ repository.refresh() })
    }
    fun openRemoteVisit(id: Long, onSuccess: () -> Unit) {
        // Una pulsación push puede llegar mientras se actualiza el historial.
        viewModelScope.launch {
            try { repository.fetchVisit(id); onSuccess() }
            catch(e:CancellationException){throw e}
            catch(e:Exception){_remote.update {it.copy(error="No se pudo abrir el aviso. Actualiza el historial y revisa la visita.")}}
        }
    }
    fun editVisit(id: Long, status: String, observations: String, onSuccess: () -> Unit) =
        remoteOperation({ repository.editVisit(id, status, observations) }, onSuccess)
    fun deleteVisit(id: Long, onSuccess: () -> Unit) = remoteOperation({ repository.deleteVisit(id) }, onSuccess)

    fun setOperator(name: String, role: String) {
        _operatorName.value = name
        _operatorRole.value = role
    }

    fun assetsForSelectedPoint(): List<CoolingAsset> =
        _catalogState.value.assets.filter { it.pointId==_form.value.pointId }

    fun onPointSelected(pointId: String) {
        _form.update {
            if (it.pointId == pointId) return@update it
            it.copy(
                pointId = pointId,
                assetId = "",
                latitude = null,
                longitude = null,
                locationAccuracyMeters = null,
                distanceToPointMeters = null,
                presenceVerified = false,
                isLocating = false,
                locationMessage = "",
                errors = it.errors.copy(point = null, asset = null, location = null)
            )
        }
    }

    fun onAssetSelected(assetId: String) {
        _form.update { it.copy(assetId = assetId, errors = it.errors.copy(asset = null)) }
    }

    fun onVisitTypeSelected(value: String) {
        _form.update { it.copy(visitType = value, errors = it.errors.copy(visitType = null)) }
    }

    fun onEquipmentStatusSelected(value: String) {
        _form.update {
            it.copy(
                equipmentStatus = value,
                errors = it.errors.copy(equipmentStatus = null, observations = null)
            )
        }
    }

    fun onObservationsChange(value: String) {
        _form.update { it.copy(observations = value, errors = it.errors.copy(observations = null)) }
    }

    fun setPhoto(uri: String) {
        _form.update { it.copy(photoUri = uri, errors = it.errors.copy(photo = null)) }
    }

    fun setLocating(value: Boolean) {
        _form.update { it.copy(isLocating = value) }
    }

    fun setLocationError(message: String) {
        _form.update {
            it.copy(
                isLocating = false,
                locationMessage = message,
                errors = it.errors.copy(location = message)
            )
        }
    }

    fun setLocation(location: Location) {
        val state = _form.value
        val point = pointById(state.pointId)

        if (point == null) {
            setLocationError("Selecciona un punto de venta antes de registrar la ubicación.")
            return
        }

        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            point.latitude,
            point.longitude,
            results
        )
        val distance = results[0]
        val verified = distance <= PRESENCE_RADIUS_METERS

        _form.update {
            it.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                locationAccuracyMeters = location.accuracy,
                distanceToPointMeters = distance,
                presenceVerified = verified,
                isLocating = false,
                locationMessage = if (verified) {
                    "Presencia compatible con el punto de venta."
                } else {
                    "Ubicación registrada fuera del radio de ${PRESENCE_RADIUS_METERS.toInt()} m."
                },
                errors = it.errors.copy(location = null)
            )
        }
    }

    fun validateBeforeEvidence(): Boolean {
        val s = _form.value
        val errors = VisitErrors(
            point = if (s.pointId.isBlank()) "Selecciona un punto de venta." else null,
            asset = if (s.assetId.isBlank()) "Selecciona un activo." else null,
            visitType = if (s.visitType.isBlank()) "Selecciona el tipo de visita." else null,
            equipmentStatus = if (s.equipmentStatus.isBlank()) "Selecciona el estado del equipo." else null,
            observations = cl.duoc.tacticaltrack.domain.VisitRules.editError(s.equipmentStatus, s.observations)
        )
        _form.update { it.copy(errors = errors) }
        return listOf(
            errors.point,
            errors.asset,
            errors.visitType,
            errors.equipmentStatus,
            errors.observations
        ).all { it == null }
    }

    private fun validateEvidence(): Boolean {
        val s = _form.value
        val errors = s.errors.copy(
            photo = if (s.photoUri.isBlank()) "Debes adjuntar evidencia fotográfica." else null,
            location = if (s.latitude == null || s.longitude == null) {
                "Debes registrar la ubicación del dispositivo."
            } else null
        )
        _form.update { it.copy(errors = errors) }
        return errors.photo == null && errors.location == null
    }

    fun saveVisit(onSaved: (Long) -> Unit) {
        if (!saveInProgress.compareAndSet(false, true)) return

        viewModelScope.launch {
            if (!validateBeforeEvidence() || !validateEvidence()) return@launch

            val s = _form.value
            val point = pointById(s.pointId) ?: return@launch
            val asset = assetById(s.assetId) ?: return@launch

            _form.update { it.copy(isSaving = true, saveSuccess = false, saveError = null) }

            val id = try {
                repository.saveVisit(
                    VisitEntity(
                        operatorName = _operatorName.value,
                        operatorRole = _operatorRole.value,
                        pointId = point.id,
                        pointName = point.name,
                        assetId = asset.id,
                        assetCode = asset.code,
                        assetType = asset.type,
                        visitType = s.visitType,
                        equipmentStatus = s.equipmentStatus,
                        observations = s.observations.trim(),
                        photoUri = s.photoUri,
                        latitude = s.latitude!!,
                        longitude = s.longitude!!,
                        locationAccuracyMeters = s.locationAccuracyMeters,
                        distanceToPointMeters = s.distanceToPointMeters ?: Float.MAX_VALUE,
                        presenceVerified = s.presenceVerified,
                        timestamp = System.currentTimeMillis()
                    ),
                    requestId = requestId
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _form.update {
                    it.copy(saveError = "No se pudo guardar la visita. Inténtalo nuevamente.", saveSuccess = false)
                }
                return@launch
            }
            _form.update { it.copy(isSaving = false, saveSuccess = true) }
            onSaved(id)
        }.invokeOnCompletion {
            _form.update { it.copy(isSaving = false) }
            saveInProgress.set(false)
        }
    }

    fun clearForm() {
        requestId = java.util.UUID.randomUUID().toString()
        _form.value = VisitFormUiState()
    }

    fun observeVisit(id: Long) = repository.observeVisit(id)

    fun observeVisitDetail(id: Long) = repository.observeVisit(id)
        .map<VisitEntity?, VisitDetailUiState> { visit ->
            if (visit == null) VisitDetailUiState.NotFound else VisitDetailUiState.Found(visit)
        }
        .onStart { emit(VisitDetailUiState.Loading) }

    fun approveVisit(id: Long) = remoteOperation({ repository.updateSupervisorStatus(id, "Aprobada") })
    fun rejectVisit(id: Long) = remoteOperation({ repository.updateSupervisorStatus(id, "Rechazada") })

    class Factory(
        private val repository: VisitRepository,
        private val catalogRepository: CatalogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VisitViewModel(repository,catalogRepository) as T
        }
    }
}

