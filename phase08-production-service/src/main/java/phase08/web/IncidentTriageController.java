package phase08.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phase08.model.TrajectoryEvent;
import phase08.model.TriageRequestDto;
import phase08.model.TriageResponseDto;
import phase08.service.IncidentTriageService;

import java.util.List;
import java.util.Objects;

/**
 * Phase 08: Spring Boot REST API Controller.
 * <p>
 * Exposes endpoints for incident triage triggering and trajectory audit retrieval.
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentTriageController {

    private final IncidentTriageService triageService;

    public IncidentTriageController(IncidentTriageService triageService) {
        this.triageService = Objects.requireNonNull(triageService, "triageService required");
    }

    /**
     * Dispatches an incident triage request.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If request is null, or request.incidentId() is null/blank, or request.description() is null/blank:
     *     return ResponseEntity.badRequest().build().
     * - Execution & Exception Handling:
     *   - Try:
     *     - TriageResponseDto response = triageService.triageIncident(request).
     *     - Return ResponseEntity.ok(response).
     *   - Catch (IllegalArgumentException ex):
     *     - Return ResponseEntity.badRequest().build().
     *   - Catch (SecurityException ex):
     *     - Return ResponseEntity.status(403).build().
     */
    @PostMapping("/triage")
    public ResponseEntity<TriageResponseDto> triageIncident(@RequestBody TriageRequestDto request) {
        throw new UnsupportedOperationException("TODO: Implement triageIncident");
    }

    /**
     * Queries the trajectory audit trail for a given incident.
     * <p>
     * Instructions:
     * - Parameter Validations:
     *   - If incidentId is null or blank:
     *     return ResponseEntity.badRequest().build().
     * - Query:
     *   - List<TrajectoryEvent> trajectory = triageService.getTrajectory(incidentId).
     *   - Return ResponseEntity.ok(trajectory).
     */
    @GetMapping("/{id}/trajectory")
    public ResponseEntity<List<TrajectoryEvent>> getTrajectory(@PathVariable("id") String incidentId) {
        throw new UnsupportedOperationException("TODO: Implement getTrajectory");
    }
}
