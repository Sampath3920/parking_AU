package com.smartparking.backend.controller;

import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.Notification;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.ParkingSlot;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Plain unit tests: no database and no Spring needed. */
class MySummaryControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final ParkingSlotRepository slotRepository = mock(ParkingSlotRepository.class);
    private final ParkingSessionRepository sessionRepository = mock(ParkingSessionRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final ServiceReportRepository reportRepository = mock(ServiceReportRepository.class);

    private final MySummaryController controller = new MySummaryController(
            userRepository, vehicleRepository, bookingRepository, paymentRepository,
            slotRepository, sessionRepository, notificationRepository, reportRepository);

    private final Authentication login = new UsernamePasswordAuthenticationToken("me@example.com", null, List.of());

    @BeforeEach
    void setUp() {
        User me = new User();
        me.setId(4L);
        me.setEmail("me@example.com");
        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(me));
    }

    private Booking booking(long id, String status, LocalDateTime start, LocalDateTime end) {
        ParkingSlot slot = new ParkingSlot();
        slot.setSlotNumber("B" + id);
        Booking booking = new Booking();
        booking.setId(id);
        booking.setStatus(status);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setParkingSlot(slot);
        return booking;
    }

    @Test
    @SuppressWarnings("unchecked")
    void numbersCoverOnlyMyOwnRecords() {
        Vehicle approved = new Vehicle();
        approved.setVerificationStatus("VERIFIED");
        Vehicle pending = new Vehicle();
        pending.setVerificationStatus("PENDING");
        when(vehicleRepository.findByUserId(4L)).thenReturn(List.of(approved, pending));

        Payment paid = new Payment();
        paid.setStatus("SUCCESS");
        paid.setAmount(40);
        Payment waiting = new Payment();
        waiting.setStatus("PENDING_VERIFICATION");
        waiting.setAmount(40);
        when(paymentRepository.findByBookingUserIdOrderByPaymentTimeDesc(4L)).thenReturn(List.of(paid, waiting));

        Notification unread = new Notification();
        unread.setRead(false);
        Notification read = new Notification();
        read.setRead(true);
        when(notificationRepository.findByUserId(4L)).thenReturn(List.of(unread, read));

        ServiceReport open = new ServiceReport();
        open.setStatus("IN_PROGRESS");
        ServiceReport closed = new ServiceReport();
        closed.setStatus("RESOLVED");
        when(reportRepository.findByReporterIdOrderByCreatedAtDesc(4L)).thenReturn(List.of(open, closed));

        ParkingSlot free = new ParkingSlot();
        free.setStatus("AVAILABLE");
        ParkingSlot taken = new ParkingSlot();
        taken.setStatus("BOOKED");
        when(slotRepository.findAll()).thenReturn(List.of(free, taken));

        Map<String, Object> summary = controller.summary(login);

        Map<String, Object> vehicles = (Map<String, Object>) summary.get("vehicles");
        assertEquals(2, vehicles.get("total"));
        assertEquals(3, vehicles.get("limit"));
        assertEquals(1L, vehicles.get("approved"));
        assertEquals(1L, vehicles.get("pending"));

        Map<String, Object> payments = (Map<String, Object>) summary.get("payments");
        assertEquals(40.0, (double) payments.get("totalPaid"), 0.001);
        assertEquals(1L, payments.get("pending"));

        Map<String, Object> reports = (Map<String, Object>) summary.get("reports");
        assertEquals(1L, reports.get("unresolved"));

        assertEquals(1L, summary.get("unreadNotifications"));
        assertEquals(1L, summary.get("availableSlots"));
        assertNull(summary.get("activeBooking"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void activeBookingIsTheNextApprovedBookingThatHasNotEnded() {
        LocalDateTime now = LocalDateTime.now();
        Booking ended = booking(1, "BOOKED", now.minusHours(5), now.minusHours(3));
        Booking later = booking(2, "BOOKED", now.plusHours(5), now.plusHours(7));
        Booking sooner = booking(3, "BOOKED", now.plusHours(1), now.plusHours(2));
        Booking waiting = booking(4, "PENDING", now.plusMinutes(10), now.plusHours(1));
        when(bookingRepository.findByUserId(4L)).thenReturn(List.of(ended, later, sooner, waiting));

        Map<String, Object> summary = controller.summary(login);

        Map<String, Object> active = (Map<String, Object>) summary.get("activeBooking");
        assertNotNull(active);
        assertEquals(3L, active.get("id"));
        assertEquals("B3", active.get("slotNumber"));
        assertEquals(false, active.get("checkedIn"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void activeBookingShowsWhenTheVehicleIsCheckedIn() {
        LocalDateTime now = LocalDateTime.now();
        Booking parked = booking(7, "BOOKED", now.minusMinutes(30), now.plusHours(1));
        when(bookingRepository.findByUserId(4L)).thenReturn(List.of(parked));
        ParkingSession session = new ParkingSession();
        session.setStatus("ACTIVE");
        when(sessionRepository.findByBookingId(7L)).thenReturn(Optional.of(session));

        Map<String, Object> active = (Map<String, Object>) controller.summary(login).get("activeBooking");

        assertEquals(true, active.get("checkedIn"));
    }
}
