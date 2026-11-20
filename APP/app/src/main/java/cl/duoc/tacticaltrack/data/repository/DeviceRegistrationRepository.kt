package cl.duoc.tacticaltrack.data.repository
import cl.duoc.tacticaltrack.data.remote.DeviceApi
import cl.duoc.tacticaltrack.data.remote.DeviceRegistration
class DeviceRegistrationRepository(private val api:DeviceApi){
 suspend fun sync(installationId:String,supervisorId:String,token:String?,enabled:Boolean){
  require(installationId.matches(Regex("[a-zA-Z0-9-]{8,100}")))
  if(enabled){require(!token.isNullOrBlank());require(supervisorId.isNotBlank());api.register(installationId,DeviceRegistration(supervisorId,token))}
  else api.unregister(installationId)
 }
}
