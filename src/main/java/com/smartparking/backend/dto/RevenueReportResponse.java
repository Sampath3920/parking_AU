package com.smartparking.backend.dto;

public class RevenueReportResponse {

    private double totalRevenue;
    private long totalPayments;

    public RevenueReportResponse(
            double totalRevenue,
            long totalPayments) {

        this.totalRevenue = totalRevenue;
        this.totalPayments = totalPayments;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public long getTotalPayments() {
        return totalPayments;
    }
}