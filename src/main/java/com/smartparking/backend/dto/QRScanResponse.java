package com.smartparking.backend.dto;

public class QRScanResponse {

    private Long bookingId;
    private String message;
    private String status;

    public QRScanResponse(Long bookingId, String message, String status) {
        this.bookingId = bookingId;
        this.message = message;
        this.status = status;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }
}