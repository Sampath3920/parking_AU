package com.smartparking.backend.controller;

import com.smartparking.backend.dto.BookingDecisionRequest;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.service.BookingService;
import com.smartparking.backend.model.ParkingSlot;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;

/**
 * Who may call what is decided in SecurityConfig (roles) and
 * OwnershipInterceptor (a user can only touch their own bookings).
 */
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public BookingController(BookingRepository bookingRepository,
                             BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    @PostMapping("/create")
    public Booking createBooking(
            @RequestParam Long userId,
            @RequestParam Long vehicleId,
            @RequestParam Long slotId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return bookingService.createBooking(userId, vehicleId, slotId, startTime, endTime);
    }

    /** Slots with no booking in the chosen time window. */
    @GetMapping("/available-slots")
    public List<ParkingSlot> availableSlots(
            @RequestParam String vehicleType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return bookingService.findAvailableSlots(vehicleType, startTime, endTime);
    }

    /** Guards and admins. Optional filter: /api/bookings?status=PENDING */
    @GetMapping
    public List<Booking> getAllBookings(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return bookingRepository.findAll();
        }
        return bookingRepository.findByStatus(status.trim().toUpperCase());
    }

    // Admin only (see SecurityConfig)
    @PutMapping("/{id}/approve")
    public Booking approveBooking(@PathVariable Long id) {
        return bookingService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public Booking rejectBooking(@PathVariable Long id,
                                 @RequestBody BookingDecisionRequest request) {
        return bookingService.reject(id, request == null ? null : request.reason());
    }

    @GetMapping("/user/{userId}")
    public List<Booking> getUserBookings(@PathVariable Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public Booking getBooking(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
    }

    @PutMapping("/{id}/cancel")
    public Map<String, String> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return Map.of("message", "Booking cancelled successfully");
    }

    // Old URL kept so existing code keeps working. It now cancels instead of deleting.
    @DeleteMapping("/{id}")
    public Map<String, String> cancelBookingOldUrl(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return Map.of("message", "Booking cancelled successfully");
    }
}
