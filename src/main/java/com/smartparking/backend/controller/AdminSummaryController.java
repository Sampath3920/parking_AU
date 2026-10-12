package com.smartparking.backend.controller;

import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.service.ServiceReportService;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;
import com.smartparking.backend.security.CustomUserDetailsService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * All the numbers for the admin dashboard cards, counted live from the database.
 * ADMIN only (URL starts with /api/admin, see SecurityConfig).
 * The older /api/admin/dashboard endpoint is kept unchanged.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminSummaryController {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceReportService reportService;

    public AdminSummaryController(UserRepository userRepository,
                                  VehicleRepository vehicleRepository,
                                  BookingRepository bookingRepository,
                                  ParkingSlotRepository slotRepository,
                                  ParkingSessionRepository sessionRepository,
                                  PaymentRepository paymentRepository,
                                  ServiceReportService reportService) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.sessionRepository = sessionRepository;
        this.paymentRepository = paymentRepository;
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", userNumbers());
        result.put("vehicles", vehicleNumbers());
        result.put("bookings", bookingNumbers());
        result.put("slots", slotNumbers());
        result.put("payments", paymentNumbers());
        result.put("reports", reportService.summary());
        return result;
    }

    private Map<String, Object> userNumbers() {
        List<User> users = userRepository.findAll();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", users.size());
        map.put("students", count(users, u -> hasRole(u, "STUDENT")));
        map.put("faculty", count(users, u -> hasRole(u, "FACULTY")));
        map.put("staff", count(users, u -> hasRole(u, "STAFF")));
        map.put("visitors", count(users, u -> hasRole(u, "VISITOR")));
        map.put("guards", count(users, u -> hasRole(u, "GUARD")));
        map.put("admins", count(users, u -> hasRole(u, "ADMIN")));
        map.put("inactive", count(users, u -> !u.isActive()));
        return map;
    }

    private Map<String, Object> vehicleNumbers() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", vehicles.size());
        map.put("twoWheeler", count(vehicles, v -> "2-WHEELER".equalsIgnoreCase(v.getVehicleType())));
        map.put("fourWheeler", count(vehicles, v -> "4-WHEELER".equalsIgnoreCase(v.getVehicleType())));
        map.put("pending", count(vehicles, v -> "PENDING".equalsIgnoreCase(v.getVerificationStatus())));
        map.put("approved", count(vehicles, v -> "VERIFIED".equalsIgnoreCase(v.getVerificationStatus())));
        map.put("rejected", count(vehicles, v -> "REJECTED".equalsIgnoreCase(v.getVerificationStatus())));
        return map;
    }

    private Map<String, Object> bookingNumbers() {
        List<Booking> bookings = bookingRepository.findAll();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", bookings.size());
        map.put("pending", count(bookings, b -> "PENDING".equals(b.getStatus())));
        map.put("approved", count(bookings, b -> "BOOKED".equals(b.getStatus())));
        map.put("completed", count(bookings, b -> "COMPLETED".equals(b.getStatus())));
        map.put("cancelled", count(bookings, b -> "CANCELLED".equals(b.getStatus())));
        map.put("rejected", count(bookings, b -> "REJECTED".equals(b.getStatus())));
        map.put("expired", count(bookings, b -> "EXPIRED".equals(b.getStatus())));
        return map;
    }

    /** Same rules as /api/parking-slots/summary: a slot with a car parked now is "occupied". */
    private Map<String, Object> slotNumbers() {
        Set<Long> occupiedIds = new HashSet<>();
        for (ParkingSession session : sessionRepository.findByStatus("ACTIVE")) {
            if (session.getBooking() != null && session.getBooking().getParkingSlot() != null) {
                occupiedIds.add(session.getBooking().getParkingSlot().getId());
            }
        }

        long total = 0;
        long available = 0;
        long reserved = 0;
        long occupied = 0;
        long maintenance = 0;
        long outOfService = 0;

        for (ParkingSlot slot : slotRepository.findAll()) {
            total++;
            if (occupiedIds.contains(slot.getId())) {
                occupied++;
            } else if ("BOOKED".equals(slot.getStatus())) {
                reserved++;
            } else if ("MAINTENANCE".equals(slot.getStatus())) {
                maintenance++;
            } else if ("OUT_OF_SERVICE".equals(slot.getStatus())) {
                outOfService++;
            } else {
                available++;
            }
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", total);
        map.put("available", available);
        map.put("reserved", reserved);
        map.put("occupied", occupied);
        map.put("currentlyParked", occupied);
        map.put("maintenance", maintenance);
        map.put("outOfService", outOfService);
        return map;
    }

    private Map<String, Object> paymentNumbers() {
        List<Payment> payments = paymentRepository.findAll();
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.from(today);

        double revenueToday = 0;
        double revenueMonth = 0;
        double revenueTotal = 0;
        for (Payment payment : payments) {
            if (!"SUCCESS".equals(payment.getStatus())) {
                continue;
            }
            revenueTotal += payment.getAmount();
            if (payment.getPaymentTime() != null) {
                LocalDate day = payment.getPaymentTime().toLocalDate();
                if (day.equals(today)) {
                    revenueToday += payment.getAmount();
                }
                if (YearMonth.from(day).equals(month)) {
                    revenueMonth += payment.getAmount();
                }
            }
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pending", count(payments, p -> "PENDING_VERIFICATION".equals(p.getStatus())));
        map.put("successful", count(payments, p -> "SUCCESS".equals(p.getStatus())));
        map.put("failed", count(payments, p -> "FAILED".equals(p.getStatus())));
        map.put("refunded", count(payments, p -> "REFUNDED".equals(p.getStatus())));
        map.put("revenueToday", revenueToday);
        map.put("revenueThisMonth", revenueMonth);
        map.put("revenueTotal", revenueTotal);
        return map;
    }

    private static boolean hasRole(User user, String role) {
        return role.equals(CustomUserDetailsService.normalizeRole(user.getRole()));
    }

    private static <T> long count(List<T> list, Predicate<T> test) {
        return list.stream().filter(test).count();
    }
}
