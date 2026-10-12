package com.smartparking.backend.service;

import com.smartparking.backend.model.Booking;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Every minute: a PENDING or BOOKED booking whose end time has passed and was never used becomes EXPIRED. */
@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingRepository bookingRepository;
    private final ParkingSessionRepository sessionRepository;
    private final BookingService bookingService;

    public BookingExpiryJob(BookingRepository bookingRepository,
                            ParkingSessionRepository sessionRepository,
                            BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Transactional
    public void expireOldBookings() {
        for (Booking booking : bookingRepository
                .findByStatusInAndEndTimeBefore(BookingService.ACTIVE_STATUSES, LocalDateTime.now())) {

            // Checked-in cars stay BOOKED until check-out.
            if (sessionRepository.findByBookingId(booking.getId()).isPresent()) {
                continue;
            }

            booking.setStatus("EXPIRED");
            bookingRepository.save(booking);
            bookingService.refreshSlotStatus(booking.getParkingSlot().getId());
            log.info("Booking {} expired", booking.getId());
        }
    }
}
