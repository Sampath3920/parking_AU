package com.smartparking.backend.repository;

import com.smartparking.backend.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByUserId(Long userId);

    boolean existsByVehicleNumberIgnoreCase(String vehicleNumber);
}
