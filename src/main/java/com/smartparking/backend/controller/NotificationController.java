package com.smartparking.backend.controller;

import com.smartparking.backend.exception.NotFoundException;
import com.smartparking.backend.model.Notification;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.NotificationRepository;
import com.smartparking.backend.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {

        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /** Admin only (see SecurityConfig). */
    @PostMapping("/create/{userId}")
    public Notification createNotification(
            @PathVariable Long userId,
            @RequestBody Notification notification) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        notification.setUser(user);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    /** Admin only (see SecurityConfig). */
    @GetMapping
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    /** Ownership of {userId} is checked by OwnershipInterceptor. Newest first. */
    @GetMapping("/user/{userId}")
    public List<Notification> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {

        List<Notification> all = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (!unreadOnly) {
            return all;
        }
        return all.stream().filter(n -> !n.isRead()).toList();
    }

    /** Number of unread notifications of the logged-in user (shown as a badge in the menu). */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication authentication) {
        User me = currentUser(authentication);
        long count = notificationRepository.findByUserId(me.getId()).stream()
                .filter(n -> !n.isRead())
                .count();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("count", count);
        return result;
    }

    /** Marks every notification of the logged-in user as read. */
    @PutMapping("/read-all")
    public Map<String, Object> markAllAsRead(Authentication authentication) {
        User me = currentUser(authentication);
        List<Notification> unread = notificationRepository.findByUserId(me.getId()).stream()
                .filter(n -> !n.isRead())
                .toList();
        for (Notification notification : unread) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unread);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", unread.size());
        return result;
    }

    @PutMapping("/read/{id}")
    public Notification markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        Notification notification = findOwned(id, authentication);
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @DeleteMapping("/{id}")
    public String deleteNotification(
            @PathVariable Long id,
            Authentication authentication) {

        Notification notification = findOwned(id, authentication);
        notificationRepository.delete(notification);
        return "Notification deleted successfully";
    }

    // ---------- helpers

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** A person may only change their own notifications (an administrator may change any). */
    private Notification findOwned(Long id, Authentication authentication) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        boolean admin = false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                admin = true;
            }
        }

        User owner = notification.getUser();
        boolean mine = owner != null && owner.getEmail() != null
                && owner.getEmail().equalsIgnoreCase(authentication.getName());

        if (!admin && !mine) {
            throw new AccessDeniedException("Not your notification");
        }
        return notification;
    }
}
