package com.smartparking.backend.controller;

import com.smartparking.backend.dto.VehicleDecisionRequest;
import com.smartparking.backend.model.AuditLog;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.AuditLogRepository;
import com.smartparking.backend.repository.VehicleRepository;
import com.smartparking.backend.service.AuditService;
import com.smartparking.backend.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Admin only: /api/admin/** is limited to ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/vehicles")
@CrossOrigin(origins = "http://localhost:5173")
public class VehicleReviewController {

    private final VehicleRepository vehicleRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public VehicleReviewController(VehicleRepository vehicleRepository,
                                   AuditLogRepository auditLogRepository,
                                   AuditService auditService,
                                   NotificationService notificationService) {
        this.vehicleRepository = vehicleRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    /** Optional filters: ?status=PENDING&search=ts09 */
    @GetMapping
    public List<Vehicle> list(@RequestParam(required = false) String status,
                              @RequestParam(required = false) String search) {

        String text = search == null ? "" : search.trim().toLowerCase();

        return vehicleRepository.findAll().stream()
                .filter(v -> status == null || status.isBlank()
                        || status.equalsIgnoreCase(v.getVerificationStatus()))
                .filter(v -> text.isEmpty()
                        || contains(v.getVehicleNumber(), text)
                        || contains(v.getVehicleModel(), text)
                        || (v.getUser() != null && (contains(v.getUser().getName(), text)
                                || contains(v.getUser().getEmail(), text))))
                .sorted(Comparator.comparing(Vehicle::getId).reversed())
                .toList();
    }

    @PutMapping("/{id}/decision")
    @Transactional
    public Vehicle decide(@PathVariable Long id,
                          @Valid @RequestBody VehicleDecisionRequest request) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        String decision = request.status().trim().toUpperCase();
        if (!decision.equals("VERIFIED") && !decision.equals("REJECTED")) {
            throw new IllegalArgumentException("Decision must be VERIFIED or REJECTED");
        }

        String reason = request.reason() == null ? "" : request.reason().trim();
        if (decision.equals("REJECTED") && reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required when rejecting a vehicle");
        }
        if (reason.length() > 500) {
            throw new IllegalArgumentException("Reason must be 500 characters or fewer");
        }

        vehicle.setVerificationStatus(decision);
        vehicle.setReviewReason(reason.isBlank() ? null : reason);
        vehicle.setReviewedAt(LocalDateTime.now());
        Vehicle saved = vehicleRepository.save(vehicle);

        auditService.log("VEHICLE_" + decision, "VEHICLE", id,
                vehicle.getVehicleNumber() + (reason.isBlank() ? "" : " - " + reason));

        notificationService.notifyUser(vehicle.getUser(), "VEHICLE_DECISION",
                "Vehicle " + vehicle.getVehicleNumber()
                        + (decision.equals("VERIFIED")
                        ? " was approved. You can now book parking."
                        : " was rejected: " + reason));

        return saved;
    }

    @GetMapping("/{id}/history")
    public List<AuditLog> history(@PathVariable Long id) {
        return auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("VEHICLE", id);
    }

    private boolean contains(String value, String text) {
        return value != null && value.toLowerCase().contains(text);
    }
}
