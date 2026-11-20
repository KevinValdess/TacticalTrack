package cl.duoc.tacticaltrack.push
import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.duoc.tacticaltrack.MainActivity
import cl.duoc.tacticaltrack.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TacticalMessagingService:FirebaseMessagingService(){
 override fun onNewToken(token:String){PushRegistration.tokenChanged(this,token)}
 override fun onMessageReceived(message:RemoteMessage){
  val prefs=PushRegistration.preferences(this)
  if(!prefs.getBoolean("enabled",false)||!prefs.getBoolean("supervisor",false))return
  if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
  val id=message.data["visitId"]?.toLongOrNull()?.takeIf {it>0}?:return
  val intent=Intent(this,MainActivity::class.java).putExtra("visitId",id.toString())
   .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
  val pending=PendingIntent.getActivity(this,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notification=NotificationCompat.Builder(this,PushRegistration.CHANNEL)
   .setSmallIcon(R.drawable.ic_stat_visit).setContentTitle("Nueva visita registrada")
   .setContentText("Hay una visita pendiente de revisión.").setContentIntent(pending).setAutoCancel(true).build()
  NotificationManagerCompat.from(this).notify("visit-$id",0,notification)
 }
}
