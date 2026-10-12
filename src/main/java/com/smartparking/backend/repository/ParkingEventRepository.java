package com.smartparking.backend.repository;

import com.smartparking.backend.model.ParkingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ParkingEventRepository
        extends JpaRepository<ParkingEvent, Long> {

    List<ParkingEvent> findByEventDate(LocalDate eventDate);

    List<ParkingEvent> findByStatus(String status);

    List<ParkingEvent> findByParkingLocationId(Long parkingLocationId);
}