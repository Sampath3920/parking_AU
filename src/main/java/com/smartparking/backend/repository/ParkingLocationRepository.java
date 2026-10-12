package com.smartparking.backend.repository;

import com.smartparking.backend.model.ParkingLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingLocationRepository
        extends JpaRepository<ParkingLocation, Long> {

    List<ParkingLocation> findByStatus(String status);
}