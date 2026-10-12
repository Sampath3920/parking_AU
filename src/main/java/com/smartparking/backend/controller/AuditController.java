package com.smartparking.backend.controller;

import com.smartparking.backend.model.AuditLog;
import com.smartparking.backend.repository.AuditLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin only (URL starts with /api/admin, see SecurityConfig). */
@RestController
@RequestMapping("/api/admin/audit")
@CrossOrigin(origins = "http://localhost:5173")
public class AuditController {

    private final AuditLogRepository repository;

    public AuditController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AuditLog> latest(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId) {

        if (targetType != null && targetId != null) {
            return repository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
        }
        return repository.findTop200ByOrderByCreatedAtDesc();
    }
}
