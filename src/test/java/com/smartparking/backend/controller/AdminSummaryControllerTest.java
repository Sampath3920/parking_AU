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
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;
import com.smartparking.backend.service.ServiceReportService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Plain unit tests: no database and no Spring needed. */
class AdminSummaryControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final ParkingSlotRepository slotRepository = mock(ParkingSlotRepository.class);
    private final ParkingSessionRepository sessionRepository = mock(ParkingSessionRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final ServiceReportService reportService = mock(ServiceReportService.class);
    private final AdminSummaryController controller = new AdminSummaryController(
            userRepository, vehicleRepository, bookingRepository,
            slotRepository, sessionRepository, paymentRepository, reportService);

    private User user(String role, boolean active) {
        User user = new User();
        user.setRole(role);
        user.setActive(active);
        return user;
    }

    private Vehicle vehicle(String type, String status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleType(type);
        vehicle.setVerificationStatus(status);
        return vehicle;
    }

    private Booking booking(String status) {
        Booking booking = new Booking();
        booking.setStatus(status);
        return booking;
    }

    private ParkingSlot slot(long id, String status) {
        ParkingSlot slot = new ParkingSlot();
        slot.setId(id);
        slot.setStatus(status);
        return slot;
    }

    @Test
    @SuppressWarnings("unchecked")
    void usersAreCountedByCategory() {
        when(userRepository.findAll()).thenReturn(List.of(
                user("STUDENT", true), user("student", true), user("FACULTY", true),
                user("STAFF", true), user("VISITOR", false), user("GUARD", true), user("ADMIN", true)));

        Map<String, Object> users = (Map<String, Object>) controller.summary().get("users");

        assertEquals(7, users.get("total"));
        assertEquals(2L, users.get("students"));
        assertEquals(1L, users.get("faculty"));
        assertEquals(1L, users.get("staff"));
        assertEquals(1L, users.get("visitors"));
        assertEquals(1L, users.get("guards"));
        assertEquals(1L, users.get("admins"));
        assertEquals(1L, users.get("inactive"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void vehiclesAndBookingsAreCountedByStatus() {
        when(vehicleRepository.findAll()).thenReturn(List.of(
                vehicle("2-WHEELER", "PENDING"), vehicle("4-WHEELER", "VERIFIED"),
                vehicle("4-WHEELER", "REJECTED")));
        when(bookingRepository.findAll()).thenReturn(List.of(
                booking("PENDING"), booking("PENDING"), booking("BOOKED"), booking("COMPLETED")));

        Map<String, Object> summary = controller.summary();
        Map<String, Object> vehicles = (Map<String, Object>) summary.get("vehicles");
        Map<String, Object> bookings = (Map<String, Object>) summary.get("bookings");

        assertEquals(1L, vehicles.get("twoWheeler"));
        assertEquals(2L, vehicles.get("fourWheeler"));
        assertEquals(1L, vehicles.get("pending"));
        assertEquals(1L, vehicles.get("approved"));
        assertEquals(2L, bookings.get("pending"));
        assertEquals(1L, bookings.get("approved"));
        assertEquals(1L, bookings.get("completed"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void occupiedSlotsComeFromActiveParkingSessions() {
        ParkingSlot parked = slot(1, "BOOKED");
        Booking parkedBooking = booking("BOOKED");
        parkedBooking.setParkingSlot(parked);
        ParkingSession session = new ParkingSession();
        session.setBooking(parkedBooking);
        session.setStatus("ACTIVE");

        when(slotRepository.findAll()).thenReturn(List.of(
                parked, slot(2, "BOOKED"), slot(3, "AVAILABLE"),
                slot(4, "MAINTENANCE"), slot(5, "OUT_OF_SERVICE")));
        when(sessionRepository.findByStatus("ACTIVE")).thenReturn(List.of(session));

        Map<String, Object> slots = (Map<String, Object>) controller.summary().get("slots");

        assertEquals(5L, slots.get("total"));
        assertEquals(1L, slots.get("occupied"));
        assertEquals(1L, slots.get("currentlyParked"));
        assertEquals(1L, slots.get("reserved"));
        assertEquals(1L, slots.get("available"));
        assertEquals(1L, slots.get("maintenance"));
        assertEquals(1L, slots.get("outOfService"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void revenueCountsSuccessfulPaymentsOnly() {
        Payment paid = new Payment();
        paid.setStatus("SUCCESS");
        paid.setAmount(50);
        paid.setPaymentTime(LocalDateTime.now());
        Payment pending = new Payment();
        pending.setStatus("PENDING_VERIFICATION");
        pending.setAmount(50);
        pending.setPaymentTime(LocalDateTime.now());
        when(paymentRepository.findAll()).thenReturn(List.of(paid, pending));

        Map<String, Object> payments = (Map<String, Object>) controller.summary().get("payments");

        assertEquals(50.0, (double) payments.get("revenueTotal"), 0.001);
        assertEquals(50.0, (double) payments.get("revenueToday"), 0.001);
        assertEquals(1L, payments.get("pending"));
        assertEquals(1L, payments.get("successful"));
    }
}
