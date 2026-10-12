package com.smartparking.backend.controller;

import com.smartparking.backend.dto.QRScanRequest;
import com.smartparking.backend.dto.QRScanResponse;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.repository.BookingRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr")
@CrossOrigin(origins = "http://localhost:5173")
public class QRScanController {

    private final BookingRepository bookingRepository;

    public QRScanController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /** Guards and admins only (see SecurityConfig). Checks the QR text and returns the booking. */
    @PostMapping("/scan")
    public QRScanResponse scanQR(@RequestBody QRScanRequest request) {

        String qrData = request.getQrData() == null ? "" : request.getQrData().trim();

        if (!qrData.startsWith("PARKING:") || qrData.length() <= "PARKING:".length()) {
            throw new IllegalArgumentException("Invalid QR code");
        }

        String token = qrData.substring("PARKING:".length());

        Booking booking = bookingRepository.findByQrToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));

        if (!"BOOKED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "This QR code is no longer valid (booking is " + booking.getStatus() + ")");
        }

        return new QRScanResponse(
                booking.getId(),
                "QR code verified for vehicle "
                        + booking.getVehicle().getVehicleNumber()
                        + ", slot "
                        + booking.getParkingSlot().getSlotNumber(),
                booking.getStatus()
        );
    }
}
