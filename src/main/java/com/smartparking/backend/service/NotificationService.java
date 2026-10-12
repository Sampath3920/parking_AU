package com.smartparking.backend.service;

import com.smartparking.backend.model.Notification;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.NotificationRepository;
import com.smartparking.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /** Turns "PAYMENT_DECISION" into "Payment decision" for notifications without their own title. */
    public static String defaultTitle(String type) {
        if (type == null || type.isBlank()) {
            return "Notification";
        }
        String text = type.trim().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public void notifyUser(User user, String type, String message) {
        notifyUser(user, type, defaultTitle(type), message);
    }

    public void notifyUser(User user, String type, String title, String message) {
        if (user == null) {
            return;
        }
        notificationRepository.save(build(user, type, title, message));
    }

    public void notifyRole(String role, String type, String message) {
        for (User user : userRepository.findByRole(role)) {
            notifyUser(user, type, message);
        }
    }

    /** One notification per user, saved together. Used for announcements. */
    public int notifyUsers(Collection<User> users, String type, String title, String message) {
        List<Notification> notifications = new ArrayList<>();
        for (User user : users) {
            if (user != null) {
                notifications.add(build(user, type, title, message));
            }
        }
        notificationRepository.saveAll(notifications);
        return notifications.size();
    }

    private Notification build(User user, String type, String title, String message) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        notification.setUser(user);
        return notification;
    }
}
