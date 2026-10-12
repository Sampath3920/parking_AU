package com.smartparking.backend.dto;

public class AdminDashboardResponse {

    private long totalUsers;
    private long totalVehicles;
    private long totalLocations;
    private long totalSlots;
    private long availableSlots;
    private long bookedSlots;
    private long totalBookings;
    private long activeSessions;
    private double totalRevenue;

    public AdminDashboardResponse(
            long totalUsers,
            long totalVehicles,
            long totalLocations,
            long totalSlots,
            long availableSlots,
            long bookedSlots,
            long totalBookings,
            long activeSessions,
            double totalRevenue) {

        this.totalUsers = totalUsers;
        this.totalVehicles = totalVehicles;
        this.totalLocations = totalLocations;
        this.totalSlots = totalSlots;
        this.availableSlots = availableSlots;
        this.bookedSlots = bookedSlots;
        this.totalBookings = totalBookings;
        this.activeSessions = activeSessions;
        this.totalRevenue = totalRevenue;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalVehicles() {
        return totalVehicles;
    }

    public long getTotalLocations() {
        return totalLocations;
    }

    public long getTotalSlots() {
        return totalSlots;
    }

    public long getAvailableSlots() {
        return availableSlots;
    }

    public long getBookedSlots() {
        return bookedSlots;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public long getActiveSessions() {
        return activeSessions;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }
}