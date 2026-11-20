package cl.duoc.tacticaltrack.backend;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visits")
public class VisitController {
    private final VisitService service;
    public VisitController(VisitService service) { this.service = service; }
    public record EditRequest(String equipmentStatus, String observations) {}
    public record StatusRequest(String status) {}
    @GetMapping public List<Visit> all() { return service.all(); }
    @GetMapping("/{id}") public Visit get(@PathVariable long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Visit create(@RequestBody Visit visit) { return service.create(visit); }
    @PutMapping("/{id}")
    public Visit edit(@PathVariable long id, @RequestBody EditRequest request) {
        return service.edit(id, request.equipmentStatus(), request.observations());
    }
    @PatchMapping("/{id}/status")
    public Visit status(@PathVariable long id, @RequestBody StatusRequest request) { return service.status(id, request.status()); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
