package com.smartparking.backend.controller;

import com.smartparking.backend.model.ParkingAlert;
import com.smartparking.backend.model.ParkingViolation;
import com.smartparking.backend.model.VehicleDuplicate;

import com.smartparking.backend.repository.ParkingAlertRepository;
import com.smartparking.backend.repository.ParkingViolationRepository;
import com.smartparking.backend.repository.VehicleDuplicateRepository;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/security-activity")
@CrossOrigin(origins = "http://localhost:5173")
public class SecurityActivityController {

    private final ParkingAlertRepository alertRepository;
    private final ParkingViolationRepository violationRepository;
    private final VehicleDuplicateRepository duplicateRepository;

    public SecurityActivityController(
            ParkingAlertRepository alertRepository,
            ParkingViolationRepository violationRepository,
            VehicleDuplicateRepository duplicateRepository) {

        this.alertRepository = alertRepository;
        this.violationRepository = violationRepository;
        this.duplicateRepository = duplicateRepository;
    }

    @GetMapping
    public Map<String, Object> getSecurityActivity() {

        Map<String, Object> activity = new HashMap<>();

        List<ParkingAlert> openAlerts =
                alertRepository.findByStatus("OPEN");

        List<ParkingViolation> openViolations =
                violationRepository.findByStatus("OPEN");

        List<VehicleDuplicate> openDuplicates =
                duplicateRepository.findByStatus("OPEN");

        activity.put("openAlerts", openAlerts);
        activity.put("openViolations", openViolations);
        activity.put("openDuplicates", openDuplicates);

        return activity;
    }

    @GetMapping("/critical-alerts")
    public Map<String, Object> getCriticalAlerts() {

        Map<String, Object> alerts = new HashMap<>();

        List<ParkingAlert> highAlerts =
                alertRepository.findBySeverity("HIGH");

        List<ParkingAlert> criticalAlerts =
                alertRepository.findBySeverity("CRITICAL");

        alerts.put("highSeverityAlerts", highAlerts);
        alerts.put("criticalAlerts", criticalAlerts);

        return alerts;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSecurityActivitySummary() {

        Map<String, Object> summary = new HashMap<>();

        summary.put("openAlerts",
                alertRepository.findByStatus("OPEN").size());

        summary.put("openViolations",
                violationRepository.findByStatus("OPEN").size());

        summary.put("openDuplicates",
                duplicateRepository.findByStatus("OPEN").size());

        summary.put("highSeverityAlerts",
                alertRepository.findBySeverity("HIGH").size());

        summary.put("criticalAlerts",
                alertRepository.findBySeverity("CRITICAL").size());

        return summary;
    }
}