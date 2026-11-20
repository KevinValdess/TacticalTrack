package cl.duoc.tacticaltrack.push
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PushSettingsCard(){
 val context=LocalContext.current
 val prefs=remember {PushRegistration.preferences(context)}
 var enabled by remember {mutableStateOf(prefs.getBoolean("enabled",false))}
 var status by remember {mutableStateOf(prefs.getString("status","").orEmpty())}
 DisposableEffect(prefs){
  val listener=SharedPreferences.OnSharedPreferenceChangeListener { p,_->enabled=p.getBoolean("enabled",false);status=p.getString("status","").orEmpty() }
  prefs.registerOnSharedPreferenceChangeListener(listener)
  onDispose {prefs.unregisterOnSharedPreferenceChangeListener(listener)}
 }
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
  if(granted)PushRegistration.setEnabled(context,true)
  else prefs.edit().putString("status","Permiso denegado. Puedes revisar visitas desde el historial.").apply()
 }
 Card(Modifier.fillMaxWidth()){
  Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text("Avisos de nuevas visitas",style=MaterialTheme.typography.titleMedium)
   if(!PushRegistration.configured(context))Text("Las notificaciones todavía no están configuradas.")
   else {
    Text(if(enabled)"Avisos activados en este dispositivo." else "Recibe avisos cuando se registren nuevas visitas.")
    TextButton(onClick={
     if(enabled)PushRegistration.setEnabled(context,false)
     else if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
     else PushRegistration.setEnabled(context,true)
    }){Text(if(enabled)"Desactivar avisos" else "Activar avisos")}
    if(enabled)TextButton(onClick={PushRegistration.refreshToken(context)}){Text("Volver a conectar")}
   }
   if(status.isNotBlank())Text(status,style=MaterialTheme.typography.bodySmall)
  }
 }
}
