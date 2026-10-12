package com.smartparking.backend.repository;

import com.smartparking.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByStatus(String status);

    List<Payment> findByBookingUserIdOrderByPaymentTimeDesc(Long userId);

    boolean existsByTransactionReferenceIgnoreCase(String transactionReference);
}