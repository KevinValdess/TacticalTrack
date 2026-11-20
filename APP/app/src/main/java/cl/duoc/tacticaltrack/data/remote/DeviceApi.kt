package cl.duoc.tacticaltrack.data.remote
import retrofit2.http.*
data class DeviceRegistration(val supervisorId:String,val fcmToken:String)
interface DeviceApi {
 @PUT("api/devices/{id}") suspend fun register(@Path("id") installationId:String,@Body registration:DeviceRegistration)
 @DELETE("api/devices/{id}") suspend fun unregister(@Path("id") installationId:String)
}
