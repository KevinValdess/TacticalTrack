package cl.duoc.tacticaltrack.backend;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional
public class DeviceService {
 private final DeviceRepository devices;private final CatalogService catalog;
 public DeviceService(DeviceRepository devices,CatalogService catalog){this.devices=devices;this.catalog=catalog;}
 public void register(String id,String supervisorId,String token){
  validateId(id);
  if(supervisorId==null||!catalog.supervisorExists(supervisorId))bad("Supervisor desconocido");
  if(token==null||token.isBlank()||token.length()>4096)bad("Token inválido");
  var device=devices.findById(id).orElseGet(SupervisorDevice::new);
  device.installationId=id;device.supervisorId=supervisorId;device.fcmToken=token;device.active=true;device.updatedAt=System.currentTimeMillis();
  devices.save(device);
 }
 public void unregister(String id){validateId(id);devices.findById(id).ifPresent(d->{d.active=false;devices.save(d);});}
 private void validateId(String id){if(id==null||!id.matches("[a-zA-Z0-9-]{8,100}"))bad("Instalación inválida");}
 private void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
