package com.smartparking.backend.controller;

import com.smartparking.backend.dto.PaymentReviewRequest;
import com.smartparking.backend.dto.PaymentSubmissionRequest;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.Notification;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.NotificationRepository;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.exception.ConflictException;
import jakarta.validation.Valid;
import com.smartparking.backend.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final double manualPaymentAmount;

    // Optional: filled by Spring, null in unit tests
    @Autowired(required = false)
    private AuditService auditService;

    public PaymentController(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            @Value("${app.payment.manual-amount:50.00}") double manualPaymentAmount) {

        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        if (!Double.isFinite(manualPaymentAmount) || manualPaymentAmount <= 0) {
            throw new IllegalArgumentException("Manual payment amount must be greater than zero");
        }
        this.manualPaymentAmount = manualPaymentAmount;
    }

    @PostMapping("/create/{bookingId}")
    @Transactional
    public Payment createPayment(
            @PathVariable Long bookingId,
            @Valid @RequestBody PaymentSubmissionRequest request) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found"));

        if (paymentRepository.findByBookingId(bookingId).isPresent()) {
            throw new ConflictException("A payment record already exists for this booking");
        }

        String paymentMethod = request.paymentMethod().trim().toUpperCase();
        if (!List.of("UPI", "BANK_TRANSFER", "CASH").contains(paymentMethod)) {
            throw new IllegalArgumentException(
                    "Choose UPI, bank transfer, or cash for manual verification");
        }

        String reference = request.transactionReference().trim();
        if (reference.length() > 100) {
            throw new IllegalArgumentException("Payment reference must be 100 characters or fewer");
        }
        if (paymentRepository.existsByTransactionReferenceIgnoreCase(reference)) {
            throw new ConflictException("This payment reference has already been submitted");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(manualPaymentAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setTransactionReference(reference);
        payment.setStatus("PENDING_VERIFICATION");
        Payment savedPayment = paymentRepository.save(payment);

        for (User admin : userRepository.findByRole("ADMIN")) {
            Notification notification = new Notification();
            notification.setMessage(
                    "Payment verification requested for booking #" + bookingId
                            + " (payment #" + savedPayment.getId() + ")");
            notification.setType("PAYMENT_REVIEW");
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            notification.setUser(admin);
            notificationRepository.save(notification);
        }

        return savedPayment;
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<Payment> getPaymentsByUser(@PathVariable Long userId) {
        return paymentRepository.findByBookingUserIdOrderByPaymentTimeDesc(userId);
    }

    @GetMapping("/manual-amount")
    public Map<String, Double> getManualPaymentAmount() {
        return Map.of("amount", manualPaymentAmount);
    }

    @GetMapping("/{id}")
    public Payment getPayment(@PathVariable Long id) {

        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));
    }

    @PutMapping("/{id}/verify")
    @Transactional
    public Payment reviewPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentReviewRequest request) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!"PENDING_VERIFICATION".equals(payment.getStatus())) {
            throw new ConflictException("Only pending payments can be reviewed");
        }

        String decision = request.status().trim().toUpperCase();
        if (!List.of("SUCCESS", "FAILED").contains(decision)) {
            throw new IllegalArgumentException("Payment decision must be SUCCESS or FAILED");
        }

        String reason = request.reason() == null ? "" : request.reason().trim();
        if ("FAILED".equals(decision) && reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required when rejecting a payment");
        }

        payment.setStatus(decision);
        payment.setReviewReason(reason.isBlank() ? null : reason);
        Payment savedPayment = paymentRepository.save(payment);

        if (auditService != null) {
            auditService.log("PAYMENT_" + decision, "PAYMENT", id,
                    reason.isBlank() ? "verified" : reason);
        }

        Booking booking = payment.getBooking();
        if (booking != null && booking.getUser() != null) {
            Notification notification = new Notification();
            notification.setMessage("Payment #" + payment.getId()
                    + ("SUCCESS".equals(decision)
                    ? " was verified successfully."
                    : " was rejected: " + reason));
            notification.setType("PAYMENT_DECISION");
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            notification.setUser(booking.getUser());
            notificationRepository.save(notification);
        }
        return savedPayment;
    }

    @GetMapping("/booking/{bookingId}")
    public Payment getPaymentByBooking(
            @PathVariable Long bookingId) {

        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));
    }
}