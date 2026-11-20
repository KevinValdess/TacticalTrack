package cl.duoc.tacticaltrack.backend;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @ConditionalOnProperty(name="push.enabled",havingValue="true")
public class FcmPushSender implements PushSender {
 private final FirebaseMessaging messaging;
 public FcmPushSender(FirebaseMessaging messaging){this.messaging=messaging;}
 public Result send(String token,long visitId){
  var message=Message.builder().setToken(token)
   .setNotification(Notification.builder().setTitle("Nueva visita registrada").setBody("Hay una visita pendiente de revisión.").build())
   .putData("visitId",Long.toString(visitId)).putData("eventId","visit-"+visitId)
   .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).setTtl(86400000)
    .setNotification(AndroidNotification.builder().setChannelId("visits_supervisor").setTag("visit-"+visitId).build()).build()).build();
  try{messaging.send(message);return Result.ACCEPTED;}
  catch(FirebaseMessagingException e){return e.getMessagingErrorCode()==MessagingErrorCode.UNREGISTERED?Result.INVALID_TOKEN:Result.RETRY;}
 }
}
