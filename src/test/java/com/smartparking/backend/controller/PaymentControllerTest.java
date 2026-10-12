package com.smartparking.backend.controller;

import com.smartparking.backend.dto.PaymentReviewRequest;
import com.smartparking.backend.dto.PaymentSubmissionRequest;
import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.NotificationRepository;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final NotificationRepository notificationRepository =
            mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PaymentController controller = new PaymentController(
            paymentRepository, bookingRepository, notificationRepository, userRepository, 75.0);

    @Test
    void submissionCreatesPendingRecordAndNotifiesAdmins() {
        Booking booking = new Booking();
        booking.setId(4L);
        User admin = new User();
        admin.setRole("ADMIN");
        when(bookingRepository.findById(4L)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(12L);
            return payment;
        });
        when(userRepository.findByRole("ADMIN")).thenReturn(List.of(admin));

        Payment result = controller.createPayment(
                4L, new PaymentSubmissionRequest("upi", " ref-abc ", true));

        assertEquals("PENDING_VERIFICATION", result.getStatus());
        assertEquals(75.0, result.getAmount());
        assertEquals("UPI", result.getPaymentMethod());
        assertEquals("ref-abc", result.getTransactionReference());
        verify(notificationRepository).save(any());
    }

    @Test
    void secondPaymentForBookingIsRejected() {
        when(bookingRepository.findById(4L)).thenReturn(Optional.of(new Booking()));
        when(paymentRepository.findByBookingId(4L)).thenReturn(Optional.of(new Payment()));

        assertThrows(ConflictException.class, () -> controller.createPayment(
                4L, new PaymentSubmissionRequest("UPI", "ref-abc", true)));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void duplicateTransactionReferenceIsRejected() {
        when(bookingRepository.findById(4L)).thenReturn(Optional.of(new Booking()));
        when(paymentRepository.existsByTransactionReferenceIgnoreCase("ref-abc"))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> controller.createPayment(
                4L, new PaymentSubmissionRequest("UPI", "ref-abc", true)));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectedPaymentRequiresAReason() {
        Payment payment = new Payment();
        payment.setId(8L);
        payment.setStatus("PENDING_VERIFICATION");
        when(paymentRepository.findById(8L)).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class, () -> controller.reviewPayment(
                8L, new PaymentReviewRequest("FAILED", "  ")));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void verifiedPaymentNotifiesTheBookingOwner() {
        User owner = new User();
        owner.setId(22L);
        Booking booking = new Booking();
        booking.setUser(owner);
        Payment payment = new Payment();
        payment.setId(9L);
        payment.setStatus("PENDING_VERIFICATION");
        payment.setBooking(booking);
        when(paymentRepository.findById(9L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment result = controller.reviewPayment(
                9L, new PaymentReviewRequest("SUCCESS", null));

        assertEquals("SUCCESS", result.getStatus());
        verify(notificationRepository).save(argThat(notification ->
                notification.getUser() == owner
                        && notification.getType().equals("PAYMENT_DECISION")));
    }
}
