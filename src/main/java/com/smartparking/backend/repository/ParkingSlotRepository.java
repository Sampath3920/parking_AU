package com.smartparking.backend.repository;

import com.smartparking.backend.model.ParkingSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingSlotRepository
        extends JpaRepository<ParkingSlot, Long> {

    List<ParkingSlot> findByParkingLocationId(Long parkingLocationId);

    List<ParkingSlot> findByStatus(String status);

    List<ParkingSlot> findByVehicleType(String vehicleType);

    List<ParkingSlot> findByVehicleTypeAndStatus(
            String vehicleType,
            String status);

    boolean existsByParkingLocationIdAndSlotNumberIgnoreCase(Long parkingLocationId, String slotNumber);

    long countByParkingLocationId(Long parkingLocationId);

    long countByParkingLocationIdAndStatus(Long parkingLocationId, String status);

    /** Locks the slot row until the transaction ends, so two people cannot book it together. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ParkingSlot s where s.id = :id")
    Optional<ParkingSlot> findByIdForUpdate(@Param("id") Long id);
}
