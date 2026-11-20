package cl.duoc.tacticaltrack.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VisitServiceTest {
    VisitJpaRepository repository;
    VisitService service;
    @BeforeEach void setup() { repository = mock(VisitJpaRepository.class); 
        var catalog = mock(CatalogService.class);
        when(catalog.point(any())).thenReturn(new PointOfSale("PV-001","Punto Demo","Demo",-33.5,-70.7,"SUP-001"));
        when(catalog.asset(any(),any())).thenReturn(new CoolingAsset("ACT-001","PV-001","FRZ-001","Freezer"));
        service = new VisitService(repository,catalog,mock(OutboxService.class)); }
    static Visit valid() {
        Visit v = new Visit();
        v.clientRequestId = "request-123"; v.operatorName = "Kevin"; v.operatorRole = "Operario";
        v.pointId = "PV-001"; v.pointName = "Punto Demo"; v.assetId = "ACT-001"; v.assetCode = "FRZ-001";
        v.assetType = "Vitrina freezer"; v.visitType = "Mantenimiento"; v.equipmentStatus = "Operativo";
        v.observations = ""; v.latitude = -33.5; v.longitude = -70.7; v.distanceToPointMeters = 20;
        v.locationAccuracyMeters = 5f; v.timestamp = 123456L; v.evidenceBase64 = "aW1hZ2Vu";
        return v;
    }
    @Test void createStoresVisitAndComputesPresence() {
        Visit v = valid(); when(repository.save(v)).thenReturn(v);
        assertSame(v, service.create(v)); assertTrue(v.presenceVerified);
        assertEquals("Pendiente", v.supervisorStatus); verify(repository).save(v);
    }
    @Test void retryUsesSameVisit() {
        Visit v = valid(); v.id = 7L;
        when(repository.findByClientRequestId(v.clientRequestId)).thenReturn(Optional.of(v));
        assertEquals(7L, service.create(valid()).id); verify(repository, never()).save(any());
    }
    @Test void missingVisitIs404() {
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.get(99)).getStatusCode().value());
    }
    @Test void editPersistsAndTrims() {
        Visit v = valid(); when(repository.findById(1L)).thenReturn(Optional.of(v)); when(repository.save(v)).thenReturn(v);
        assertEquals("Falla de motor", service.edit(1L, "Con falla", " Falla de motor ").observations);
    }
    @Test void invalidEditDoesNotWrite() {
        assertThrows(ResponseStatusException.class, () -> service.edit(1, "Con falla", "x"));
        assertThrows(ResponseStatusException.class, () -> service.edit(1, null, ""));
        assertThrows(ResponseStatusException.class, () -> service.edit(1, "Operativo", "x".repeat(2001)));
        verify(repository, never()).save(any());
    }
    @Test void approvesAndRejects() {
        Visit v = valid(); when(repository.findById(1L)).thenReturn(Optional.of(v)); when(repository.save(v)).thenReturn(v);
        assertEquals("Aprobada", service.status(1, "Aprobada").supervisorStatus);
        assertEquals("Rechazada", service.status(1, "Rechazada").supervisorStatus);
        assertThrows(ResponseStatusException.class, () -> service.status(1, "otro"));
    }
    @Test void deleteRequiresExistingVisit() {
        Visit v = valid(); when(repository.findById(1L)).thenReturn(Optional.of(v));
        service.delete(1); verify(repository).delete(v);
        assertThrows(ResponseStatusException.class, () -> service.delete(2));
    }
    @Test void invalidPayloadsAre400() {
        Visit v = valid(); v.latitude = Double.NaN;
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.create(v)).getStatusCode().value());
        Visit empty = valid(); empty.operatorName = " "; assertThrows(ResponseStatusException.class, () -> service.create(empty));
        Visit image = valid(); image.evidenceBase64 = "**"; assertThrows(ResponseStatusException.class, () -> service.create(image));
    }
}
