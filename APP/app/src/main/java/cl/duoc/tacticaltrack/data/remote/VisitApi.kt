package cl.duoc.tacticaltrack.data.remote

import cl.duoc.tacticaltrack.data.local.VisitEntity
import retrofit2.http.*

data class VisitDto(
    val id: Long = 0,
    val operatorName: String,
    val operatorRole: String,
    val pointId: String,
    val pointName: String,
    val assetId: String,
    val assetCode: String,
    val assetType: String,
    val visitType: String,
    val equipmentStatus: String,
    val observations: String,
    val latitude: Double,
    val longitude: Double,
    val locationAccuracyMeters: Float?,
    val distanceToPointMeters: Float,
    val presenceVerified: Boolean,
    val timestamp: Long,
    val supervisorStatus: String = "Pendiente",
    val evidenceBase64: String,
    val clientRequestId: String
) {
    fun toEntity(photoUri: String) = VisitEntity(
        id, operatorName, operatorRole, pointId, pointName, assetId, assetCode,
        assetType, visitType, equipmentStatus, observations, photoUri, latitude,
        longitude, locationAccuracyMeters, distanceToPointMeters, presenceVerified,
        timestamp, supervisorStatus, synced = true
    )
    companion object {
        fun fromEntity(v: VisitEntity, evidence: String, requestId: String) = VisitDto(
            v.id, v.operatorName, v.operatorRole, v.pointId, v.pointName, v.assetId,
            v.assetCode, v.assetType, v.visitType, v.equipmentStatus, v.observations,
            v.latitude, v.longitude, v.locationAccuracyMeters, v.distanceToPointMeters,
            v.presenceVerified, v.timestamp, v.supervisorStatus, evidence, requestId
        )
    }
}

data class EditVisitRequest(val equipmentStatus: String, val observations: String)
data class SupervisorRequest(val status: String)

interface VisitApi {
    @GET("api/visits") suspend fun all(): List<VisitDto>
    @GET("api/visits/{id}") suspend fun get(@Path("id") id: Long): VisitDto
    @POST("api/visits") suspend fun create(@Body visit: VisitDto): VisitDto
    @PUT("api/visits/{id}") suspend fun edit(@Path("id") id: Long, @Body edit: EditVisitRequest): VisitDto
    @PATCH("api/visits/{id}/status") suspend fun status(@Path("id") id: Long, @Body request: SupervisorRequest): VisitDto
    @DELETE("api/visits/{id}") suspend fun delete(@Path("id") id: Long)
}
