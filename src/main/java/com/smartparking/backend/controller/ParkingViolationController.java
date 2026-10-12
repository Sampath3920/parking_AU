package com.smartparking.backend.controller;

import com.smartparking.backend.model.ParkingViolation;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.ParkingViolationRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/parking-violations")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingViolationController {

    private final ParkingViolationRepository violationRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public ParkingViolationController(
            ParkingViolationRepository violationRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository) {
        this.violationRepository = violationRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @PostMapping("/create")
    public ParkingViolation createViolation(
            @RequestParam Long userId,
            @RequestParam Long vehicleId,
            @RequestBody ParkingViolation violation) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        violation.setUser(user);
        violation.setVehicle(vehicle);
        violation.setCreatedAt(LocalDateTime.now());

        if (violation.getStatus() == null) {
            violation.setStatus("OPEN");
        }

        return violationRepository.save(violation);
    }

    @GetMapping
    public List<ParkingViolation> getAllViolations() {
        return violationRepository.findAll();
    }

    @GetMapping("/{id}")
    public ParkingViolation getViolation(@PathVariable Long id) {
        return violationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking violation not found"));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<ParkingViolation> getVehicleViolations(
            @PathVariable Long vehicleId) {
        return violationRepository.findByVehicleId(vehicleId);
    }

    @GetMapping("/user/{userId}")
    public List<ParkingViolation> getUserViolations(
            @PathVariable Long userId) {
        return violationRepository.findByUserId(userId);
    }

    @GetMapping("/status/{status}")
    public List<ParkingViolation> getViolationsByStatus(
            @PathVariable String status) {
        return violationRepository.findByStatus(status);
    }

    @GetMapping("/type/{violationType}")
    public List<ParkingViolation> getViolationsByType(
            @PathVariable String violationType) {
        return violationRepository.findByViolationType(violationType);
    }

    @PutMapping("/{id}/status")
    public ParkingViolation updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        ParkingViolation violation = violationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking violation not found"));

        if (!status.equals("OPEN")
                && !status.equals("PAID")
                && !status.equals("CANCELLED")) {
            throw new RuntimeException("Invalid violation status");
        }

        violation.setStatus(status);

        return violationRepository.save(violation);
    }

    @PutMapping("/{id}")
    public ParkingViolation updateViolation(
            @PathVariable Long id,
            @RequestBody ParkingViolation updatedViolation) {

        ParkingViolation violation = violationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking violation not found"));

        violation.setViolationType(updatedViolation.getViolationType());
        violation.setDescription(updatedViolation.getDescription());
        violation.setFineAmount(updatedViolation.getFineAmount());

        return violationRepository.save(violation);
    }

    @DeleteMapping("/{id}")
    public String deleteViolation(@PathVariable Long id) {
        violationRepository.deleteById(id);
        return "Parking violation deleted successfully";
    }
}