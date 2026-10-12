package com.smartparking.backend.controller;

import com.smartparking.backend.dto.PageResult;
import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.service.AuditService;
import com.smartparking.backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Plain unit tests: no database and no Spring needed. */
class AdminPaymentControllerTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final AdminPaymentController controller =
            new AdminPaymentController(paymentRepository, notificationService, auditService);

    private Payment payment(long id, String status, double amount, LocalDateTime time,
                            String bookingStatus, String userName) {
        User user = new User();
        user.setId(id);
        user.setName(userName);
        user.setEmail(userName.toLowerCase().replace(' ', '.') + "@example.com");

        Booking booking = new Booking();
        booking.setId(100 + id);
        booking.setStatus(bookingStatus);
        booking.setUser(user);

        Payment payment = new Payment();
        payment.setId(id);
        payment.setStatus(status);
        payment.setAmount(amount);
        payment.setPaymentMethod("UPI");
        payment.setPaymentTime(time);
        payment.setTransactionReference("REF" + id);
        payment.setBooking(booking);
        return payment;
    }

    @Test
    void listFiltersByStatusAndSearch() {
        LocalDateTime now = LocalDateTime.now();
        when(paymentRepository.findAll()).thenReturn(List.of(
                payment(1, "SUCCESS", 50, now, "BOOKED", "Anita Rao"),
                payment(2, "FAILED", 50, now, "REJECTED", "Anita Rao"),
                payment(3, "SUCCESS", 50, now, "BOOKED", "Bala Menon")));

        PageResult<Payment> result = controller.list("SUCCESS", null, "anita", null, null, 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals(1L, result.content().get(0).getId());
    }

    @Test
    void summaryCountsRevenueOfSuccessfulPaymentsOnly() {
        LocalDateTime now = LocalDateTime.now();
        when(paymentRepository.findAll()).thenReturn(List.of(
                payment(1, "SUCCESS", 50, now, "BOOKED", "A One"),
                payment(2, "SUCCESS", 30, now.minusDays(2), "BOOKED", "B Two"),
                payment(3, "FAILED", 99, now, "REJECTED", "C Three"),
                payment(4, "PENDING_VERIFICATION", 50, now, "PENDING", "D Four"),
                payment(5, "REFUNDED", 40, now, "CANCELLED", "E Five")));

        Map<String, Object> summary = controller.summary();

        assertEquals(2L, summary.get("successful"));
        assertEquals(1L, summary.get("failed"));
        assertEquals(1L, summary.get("pending"));
        assertEquals(1L, summary.get("refunded"));
        assertEquals(80.0, (double) summary.get("revenueTotal"), 0.001);
        assertEquals(50.0, (double) summary.get("revenueToday"), 0.001);
    }

    @Test
    void refundNeedsAReason() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.refund(1L, Map.of("reason", "  ")));
    }

    @Test
    void refundIsRefusedWhileTheBookingIsStillValid() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(
                payment(1, "SUCCESS", 50, LocalDateTime.now(), "BOOKED", "Anita Rao")));

        assertThrows(ConflictException.class,
                () -> controller.refund(1L, Map.of("reason", "Customer asked")));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void onlySuccessfulPaymentsCanBeRefunded() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(
                payment(1, "FAILED", 50, LocalDateTime.now(), "CANCELLED", "Anita Rao")));

        assertThrows(ConflictException.class,
                () -> controller.refund(1L, Map.of("reason", "Customer asked")));
    }

    @Test
    void refundOfACancelledBookingIsSavedNotifiedAndAudited() {
        Payment paid = payment(1, "SUCCESS", 50, LocalDateTime.now(), "CANCELLED", "Anita Rao");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paid));
        when(paymentRepository.save(paid)).thenReturn(paid);

        Payment result = controller.refund(1L, Map.of("reason", "Booking cancelled"));

        assertEquals("REFUNDED", result.getStatus());
        verify(auditService).log("PAYMENT_REFUNDED", "PAYMENT", 1L, "Booking cancelled");
        verify(notificationService).notifyUser(eq(paid.getBooking().getUser()),
                eq("PAYMENT_DECISION"), anyString(), contains("refunded"));
    }

    @Test
    void csvHasHeaderAndNeutralisesSpreadsheetFormulas() {
        when(paymentRepository.findAll()).thenReturn(List.of(
                payment(1, "SUCCESS", 50, LocalDateTime.now(), "BOOKED", "=cmd")));

        ResponseEntity<byte[]> response = controller.reportCsv(null, null, null, null, null);
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("paymentId,bookingId,user,email,amount,method,reference,status"));
        assertTrue(csv.contains("\"'=cmd\""));
    }
}
