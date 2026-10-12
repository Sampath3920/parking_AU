package com.smartparking.backend.controller;

import com.smartparking.backend.exception.NotFoundException;
import com.smartparking.backend.model.Notification;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.NotificationRepository;
import com.smartparking.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Plain unit tests: no database and no Spring needed. */
class NotificationControllerTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationController controller =
            new NotificationController(notificationRepository, userRepository);

    private User owner() {
        User user = new User();
        user.setId(7L);
        user.setEmail("owner@example.com");
        return user;
    }

    private Notification notification(long id, boolean read) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setRead(read);
        notification.setUser(owner());
        return notification;
    }

    private Authentication login(String email, String role) {
        return new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test
    void anotherUserCannotMarkMyNotificationAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification(1, false)));

        assertThrows(AccessDeniedException.class,
                () -> controller.markAsRead(1L, login("someone.else@example.com", "STUDENT")));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void ownerCanMarkOwnNotificationAsRead() {
        Notification mine = notification(1, false);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(mine));
        when(notificationRepository.save(mine)).thenReturn(mine);

        Notification result = controller.markAsRead(1L, login("OWNER@example.com", "STUDENT"));

        assertTrue(result.isRead());
    }

    @Test
    void anotherUserCannotDeleteMyNotification() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification(1, true)));

        assertThrows(AccessDeniedException.class,
                () -> controller.deleteNotification(1L, login("someone.else@example.com", "STUDENT")));
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void missingNotificationIsNotFound() {
        when(notificationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> controller.markAsRead(9L, login("owner@example.com", "STUDENT")));
    }

    @Test
    void unreadCountIgnoresReadNotifications() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner()));
        when(notificationRepository.findByUserId(7L)).thenReturn(List.of(
                notification(1, false), notification(2, true), notification(3, false)));

        Map<String, Long> result = controller.unreadCount(login("owner@example.com", "STUDENT"));

        assertEquals(2L, result.get("count"));
    }

    @Test
    void markAllAsReadUpdatesOnlyUnreadOnes() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner()));
        Notification unread = notification(1, false);
        when(notificationRepository.findByUserId(7L)).thenReturn(List.of(unread, notification(2, true)));

        Map<String, Object> result = controller.markAllAsRead(login("owner@example.com", "STUDENT"));

        assertEquals(1, result.get("updated"));
        assertTrue(unread.isRead());
        verify(notificationRepository).saveAll(List.of(unread));
    }

    @Test
    void unreadOnlyFilterHidesReadNotifications() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(
                notification(1, false), notification(2, true)));

        List<Notification> result = controller.getUserNotifications(7L, true);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }
}
