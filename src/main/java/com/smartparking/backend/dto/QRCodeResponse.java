package com.smartparking.backend.dto;

public class QRCodeResponse {

    private Long bookingId;
    private String qrCode;
    private String message;

    public QRCodeResponse(Long bookingId, String qrCode, String message) {
        this.bookingId = bookingId;
        this.qrCode = qrCode;
        this.message = message;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getMessage() {
        return message;
    }
}