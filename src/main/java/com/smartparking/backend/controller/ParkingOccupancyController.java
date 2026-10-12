package com.smartparking.backend.controller;

import com.smartparking.backend.repository.ParkingSlotRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-occupancy")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingOccupancyController {

    private final ParkingSlotRepository parkingSlotRepository;

    public ParkingOccupancyController(
            ParkingSlotRepository parkingSlotRepository) {

        this.parkingSlotRepository = parkingSlotRepository;
    }

    @GetMapping
    public Map<String, Object> getOccupancy() {

        Map<String, Object> occupancy = new HashMap<>();

        long totalSlots = parkingSlotRepository.count();

        long bookedSlots =
                parkingSlotRepository.findByStatus("BOOKED").size();

        long availableSlots =
                parkingSlotRepository.findByStatus("AVAILABLE").size();

        double occupancyPercentage = 0;

        if (totalSlots > 0) {
            occupancyPercentage =
                    (bookedSlots * 100.0) / totalSlots;
        }

        occupancy.put("totalSlots", totalSlots);
        occupancy.put("availableSlots", availableSlots);
        occupancy.put("bookedSlots", bookedSlots);
        occupancy.put("occupancyPercentage", occupancyPercentage);

        return occupancy;
    }
}