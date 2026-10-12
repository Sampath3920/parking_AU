package com.smartparking.backend.controller;

import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.model.VehicleDuplicate;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleDuplicateRepository;
import com.smartparking.backend.repository.VehicleRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vehicle-duplicates")
@CrossOrigin(origins = "http://localhost:5173")
public class VehicleDuplicateController {

    private final VehicleDuplicateRepository duplicateRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleDuplicateController(
            VehicleDuplicateRepository duplicateRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository) {
        this.duplicateRepository = duplicateRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @PostMapping("/create")
    public VehicleDuplicate createDuplicate(
            @RequestParam Long userId,
            @RequestParam Long vehicleId,
            @RequestBody VehicleDuplicate duplicate) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        duplicate.setUser(user);
        duplicate.setVehicle(vehicle);
        duplicate.setDetectedAt(LocalDateTime.now());

        if (duplicate.getVehicleNumber() == null) {
            duplicate.setVehicleNumber(vehicle.getVehicleNumber());
        }

        if (duplicate.getStatus() == null) {
            duplicate.setStatus("OPEN");
        }

        return duplicateRepository.save(duplicate);
    }

    @GetMapping
    public List<VehicleDuplicate> getAllDuplicates() {
        return duplicateRepository.findAll();
    }

    @GetMapping("/{id}")
    public VehicleDuplicate getDuplicate(@PathVariable Long id) {
        return duplicateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Duplicate record not found"));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<VehicleDuplicate> getByVehicle(
            @PathVariable Long vehicleId) {
        return duplicateRepository.findByVehicleId(vehicleId);
    }

    @GetMapping("/user/{userId}")
    public List<VehicleDuplicate> getByUser(
            @PathVariable Long userId) {
        return duplicateRepository.findByUserId(userId);
    }

    @GetMapping("/status/{status}")
    public List<VehicleDuplicate> getByStatus(
            @PathVariable String status) {
        return duplicateRepository.findByStatus(status);
    }

    @GetMapping("/number/{vehicleNumber}")
    public List<VehicleDuplicate> getByVehicleNumber(
            @PathVariable String vehicleNumber) {
        return duplicateRepository.findByVehicleNumber(vehicleNumber);
    }

    @PutMapping("/{id}/status")
    public VehicleDuplicate updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        VehicleDuplicate duplicate = duplicateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Duplicate record not found"));

        if (!status.equals("OPEN") && !status.equals("RESOLVED")) {
            throw new RuntimeException("Invalid duplicate status");
        }

        duplicate.setStatus(status);

        return duplicateRepository.save(duplicate);
    }

    @PutMapping("/{id}")
    public VehicleDuplicate updateDuplicate(
            @PathVariable Long id,
            @RequestBody VehicleDuplicate updatedDuplicate) {

        VehicleDuplicate duplicate = duplicateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Duplicate record not found"));

        duplicate.setVehicleNumber(updatedDuplicate.getVehicleNumber());
        duplicate.setReason(updatedDuplicate.getReason());

        return duplicateRepository.save(duplicate);
    }

    @DeleteMapping("/{id}")
    public String deleteDuplicate(@PathVariable Long id) {
        duplicateRepository.deleteById(id);
        return "Vehicle duplicate record deleted successfully";
    }
}