package cl.duoc.tacticaltrack.push
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.duoc.tacticaltrack.data.remote.ApiFactory
import cl.duoc.tacticaltrack.data.repository.DeviceRegistrationRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class DeviceRegistrationWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters){
 override suspend fun doWork():Result {
  val prefs=PushRegistration.preferences(applicationContext)
  val enabled=prefs.getBoolean("enabled",false)&&prefs.getBoolean("supervisor",false)
  val id=prefs.getString("installationId",null)?:return Result.failure()
  val token=prefs.getString("token",null)
  if(enabled&&token.isNullOrBlank())return Result.retry()
  return try {
   val supervisorId=prefs.getString("supervisorId",null).orEmpty()
   if(enabled&&supervisorId.isBlank())return Result.failure()
   DeviceRegistrationRepository(ApiFactory.devices).sync(id,supervisorId,token,enabled)
   prefs.edit().putString("status",if(enabled)"Dispositivo registrado en la API." else "Dispositivo desactivado en la API.").apply()
   Result.success()
  }catch(e:CancellationException){throw e}
   catch(e:HttpException){
    prefs.edit().putString("status","La API rechazó el registro: HTTP ${e.code()}.").apply()
    if(e.code() in 400..499&&e.code()!=408&&e.code()!=429)Result.failure() else Result.retry()
   }catch(e:Exception){
    prefs.edit().putString("status","Registro pendiente: se reintentará con conexión.").apply()
    Result.retry()
   }
 }
}
