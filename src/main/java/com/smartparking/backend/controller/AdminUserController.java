package com.smartparking.backend.controller;

import com.smartparking.backend.dto.PageResult;
import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.exception.NotFoundException;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;
import com.smartparking.backend.security.CustomUserDetailsService;
import com.smartparking.backend.service.AuditService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** User management for administrators. Access is limited to ADMIN in SecurityConfig (/api/admin/**). */
@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminUserController {

    private static final Set<String> ROLES =
            Set.of("STUDENT", "FACULTY", "STAFF", "VISITOR", "GUARD", "ADMIN");

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    public AdminUserController(UserRepository userRepository,
                               VehicleRepository vehicleRepository,
                               BookingRepository bookingRepository,
                               PaymentRepository paymentRepository,
                               AuditService auditService) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
    }

    /**
     * One page of users. Optional filters: search (name, email, phone or exact id),
     * role (STUDENT, FACULTY, ...), status (ACTIVE or INACTIVE).
     * The password field is hidden from JSON by @JsonIgnore in the User class.
     */
    @GetMapping
    public PageResult<User> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String text = search == null ? "" : search.trim().toLowerCase();
        String wantedRole = role == null ? "" : role.trim().toUpperCase();
        String wantedStatus = status == null ? "" : status.trim().toUpperCase();

        List<User> matches = userRepository.findAll().stream()
                .filter(u -> wantedRole.isEmpty()
                        || wantedRole.equals(CustomUserDetailsService.normalizeRole(u.getRole())))
                .filter(u -> wantedStatus.isEmpty()
                        || ("ACTIVE".equals(wantedStatus) && u.isActive())
                        || ("INACTIVE".equals(wantedStatus) && !u.isActive()))
                .filter(u -> text.isEmpty() || matchesText(u, text))
                .sorted(Comparator.comparing(User::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return PageResult.of(matches, page, size);
    }

    /** Everything one person has: vehicles, bookings and payments. */
    @GetMapping("/{id}/details")
    public Map<String, Object> getDetails(@PathVariable Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<Vehicle> vehicles = vehicleRepository.findByUserId(id);
        List<Booking> bookings = bookingRepository.findByUserId(id).stream()
                .sorted(Comparator.comparing(Booking::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<Payment> payments = paymentRepository.findByBookingUserIdOrderByPaymentTimeDesc(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("vehicles", vehicles);
        result.put("bookings", bookings);
        result.put("payments", payments);
        return result;
    }

    @PutMapping("/{id}/role")
    public User changeRole(@PathVariable Long id,
                           @RequestBody Map<String, String> body,
                           Authentication authentication) {

        String role = body.get("role") == null ? "" : body.get("role").trim().toUpperCase();
        if (!ROLES.contains(role)) {
            throw new IllegalArgumentException("Role must be one of " + ROLES);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (isSelf(user, authentication)) {
            throw new ConflictException("You cannot change your own role");
        }

        String oldRole = CustomUserDetailsService.normalizeRole(user.getRole());
        if ("ADMIN".equals(oldRole) && !"ADMIN".equals(role) && user.isActive()
                && countActiveAdmins() <= 1) {
            throw new ConflictException("At least one active administrator must remain");
        }

        user.setRole(role);
        User saved = userRepository.save(user);
        auditService.log("USER_ROLE_CHANGED", "USER", id, oldRole + " -> " + role);
        return saved;
    }

    /** Body: {"active": true} or {"active": false}. */
    @PutMapping("/{id}/status")
    public User changeStatus(@PathVariable Long id,
                             @RequestBody Map<String, Object> body,
                             Authentication authentication) {

        if (!(body.get("active") instanceof Boolean active)) {
            throw new IllegalArgumentException("Send {\"active\": true} or {\"active\": false}");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!active) {
            if (isSelf(user, authentication)) {
                throw new ConflictException("You cannot deactivate your own account");
            }
            boolean isAdmin = "ADMIN".equals(CustomUserDetailsService.normalizeRole(user.getRole()));
            if (isAdmin && user.isActive() && countActiveAdmins() <= 1) {
                throw new ConflictException("At least one active administrator must remain");
            }
        }

        user.setActive(active);
        User saved = userRepository.save(user);
        auditService.log(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", id, user.getEmail());
        return saved;
    }

    // ---------- helpers

    private boolean matchesText(User user, String text) {
        return contains(user.getName(), text)
                || contains(user.getEmail(), text)
                || contains(user.getPhone(), text)
                || (user.getId() != null && String.valueOf(user.getId()).equals(text));
    }

    private boolean contains(String value, String text) {
        return value != null && value.toLowerCase().contains(text);
    }

    private boolean isSelf(User user, Authentication authentication) {
        return authentication != null && user.getEmail() != null
                && user.getEmail().equalsIgnoreCase(authentication.getName());
    }

    private long countActiveAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> "ADMIN".equals(CustomUserDetailsService.normalizeRole(u.getRole())))
                .filter(User::isActive)
                .count();
    }
}
