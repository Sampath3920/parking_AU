package com.smartparking.backend.controller;

import com.smartparking.backend.model.ParkingPermission;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.ParkingPermissionRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/parking-permissions")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingPermissionController {

    private final ParkingPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public ParkingPermissionController(
            ParkingPermissionRepository permissionRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository) {

        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @PostMapping("/create")
    public ParkingPermission createPermission(
            @RequestParam Long userId,
            @RequestParam Long vehicleId,
            @RequestBody ParkingPermission permission) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new RuntimeException("Vehicle not found"));

        permission.setUser(user);
        permission.setVehicle(vehicle);
        permission.setCreatedAt(LocalDateTime.now());

        if (permission.getStatus() == null) {
            permission.setStatus("PENDING");
        }

        return permissionRepository.save(permission);
    }

    @GetMapping
    public List<ParkingPermission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<ParkingPermission> getUserPermissions(
            @PathVariable Long userId) {

        return permissionRepository.findByUserId(userId);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<ParkingPermission> getVehiclePermissions(
            @PathVariable Long vehicleId) {

        return permissionRepository.findByVehicleId(vehicleId);
    }

    @GetMapping("/status/{status}")
    public List<ParkingPermission> getPermissionsByStatus(
            @PathVariable String status) {

        return permissionRepository.findByStatus(status);
    }

    @GetMapping("/{id}")
    public ParkingPermission getPermission(
            @PathVariable Long id) {

        return permissionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Parking permission not found"));
    }

    @PutMapping("/{id}/status")
    public ParkingPermission updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        ParkingPermission permission =
                permissionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Parking permission not found"));

        if (!status.equals("PENDING")
                && !status.equals("APPROVED")
                && !status.equals("REJECTED")) {

            throw new RuntimeException(
                    "Invalid permission status");
        }

        permission.setStatus(status);

        return permissionRepository.save(permission);
    }

    @PutMapping("/{id}")
    public ParkingPermission updatePermission(
            @PathVariable Long id,
            @RequestBody ParkingPermission updatedPermission) {

        ParkingPermission permission =
                permissionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Parking permission not found"));

        permission.setPermissionType(
                updatedPermission.getPermissionType());

        permission.setReason(
                updatedPermission.getReason());

        permission.setValidFrom(
                updatedPermission.getValidFrom());

        permission.setValidTo(
                updatedPermission.getValidTo());

        return permissionRepository.save(permission);
    }

    @DeleteMapping("/{id}")
    public String deletePermission(
            @PathVariable Long id) {

        permissionRepository.deleteById(id);

        return "Parking permission deleted successfully";
    }
}