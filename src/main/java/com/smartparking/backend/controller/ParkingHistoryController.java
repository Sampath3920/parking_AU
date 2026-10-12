package com.smartparking.backend.controller;

import com.smartparking.backend.dto.ParkingHistoryResponse;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/parking-history")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingHistoryController {

    private final BookingRepository bookingRepository;
    private final ParkingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;

    public ParkingHistoryController(
            BookingRepository bookingRepository,
            ParkingSessionRepository sessionRepository,
            PaymentRepository paymentRepository) {

        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/user/{userId}")
    public List<ParkingHistoryResponse> getUserHistory(
            @PathVariable Long userId) {

        List<Booking> bookings =
                bookingRepository.findByUserId(userId);

        List<ParkingHistoryResponse> history =
                new ArrayList<>();

        for (Booking booking : bookings) {

            ParkingSession session =
                    sessionRepository
                            .findByBookingId(booking.getId())
                            .orElse(null);

            ParkingHistoryResponse item =
                    new ParkingHistoryResponse(
                            booking.getId(),
                            booking.getVehicle().getVehicleNumber(),
                            booking.getParkingSlot().getSlotNumber(),
                            booking.getBookingTime(),
                            session != null
                                    ? session.getCheckInTime()
                                    : null,
                            session != null
                                    ? session.getCheckOutTime()
                                    : null,
                            booking.getStatus()
                    );

            item.setStartTime(booking.getStartTime());
            item.setEndTime(booking.getEndTime());
            item.setDecisionReason(booking.getDecisionReason());
            if (booking.getParkingSlot().getParkingLocation() != null) {
                item.setLocationName(booking.getParkingSlot().getParkingLocation().getName());
            }
            Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
            if (payment != null) {
                item.setPaymentAmount(payment.getAmount());
                item.setPaymentStatus(payment.getStatus());
            }
            history.add(item);
        }

        return history;
    }
}