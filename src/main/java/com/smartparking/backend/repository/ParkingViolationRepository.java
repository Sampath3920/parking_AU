package com.smartparking.backend.repository;

import com.smartparking.backend.model.ParkingViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParkingViolationRepository extends JpaRepository<ParkingViolation, Long> {

    List<ParkingViolation> findByVehicleId(Long vehicleId);

    List<ParkingViolation> findByUserId(Long userId);

    List<ParkingViolation> findByStatus(String status);

    List<ParkingViolation> findByViolationType(String violationType);
}