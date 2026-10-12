package com.smartparking.backend.repository;

import com.smartparking.backend.model.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository
        extends JpaRepository<ParkingSession, Long> {

    Optional<ParkingSession> findByBookingId(Long bookingId);

    List<ParkingSession> findByStatus(String status);
}