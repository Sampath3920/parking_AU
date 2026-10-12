package com.smartparking.backend.controller;

import com.smartparking.backend.model.ParkingAlert;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.ParkingAlertRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/parking-alerts")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingAlertController {

    private final ParkingAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public ParkingAlertController(
            ParkingAlertRepository alertRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @PostMapping("/create")
    public ParkingAlert createAlert(
            @RequestParam Long userId,
            @RequestParam Long vehicleId,
            @RequestBody ParkingAlert alert) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        alert.setUser(user);
        alert.setVehicle(vehicle);
        alert.setCreatedAt(LocalDateTime.now());

        if (alert.getStatus() == null) {
            alert.setStatus("OPEN");
        }

        return alertRepository.save(alert);
    }

    @GetMapping
    public List<ParkingAlert> getAllAlerts() {
        return alertRepository.findAll();
    }

    @GetMapping("/{id}")
    public ParkingAlert getAlert(@PathVariable Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking alert not found"));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<ParkingAlert> getVehicleAlerts(
            @PathVariable Long vehicleId) {
        return alertRepository.findByVehicleId(vehicleId);
    }

    @GetMapping("/user/{userId}")
    public List<ParkingAlert> getUserAlerts(
            @PathVariable Long userId) {
        return alertRepository.findByUserId(userId);
    }

    @GetMapping("/status/{status}")
    public List<ParkingAlert> getAlertsByStatus(
            @PathVariable String status) {
        return alertRepository.findByStatus(status);
    }

    @GetMapping("/severity/{severity}")
    public List<ParkingAlert> getAlertsBySeverity(
            @PathVariable String severity) {
        return alertRepository.findBySeverity(severity);
    }

    @PutMapping("/{id}/status")
    public ParkingAlert updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        ParkingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking alert not found"));

        if (!status.equals("OPEN")
                && !status.equals("RESOLVED")) {
            throw new RuntimeException("Invalid alert status");
        }

        alert.setStatus(status);

        return alertRepository.save(alert);
    }

    @PutMapping("/{id}")
    public ParkingAlert updateAlert(
            @PathVariable Long id,
            @RequestBody ParkingAlert updatedAlert) {

        ParkingAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking alert not found"));

        alert.setAlertType(updatedAlert.getAlertType());
        alert.setMessage(updatedAlert.getMessage());
        alert.setSeverity(updatedAlert.getSeverity());

        return alertRepository.save(alert);
    }

    @DeleteMapping("/{id}")
    public String deleteAlert(@PathVariable Long id) {

        alertRepository.deleteById(id);

        return "Parking alert deleted successfully";
    }
}