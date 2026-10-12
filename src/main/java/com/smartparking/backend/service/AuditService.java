package com.smartparking.backend.service;

import com.smartparking.backend.model.AuditLog;
import com.smartparking.backend.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** Saves who (the logged-in user) did what. Details are cut to 1000 characters. */
    public void log(String action, String targetType, Long targetId, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth != null && auth.getName() != null) ? auth.getName() : "system";

        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetails(details != null && details.length() > 1000
                ? details.substring(0, 1000) : details);
        entry.setCreatedAt(LocalDateTime.now());
        repository.save(entry);
    }
}
