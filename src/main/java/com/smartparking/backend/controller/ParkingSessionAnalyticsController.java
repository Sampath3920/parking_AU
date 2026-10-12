package com.smartparking.backend.controller;

import com.smartparking.backend.repository.ParkingSessionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-session-analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingSessionAnalyticsController {

    private final ParkingSessionRepository sessionRepository;

    public ParkingSessionAnalyticsController(
            ParkingSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @GetMapping
    public Map<String, Object> getSessionAnalytics() {

        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalSessions",
                sessionRepository.count());

        analytics.put("activeSessions",
                sessionRepository.findByStatus("ACTIVE").size());

        analytics.put("completedSessions",
                sessionRepository.findByStatus("COMPLETED").size());

        analytics.put("cancelledSessions",
                sessionRepository.findByStatus("CANCELLED").size());

        return analytics;
    }
}