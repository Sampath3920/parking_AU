package com.smartparking.backend.service;

import com.smartparking.backend.model.Booking;
import com.smartparking.backend.repository.BookingRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Every minute: an approved (BOOKED) booking that starts within the next 30 minutes
 * gets one reminder notification. The flag reminderSent makes sure it is sent only once.
 */
@Component
public class BookingReminderJob {

    public static final int REMINDER_MINUTES = 30;

    private static final Logger log = LoggerFactory.getLogger(BookingReminderJob.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public BookingReminderJob(BookingRepository bookingRepository,
                              NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 45000)
    @Transactional
    public void run() {
        remindAt(LocalDateTime.now());
    }

    /** Sends the reminders that are due at the given time and returns how many were sent. */
    public int remindAt(LocalDateTime now) {
        int sent = 0;
        for (Booking booking : bookingRepository.findByStatusAndStartTimeBetween(
                "BOOKED", now, now.plusMinutes(REMINDER_MINUTES))) {

            if (booking.isReminderSent()) {
                continue;
            }

            String slot = booking.getParkingSlot() != null ? booking.getParkingSlot().getSlotNumber() : "-";
            notificationService.notifyUser(booking.getUser(), "BOOKING_REMINDER", "Booking starts soon",
                    "Your booking #" + booking.getId() + " (slot " + slot + ") starts at "
                            + booking.getStartTime().format(TIME) + ". Show your QR code at the gate.");

            booking.setReminderSent(true);
            bookingRepository.save(booking);
            sent++;
            log.info("Reminder sent for booking {}", booking.getId());
        }
        return sent;
    }
}
