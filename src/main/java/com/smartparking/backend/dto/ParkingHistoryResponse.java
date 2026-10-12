package com.smartparking.backend.dto;

import java.time.LocalDateTime;

public class ParkingHistoryResponse {

    private Long bookingId;
    private String vehicleNumber;
    private String slotNumber;
    private LocalDateTime bookingTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String decisionReason;
    private String locationName;
    private Double paymentAmount;
    private String paymentStatus;

    public ParkingHistoryResponse(
            Long bookingId,
            String vehicleNumber,
            String slotNumber,
            LocalDateTime bookingTime,
            LocalDateTime checkInTime,
            LocalDateTime checkOutTime,
            String status) {

        this.bookingId = bookingId;
        this.vehicleNumber = vehicleNumber;
        this.slotNumber = slotNumber;
        this.bookingTime = bookingTime;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Double getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }
}
