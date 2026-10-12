package com.smartparking.backend.controller;

import com.smartparking.backend.repository.VehicleRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-vehicle-analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingVehicleAnalyticsController {

    private final VehicleRepository vehicleRepository;

    public ParkingVehicleAnalyticsController(
            VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping
    public Map<String, Object> getVehicleAnalytics() {

        Map<String, Object> analytics = new HashMap<>();

        long totalVehicles = vehicleRepository.count();

        long twoWheelers = vehicleRepository.findAll()
                .stream()
                .filter(v -> "2-WHEELER".equals(v.getVehicleType()))
                .count();

        long fourWheelers = vehicleRepository.findAll()
                .stream()
                .filter(v -> "4-WHEELER".equals(v.getVehicleType()))
                .count();

        long verifiedVehicles = vehicleRepository.findAll()
                .stream()
                .filter(v -> "VERIFIED".equals(v.getVerificationStatus()))
                .count();

        analytics.put("totalVehicles", totalVehicles);
        analytics.put("twoWheelers", twoWheelers);
        analytics.put("fourWheelers", fourWheelers);
        analytics.put("verifiedVehicles", verifiedVehicles);

        return analytics;
    }
}