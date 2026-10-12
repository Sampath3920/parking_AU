package com.smartparking.backend.controller;

import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;

import com.smartparking.backend.service.BookingService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-sessions")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingSessionController {

    private final ParkingSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final ParkingSlotRepository slotRepository;

    public ParkingSessionController(
            ParkingSessionRepository sessionRepository,
            BookingRepository bookingRepository,
            ParkingSlotRepository slotRepository) {

        this.sessionRepository = sessionRepository;
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
    }

    @Transactional
    @PostMapping("/check-in/{bookingId}")
    public Map<String, Object> checkIn(
            @PathVariable Long bookingId) {

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found"));

        if (!"BOOKED".equals(booking.getStatus())) {
            throw new RuntimeException(
                    "Booking is not active");
        }

        if (sessionRepository
                .findByBookingId(bookingId)
                .isPresent()) {

            throw new RuntimeException(
                    "Already checked in");
        }

        // Check-in is allowed from 30 minutes before the start until the end time.
        if (booking.getStartTime() != null && booking.getEndTime() != null) {
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(booking.getStartTime().minusMinutes(30))) {
                throw new IllegalArgumentException(
                        "Too early. Check-in opens at "
                                + booking.getStartTime().minusMinutes(30)
                                        .toLocalTime().withSecond(0).withNano(0));
            }
            if (now.isAfter(booking.getEndTime())) {
                throw new IllegalArgumentException(
                        "The booking time is over");
            }
        }

        ParkingSession session = new ParkingSession();

        session.setBooking(booking);
        session.setCheckInTime(
                LocalDateTime.now());
        session.setStatus("ACTIVE");

        ParkingSession savedSession =
                sessionRepository.save(session);

        Map<String, Object> response =
                new HashMap<>();

        response.put("sessionId",
                savedSession.getId());

        response.put("bookingId",
                booking.getId());

        response.put("vehicleNumber",
                booking.getVehicle().getVehicleNumber());

        response.put("slotNumber",
                booking.getParkingSlot().getSlotNumber());

        response.put("checkInTime",
                savedSession.getCheckInTime());

        response.put("status",
                savedSession.getStatus());

        response.put("message",
                "Check-in successful");

        return response;
    }

    @Transactional
    @PostMapping("/check-out/{bookingId}")
    public Map<String, Object> checkOut(
            @PathVariable Long bookingId) {

        // Lock the booking row so two check-outs cannot run at the same time.
        bookingRepository.findByIdForUpdate(bookingId);

        ParkingSession session =
                sessionRepository
                        .findByBookingId(bookingId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Parking session not found"));

        if (!"ACTIVE".equals(session.getStatus())) {
            throw new RuntimeException(
                    "Session is not active");
        }

        session.setCheckOutTime(
                LocalDateTime.now());

        session.setStatus("COMPLETED");

        Booking booking = session.getBooking();

        booking.setStatus("COMPLETED");

        bookingRepository.save(booking);

        ParkingSlot slot =
                booking.getParkingSlot();

        // Keep the slot BOOKED if someone has a later booking for it.
        // (A slot the admin put in MAINTENANCE / OUT_OF_SERVICE stays that way.)
        if ("AVAILABLE".equals(slot.getStatus()) || "BOOKED".equals(slot.getStatus())) {
            slot.setStatus(bookingRepository.existsByParkingSlotIdAndStatusIn(
                    slot.getId(), BookingService.ACTIVE_STATUSES) ? "BOOKED" : "AVAILABLE");
        }

        slotRepository.save(slot);

        ParkingSession savedSession =
                sessionRepository.save(session);

        Map<String, Object> response =
                new HashMap<>();

        response.put("sessionId",
                savedSession.getId());

        response.put("bookingId",
                booking.getId());

        response.put("vehicleNumber",
                booking.getVehicle().getVehicleNumber());

        response.put("slotNumber",
                slot.getSlotNumber());

        response.put("checkInTime",
                savedSession.getCheckInTime());

        response.put("checkOutTime",
                savedSession.getCheckOutTime());

        response.put("status",
                savedSession.getStatus());

        response.put("message",
                "Check-out successful");

        return response;
    }

    @GetMapping
    public List<ParkingSession> getAllSessions() {
        return sessionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ParkingSession getSession(
            @PathVariable Long id) {

        return sessionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Parking session not found"));
    }

    @GetMapping("/booking/{bookingId}")
    public ParkingSession getSessionByBooking(
            @PathVariable Long bookingId) {

        return sessionRepository
                .findByBookingId(bookingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Parking session not found"));
    }
}