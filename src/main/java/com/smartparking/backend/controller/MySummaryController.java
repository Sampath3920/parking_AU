package com.smartparking.backend.controller;

import com.smartparking.backend.exception.NotFoundException;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.model.ServiceReport;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.NotificationRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.repository.ServiceReportRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Numbers for the dashboard of the logged-in person. Only that person's own data is counted,
 * because the user is taken from the login (Authentication), never from the URL.
 */
@RestController
@RequestMapping("/api/me")
@CrossOrigin(origins = "http://localhost:5173")
public class MySummaryController {

    public static final int VEHICLE_LIMIT = 3;

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final NotificationRepository notificationRepository;
    private final ServiceReportRepository reportRepository;

    public MySummaryController(UserRepository userRepository,
                               VehicleRepository vehicleRepository,
                               BookingRepository bookingRepository,
                               PaymentRepository paymentRepository,
                               ParkingSlotRepository slotRepository,
                               ParkingSessionRepository sessionRepository,
                               NotificationRepository notificationRepository,
                               ServiceReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.slotRepository = slotRepository;
        this.sessionRepository = sessionRepository;
        this.notificationRepository = notificationRepository;
        this.reportRepository = reportRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(Authentication authentication) {

        User me = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Long id = me.getId();

        List<Vehicle> vehicles = vehicleRepository.findByUserId(id);
        List<Booking> bookings = bookingRepository.findByUserId(id);
        List<Payment> payments = paymentRepository.findByBookingUserIdOrderByPaymentTimeDesc(id);
        List<ServiceReport> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(id);

        Map<String, Object> vehicleNumbers = new LinkedHashMap<>();
        vehicleNumbers.put("total", vehicles.size());
        vehicleNumbers.put("limit", VEHICLE_LIMIT);
        vehicleNumbers.put("approved", count(vehicles, v -> "VERIFIED".equalsIgnoreCase(v.getVerificationStatus())));
        vehicleNumbers.put("pending", count(vehicles, v -> "PENDING".equalsIgnoreCase(v.getVerificationStatus())));
        vehicleNumbers.put("rejected", count(vehicles, v -> "REJECTED".equalsIgnoreCase(v.getVerificationStatus())));

        Map<String, Object> bookingNumbers = new LinkedHashMap<>();
        bookingNumbers.put("total", bookings.size());
        bookingNumbers.put("pending", count(bookings, b -> "PENDING".equals(b.getStatus())));
        bookingNumbers.put("approved", count(bookings, b -> "BOOKED".equals(b.getStatus())));
        bookingNumbers.put("completed", count(bookings, b -> "COMPLETED".equals(b.getStatus())));
        bookingNumbers.put("rejected", count(bookings, b -> "REJECTED".equals(b.getStatus())));
        bookingNumbers.put("cancelled", count(bookings, b -> "CANCELLED".equals(b.getStatus())));

        double totalPaid = payments.stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();
        Map<String, Object> paymentNumbers = new LinkedHashMap<>();
        paymentNumbers.put("pending", count(payments, p -> "PENDING_VERIFICATION".equals(p.getStatus())));
        paymentNumbers.put("successful", count(payments, p -> "SUCCESS".equals(p.getStatus())));
        paymentNumbers.put("failed", count(payments, p -> "FAILED".equals(p.getStatus())));
        paymentNumbers.put("refunded", count(payments, p -> "REFUNDED".equals(p.getStatus())));
        paymentNumbers.put("totalPaid", totalPaid);

        Map<String, Object> reportNumbers = new LinkedHashMap<>();
        reportNumbers.put("total", reports.size());
        reportNumbers.put("unresolved", count(reports,
                r -> "SUBMITTED".equals(r.getStatus()) || "IN_PROGRESS".equals(r.getStatus())));

        long unread = notificationRepository.findByUserId(id).stream().filter(n -> !n.isRead()).count();
        long availableSlots = slotRepository.findAll().stream()
                .filter(s -> "AVAILABLE".equals(s.getStatus()))
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vehicles", vehicleNumbers);
        result.put("bookings", bookingNumbers);
        result.put("activeBooking", activeBooking(bookings));
        result.put("payments", paymentNumbers);
        result.put("reports", reportNumbers);
        result.put("unreadNotifications", unread);
        result.put("availableSlots", availableSlots);
        return result;
    }

    /**
     * The approved booking that matters now: the one whose time has not ended yet and that starts first.
     * Returns null when there is none.
     */
    private Map<String, Object> activeBooking(List<Booking> bookings) {
        LocalDateTime now = LocalDateTime.now();
        Optional<Booking> next = bookings.stream()
                .filter(b -> "BOOKED".equals(b.getStatus()))
                .filter(b -> b.getEndTime() == null || b.getEndTime().isAfter(now))
                .min(Comparator.comparing(Booking::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        if (next.isEmpty()) {
            return null;
        }

        Booking booking = next.get();
        boolean checkedIn = sessionRepository.findByBookingId(booking.getId())
                .map(ParkingSession::getStatus)
                .map("ACTIVE"::equals)
                .orElse(false);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", booking.getId());
        map.put("slotNumber", booking.getParkingSlot() != null ? booking.getParkingSlot().getSlotNumber() : null);
        map.put("locationName", booking.getParkingSlot() != null && booking.getParkingSlot().getParkingLocation() != null
                ? booking.getParkingSlot().getParkingLocation().getName() : null);
        map.put("vehicleNumber", booking.getVehicle() != null ? booking.getVehicle().getVehicleNumber() : null);
        map.put("startTime", booking.getStartTime());
        map.put("endTime", booking.getEndTime());
        map.put("checkedIn", checkedIn);
        return map;
    }

    private static <T> long count(List<T> list, Predicate<T> test) {
        return list.stream().filter(test).count();
    }
}
