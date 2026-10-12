package com.smartparking.backend.controller;

import com.smartparking.backend.dto.AdminDashboardResponse;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingLocationRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingLocationRepository locationRepository;
    private final ParkingSlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final ParkingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboardController(
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            ParkingLocationRepository locationRepository,
            ParkingSlotRepository slotRepository,
            BookingRepository bookingRepository,
            ParkingSessionRepository sessionRepository,
            PaymentRepository paymentRepository) {

        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.locationRepository = locationRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {

        long totalUsers = userRepository.count();
        long totalVehicles = vehicleRepository.count();
        long totalLocations = locationRepository.count();
        long totalSlots = slotRepository.count();

        long availableSlots = slotRepository.findAll()
                .stream()
                .filter(slot -> "AVAILABLE".equals(slot.getStatus()))
                .count();

        long bookedSlots = slotRepository.findAll()
                .stream()
                .filter(slot -> "BOOKED".equals(slot.getStatus()))
                .count();

        long totalBookings = bookingRepository.count();

        long activeSessions = sessionRepository.findAll()
                .stream()
                .filter(session -> "ACTIVE".equals(session.getStatus()))
                .count();

        double totalRevenue = paymentRepository.findAll()
                .stream()
                .filter(payment -> "SUCCESS".equals(payment.getStatus()))
                .mapToDouble(payment -> payment.getAmount())
                .sum();

        return new AdminDashboardResponse(
                totalUsers,
                totalVehicles,
                totalLocations,
                totalSlots,
                availableSlots,
                bookedSlots,
                totalBookings,
                activeSessions,
                totalRevenue
        );
    }
}