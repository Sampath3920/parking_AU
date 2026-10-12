package com.smartparking.backend.repository;

import com.smartparking.backend.model.ParkingAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingAlertRepository
        extends JpaRepository<ParkingAlert, Long> {

    List<ParkingAlert> findByVehicleId(Long vehicleId);

    List<ParkingAlert> findByUserId(Long userId);

    List<ParkingAlert> findByStatus(String status);

    List<ParkingAlert> findBySeverity(String severity);
}