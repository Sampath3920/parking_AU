package com.smartparking.backend.repository;

import com.smartparking.backend.model.ParkingPass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingPassRepository
        extends JpaRepository<ParkingPass, Long> {

    List<ParkingPass> findByUserId(Long userId);

    List<ParkingPass> findByVehicleId(Long vehicleId);
}