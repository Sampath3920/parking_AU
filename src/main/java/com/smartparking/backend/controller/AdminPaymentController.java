package com.smartparking.backend.controller;

import com.smartparking.backend.dto.PageResult;
import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.exception.NotFoundException;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.service.AuditService;
import com.smartparking.backend.service.CsvUtil;
import com.smartparking.backend.service.NotificationService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Payment management for administrators (ADMIN only, URL starts with /api/admin).
 * Verifying or rejecting a submitted payment is done with the existing
 * PUT /api/payments/{id}/verify. Everything here is for looking, reporting and refunds.
 */
@RestController
@RequestMapping("/api/admin/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminPaymentController {

    public static final Set<String> STATUSES =
            Set.of("PENDING_VERIFICATION", "SUCCESS", "FAILED", "REFUNDED");

    /** A payment can only be refunded when the booking it paid for will not be used. */
    private static final Set<String> REFUNDABLE_BOOKING_STATUSES =
            Set.of("CANCELLED", "REJECTED", "EXPIRED");

    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public AdminPaymentController(PaymentRepository paymentRepository,
                                  NotificationService notificationService,
                                  AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    /** One page of payments, newest first. All filters are optional. */
    @GetMapping
    public PageResult<Payment> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return PageResult.of(filtered(status, method, search, from, to), page, size);
    }

    /** Counts per status and revenue: today, this month, all time, last 14 days, last 6 months. */
    @GetMapping("/summary")
    public Map<String, Object> summary() {

        List<Payment> all = paymentRepository.findAll();
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);

        Map<LocalDate, Double> perDay = new LinkedHashMap<>();
        for (int i = 13; i >= 0; i--) {
            perDay.put(today.minusDays(i), 0.0);
        }
        Map<YearMonth, Double> perMonth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            perMonth.put(thisMonth.minusMonths(i), 0.0);
        }

        double revenueToday = 0;
        double revenueMonth = 0;
        double revenueTotal = 0;

        for (Payment payment : all) {
            if (!"SUCCESS".equals(payment.getStatus())) {
                continue;
            }
            double amount = payment.getAmount();
            revenueTotal += amount;
            if (payment.getPaymentTime() == null) {
                continue;
            }
            LocalDate day = payment.getPaymentTime().toLocalDate();
            if (day.equals(today)) {
                revenueToday += amount;
            }
            if (YearMonth.from(day).equals(thisMonth)) {
                revenueMonth += amount;
            }
            if (perDay.containsKey(day)) {
                perDay.put(day, perDay.get(day) + amount);
            }
            YearMonth month = YearMonth.from(day);
            if (perMonth.containsKey(month)) {
                perMonth.put(month, perMonth.get(month) + amount);
            }
        }

        List<Map<String, Object>> daily = new ArrayList<>();
        perDay.forEach((day, amount) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", day.toString());
            row.put("amount", amount);
            daily.add(row);
        });
        List<Map<String, Object>> monthly = new ArrayList<>();
        perMonth.forEach((month, amount) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month.toString());
            row.put("amount", amount);
            monthly.add(row);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pending", countStatus(all, "PENDING_VERIFICATION"));
        result.put("successful", countStatus(all, "SUCCESS"));
        result.put("failed", countStatus(all, "FAILED"));
        result.put("refunded", countStatus(all, "REFUNDED"));
        result.put("revenueToday", revenueToday);
        result.put("revenueThisMonth", revenueMonth);
        result.put("revenueTotal", revenueTotal);
        result.put("daily", daily);
        result.put("monthly", monthly);
        return result;
    }

    @GetMapping("/{id}")
    public Payment details(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    /** Body: {"reason": "..."}. Only a SUCCESS payment of a cancelled, rejected or expired booking. */
    @PutMapping("/{id}/refund")
    @Transactional
    public Payment refund(@PathVariable Long id, @RequestBody Map<String, String> body) {

        String reason = body.get("reason") == null ? "" : body.get("reason").trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("A reason is required for a refund");
        }
        if (reason.length() > 200) {
            throw new IllegalArgumentException("The reason must be 200 characters or fewer");
        }

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new ConflictException("Only a successful payment can be refunded");
        }

        Booking booking = payment.getBooking();
        if (booking == null || !REFUNDABLE_BOOKING_STATUSES.contains(booking.getStatus())) {
            throw new ConflictException(
                    "Cancel or reject the booking first. Only payments of cancelled, rejected or expired bookings can be refunded");
        }

        payment.setStatus("REFUNDED");
        payment.setReviewReason("Refunded: " + reason);
        Payment saved = paymentRepository.save(payment);

        auditService.log("PAYMENT_REFUNDED", "PAYMENT", id, reason);
        notificationService.notifyUser(booking.getUser(), "PAYMENT_DECISION", "Payment refunded",
                "Payment #" + id + " was refunded: " + reason);
        return saved;
    }

    /** Same filters as the list, as a CSV file. */
    @GetMapping("/report.csv")
    public ResponseEntity<byte[]> reportCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        StringBuilder csv = new StringBuilder(
                "paymentId,bookingId,user,email,amount,method,reference,status,paymentTime,note\n");

        for (Payment p : filtered(status, method, search, from, to)) {
            Booking b = p.getBooking();
            csv.append(p.getId()).append(',')
               .append(b != null ? b.getId() : "").append(',')
               .append(CsvUtil.cell(b != null && b.getUser() != null ? b.getUser().getName() : "")).append(',')
               .append(CsvUtil.cell(b != null && b.getUser() != null ? b.getUser().getEmail() : "")).append(',')
               .append(p.getAmount()).append(',')
               .append(CsvUtil.cell(p.getPaymentMethod())).append(',')
               .append(CsvUtil.cell(p.getTransactionReference())).append(',')
               .append(CsvUtil.cell(p.getStatus())).append(',')
               .append(CsvUtil.cell(p.getPaymentTime())).append(',')
               .append(CsvUtil.cell(p.getReviewReason())).append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payments.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(CsvUtil.bytes(csv.toString()));
    }

    // ---------- helpers

    private List<Payment> filtered(String status, String method, String search,
                                   LocalDate from, LocalDate to) {

        String wantedStatus = status == null ? "" : status.trim().toUpperCase();
        String wantedMethod = method == null ? "" : method.trim().toUpperCase();
        String text = search == null ? "" : search.trim().toLowerCase();

        return paymentRepository.findAll().stream()
                .filter(p -> wantedStatus.isEmpty() || wantedStatus.equals(p.getStatus()))
                .filter(p -> wantedMethod.isEmpty() || wantedMethod.equalsIgnoreCase(p.getPaymentMethod()))
                .filter(p -> from == null
                        || (p.getPaymentTime() != null && !p.getPaymentTime().toLocalDate().isBefore(from)))
                .filter(p -> to == null
                        || (p.getPaymentTime() != null && !p.getPaymentTime().toLocalDate().isAfter(to)))
                .filter(p -> text.isEmpty() || matchesText(p, text))
                .sorted(Comparator.comparing(Payment::getPaymentTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private boolean matchesText(Payment p, String text) {
        Booking b = p.getBooking();
        return contains(p.getTransactionReference(), text)
                || (b != null && b.getUser() != null && contains(b.getUser().getName(), text))
                || (b != null && b.getUser() != null && contains(b.getUser().getEmail(), text))
                || (p.getId() != null && String.valueOf(p.getId()).equals(text))
                || (b != null && b.getId() != null && String.valueOf(b.getId()).equals(text));
    }

    private boolean contains(String value, String text) {
        return value != null && value.toLowerCase().contains(text);
    }

    private long countStatus(List<Payment> payments, String status) {
        return payments.stream().filter(p -> status.equals(p.getStatus())).count();
    }
}
