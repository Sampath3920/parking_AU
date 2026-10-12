package com.smartparking.backend.controller;

import com.smartparking.backend.model.ParkingLocation;
import com.smartparking.backend.repository.ParkingLocationRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-locations")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingLocationController {

    private final ParkingLocationRepository repository;
    private final ParkingSlotRepository slotRepository;

    public ParkingLocationController(ParkingLocationRepository repository,
                                     ParkingSlotRepository slotRepository) {
        this.repository = repository;
        this.slotRepository = slotRepository;
    }

    @PostMapping
    public ParkingLocation addLocation(
            @RequestBody ParkingLocation parkingLocation) {

        if (parkingLocation.getName() == null || parkingLocation.getName().isBlank()) {
            throw new IllegalArgumentException("Location name is required");
        }
        return repository.save(parkingLocation);
    }

    /** Optional search: /api/parking-locations?search=main */
    @GetMapping
    public List<ParkingLocation> getLocations(
            @RequestParam(required = false) String search) {

        List<ParkingLocation> locations = repository.findAll();

        if (search != null && !search.isBlank()) {
            String text = search.trim().toLowerCase();
            locations = locations.stream()
                    .filter(l -> contains(l.getName(), text) || contains(l.getLocation(), text))
                    .toList();
        }

        locations.forEach(this::fillLiveCounts);
        return locations;
    }

    @GetMapping("/{id}")
    public ParkingLocation getLocation(@PathVariable Long id) {
        ParkingLocation location = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking location not found"));
        fillLiveCounts(location);
        return location;
    }

    @PutMapping("/{id}")
    public ParkingLocation updateLocation(
            @PathVariable Long id,
            @RequestBody ParkingLocation updatedLocation) {

        ParkingLocation location = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking location not found"));

        location.setName(updatedLocation.getName());
        location.setLocation(updatedLocation.getLocation());
        location.setTotalSlots(updatedLocation.getTotalSlots());
        location.setAvailableSlots(updatedLocation.getAvailableSlots());
        location.setStatus(updatedLocation.getStatus());

        return repository.save(location);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteLocation(@PathVariable Long id) {

        if (slotRepository.countByParkingLocationId(id) > 0) {
            throw new IllegalArgumentException(
                    "Delete or move the parking slots of this location first");
        }
        repository.deleteById(id);
        return Map.of("message", "Parking location deleted successfully");
    }

    /** Counts come from the real slot records, not from a number typed in by hand. */
    private void fillLiveCounts(ParkingLocation location) {
        location.setTotalSlots((int) slotRepository.countByParkingLocationId(location.getId()));
        location.setAvailableSlots((int) slotRepository
                .countByParkingLocationIdAndStatus(location.getId(), "AVAILABLE"));
    }

    private boolean contains(String value, String text) {
        return value != null && value.toLowerCase().contains(text);
    }
}
