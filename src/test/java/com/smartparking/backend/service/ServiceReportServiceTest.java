package com.smartparking.backend.service;

import com.smartparking.backend.dto.PageResult;
import com.smartparking.backend.dto.ServiceReportDecisionRequest;
import com.smartparking.backend.dto.ServiceReportRequest;
import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.model.Booking;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Plain unit tests: no database and no Spring needed. */
class ServiceReportServiceTest {

    private final ServiceReportRepository reportRepository = mock(ServiceReportRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final ParkingSlotRepository slotRepository = mock(ParkingSlotRepository.class);
    private final ParkingSessionRepository sessionRepository = mock(ParkingSessionRepository.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditService auditService = mock(AuditService.class);

    private final ServiceReportService service = new ServiceReportService(
            reportRepository, vehicleRepository, slotRepository, sessionRepository,
            bookingRepository, notificationService, auditService);

    private User student;
    private User admin;

    @BeforeEach
    void setUp() {
        student = user(1, "Student One", "student@example.com");
        admin = user(2, "Admin One", "admin@example.com");
        when(reportRepository.save(any(ServiceReport.class))).thenAnswer(i -> {
            ServiceReport saved = i.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(10L);
            }
            return saved;
        });
    }

    private User user(long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private ServiceReportRequest complaint(String category) {
        return new ServiceReportRequest("complaint", category, "Gate was closed", "Nobody opened the gate", null, null, null);
    }

    private ParkingSlot slot(long id, String status) {
        ParkingSlot slot = new ParkingSlot();
        slot.setId(id);
        slot.setSlotNumber("A" + id);
        slot.setStatus(status);
        return slot;
    }

    private ServiceReport report(String type, String status, ParkingSlot slot) {
        ServiceReport report = new ServiceReport();
        report.setId(10L);
        report.setType(type);
        report.setStatus(status);
        report.setReporter(student);
        report.setParkingSlot(slot);
        report.setSubject("Subject");
        return report;
    }

    // ---------------------------------------------------------------- submit

    @Test
    void complaintIsSavedAsSubmittedAndAdminsAreNotified() {
        ServiceReport saved = service.submit(student, complaint("booking"));

        assertEquals("COMPLAINT", saved.getType());
        assertEquals("BOOKING", saved.getCategory());
        assertEquals("SUBMITTED", saved.getStatus());
        assertEquals(student, saved.getReporter());
        verify(auditService).log(eq("REPORT_SUBMITTED"), eq("SERVICE_REPORT"), eq(10L), anyString());
        verify(notificationService).notifyRole(eq("ADMIN"), eq("REPORT_SUBMITTED"), contains("Gate was closed"));
    }

    @Test
    void categoryMustMatchTheType() {
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(student, complaint("DAMAGED_VEHICLE")));
        verify(reportRepository, never()).save(any());
    }

    @Test
    void unknownTypeIsRejected() {
        ServiceReportRequest request = new ServiceReportRequest("OTHER", "X", "S", "D", null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.submit(student, request));
    }

    @Test
    void incidentDateCannotBeInTheFuture() {
        ServiceReportRequest request = new ServiceReportRequest(
                "COMPLAINT", "OTHER", "S", "D", LocalDate.now().plusDays(1), null, null);
        assertThrows(IllegalArgumentException.class, () -> service.submit(student, request));
    }

    @Test
    void damageReportNeedsAVehicleOrASlot() {
        ServiceReportRequest request = new ServiceReportRequest(
                "DAMAGE", "SLOT_DAMAGE", "Crack", "Big crack", null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.submit(student, request));
    }

    @Test
    void areaIssueNeedsNeitherVehicleNorSlot() {
        ServiceReportRequest request = new ServiceReportRequest(
                "DAMAGE", "AREA_ISSUE", "Light broken", "Street light is off", null, null, null);

        ServiceReport saved = service.submit(student, request);

        assertEquals("DAMAGE", saved.getType());
    }

    @Test
    void reportingSomeoneElsesVehicleIsForbidden() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(5L);
        vehicle.setUser(user(99, "Other", "other@example.com"));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));

        ServiceReportRequest request = new ServiceReportRequest(
                "DAMAGE", "DAMAGED_VEHICLE", "Scratch", "Scratched door", null, 5L, null);

        assertThrows(AccessDeniedException.class, () -> service.submit(student, request));
    }

    @Test
    void ownVehicleAndSlotAreAccepted() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(5L);
        vehicle.setUser(student);
        ParkingSlot slot = slot(3, "AVAILABLE");
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(slotRepository.findById(3L)).thenReturn(Optional.of(slot));

        ServiceReport saved = service.submit(student, new ServiceReportRequest(
                "DAMAGE", "DAMAGED_VEHICLE", "Scratch", "Scratched door", LocalDate.now(), 5L, 3L));

        assertEquals(vehicle, saved.getVehicle());
        assertEquals(slot, saved.getParkingSlot());
    }

    // ---------------------------------------------------------------- reading

    @Test
    void anotherUserCannotOpenMyReport() {
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report("COMPLAINT", "SUBMITTED", null)));

        assertThrows(AccessDeniedException.class,
                () -> service.getFor(10L, user(77, "Other", "other@example.com"), false));
    }

    @Test
    void ownerAndAdminCanOpenTheReport() {
        ServiceReport report = report("COMPLAINT", "SUBMITTED", null);
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        assertSame(report, service.getFor(10L, student, false));
        assertSame(report, service.getFor(10L, admin, true));
    }

    @Test
    void searchCanShowOnlyUnresolvedReports() {
        when(reportRepository.findAll()).thenReturn(List.of(
                report("COMPLAINT", "SUBMITTED", null),
                report("COMPLAINT", "IN_PROGRESS", null),
                report("COMPLAINT", "RESOLVED", null),
                report("DAMAGE", "SUBMITTED", null)));

        PageResult<ServiceReport> result = service.search("COMPLAINT", "UNRESOLVED", null, 0, 10);

        assertEquals(2, result.totalElements());
    }

    @Test
    void summaryCountsUnresolvedPerType() {
        when(reportRepository.findAll()).thenReturn(List.of(
                report("COMPLAINT", "SUBMITTED", null),
                report("COMPLAINT", "RESOLVED", null),
                report("DAMAGE", "IN_PROGRESS", null)));

        var summary = service.summary();

        assertEquals(2L, summary.get("complaintsTotal"));
        assertEquals(1L, summary.get("complaintsUnresolved"));
        assertEquals(1L, summary.get("damageTotal"));
        assertEquals(1L, summary.get("damageUnresolved"));
    }

    // ---------------------------------------------------------------- admin update

    @Test
    void resolvingNeedsAResponse() {
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report("COMPLAINT", "SUBMITTED", null)));

        assertThrows(IllegalArgumentException.class, () -> service.update(10L,
                new ServiceReportDecisionRequest("RESOLVED", null, null, null, null), admin));
    }

    @Test
    void unknownStatusIsRejected() {
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report("COMPLAINT", "SUBMITTED", null)));

        assertThrows(IllegalArgumentException.class, () -> service.update(10L,
                new ServiceReportDecisionRequest("DONE", null, null, null, null), admin));
    }

    @Test
    void emptyUpdateIsRejected() {
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report("COMPLAINT", "SUBMITTED", null)));

        assertThrows(IllegalArgumentException.class, () -> service.update(10L,
                new ServiceReportDecisionRequest(null, null, null, null, null), admin));
    }

    @Test
    void resolvingWithAResponseNotifiesTheReporterAndIsAudited() {
        ServiceReport report = report("COMPLAINT", "IN_PROGRESS", null);
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        ServiceReport result = service.update(10L,
                new ServiceReportDecisionRequest("resolved", "The gate was repaired", "ME", "checked with guard", null), admin);

        assertEquals("RESOLVED", result.getStatus());
        assertNotNull(result.getResolvedAt());
        assertEquals("Admin One", result.getAssignedToName());
        verify(notificationService).notifyUser(eq(student), eq("REPORT_UPDATE"), contains("#10"), contains("gate was repaired"));
        verify(auditService).log(eq("REPORT_UPDATED"), eq("SERVICE_REPORT"), eq(10L), contains("IN_PROGRESS -> RESOLVED"));
    }

    @Test
    void noteOnlyDoesNotBotherTheReporter() {
        ServiceReport report = report("COMPLAINT", "SUBMITTED", null);
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        service.update(10L, new ServiceReportDecisionRequest(null, null, null, "called the guard", null), admin);

        verify(notificationService, never()).notifyUser(any(), anyString(), anyString(), anyString());
        verify(auditService).log(eq("REPORT_UPDATED"), eq("SERVICE_REPORT"), eq(10L), contains("called the guard"));
    }

    // ---------------------------------------------------------------- slot actions

    @Test
    void flaggingASlotMakesItUnderMaintenance() {
        ParkingSlot slot = slot(3, "AVAILABLE");
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report("DAMAGE", "SUBMITTED", slot)));
        when(sessionRepository.findByStatus("ACTIVE")).thenReturn(List.of());

        service.update(10L, new ServiceReportDecisionRequest("IN_PROGRESS", null, null, null, "flag"), admin);

        assertEquals("MAINTENANCE", slot.getStatus());
        verify(slotRepository).save(slot);
    }

    @Test
    void aSlotWithAParkedVehicleCannotBeFlagged() {
        ParkingSlot slot = slot(3, "BOOKED");
        Booking booking = new Booking();
        booking.setParkingSlot(slot);
        ParkingSession session = new ParkingSession();
        session.setBooking(booking);
        session.setStatus("ACTIVE");
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report("DAMAGE", "SUBMITTED", slot)));
        when(sessionRepository.findByStatus("ACTIVE")).thenReturn(List.of(session));

        assertThrows(ConflictException.class, () -> service.update(10L,
                new ServiceReportDecisionRequest(null, null, null, null, "FLAG"), admin));
        verify(slotRepository, never()).save(any());
    }

    @Test
    void clearingGivesTheSlotBackAsAvailableWhenNoBookingsExist() {
        ParkingSlot slot = slot(3, "MAINTENANCE");
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report("DAMAGE", "IN_PROGRESS", slot)));
        when(bookingRepository.existsByParkingSlotIdAndStatusIn(eq(3L), any())).thenReturn(false);

        service.update(10L, new ServiceReportDecisionRequest(null, null, null, null, "CLEAR"), admin);

        assertEquals("AVAILABLE", slot.getStatus());
    }

    @Test
    void clearingKeepsTheSlotBookedWhenBookingsStillExist() {
        ParkingSlot slot = slot(3, "MAINTENANCE");
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report("DAMAGE", "IN_PROGRESS", slot)));
        when(bookingRepository.existsByParkingSlotIdAndStatusIn(eq(3L), any())).thenReturn(true);

        service.update(10L, new ServiceReportDecisionRequest(null, null, null, null, "CLEAR"), admin);

        assertEquals("BOOKED", slot.getStatus());
    }

    @Test
    void slotActionNeedsASlotOnTheReport() {
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report("COMPLAINT", "SUBMITTED", null)));

        assertThrows(IllegalArgumentException.class, () -> service.update(10L,
                new ServiceReportDecisionRequest(null, null, null, null, "FLAG"), admin));
    }
}
