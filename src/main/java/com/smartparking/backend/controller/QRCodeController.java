package com.smartparking.backend.controller;

import com.smartparking.backend.dto.QRCodeResponse;
import com.smartparking.backend.model.Booking;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.service.QRCodeService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/qr")
@CrossOrigin(origins = "http://localhost:5173")
public class QRCodeController {

    private final BookingRepository bookingRepository;
    private final QRCodeService qrCodeService;

    public QRCodeController(
            BookingRepository bookingRepository,
            QRCodeService qrCodeService) {
        this.bookingRepository = bookingRepository;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/generate/{bookingId}")
    public QRCodeResponse generateQR(@PathVariable Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!"BOOKED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                    "QR code is only available for active bookings (this booking is "
                            + booking.getStatus() + ")");
        }

        // Older bookings made before this change have no token yet.
        if (booking.getQrToken() == null) {
            booking.setQrToken(UUID.randomUUID().toString());
            booking = bookingRepository.save(booking);
        }

        String qrData = "PARKING:" + booking.getQrToken();
        String qrCode = qrCodeService.generateQRCode(qrData);

        return new QRCodeResponse(
                booking.getId(),
                qrCode,
                "QR code generated successfully"
        );
    }
}
