package com.smartparking.backend.service;

import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.Payment;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Booking life cycle:
 *   create  -> PENDING (reserves the slot, waits for admin)
 *   approve -> BOOKED  (QR code token is created; payment must be verified first)
 *   reject  -> REJECTED (reason required, slot is released)
 *   cancel  -> CANCELLED (PENDING or BOOKED, not after check-in)
 *   unused after the end time -> EXPIRED (BookingExpiryJob)
 * Check-in / check-out (ParkingSessionController) finishes with COMPLETED.
 */
@Service
public class BookingService {

    // Rules for a booking time window
    static final Duration MIN_LENGTH = Duration.ofMinutes(15);
    static final Duration MAX_LENGTH = Duration.ofHours(24);
    static final Duration MAX_ADVANCE = Duration.ofDays(7);
    static final Duration PAST_GRACE = Duration.ofMinutes(10);
    static final Duration DEFAULT_LENGTH = Duration.ofHours(2);

    /** Statuses that keep a slot reserved. */
    public static final List<String> ACTIVE_STATUSES = List.of("BOOKED", "PENDING");

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final boolean requirePayment;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          VehicleRepository vehicleRepository,
                          ParkingSlotRepository slotRepository,
                          ParkingSessionRepository sessionRepository,
                          PaymentRepository paymentRepository,
                          NotificationService notificationService,
                          AuditService auditService,
                          @Value("${app.booking.require-payment:true}") boolean requirePayment) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.slotRepository = slotRepository;
        this.sessionRepository = sessionRepository;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.requirePayment = requirePayment;
    }

    /** Throws IllegalArgumentException with a clear message when the window is not allowed. */
    public void validateWindow(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();

        if (start == null || end == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (start.isBefore(now.minus(PAST_GRACE))) {
            throw new IllegalArgumentException("Start time cannot be in the past");
        }
        if (start.isAfter(now.plus(MAX_ADVANCE))) {
            throw new IllegalArgumentException("You can book at most 7 days ahead");
        }

        Duration length = Duration.between(start, end);
        if (length.compareTo(MIN_LENGTH) < 0) {
            throw new IllegalArgumentException("Minimum booking time is 15 minutes");
        }
        if (length.compareTo(MAX_LENGTH) > 0) {
            throw new IllegalArgumentException("Maximum booking time is 24 hours");
        }
    }

    /** Slots of this vehicle type that nobody has requested or booked during [start, end). */
    public List<ParkingSlot> findAvailableSlots(String vehicleType,
                                                LocalDateTime start,
                                                LocalDateTime end) {
        validateWindow(start, end);

        return slotRepository.findByVehicleType(vehicleType).stream()
                .filter(slot -> isOpenForBooking(slot))
                .filter(slot -> !bookingRepository.existsOverlap(slot.getId(), start, end))
                .toList();
    }

    @Transactional
    public Booking createBooking(Long userId, Long vehicleId, Long slotId,
                                 LocalDateTime start, LocalDateTime end) {

        // Old calls without times still work: book from now for 2 hours.
        if (start == null && end == null) {
            start = LocalDateTime.now();
            end = start.plus(DEFAULT_LENGTH);
        }
        validateWindow(start, end);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        if (vehicle.getUser() == null || !userId.equals(vehicle.getUser().getId())) {
            throw new IllegalArgumentException("This vehicle does not belong to you");
        }

        if (!"VERIFIED".equals(vehicle.getVerificationStatus())) {
            throw new IllegalArgumentException(
                    "Your vehicle must be approved by the admin before you can book");
        }

        // Lock the slot row. A second request for the same slot waits here,
        // then the overlap check below sees the first request and refuses it.
        ParkingSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found"));

        if (!isOpenForBooking(slot)) {
            throw new ConflictException("Parking slot is not open for booking");
        }

        if (slot.getVehicleType() != null && vehicle.getVehicleType() != null
                && !slot.getVehicleType().equalsIgnoreCase(vehicle.getVehicleType())) {
            throw new IllegalArgumentException("This slot is not for your vehicle type");
        }

        if (bookingRepository.existsOverlap(slotId, start, end)) {
            throw new ConflictException(
                    "This slot is already requested or booked for that time. Choose another time or slot.");
        }

        if (bookingRepository.existsByVehicleIdAndStatusIn(vehicleId, ACTIVE_STATUSES)) {
            throw new ConflictException("This vehicle already has an active or pending booking");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setParkingSlot(slot);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setStatus("PENDING");

        slot.setStatus("BOOKED");
        slotRepository.save(slot);

        Booking saved = bookingRepository.save(booking);

        notificationService.notifyRole("ADMIN", "BOOKING_REQUEST",
                "New parking request #" + saved.getId() + " from " + user.getName()
                        + " for slot " + slot.getSlotNumber());

        return saved;
    }

    /** Admin approves a PENDING request: payment must be verified, no overlap with approved bookings. */
    @Transactional
    public Booking approve(Long bookingId) {

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!"PENDING".equals(booking.getStatus())) {
            throw new ConflictException("Only pending requests can be approved");
        }
        if (booking.getEndTime() != null && booking.getEndTime().isBefore(LocalDateTime.now())) {
            throw new ConflictException("This request has already expired");
        }

        ParkingSlot slot = slotRepository.findByIdForUpdate(booking.getParkingSlot().getId())
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found"));

        if (!isOpenForBooking(slot)) {
            throw new ConflictException("The slot is " + slot.getStatus()
                    + " and cannot be approved. Reject the request or fix the slot first.");
        }

        if (booking.getStartTime() != null && bookingRepository.existsApprovedOverlap(
                slot.getId(), booking.getId(), booking.getStartTime(), booking.getEndTime())) {
            throw new ConflictException("Another approved booking overlaps this time");
        }

        if (requirePayment) {
            Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
            if (payment == null || !"SUCCESS".equals(payment.getStatus())) {
                throw new ConflictException(
                        "The payment must be verified (SUCCESS) before this request can be approved");
            }
        }

        booking.setStatus("BOOKED");
        booking.setDecisionReason(null);
        booking.setDecidedAt(LocalDateTime.now());
        if (booking.getQrToken() == null) {
            booking.setQrToken(UUID.randomUUID().toString());
        }
        Booking saved = bookingRepository.save(booking);

        slot.setStatus("BOOKED");
        slotRepository.save(slot);

        notificationService.notifyUser(booking.getUser(), "BOOKING_DECISION",
                "Your parking request #" + bookingId
                        + " was approved. Your QR code is now available on the QR page.");
        auditService.log("BOOKING_APPROVED", "BOOKING", bookingId,
                "slot " + slot.getSlotNumber());

        return saved;
    }

    /** Admin rejects a PENDING request. A reason is required. */
    @Transactional
    public Booking reject(Long bookingId, String reason) {

        String text = reason == null ? "" : reason.trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("A reason is required when rejecting a request");
        }
        if (text.length() > 500) {
            throw new IllegalArgumentException("Reason must be 500 characters or fewer");
        }

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!"PENDING".equals(booking.getStatus())) {
            throw new ConflictException("Only pending requests can be rejected");
        }

        booking.setStatus("REJECTED");
        booking.setDecisionReason(text);
        booking.setDecidedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        refreshSlotStatus(booking.getParkingSlot().getId());

        notificationService.notifyUser(booking.getUser(), "BOOKING_DECISION",
                "Your parking request #" + bookingId + " was rejected: " + text);
        auditService.log("BOOKING_REJECTED", "BOOKING", bookingId, text);

        return saved;
    }

    /** Cancelling keeps the booking in history with status CANCELLED. */
    @Transactional
    public Booking cancelBooking(Long bookingId) {

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw new ConflictException("Only pending or approved bookings can be cancelled");
        }

        if (sessionRepository.findByBookingId(bookingId).isPresent()) {
            throw new ConflictException("A booking cannot be cancelled after check-in");
        }

        booking.setStatus("CANCELLED");
        Booking saved = bookingRepository.save(booking);

        refreshSlotStatus(booking.getParkingSlot().getId());
        return saved;
    }

    /**
     * The slot is BOOKED while it has a pending or approved booking, otherwise AVAILABLE.
     * Slots that the admin put in MAINTENANCE or OUT_OF_SERVICE are left alone.
     */
    @Transactional
    public void refreshSlotStatus(Long slotId) {
        ParkingSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found"));

        if (!isOpenForBooking(slot)) {
            return;
        }

        boolean hasActive = bookingRepository.existsByParkingSlotIdAndStatusIn(slotId, ACTIVE_STATUSES);
        slot.setStatus(hasActive ? "BOOKED" : "AVAILABLE");
        slotRepository.save(slot);
    }

    private boolean isOpenForBooking(ParkingSlot slot) {
        return "AVAILABLE".equals(slot.getStatus()) || "BOOKED".equals(slot.getStatus());
    }
}
