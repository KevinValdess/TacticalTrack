package cl.duoc.tacticaltrack.backend;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:devicehttp;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","push.enabled=false"})
class CatalogDeviceHttpTest {
 @Autowired CatalogService catalog;@Autowired DeviceService devices;@Autowired VisitService visits;@Autowired OutboxRepository jobs;
 @Test void catalogDevicesAndNotificationStatusHaveHttpContract()throws Exception{
  var mvc=MockMvcBuilders.standaloneSetup(new CatalogController(catalog),new DeviceController(devices),new NotificationController(jobs)).build();
  mvc.perform(get("/api/points")).andExpect(status().isOk()).andExpect(jsonPath("$[0].supervisorId").value("SUP-001"));
  mvc.perform(get("/api/assets").param("pointId","PV-001")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
  mvc.perform(put("/api/devices/installation-http").contentType(MediaType.APPLICATION_JSON)
    .content("{\"supervisorId\":\"SUP-001\",\"fcmToken\":\"test-token\"}")).andExpect(status().isNoContent());
  mvc.perform(put("/api/devices/installation-http").contentType(MediaType.APPLICATION_JSON)
    .content("{\"supervisorId\":\"UNKNOWN\",\"fcmToken\":\"test-token\"}")).andExpect(status().isBadRequest());
  var v=VisitServiceTest.valid();v.clientRequestId="device-http";v=visits.create(v);
  mvc.perform(get("/api/notifications/"+v.id)).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PENDING"));
  mvc.perform(delete("/api/devices/installation-http")).andExpect(status().isNoContent());
 }
}
