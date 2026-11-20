package cl.duoc.tacticaltrack.backend;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
class DeviceServiceTest {
 @Test void registerRotatesTokenAndUnregisterDisables(){
  var repo=mock(DeviceRepository.class);var catalog=mock(CatalogService.class);when(catalog.supervisorExists("SUP-001")).thenReturn(true);
  var service=new DeviceService(repo,catalog);var device=new SupervisorDevice();device.installationId="installation-123";
  when(repo.findById(device.installationId)).thenReturn(Optional.of(device));
  service.register(device.installationId,"SUP-001","token-new");assertEquals("token-new",device.fcmToken);assertTrue(device.active);
  service.unregister(device.installationId);assertFalse(device.active);
 }
 @Test void unknownSupervisorAndEmptyTokenAreRejected(){
  var repo=mock(DeviceRepository.class);var catalog=mock(CatalogService.class);var service=new DeviceService(repo,catalog);
  assertThrows(ResponseStatusException.class,()->service.register("installation-123","unknown","token"));
  when(catalog.supervisorExists("SUP-001")).thenReturn(true);
  assertThrows(ResponseStatusException.class,()->service.register("installation-123","SUP-001",""));
  assertThrows(ResponseStatusException.class,()->service.unregister("x"));verify(repo,never()).save(any());
 }
}
