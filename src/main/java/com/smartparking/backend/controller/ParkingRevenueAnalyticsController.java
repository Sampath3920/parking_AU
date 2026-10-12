package com.smartparking.backend.controller;

import com.smartparking.backend.model.Payment;
import com.smartparking.backend.repository.PaymentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-revenue")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingRevenueAnalyticsController {

    private final PaymentRepository paymentRepository;

    public ParkingRevenueAnalyticsController(
            PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping
    public Map<String, Object> getRevenueAnalytics() {

        Map<String, Object> revenue = new HashMap<>();

        List<Payment> successfulPayments =
                paymentRepository.findByStatus("SUCCESS");

        List<Payment> pendingPayments =
                paymentRepository.findByStatus("PENDING");

        List<Payment> failedPayments =
                paymentRepository.findByStatus("FAILED");

        double totalRevenue = 0;

        for (Payment payment : successfulPayments) {
            totalRevenue += payment.getAmount();
        }

        revenue.put("totalRevenue", totalRevenue);
        revenue.put("totalPayments", paymentRepository.count());
        revenue.put("successfulPayments", successfulPayments.size());
        revenue.put("pendingPayments", pendingPayments.size());
        revenue.put("failedPayments", failedPayments.size());

        return revenue;
    }
}