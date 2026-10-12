package com.smartparking.backend.controller;

import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.ParkingSession;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** CSV downloads for the admin (ADMIN only, see SecurityConfig: /api/reports/**). */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:5173")
public class ReportExportController {

    private final BookingRepository bookingRepository;
    private final ParkingSessionRepository sessionRepository;

    public ReportExportController(BookingRepository bookingRepository,
                                  ParkingSessionRepository sessionRepository) {
        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/bookings.csv")
    public ResponseEntity<byte[]> bookingsCsv() {

        StringBuilder csv = new StringBuilder(
                "bookingId,user,vehicle,slot,bookingTime,status\n");

        List<Booking> bookings = bookingRepository.findAll();
        for (Booking b : bookings) {
            csv.append(b.getId()).append(',')
               .append(cell(b.getUser() != null ? b.getUser().getName() : "")).append(',')
               .append(cell(b.getVehicle() != null ? b.getVehicle().getVehicleNumber() : "")).append(',')
               .append(cell(b.getParkingSlot() != null ? b.getParkingSlot().getSlotNumber() : "")).append(',')
               .append(cell(String.valueOf(b.getBookingTime()))).append(',')
               .append(cell(b.getStatus())).append('\n');
        }
        return download(csv.toString(), "bookings.csv");
    }

    @GetMapping("/sessions.csv")
    public ResponseEntity<byte[]> sessionsCsv() {

        StringBuilder csv = new StringBuilder(
                "sessionId,bookingId,checkInTime,checkOutTime,status\n");

        List<ParkingSession> sessions = sessionRepository.findAll();
        for (ParkingSession s : sessions) {
            csv.append(s.getId()).append(',')
               .append(s.getBooking() != null ? s.getBooking().getId() : "").append(',')
               .append(cell(String.valueOf(s.getCheckInTime()))).append(',')
               .append(cell(String.valueOf(s.getCheckOutTime()))).append(',')
               .append(cell(s.getStatus())).append('\n');
        }
        return download(csv.toString(), "sessions.csv");
    }

    private ResponseEntity<byte[]> download(String text, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Wraps text in quotes and stops spreadsheet formula injection (=, +, -, @ at the start). */
    private String cell(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
