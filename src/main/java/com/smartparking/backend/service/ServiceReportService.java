package com.smartparking.backend.service;

import com.smartparking.backend.dto.PageResult;
import com.smartparking.backend.dto.ServiceReportDecisionRequest;
import com.smartparking.backend.dto.ServiceReportRequest;
import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.exception.NotFoundException;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.model.ServiceReport;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;
import com.smartparking.backend.repository.ServiceReportRepository;
import com.smartparking.backend.repository.VehicleRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Complaints and damage reports: submitting, handling by the administrator, notifications and audit trail. */
@Service
public class ServiceReportService {

    public static final String COMPLAINT = "COMPLAINT";
    public static final String DAMAGE = "DAMAGE";

    public static final Set<String> STATUSES = Set.of("SUBMITTED", "IN_PROGRESS", "RESOLVED", "REJECTED");
    private static final Set<String> UNRESOLVED = Set.of("SUBMITTED", "IN_PROGRESS");

    public static final Set<String> COMPLAINT_CATEGORIES =
            Set.of("BOOKING", "PAYMENT", "STAFF", "SAFETY", "OTHER");
    public static final Set<String> DAMAGE_CATEGORIES =
            Set.of("DAMAGED_VEHICLE", "SLOT_DAMAGE", "AREA_ISSUE");

