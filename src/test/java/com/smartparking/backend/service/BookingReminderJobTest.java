package com.smartparking.backend.service;

import com.smartparking.backend.model.Booking;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.BookingRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Plain unit tests: no database and no Spring needed. */
class BookingReminderJobTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final BookingReminderJob job = new BookingReminderJob(bookingRepository, notificationService);

    private final LocalDateTime now = LocalDateTime.of(2026, 10, 12, 9, 0);

    private Booking booking(long id, LocalDateTime start) {
        User user = new User();
        user.setId(1L);
        ParkingSlot slot = new ParkingSlot();
        slot.setSlotNumber("A1");
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(user);
        booking.setParkingSlot(slot);
        booking.setStartTime(start);
        booking.setStatus("BOOKED");
        return booking;
    }

    @Test
    void bookingStartingSoonGetsOneReminder() {
        Booking soon = booking(5, now.plusMinutes(20));
        when(bookingRepository.findByStatusAndStartTimeBetween("BOOKED", now, now.plusMinutes(30)))
                .thenReturn(List.of(soon));

        int sent = job.remindAt(now);

        assertEquals(1, sent);
        assertTrue(soon.isReminderSent());
        verify(notificationService).notifyUser(eq(soon.getUser()), eq("BOOKING_REMINDER"),
                anyString(), contains("#5"));
        verify(bookingRepository).save(soon);
    }

    @Test
    void reminderIsNotSentTwice() {
        Booking soon = booking(5, now.plusMinutes(20));
        soon.setReminderSent(true);
        when(bookingRepository.findByStatusAndStartTimeBetween(anyString(), any(), any()))
                .thenReturn(List.of(soon));

        int sent = job.remindAt(now);

        assertEquals(0, sent);
        verify(notificationService, never()).notifyUser(any(), anyString(), anyString(), anyString());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void nothingIsSentWhenNoBookingStartsSoon() {
        when(bookingRepository.findByStatusAndStartTimeBetween(anyString(), any(), any()))
                .thenReturn(List.of());

        assertEquals(0, job.remindAt(now));
    }
}
