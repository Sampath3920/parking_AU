package com.smartparking.backend.controller;

import com.smartparking.backend.dto.AnnouncementRequest;
import com.smartparking.backend.model.Announcement;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.AnnouncementRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.security.CustomUserDetailsService;
import com.smartparking.backend.service.AuditService;
import com.smartparking.backend.service.NotificationService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Admin announcements. Writing and the full history are ADMIN only (/api/admin/** in SecurityConfig).
 * Every logged-in user can read the announcements meant for their group.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class AnnouncementController {

    public static final Set<String> AUDIENCES =
            Set.of("ALL", "STUDENT", "FACULTY", "STAFF", "VISITOR", "GUARD");

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public AnnouncementController(AnnouncementRepository announcementRepository,
                                  UserRepository userRepository,
                                  NotificationService notificationService,
                                  AuditService auditService) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @PostMapping("/admin/announcements")
    @Transactional
    public Announcement create(@Valid @RequestBody AnnouncementRequest request,
                               Authentication authentication) {

        String audience = request.audience().trim().toUpperCase();
        if (!AUDIENCES.contains(audience)) {
            throw new IllegalArgumentException("Audience must be one of " + AUDIENCES);
        }

        String title = request.title().trim();
        String message = request.message().trim();
        if (title.isEmpty() || message.isEmpty()) {
            throw new IllegalArgumentException("Title and message cannot be empty");
        }

        List<User> recipients = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            boolean everyone = "ALL".equals(audience);
            boolean sameRole = audience.equals(CustomUserDetailsService.normalizeRole(user.getRole()));
            if (user.isActive() && (everyone || sameRole)) {
                recipients.add(user);
            }
        }

        int sent = notificationService.notifyUsers(recipients, "ANNOUNCEMENT", title, message);

        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setMessage(message);
        announcement.setAudience(audience);
        announcement.setCreatedBy(authentication != null ? authentication.getName() : "system");
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setRecipientCount(sent);
        Announcement saved = announcementRepository.save(announcement);

        auditService.log("ANNOUNCEMENT_SENT", "ANNOUNCEMENT", saved.getId(),
                audience + " (" + sent + " users): " + title);
        return saved;
    }

    @GetMapping("/admin/announcements")
    public List<Announcement> history() {
        return announcementRepository.findTop100ByOrderByCreatedAtDesc();
    }

    /** Announcements for everybody plus the ones for the role of the logged-in person. */
    @GetMapping("/announcements")
    public List<Announcement> mine(Authentication authentication) {
        String role = roleOf(authentication);
        return announcementRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(a -> "ALL".equals(a.getAudience()) || role.equals(a.getAudience()))
                .toList();
    }

    private String roleOf(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                return value.substring(5);
            }
        }
        return "";
    }
}
