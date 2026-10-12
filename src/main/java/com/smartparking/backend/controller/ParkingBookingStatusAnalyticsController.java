package com.smartparking.backend.controller;

import com.smartparking.backend.repository.BookingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-booking-status-analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingBookingStatusAnalyticsController {

    private final BookingRepository bookingRepository;

    public ParkingBookingStatusAnalyticsController(
            BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public Map<String, Object> getBookingStatusAnalytics() {

        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalBookings", bookingRepository.count());

        analytics.put("bookedBookings",
                bookingRepository.findByStatus("BOOKED").size());

        analytics.put("completedBookings",
                bookingRepository.findByStatus("COMPLETED").size());

        analytics.put("cancelledBookings",
                bookingRepository.findByStatus("CANCELLED").size());

        analytics.put("pendingBookings",
                bookingRepository.findByStatus("PENDING").size());

        return analytics;
    }
}