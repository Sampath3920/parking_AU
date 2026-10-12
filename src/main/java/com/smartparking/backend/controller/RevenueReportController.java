package com.smartparking.backend.controller;

import com.smartparking.backend.dto.RevenueReportResponse;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.repository.PaymentRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:5173")
public class RevenueReportController {

    private final PaymentRepository paymentRepository;

    public RevenueReportController(
            PaymentRepository paymentRepository) {

        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/revenue")
    public RevenueReportResponse getRevenueReport() {

        List<Payment> payments = paymentRepository.findAll();

        double totalRevenue = payments.stream()
                .filter(payment ->
                        "SUCCESS".equals(payment.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();

        long totalPayments = payments.stream()
                .filter(payment ->
                        "SUCCESS".equals(payment.getStatus()))
                .count();

        return new RevenueReportResponse(
                totalRevenue,
                totalPayments
        );
    }
}