package com.smartparking.backend.controller;

import com.smartparking.backend.repository.ParkingAlertRepository;
import com.smartparking.backend.repository.ParkingViolationRepository;
import com.smartparking.backend.repository.VehicleDuplicateRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.BookingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/security-dashboard")
@CrossOrigin(origins = "http://localhost:5173")
public class SecurityDashboardController {

    private final ParkingAlertRepository alertRepository;
    private final ParkingViolationRepository violationRepository;
    private final VehicleDuplicateRepository duplicateRepository;
    private final ParkingSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;

    public SecurityDashboardController(
            ParkingAlertRepository alertRepository,
            ParkingViolationRepository violationRepository,
            VehicleDuplicateRepository duplicateRepository,
            ParkingSessionRepository sessionRepository,
            BookingRepository bookingRepository) {

        this.alertRepository = alertRepository;
        this.violationRepository = violationRepository;
        this.duplicateRepository = duplicateRepository;
        this.sessionRepository = sessionRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public Map<String, Object> getSecurityDashboard() {

        Map<String, Object> dashboard = new HashMap<>();

        dashboard.put("totalAlerts", alertRepository.count());

        dashboard.put("openAlerts",
                alertRepository.findByStatus("OPEN").size());

        dashboard.put("highSeverityAlerts",
                alertRepository.findBySeverity("HIGH").size());

        dashboard.put("criticalAlerts",
                alertRepository.findBySeverity("CRITICAL").size());

        dashboard.put("totalViolations",
                violationRepository.count());

        dashboard.put("openViolations",
                violationRepository.findByStatus("OPEN").size());

        dashboard.put("totalDuplicates",
                duplicateRepository.count());

        dashboard.put("openDuplicates",
                duplicateRepository.findByStatus("OPEN").size());

        dashboard.put("totalSessions",
                sessionRepository.count());

        dashboard.put("activeSessions",
                sessionRepository.findByStatus("ACTIVE").size());

        dashboard.put("totalBookings",
                bookingRepository.count());

        dashboard.put("bookedSlots",
                bookingRepository.findByStatus("BOOKED").size());

        return dashboard;
    }
}