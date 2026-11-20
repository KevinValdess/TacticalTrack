package cl.duoc.tacticaltrack.backend;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.io.IOException;
@Configuration @ConditionalOnProperty(name="push.enabled",havingValue="true")
public class FirebaseConfig {
 @Bean public FirebaseApp firebaseApp(@Value("${push.project-id}") String projectId)throws IOException{
  if(projectId.isBlank())throw new IllegalArgumentException("Configura FIREBASE_PROJECT_ID");
  var options=FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).setProjectId(projectId)
   .setConnectTimeout(10000).setReadTimeout(15000).build();
  return FirebaseApp.initializeApp(options,"tacticaltrack-push");
 }
 @Bean public FirebaseMessaging firebaseMessaging(FirebaseApp app){return FirebaseMessaging.getInstance(app);}
}
