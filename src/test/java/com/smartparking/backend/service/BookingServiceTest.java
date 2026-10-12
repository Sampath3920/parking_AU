package com.smartparking.backend.service;

import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.model.Booking;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Plain unit tests for booking time rules and the approval workflow. No database needed. */
class BookingServiceTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final ParkingSlotRepository slotRepository = mock(ParkingSlotRepository.class);
    private final ParkingSessionRepository sessionRepository = mock(ParkingSessionRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditService auditService = mock(AuditService.class);

    private BookingService service = newService(true);

    private User user;
    private Vehicle vehicle;
    private ParkingSlot slot;

    private BookingService newService(boolean requirePayment) {
        return new BookingService(
                bookingRepository, userRepository, vehicleRepository,
                slotRepository, sessionRepository, paymentRepository,
                notificationService, auditService, requirePayment);
    }

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test Student");

        vehicle = new Vehicle();
        vehicle.setId(2L);
        vehicle.setUser(user);
        vehicle.setVehicleType("4-WHEELER");
        vehicle.setVerificationStatus("VERIFIED");

        slot = new ParkingSlot();
        slot.setId(3L);
        slot.setStatus("AVAILABLE");
        slot.setVehicleType("4-WHEELER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(vehicle));
        when(slotRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(slot));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Booking pendingBooking() {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setParkingSlot(slot);
        booking.setStatus("PENDING");
        booking.setStartTime(LocalDateTime.now().plusHours(1));
        booking.setEndTime(LocalDateTime.now().plusHours(3));
        when(bookingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(booking));
        return booking;
    }

    private void paymentWithStatus(String status) {
        Payment payment = new Payment();
        payment.setStatus(status);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(payment));
    }

    // ---------- time rules

    @Test
    void endBeforeStartIsRejected() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        assertThrows(IllegalArgumentException.class, () ->
                service.createBooking(1L, 2L, 3L, start, start.minusMinutes(30)));
    }

    @Test
    void startInThePastIsRejected() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        assertThrows(IllegalArgumentException.class, () ->
                service.createBooking(1L, 2L, 3L, start, start.plusHours(1)));
    }

    @Test
    void tooShortBookingIsRejected() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(30);
        assertThrows(IllegalArgumentException.class, () ->
                service.createBooking(1L, 2L, 3L, start, start.plusMinutes(5)));
    }

    @Test
    void tooLongBookingIsRejected() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(30);
        assertThrows(IllegalArgumentException.class, () ->
                service.createBooking(1L, 2L, 3L, start, start.plusHours(30)));
    }

    @Test
    void overlappingBookingIsRefusedWithConflict() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);
        when(bookingRepository.existsOverlap(3L, start, end)).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                service.createBooking(1L, 2L, 3L, start, end));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void foreignVehicleIsRejected() {
        User other = new User();
        other.setId(99L);
        vehicle.setUser(other);

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        assertThrows(IllegalArgumentException.class, () ->
                service.createBooking(1L, 2L, 3L, start, start.plusHours(1)));
    }

    @Test
    void maintenanceSlotCannotBeBooked() {
        slot.setStatus("MAINTENANCE");
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        assertThrows(ConflictException.class, () ->
                service.createBooking(1L, 2L, 3L, start, start.plusHours(1)));
    }

    // ---------- vehicle approval rule

    @Test
    void unapprovedVehicleCannotBook() {
        vehicle.setVerificationStatus("PENDING");
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        assertThrows(IllegalArgumentException.class, () ->
                service.createBooking(1L, 2L, 3L, start, start.plusHours(1)));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // ---------- request is saved as pending and admins are told

    @Test
    void validRequestIsSavedAsPendingWithoutQrAndAdminsAreNotified() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);
        when(bookingRepository.existsOverlap(3L, start, end)).thenReturn(false);

        Booking booking = service.createBooking(1L, 2L, 3L, start, end);

        assertEquals("PENDING", booking.getStatus());
        assertEquals(start, booking.getStartTime());
        assertEquals(end, booking.getEndTime());
        assertNull(booking.getQrToken());
        assertEquals("BOOKED", slot.getStatus()); // slot is reserved while waiting
        verify(notificationService).notifyRole(anyString(), anyString(), anyString());
    }

    // ---------- admin approval

    @Test
    void approvalNeedsVerifiedPayment() {
        Booking booking = pendingBooking();
        paymentWithStatus("PENDING_VERIFICATION");

        assertThrows(ConflictException.class, () -> service.approve(10L));
        assertEquals("PENDING", booking.getStatus());
    }

    @Test
    void approvalWithoutAnyPaymentIsRefused() {
        pendingBooking();
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> service.approve(10L));
    }

    @Test
    void approvalWithVerifiedPaymentCreatesQrTokenAndNotifiesUser() {
        Booking booking = pendingBooking();
        paymentWithStatus("SUCCESS");
        when(bookingRepository.existsApprovedOverlap(anyLong(), anyLong(), any(), any()))
                .thenReturn(false);

        Booking result = service.approve(10L);

        assertEquals("BOOKED", result.getStatus());
        assertNotNull(booking.getQrToken());
        verify(notificationService).notifyUser(any(User.class), anyString(), anyString());
    }

    @Test
    void approvalIsRefusedWhenAnotherApprovedBookingOverlaps() {
        pendingBooking();
        paymentWithStatus("SUCCESS");
        when(bookingRepository.existsApprovedOverlap(anyLong(), anyLong(), any(), any()))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> service.approve(10L));
    }

    @Test
    void approvalCanSkipPaymentWhenNotRequired() {
        service = newService(false);
        Booking booking = pendingBooking();
        when(bookingRepository.existsApprovedOverlap(anyLong(), anyLong(), any(), any()))
                .thenReturn(false);

        assertEquals("BOOKED", service.approve(10L).getStatus());
        assertNotNull(booking.getQrToken());
    }

    @Test
    void onlyPendingRequestsCanBeApproved() {
        Booking booking = pendingBooking();
        booking.setStatus("BOOKED");

        assertThrows(ConflictException.class, () -> service.approve(10L));
    }

    // ---------- admin rejection

    @Test
    void rejectionNeedsAReason() {
        pendingBooking();

        assertThrows(IllegalArgumentException.class, () -> service.reject(10L, "   "));
        assertThrows(IllegalArgumentException.class, () -> service.reject(10L, null));
    }

    @Test
    void rejectionStoresReasonAndReleasesTheSlot() {
        Booking booking = pendingBooking();
        when(slotRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsByParkingSlotIdAndStatusIn(anyLong(), any()))
                .thenReturn(false);
        slot.setStatus("BOOKED");

        Booking result = service.reject(10L, "Documents unclear");

        assertEquals("REJECTED", result.getStatus());
        assertEquals("Documents unclear", booking.getDecisionReason());
        assertEquals("AVAILABLE", slot.getStatus());
        verify(notificationService).notifyUser(any(User.class), anyString(), anyString());
    }

    // ---------- cancel and slot status

    @Test
    void cancellingKeepsMaintenanceSlotClosed() {
        Booking booking = pendingBooking();
        slot.setStatus("MAINTENANCE");
        when(sessionRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        service.cancelBooking(10L);

        assertEquals("CANCELLED", booking.getStatus());
        assertEquals("MAINTENANCE", slot.getStatus());
    }
}
