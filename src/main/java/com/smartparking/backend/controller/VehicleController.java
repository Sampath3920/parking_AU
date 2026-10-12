package com.smartparking.backend.controller;

import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Map;

/**
 * Ownership ("you can only edit your own vehicle") is checked in OwnershipInterceptor.
 * Only guards/admins may verify vehicles (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "http://localhost:5173")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleController(VehicleRepository vehicleRepository,
                             UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/add/{userId}")
    @Transactional
    public Vehicle addVehicle(
            @PathVariable Long userId,
            @RequestBody Vehicle vehicle) {

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (vehicleRepository.findByUserId(userId).size() >= 3) {
            throw new ConflictException("Each user may register up to three vehicles");
        }

        String number = cleanNumber(vehicle.getVehicleNumber());
        if (vehicleRepository.existsByVehicleNumberIgnoreCase(number)) {
            throw new ConflictException("This vehicle number is already registered");
        }

        vehicle.setVehicleNumber(number);
        vehicle.setUser(user);
        // Nobody can mark their own vehicle as verified.
        vehicle.setVerificationStatus("PENDING");
        vehicle.setSubmittedAt(LocalDateTime.now());

        return vehicleRepository.save(vehicle);
    }

    @GetMapping
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<Vehicle> getUserVehicles(@PathVariable Long userId) {
        return vehicleRepository.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public Vehicle getVehicle(@PathVariable Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
    }

    @PutMapping("/{id}")
    public Vehicle updateVehicle(
            @PathVariable Long id,
            @RequestBody Vehicle updatedVehicle) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        String number = cleanNumber(updatedVehicle.getVehicleNumber());
        boolean changed = !number.equalsIgnoreCase(vehicle.getVehicleNumber());
        if (changed && vehicleRepository.existsByVehicleNumberIgnoreCase(number)) {
            throw new ConflictException("This vehicle number is already registered");
        }

        boolean typeChanged = !Objects.equals(
                vehicle.getVehicleType(), updatedVehicle.getVehicleType());

        vehicle.setVehicleNumber(number);
        vehicle.setVehicleType(updatedVehicle.getVehicleType());
        vehicle.setVehicleModel(updatedVehicle.getVehicleModel());

        // A changed number or type must be approved again.
        if (changed || typeChanged) {
            vehicle.setVerificationStatus("PENDING");
            vehicle.setReviewReason(null);
            vehicle.setSubmittedAt(LocalDateTime.now());
        }

        return vehicleRepository.save(vehicle);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteVehicle(@PathVariable Long id) {
        vehicleRepository.deleteById(id);
        return Map.of("message", "Vehicle deleted successfully");
    }

    private String cleanNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Vehicle number is required");
        }
        return value.trim().toUpperCase();
    }
}
