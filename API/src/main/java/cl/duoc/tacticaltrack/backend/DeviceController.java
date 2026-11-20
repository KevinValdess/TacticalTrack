package cl.duoc.tacticaltrack.backend;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/devices")
public class DeviceController {
 private final DeviceService service;
 public DeviceController(DeviceService service){this.service=service;}
 public record Registration(String supervisorId,String fcmToken){}
 @PutMapping("/{installationId}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void register(@PathVariable String installationId,@RequestBody Registration r){service.register(installationId,r.supervisorId(),r.fcmToken());}
 @DeleteMapping("/{installationId}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void unregister(@PathVariable String installationId){service.unregister(installationId);}
}
