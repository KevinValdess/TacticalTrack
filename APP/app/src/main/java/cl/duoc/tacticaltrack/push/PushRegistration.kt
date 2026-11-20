package cl.duoc.tacticaltrack.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.*
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.TimeUnit

object PushRegistration {
 const val CHANNEL="visits_supervisor"
 // Supervisor asignado a los puntos de prueba.
 const val SUPERVISOR_ID="SUP-001"
 private val _session=MutableStateFlow(false)
 val supervisorSession=_session.asStateFlow()
 fun preferences(context:Context)=context.getSharedPreferences("push_registration",Context.MODE_PRIVATE)
 fun configured(context:Context)=FirebaseApp.getApps(context).isNotEmpty()
 fun initialize(context:Context){
  val channel=NotificationChannel(CHANNEL,"Visitas para revisar",NotificationManager.IMPORTANCE_DEFAULT)
  context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  val prefs=preferences(context)
  if(!prefs.contains("installationId"))prefs.edit().putString("installationId",UUID.randomUUID().toString()).apply()
  if(prefs.getBoolean("enabled",false)&&prefs.getBoolean("supervisor",false)&&configured(context)) refreshToken(context)
 }
 fun setRole(context:Context,role:String){
  val supervisor=role=="Supervisor"
  _session.value=supervisor
  val prefs=preferences(context)
  prefs.edit().putBoolean("supervisor",supervisor).apply()
  if(supervisor&&prefs.getBoolean("enabled",false))refreshToken(context)
  else {
   enqueue(context)
   if(!supervisor&&configured(context))FirebaseMessaging.getInstance().deleteToken()
  }
 }
 fun setEnabled(context:Context,enabled:Boolean){
  preferences(context).edit().putBoolean("enabled",enabled).apply()
  if(enabled)refreshToken(context)
  else {
   enqueue(context)
   context.getSystemService(NotificationManager::class.java).cancelAll()
   if(configured(context))FirebaseMessaging.getInstance().deleteToken()
  }
 }
 fun refreshToken(context:Context){
  if(!configured(context))return
  FirebaseMessaging.getInstance().token.addOnSuccessListener { token->tokenChanged(context,token) }
   .addOnFailureListener { preferences(context).edit().putString("status","No se pudo obtener el token. Reintenta con conexión.").apply() }
 }
 fun tokenChanged(context:Context,token:String){
  val prefs=preferences(context)
  prefs.edit().putString("token",token).apply()
  if(prefs.getBoolean("enabled",false)&&prefs.getBoolean("supervisor",false))enqueue(context)
 }
 private fun enqueue(context:Context){
  val request=OneTimeWorkRequestBuilder<DeviceRegistrationWorker>()
   .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
   .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
  WorkManager.getInstance(context).enqueueUniqueWork("register-push-device",ExistingWorkPolicy.REPLACE,request)
 }
}
