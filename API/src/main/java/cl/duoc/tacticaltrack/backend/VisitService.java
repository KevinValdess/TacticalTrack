package cl.duoc.tacticaltrack.backend;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class VisitService {
    private final VisitJpaRepository repository;
    private final CatalogService catalog;
    private final OutboxService outbox;
    private static final Set<String> STATES = Set.of("Pendiente", "Aprobada", "Rechazada");
    private static final Set<String> EQUIPMENT = Set.of("Operativo", "Operativo con observaciones", "Con falla");
    public VisitService(VisitJpaRepository repository, CatalogService catalog, OutboxService outbox) { this.repository = repository; this.catalog = catalog; this.outbox = outbox; }

    public List<Visit> all() { return repository.findAllByOrderByTimestampDesc(); }
    public Visit get(long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita no encontrada"));
    }
    public Visit create(Visit visit) {
        requireText(visit.clientRequestId, "Identificador de solicitud", 100);
        var existing = repository.findByClientRequestId(visit.clientRequestId);
        if (existing.isPresent()) return existing.get(); // Reintentos tras corte de red.
        requireText(visit.operatorName, "Operario", 100);
        requireText(visit.operatorRole, "Rol", 40);
        requireText(visit.pointId, "Punto", 40);
        requireText(visit.pointName, "Nombre de punto", 150);
        requireText(visit.assetId, "Activo", 40);
        requireText(visit.assetCode, "Código de activo", 40);
        requireText(visit.assetType, "Tipo de activo", 100);
        if (!Set.of("Mantenimiento", "Venta", "Transporte").contains(safe(visit.visitType))) bad("Tipo de visita inválido");
        validateEdit(visit.equipmentStatus, visit.observations);
        if (!Double.isFinite(visit.latitude) || Math.abs(visit.latitude) > 90 ||
            !Double.isFinite(visit.longitude) || Math.abs(visit.longitude) > 180) bad("Coordenadas inválidas");
        if (!Float.isFinite(visit.distanceToPointMeters) || visit.distanceToPointMeters < 0) bad("Distancia inválida");
        if (visit.locationAccuracyMeters != null && (!Float.isFinite(visit.locationAccuracyMeters) || visit.locationAccuracyMeters < 0)) bad("Precisión inválida");
        if (visit.timestamp <= 0) bad("Fecha inválida");
        requireText(visit.evidenceBase64, "Evidencia", 3_000_000);
        try { Base64.getDecoder().decode(visit.evidenceBase64); }
        catch (IllegalArgumentException e) { bad("Evidencia Base64 inválida"); }
        visit.id = null;
        visit.supervisorStatus = "Pendiente";
        visit.presenceVerified = visit.distanceToPointMeters <= 150;
        var point = catalog.point(visit.pointId);
        var asset = catalog.asset(visit.pointId, visit.assetId);
        visit.pointName = point.name; visit.assetCode = asset.code; visit.assetType = asset.type;
        visit.supervisorId = point.supervisorId;
        Visit saved = repository.save(visit);
        outbox.enqueue(saved); // Mismo commit que la visita; no se llama a FCM aquí.
        return saved;
    }
    public Visit edit(long id, String equipmentStatus, String observations) {
        validateEdit(equipmentStatus, observations);
        Visit visit = get(id);
        visit.equipmentStatus = equipmentStatus;
        visit.observations = observations.trim();
        return repository.save(visit);
    }
    public Visit status(long id, String status) {
        if (!STATES.contains(safe(status))) bad("Estado de supervisión inválido");
        Visit visit = get(id);
        visit.supervisorStatus = status;
        return repository.save(visit);
    }
    public void delete(long id) { Visit visit=get(id); outbox.cancel(id); repository.delete(visit); }

    static void validateEdit(String equipmentStatus, String observations) {
        if (!EQUIPMENT.contains(safe(equipmentStatus))) bad("Estado del equipo inválido");
        if (observations == null || observations.length() > 2000) bad("Observaciones: máximo 2000 caracteres");
        if (!"Operativo".equals(equipmentStatus) && observations.trim().length() < 5) bad("Describe la falla con al menos 5 caracteres");
    }
    private static String safe(String value) { return value == null ? "" : value; }
    private static void requireText(String value, String name, int max) {
        if (value == null || value.isBlank() || value.length() > max) bad(name + " requerido o demasiado extenso");
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
