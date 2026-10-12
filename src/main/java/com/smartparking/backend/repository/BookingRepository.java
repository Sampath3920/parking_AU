package com.smartparking.backend.repository;

import com.smartparking.backend.model.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Bookings with this status that start inside a time window (used for reminders)
    List<Booking> findByStatusAndStartTimeBetween(String status, LocalDateTime from, LocalDateTime to);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByParkingSlotId(Long parkingSlotId);

    List<Booking> findByStatus(String status);

    boolean existsByVehicleIdAndStatus(Long vehicleId, String status);

    boolean existsByVehicleIdAndStatusIn(Long vehicleId, Collection<String> statuses);

    boolean existsByParkingSlotIdAndStatus(Long parkingSlotId, String status);

    boolean existsByParkingSlotIdAndStatusIn(Long parkingSlotId, Collection<String> statuses);

    Optional<Booking> findByQrToken(String qrToken);

    List<Booking> findByStatusInAndEndTimeBefore(Collection<String> statuses, LocalDateTime time);

    /**
     * True when a PENDING or BOOKED (approved) booking of this slot overlaps [start, end).
     * Older bookings that have no times block the slot.
     */
    @Query("select count(b) > 0 from Booking b "
            + "where b.parkingSlot.id = :slotId and b.status in ('BOOKED', 'PENDING') "
            + "and (b.startTime is null or (b.startTime < :endTime and b.endTime > :startTime))")
    boolean existsOverlap(@Param("slotId") Long slotId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    /** Same check for approval: only already approved (BOOKED) bookings, not the booking itself. */
    @Query("select count(b) > 0 from Booking b "
            + "where b.parkingSlot.id = :slotId and b.id <> :excludeId and b.status = 'BOOKED' "
            + "and (b.startTime is null or (b.startTime < :endTime and b.endTime > :startTime))")
    boolean existsApprovedOverlap(@Param("slotId") Long slotId,
                                  @Param("excludeId") Long excludeId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
