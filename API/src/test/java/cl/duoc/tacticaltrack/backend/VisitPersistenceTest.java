package cl.duoc.tacticaltrack.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:visittest;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop"})
class VisitPersistenceTest {
    @Autowired VisitService service;
    @Test void fullCrudWithRealDatabase() {
        Visit v = service.create(VisitServiceTest.valid());
        long id = v.id;
        assertEquals("Kevin", service.get(id).operatorName);
        service.edit(id, "Con falla", "Motor detenido");
        assertEquals("Motor detenido", service.get(id).observations);
        service.status(id, "Aprobada");
        assertEquals("Aprobada", service.get(id).supervisorStatus);
        assertTrue(service.all().stream().anyMatch(item -> item.id == id));
        service.delete(id);
        assertThrows(Exception.class, () -> service.get(id));
    }
}
