package com.smartparking.backend.controller;

import com.smartparking.backend.model.Booking;
import com.smartparking.backend.repository.BookingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-booking-vehicle-analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingBookingVehicleAnalyticsController {

    private final BookingRepository bookingRepository;

    public ParkingBookingVehicleAnalyticsController(
            BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public Map<String, Object> getBookingVehicleAnalytics() {

        Map<String, Object> analytics = new HashMap<>();

        List<Booking> bookings = bookingRepository.findAll();

        long twoWheelerBookings = bookings.stream()
                .filter(b -> b.getVehicle() != null)
                .filter(b -> "2-WHEELER"
                        .equals(b.getVehicle().getVehicleType()))
                .count();

        long fourWheelerBookings = bookings.stream()
                .filter(b -> b.getVehicle() != null)
                .filter(b -> "4-WHEELER"
                        .equals(b.getVehicle().getVehicleType()))
                .count();

        long completedTwoWheelerBookings = bookings.stream()
                .filter(b -> b.getVehicle() != null)
                .filter(b -> "2-WHEELER"
                        .equals(b.getVehicle().getVehicleType()))
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .count();

        long completedFourWheelerBookings = bookings.stream()
                .filter(b -> b.getVehicle() != null)
                .filter(b -> "4-WHEELER"
                        .equals(b.getVehicle().getVehicleType()))
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .count();

        analytics.put("totalBookings", bookings.size());
        analytics.put("twoWheelerBookings", twoWheelerBookings);
        analytics.put("fourWheelerBookings", fourWheelerBookings);
        analytics.put("completedTwoWheelerBookings",
                completedTwoWheelerBookings);
        analytics.put("completedFourWheelerBookings",
                completedFourWheelerBookings);

        return analytics;
    }
}