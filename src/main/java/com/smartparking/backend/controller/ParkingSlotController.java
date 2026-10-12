package com.smartparking.backend.controller;

import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.ParkingLocation;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingLocationRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;
import com.smartparking.backend.service.AuditService;
import com.smartparking.backend.service.BookingService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reading slots: any logged-in user. Add, change, delete: ADMIN only (SecurityConfig).
 *
 * Stored status values: AVAILABLE, BOOKED (set automatically while a booking waits or is approved),
 * MAINTENANCE and OUT_OF_SERVICE (set by the admin; nobody can book them).
 * "Occupied" is not stored: it is worked out from the ACTIVE parking sessions.
 */
@RestController
@RequestMapping("/api/parking-slots")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingSlotController {

    private static final Set<String> VEHICLE_TYPES = Set.of("2-WHEELER", "4-WHEELER");
    private static final Set<String> ADMIN_STATUSES =
            Set.of("AVAILABLE", "MAINTENANCE", "OUT_OF_SERVICE");

    private final ParkingSlotRepository slotRepository;
    private final ParkingLocationRepository locationRepository;
    private final BookingRepository bookingRepository;
    private final ParkingSessionRepository sessionRepository;
    private final BookingService bookingService;
    private final AuditService auditService;

    public ParkingSlotController(
            ParkingSlotRepository slotRepository,
            ParkingLocationRepository locationRepository,
            BookingRepository bookingRepository,
            ParkingSessionRepository sessionRepository,
            BookingService bookingService,
            AuditService auditService) {
        this.slotRepository = slotRepository;
        this.locationRepository = locationRepository;
        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
        this.bookingService = bookingService;
        this.auditService = auditService;
    }

    @PostMapping("/add/{locationId}")
    @Transactional
    public ParkingSlot addSlot(
            @PathVariable Long locationId,
            @RequestBody ParkingSlot slot) {

        ParkingLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Parking location not found"));

        slot.setId(null);
        clean(slot);
        ensureUniqueNumber(location.getId(), slot.getSlotNumber(), null);
        slot.setParkingLocation(location);

        ParkingSlot saved = slotRepository.save(slot);
        auditService.log("SLOT_CREATED", "SLOT", saved.getId(),
                location.getName() + " / " + saved.getSlotNumber());
        return saved;
    }

    /** Filters (all optional): ?locationId=1&vehicleType=2-WHEELER&status=AVAILABLE&search=A1 */
    @GetMapping
    public List<ParkingSlot> getAllSlots(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        String text = search == null ? "" : search.trim().toLowerCase();

        return slotRepository.findAll().stream()
                .filter(s -> locationId == null || (s.getParkingLocation() != null
                        && locationId.equals(s.getParkingLocation().getId())))
                .filter(s -> vehicleType == null || vehicleType.isBlank()
                        || vehicleType.equalsIgnoreCase(s.getVehicleType()))
                .filter(s -> status == null || status.isBlank()
                        || status.equalsIgnoreCase(s.getStatus()))
                .filter(s -> text.isEmpty()
                        || (s.getSlotNumber() != null && s.getSlotNumber().toLowerCase().contains(text)))
                .toList();
    }

    /** Totals for the admin screen. occupied = a car is parked now. */
    @GetMapping("/summary")
    public Map<String, Long> summary(@RequestParam(required = false) Long locationId) {

        Set<Long> occupiedIds = occupiedSlotIds();
        long available = 0;
        long reserved = 0;
        long occupied = 0;
        long maintenance = 0;
        long outOfService = 0;
        long total = 0;

        for (ParkingSlot slot : slotRepository.findAll()) {
            if (locationId != null && (slot.getParkingLocation() == null
                    || !locationId.equals(slot.getParkingLocation().getId()))) {
                continue;
            }
            total++;
            if (occupiedIds.contains(slot.getId())) {
                occupied++;
            } else if ("BOOKED".equals(slot.getStatus())) {
                reserved++;
            } else if ("MAINTENANCE".equals(slot.getStatus())) {
                maintenance++;
            } else if ("OUT_OF_SERVICE".equals(slot.getStatus())) {
                outOfService++;
            } else {
                available++;
            }
        }

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("available", available);
        result.put("reserved", reserved);
        result.put("occupied", occupied);
        result.put("maintenance", maintenance);
        result.put("outOfService", outOfService);
        return result;
    }

    @GetMapping("/location/{locationId}")
    public List<ParkingSlot> getSlotsByLocation(
            @PathVariable Long locationId) {

        return slotRepository.findByParkingLocationId(locationId);
    }

    @GetMapping("/{id}")
    public ParkingSlot getSlot(@PathVariable Long id) {

        return slotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found"));
    }

    @PutMapping("/{id}")
    @Transactional
    public ParkingSlot updateSlot(
            @PathVariable Long id,
            @RequestBody ParkingSlot updatedSlot) {

        ParkingSlot slot = slotRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found"));

        // The status of a slot that currently has a booking is BOOKED; the admin form sends
        // that back unchanged. Treat it as AVAILABLE so the system works it out again.
        if ("BOOKED".equals(updatedSlot.getStatus())) {
            updatedSlot.setStatus("AVAILABLE");
        }
        clean(updatedSlot);

        ParkingLocation location = slot.getParkingLocation();
        if (updatedSlot.getParkingLocation() != null && updatedSlot.getParkingLocation().getId() != null) {
            location = locationRepository.findById(updatedSlot.getParkingLocation().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Parking location not found"));
        }
        ensureUniqueNumber(location.getId(), updatedSlot.getSlotNumber(), id);

        long activeBookings = activeBookingCount(id);
        boolean typeChanged = !updatedSlot.getVehicleType().equalsIgnoreCase(slot.getVehicleType());
        boolean locationChanged = !location.getId().equals(slot.getParkingLocation().getId());
        boolean closing = !"AVAILABLE".equals(updatedSlot.getStatus());

        if (activeBookings > 0 && (typeChanged || locationChanged || closing)) {
            throw new ConflictException("This slot has " + activeBookings
                    + " pending or approved booking(s). Cancel or reject them first.");
        }
        if (closing && occupiedSlotIds().contains(id)) {
            throw new ConflictException("A vehicle is parked in this slot right now");
        }

        slot.setSlotNumber(updatedSlot.getSlotNumber());
        slot.setVehicleType(updatedSlot.getVehicleType());
        slot.setFloor(updatedSlot.getFloor());
        slot.setParkingLocation(location);
        slot.setStatus(updatedSlot.getStatus());
        ParkingSlot saved = slotRepository.save(slot);

        // AVAILABLE becomes BOOKED again if a booking still needs the slot.
        bookingService.refreshSlotStatus(id);

        auditService.log("SLOT_UPDATED", "SLOT", id,
                saved.getSlotNumber() + " -> " + updatedSlot.getStatus());
        return slotRepository.findById(id).orElse(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, String> deleteSlot(@PathVariable Long id) {

        ParkingSlot slot = slotRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking slot not found"));

        List<Booking> bookings = bookingRepository.findByParkingSlotId(id);
        if (activeBookingCount(id) > 0 || occupiedSlotIds().contains(id)) {
            throw new ConflictException(
                    "This slot has an active booking or parking session and cannot be deleted");
        }
        if (!bookings.isEmpty()) {
            throw new ConflictException(
                    "This slot has booking history. Set it to OUT_OF_SERVICE instead of deleting it");
        }

        slotRepository.delete(slot);
        auditService.log("SLOT_DELETED", "SLOT", id, slot.getSlotNumber());
        return Map.of("message", "Parking slot deleted successfully");
    }

    // ---------- helpers

    private void clean(ParkingSlot slot) {
        String number = slot.getSlotNumber() == null ? "" : slot.getSlotNumber().trim();
        if (number.isEmpty() || number.length() > 20) {
            throw new IllegalArgumentException("Slot number is required (up to 20 characters)");
        }
        slot.setSlotNumber(number);

        String type = slot.getVehicleType() == null ? "" : slot.getVehicleType().trim().toUpperCase();
        if (!VEHICLE_TYPES.contains(type)) {
            throw new IllegalArgumentException("Vehicle type must be 2-WHEELER or 4-WHEELER");
        }
        slot.setVehicleType(type);

        String status = slot.getStatus() == null || slot.getStatus().isBlank()
                ? "AVAILABLE" : slot.getStatus().trim().toUpperCase();
        if (!ADMIN_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Status must be AVAILABLE, MAINTENANCE or OUT_OF_SERVICE (BOOKED is set automatically)");
        }
        slot.setStatus(status);

        String floor = slot.getFloor() == null ? null : slot.getFloor().trim();
        slot.setFloor(floor == null || floor.isEmpty() ? null : floor);
    }

    private void ensureUniqueNumber(Long locationId, String number, Long selfId) {
        boolean duplicate = slotRepository.findByParkingLocationId(locationId).stream()
                .anyMatch(s -> s.getSlotNumber() != null
                        && s.getSlotNumber().equalsIgnoreCase(number)
                        && !s.getId().equals(selfId));
        if (duplicate) {
            throw new ConflictException("Slot number " + number + " already exists in this location");
        }
    }

    private long activeBookingCount(Long slotId) {
        return bookingRepository.findByParkingSlotId(slotId).stream()
                .filter(b -> BookingService.ACTIVE_STATUSES.contains(b.getStatus()))
                .count();
    }

    private Set<Long> occupiedSlotIds() {
        Set<Long> ids = new HashSet<>();
        for (ParkingSession session : sessionRepository.findByStatus("ACTIVE")) {
            if (session.getBooking() != null && session.getBooking().getParkingSlot() != null) {
                ids.add(session.getBooking().getParkingSlot().getId());
            }
        }
        return ids;
    }
}
