package com.smartparking.backend.controller;

import com.smartparking.backend.model.ParkingLocation;
import com.smartparking.backend.repository.ParkingLocationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-location-analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingLocationAnalyticsController {

    private final ParkingLocationRepository parkingLocationRepository;

    public ParkingLocationAnalyticsController(
            ParkingLocationRepository parkingLocationRepository) {
        this.parkingLocationRepository = parkingLocationRepository;
    }

    @GetMapping
    public List<Map<String, Object>> getLocationAnalytics() {

        List<ParkingLocation> locations =
                parkingLocationRepository.findAll();

        List<Map<String, Object>> result = new ArrayList<>();

        for (ParkingLocation location : locations) {

            Map<String, Object> analytics = new HashMap<>();

            int totalSlots = location.getTotalSlots();
            int availableSlots = location.getAvailableSlots();
            int bookedSlots = totalSlots - availableSlots;

            double occupancyPercentage = 0;

            if (totalSlots > 0) {
                occupancyPercentage =
                        (bookedSlots * 100.0) / totalSlots;
            }

            analytics.put("locationId", location.getId());
            analytics.put("name", location.getName());
            analytics.put("location", location.getLocation());
            analytics.put("status", location.getStatus());
            analytics.put("totalSlots", totalSlots);
            analytics.put("availableSlots", availableSlots);
            analytics.put("bookedSlots", bookedSlots);
            analytics.put("occupancyPercentage", occupancyPercentage);

            result.add(analytics);
        }

        return result;
    }
}