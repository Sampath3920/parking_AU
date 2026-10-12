package com.smartparking.backend.controller;

import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingAnalyticsController {

    private final BookingRepository bookingRepository;
    private final ParkingSessionRepository sessionRepository;

    public ParkingAnalyticsController(
            BookingRepository bookingRepository,
            ParkingSessionRepository sessionRepository) {

        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping
    public Map<String, Object> getAnalytics() {

        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalBookings", bookingRepository.count());
        analytics.put("bookedBookings",
                bookingRepository.findByStatus("BOOKED").size());
        analytics.put("completedBookings",
                bookingRepository.findByStatus("COMPLETED").size());
        analytics.put("activeSessions",
                sessionRepository.findByStatus("ACTIVE").size());
        analytics.put("totalSessions",
                sessionRepository.count());

        return analytics;
    }
}