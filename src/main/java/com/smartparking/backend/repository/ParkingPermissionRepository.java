package com.smartparking.backend.repository;

import com.smartparking.backend.model.ParkingPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingPermissionRepository
        extends JpaRepository<ParkingPermission, Long> {

    List<ParkingPermission> findByUserId(Long userId);

    List<ParkingPermission> findByVehicleId(Long vehicleId);

    List<ParkingPermission> findByStatus(String status);
}