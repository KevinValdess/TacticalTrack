package cl.duoc.tacticaltrack.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:httptest;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop"})
class VisitHttpTest {
    @Autowired VisitService service;
    @Test void endpointsSupportFullCrudAndValidation() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new VisitController(service)).build();
        String body = """
            {"clientRequestId":"http-test","operatorName":"Kevin","operatorRole":"Operario",
             "pointId":"PV-001","pointName":"Demo","assetId":"ACT-001","assetCode":"FRZ-001",
             "assetType":"Freezer","visitType":"Mantenimiento","equipmentStatus":"Operativo",
             "observations":"","latitude":-33.5,"longitude":-70.7,"distanceToPointMeters":20,
             "timestamp":123456,"evidenceBase64":"aW1hZ2Vu"}
            """;
        mvc.perform(post("/api/visits").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.operatorName").value("Kevin"));
        long id = service.all().get(0).id;
        mvc.perform(get("/api/visits/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/visits")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(put("/api/visits/" + id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"equipmentStatus\":\"Con falla\",\"observations\":\"Motor detenido\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.observations").value("Motor detenido"));
        mvc.perform(patch("/api/visits/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"Aprobada\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.supervisorStatus").value("Aprobada"));
        mvc.perform(put("/api/visits/" + id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"equipmentStatus\":\"Con falla\",\"observations\":\"x\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/visits/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/visits/" + id)).andExpect(status().isNotFound());
    }
}