    private final ServiceReportRepository reportRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public ServiceReportService(ServiceReportRepository reportRepository,
                                VehicleRepository vehicleRepository,
                                ParkingSlotRepository slotRepository,
                                ParkingSessionRepository sessionRepository,
                                BookingRepository bookingRepository,
                                NotificationService notificationService,
                                AuditService auditService) {
        this.reportRepository = reportRepository;
        this.vehicleRepository = vehicleRepository;
        this.slotRepository = slotRepository;
        this.sessionRepository = sessionRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ users

    @Transactional
    public ServiceReport submit(User reporter, ServiceReportRequest request) {

        String type = request.type().trim().toUpperCase();
        String category = request.category().trim().toUpperCase();
        if (!COMPLAINT.equals(type) && !DAMAGE.equals(type)) {
            throw new IllegalArgumentException("Type must be COMPLAINT or DAMAGE");
        }
        Set<String> allowed = COMPLAINT.equals(type) ? COMPLAINT_CATEGORIES : DAMAGE_CATEGORIES;
        if (!allowed.contains(category)) {
            throw new IllegalArgumentException("Category must be one of " + allowed);
        }

        String subject = request.subject().trim();
        String description = request.description().trim();
        if (subject.isEmpty() || description.isEmpty()) {
            throw new IllegalArgumentException("Subject and description cannot be empty");
        }
        if (request.incidentDate() != null && request.incidentDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The incident date cannot be in the future");
        }

        Vehicle vehicle = null;
        if (request.vehicleId() != null) {
            vehicle = vehicleRepository.findById(request.vehicleId())
                    .orElseThrow(() -> new NotFoundException("Vehicle not found"));
            if (vehicle.getUser() == null || !reporter.getId().equals(vehicle.getUser().getId())) {
                throw new AccessDeniedException("You can only report your own vehicle");
            }
        }

        ParkingSlot slot = null;
        if (request.slotId() != null) {
            slot = slotRepository.findById(request.slotId())
                    .orElseThrow(() -> new NotFoundException("Parking slot not found"));
        }

        // A damaged vehicle or slot must say which one. A general area issue does not need either.
        if (DAMAGE.equals(type) && !"AREA_ISSUE".equals(category) && vehicle == null && slot == null) {
            throw new IllegalArgumentException("Choose the vehicle or the parking slot that was damaged");
        }

        ServiceReport report = new ServiceReport();
        report.setType(type);
        report.setCategory(category);
        report.setSubject(subject);
        report.setDescription(description);
        report.setIncidentDate(request.incidentDate());
        report.setStatus("SUBMITTED");
        report.setReporter(reporter);
        report.setVehicle(vehicle);
        report.setParkingSlot(slot);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(report.getCreatedAt());
        ServiceReport saved = reportRepository.save(report);

        auditService.log("REPORT_SUBMITTED", "SERVICE_REPORT", saved.getId(), type + ": " + subject);
        notificationService.notifyRole("ADMIN", "REPORT_SUBMITTED",
                label(type) + " #" + saved.getId() + " from " + reporter.getName() + ": " + subject);
        return saved;
    }

    public List<ServiceReport> mine(Long userId, String type) {
        String wanted = type == null ? "" : type.trim().toUpperCase();
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(userId).stream()
                .filter(r -> wanted.isEmpty() || wanted.equals(r.getType()))
                .toList();
    }

    /** One report. Allowed for the person who sent it and for administrators. */
    public ServiceReport getFor(Long id, User viewer, boolean admin) {
        ServiceReport report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));
        boolean mine = report.getReporter() != null && viewer != null
                && report.getReporter().getId().equals(viewer.getId());
        if (!admin && !mine) {
            throw new AccessDeniedException("Not your report");
        }
        return report;
    }

    // ------------------------------------------------------------------ admin

    /** Filters are optional. status may also be UNRESOLVED (= SUBMITTED or IN_PROGRESS). */
    public PageResult<ServiceReport> search(String type, String status, String search, int page, int size) {

        String wantedType = type == null ? "" : type.trim().toUpperCase();
        String wantedStatus = status == null ? "" : status.trim().toUpperCase();
        String text = search == null ? "" : search.trim().toLowerCase();

        List<ServiceReport> matches = reportRepository.findAll().stream()
                .filter(r -> wantedType.isEmpty() || wantedType.equals(r.getType()))
                .filter(r -> wantedStatus.isEmpty()
                        || ("UNRESOLVED".equals(wantedStatus) && UNRESOLVED.contains(r.getStatus()))
                        || wantedStatus.equals(r.getStatus()))
                .filter(r -> text.isEmpty() || matchesText(r, text))
                .sorted(Comparator.comparing(ServiceReport::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return PageResult.of(matches, page, size);
    }

    public Map<String, Object> summary() {
        List<ServiceReport> all = reportRepository.findAll();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("complaintsTotal", count(all, COMPLAINT, false));
        map.put("complaintsUnresolved", count(all, COMPLAINT, true));
        map.put("damageTotal", count(all, DAMAGE, false));
        map.put("damageUnresolved", count(all, DAMAGE, true));
        return map;
    }

    @Transactional
    public ServiceReport update(Long id, ServiceReportDecisionRequest request, User admin) {

        ServiceReport report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        String oldStatus = report.getStatus();
        StringBuilder log = new StringBuilder();
        boolean tellReporter = false;

        if (request.status() != null && !request.status().isBlank()) {
            String newStatus = request.status().trim().toUpperCase();
            if (!STATUSES.contains(newStatus)) {
                throw new IllegalArgumentException("Status must be one of " + STATUSES);
            }
            if (!newStatus.equals(oldStatus)) {
                report.setStatus(newStatus);
                report.setResolvedAt(UNRESOLVED.contains(newStatus) ? null : LocalDateTime.now());
                log.append("status ").append(oldStatus).append(" -> ").append(newStatus).append("; ");
                tellReporter = true;
            }
        }

        if (request.response() != null) {
            String response = request.response().trim();
            String current = report.getAdminResponse() == null ? "" : report.getAdminResponse();
            if (!response.equals(current)) {
                report.setAdminResponse(response.isEmpty() ? null : response);
                log.append("response changed; ");
                tellReporter = true;
            }
        }

        // A rejection or a resolution should always come with an answer for the user.
        boolean closing = "RESOLVED".equals(report.getStatus()) || "REJECTED".equals(report.getStatus());
        if (closing && (report.getAdminResponse() == null || report.getAdminResponse().isBlank())) {
            throw new IllegalArgumentException("Write a response before resolving or rejecting a report");
        }

        if ("ME".equalsIgnoreCase(request.assignment())) {
            report.setAssignedTo(admin);
            log.append("assigned to ").append(admin.getName()).append("; ");
        } else if ("NONE".equalsIgnoreCase(request.assignment())) {
            report.setAssignedTo(null);
            log.append("unassigned; ");
        }

        if (request.slotAction() != null && !request.slotAction().isBlank()) {
            log.append(applySlotAction(report, request.slotAction().trim().toUpperCase())).append("; ");
        }

        if (request.note() != null && !request.note().isBlank()) {
            log.append("note: ").append(request.note().trim());
        }

        if (log.length() == 0) {
            throw new IllegalArgumentException("Nothing to change");
        }

        report.setUpdatedAt(LocalDateTime.now());
        ServiceReport saved = reportRepository.save(report);
        auditService.log("REPORT_UPDATED", "SERVICE_REPORT", id, log.toString().trim());

        if (tellReporter) {
            String text = "Status: " + saved.getStatus().toLowerCase().replace('_', ' ');
            if (saved.getAdminResponse() != null) {
                text += ". " + saved.getAdminResponse();
            }
            notificationService.notifyUser(saved.getReporter(), "REPORT_UPDATE",
                    label(saved.getType()) + " #" + id + " update", text);
        }
        return saved;
    }

    // ------------------------------------------------------------------ helpers

    /** FLAG: nobody can book the slot until it is cleared. CLEAR: back to AVAILABLE (or BOOKED if it still has bookings). */
    private String applySlotAction(ServiceReport report, String action) {
        ParkingSlot slot = report.getParkingSlot();
        if (slot == null) {
            throw new IllegalArgumentException("This report has no parking slot");
        }

        if ("FLAG".equals(action)) {
            for (ParkingSession session : sessionRepository.findByStatus("ACTIVE")) {
                if (session.getBooking() != null && session.getBooking().getParkingSlot() != null
                        && slot.getId().equals(session.getBooking().getParkingSlot().getId())) {
                    throw new ConflictException("A vehicle is parked in this slot now. Flag it after check-out");
                }
            }
            slot.setStatus("MAINTENANCE");
            slotRepository.save(slot);
            return "slot " + slot.getSlotNumber() + " flagged as under maintenance";
        }

        if ("CLEAR".equals(action)) {
            if (!"MAINTENANCE".equals(slot.getStatus())) {
                throw new ConflictException("The slot is not under maintenance");
            }
            boolean hasBookings = bookingRepository.existsByParkingSlotIdAndStatusIn(
                    slot.getId(), BookingService.ACTIVE_STATUSES);
            slot.setStatus(hasBookings ? "BOOKED" : "AVAILABLE");
            slotRepository.save(slot);
            return "slot " + slot.getSlotNumber() + " cleared";
        }

        throw new IllegalArgumentException("slotAction must be FLAG or CLEAR");
    }

    private boolean matchesText(ServiceReport r, String text) {
        return contains(r.getSubject(), text)
                || contains(r.getDescription(), text)
                || (r.getReporter() != null && contains(r.getReporter().getName(), text))
                || (r.getReporter() != null && contains(r.getReporter().getEmail(), text))
                || (r.getId() != null && String.valueOf(r.getId()).equals(text));
    }

    private boolean contains(String value, String text) {
        return value != null && value.toLowerCase().contains(text);
    }

    private long count(List<ServiceReport> all, String type, boolean onlyUnresolved) {
        return all.stream()
                .filter(r -> type.equals(r.getType()))
                .filter(r -> !onlyUnresolved || UNRESOLVED.contains(r.getStatus()))
                .count();
    }

    private String label(String type) {
        return COMPLAINT.equals(type) ? "Complaint" : "Damage report";
    }
}
