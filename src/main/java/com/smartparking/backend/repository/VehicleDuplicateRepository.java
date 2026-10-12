package com.smartparking.backend.repository;

import com.smartparking.backend.model.VehicleDuplicate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleDuplicateRepository extends JpaRepository<VehicleDuplicate, Long> {

    List<VehicleDuplicate> findByVehicleId(Long vehicleId);

    List<VehicleDuplicate> findByUserId(Long userId);

    List<VehicleDuplicate> findByStatus(String status);

    List<VehicleDuplicate> findByVehicleNumber(String vehicleNumber);
}