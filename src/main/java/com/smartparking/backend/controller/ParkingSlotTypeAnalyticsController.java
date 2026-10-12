package com.smartparking.backend.controller;

import com.smartparking.backend.repository.ParkingSlotRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-slot-types")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingSlotTypeAnalyticsController {

    private final ParkingSlotRepository parkingSlotRepository;

    public ParkingSlotTypeAnalyticsController(
            ParkingSlotRepository parkingSlotRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
    }

    @GetMapping
    public Map<String, Object> getSlotTypeAnalytics() {

        Map<String, Object> analytics = new HashMap<>();

        int twoWheelerTotal =
                parkingSlotRepository.findByVehicleType("2-WHEELER").size();

        int twoWheelerAvailable =
                parkingSlotRepository
                        .findByVehicleTypeAndStatus("2-WHEELER", "AVAILABLE")
                        .size();

        int twoWheelerBooked =
                parkingSlotRepository
                        .findByVehicleTypeAndStatus("2-WHEELER", "BOOKED")
                        .size();

        int fourWheelerTotal =
                parkingSlotRepository.findByVehicleType("4-WHEELER").size();

        int fourWheelerAvailable =
                parkingSlotRepository
                        .findByVehicleTypeAndStatus("4-WHEELER", "AVAILABLE")
                        .size();

        int fourWheelerBooked =
                parkingSlotRepository
                        .findByVehicleTypeAndStatus("4-WHEELER", "BOOKED")
                        .size();

        analytics.put("twoWheelerTotal", twoWheelerTotal);
        analytics.put("twoWheelerAvailable", twoWheelerAvailable);
        analytics.put("twoWheelerBooked", twoWheelerBooked);

        analytics.put("fourWheelerTotal", fourWheelerTotal);
        analytics.put("fourWheelerAvailable", fourWheelerAvailable);
        analytics.put("fourWheelerBooked", fourWheelerBooked);

        return analytics;
    }
}