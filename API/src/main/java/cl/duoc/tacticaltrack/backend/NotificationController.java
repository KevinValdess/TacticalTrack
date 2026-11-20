package cl.duoc.tacticaltrack.backend;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/notifications")
public class NotificationController {
 private final OutboxRepository jobs;
 public NotificationController(OutboxRepository jobs){this.jobs=jobs;}
 public record Status(long visitId,String state,int attempts,String lastError){}
 @GetMapping("/{visitId}") public Status status(@PathVariable long visitId){
  var job=jobs.findById(visitId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Aviso no encontrado"));
  return new Status(visitId,job.state,job.attempts,job.lastError);
 }
}
