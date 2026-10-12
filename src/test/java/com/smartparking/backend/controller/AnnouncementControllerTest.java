package com.smartparking.backend.controller;

import com.smartparking.backend.dto.AnnouncementRequest;
import com.smartparking.backend.model.Announcement;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.AnnouncementRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.service.AuditService;
import com.smartparking.backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Plain unit tests: no database and no Spring needed. */
class AnnouncementControllerTest {

    private final AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final AnnouncementController controller = new AnnouncementController(
            announcementRepository, userRepository, notificationService, auditService);

    private final Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    private User user(long id, String role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setEmail("u" + id + "@example.com");
        user.setRole(role);
        user.setActive(active);
        return user;
    }

    @Test
    @SuppressWarnings("unchecked")
    void announcementForStudentsReachesActiveStudentsOnly() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1, "STUDENT", true),
                user(2, "FACULTY", true),
                user(3, "STUDENT", false),
                user(4, "student", true)));
        when(notificationService.notifyUsers(any(), eq("ANNOUNCEMENT"), any(), any())).thenReturn(2);
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(i -> i.getArgument(0));

        Announcement saved = controller.create(
                new AnnouncementRequest("Holiday", "Parking closed on Friday", "student"), admin);

        ArgumentCaptor<Collection<User>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(notificationService).notifyUsers(captor.capture(), eq("ANNOUNCEMENT"),
                eq("Holiday"), eq("Parking closed on Friday"));
        assertEquals(2, captor.getValue().size());
        assertEquals("STUDENT", saved.getAudience());
        assertEquals(2, saved.getRecipientCount());
        assertEquals("admin@example.com", saved.getCreatedBy());
        verify(auditService).log(eq("ANNOUNCEMENT_SENT"), eq("ANNOUNCEMENT"), any(), any());
    }

    @Test
    void unknownAudienceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> controller.create(
                new AnnouncementRequest("Hello", "Message", "EVERYONE"), admin));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void blankTitleAfterTrimmingIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> controller.create(
                new AnnouncementRequest("   ", "Message", "ALL"), admin));
    }

    @Test
    void userSeesAnnouncementsForEveryoneAndForOwnRoleOnly() {
        Announcement forAll = new Announcement();
        forAll.setAudience("ALL");
        Announcement forStudents = new Announcement();
        forStudents.setAudience("STUDENT");
        Announcement forFaculty = new Announcement();
        forFaculty.setAudience("FACULTY");
        when(announcementRepository.findTop100ByOrderByCreatedAtDesc())
                .thenReturn(List.of(forAll, forStudents, forFaculty));

        Authentication student = new UsernamePasswordAuthenticationToken(
                "student@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        List<Announcement> result = controller.mine(student);

        assertEquals(2, result.size());
        assertTrue(result.contains(forAll));
        assertTrue(result.contains(forStudents));
    }
}
