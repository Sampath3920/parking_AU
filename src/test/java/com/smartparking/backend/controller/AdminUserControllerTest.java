package com.smartparking.backend.controller;

import com.smartparking.backend.dto.PageResult;
import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.PaymentRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;
import com.smartparking.backend.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Plain unit tests: no database and no Spring needed. */
class AdminUserControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final AdminUserController controller = new AdminUserController(
            userRepository,
            mock(VehicleRepository.class),
            mock(BookingRepository.class),
            mock(PaymentRepository.class),
            auditService);

    private final Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    private User user(long id, String name, String email, String role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    @Test
    void searchAndRoleFilterNarrowTheList() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1, "Anita Rao", "anita@example.com", "STUDENT"),
                user(2, "Anil Kumar", "anil@example.com", "FACULTY"),
                user(3, "Bala", "bala@example.com", "STUDENT")));

        PageResult<User> result = controller.getUsers("an", "STUDENT", null, 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("Anita Rao", result.content().get(0).getName());
    }

    @Test
    void statusFilterShowsOnlyDeactivatedUsers() {
        User off = user(1, "Off", "off@example.com", "STUDENT");
        off.setActive(false);
        when(userRepository.findAll()).thenReturn(
                List.of(off, user(2, "On", "on@example.com", "STUDENT")));

        PageResult<User> result = controller.getUsers(null, null, "INACTIVE", 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("Off", result.content().get(0).getName());
    }

    @Test
    void pagesAreCutCorrectly() {
        List<User> users = new ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            users.add(user(i, "User " + i, "u" + i + "@example.com", "STUDENT"));
        }
        when(userRepository.findAll()).thenReturn(users);

        PageResult<User> last = controller.getUsers(null, null, null, 2, 10);

        assertEquals(25, last.totalElements());
        assertEquals(3, last.totalPages());
        assertEquals(5, last.content().size());
    }

    @Test
    void adminCannotDeactivateOwnAccount() {
        when(userRepository.findById(1L)).thenReturn(
                Optional.of(user(1, "Admin", "admin@example.com", "ADMIN")));

        assertThrows(ConflictException.class,
                () -> controller.changeStatus(1L, Map.of("active", false), admin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void lastActiveAdministratorCannotBeDeactivated() {
        User other = user(2, "Other Admin", "other@example.com", "ADMIN");
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(userRepository.findAll()).thenReturn(List.of(other));

        assertThrows(ConflictException.class,
                () -> controller.changeStatus(2L, Map.of("active", false), admin));
    }

    @Test
    void deactivatingAStudentIsSavedAndAudited() {
        User student = user(5, "Student", "student@example.com", "STUDENT");
        when(userRepository.findById(5L)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);

        User saved = controller.changeStatus(5L, Map.of("active", false), admin);

        assertFalse(saved.isActive());
        verify(auditService).log("USER_DEACTIVATED", "USER", 5L, "student@example.com");
    }

    @Test
    void statusBodyMustContainABoolean() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.changeStatus(5L, Map.of("active", "yes"), admin));
    }

    @Test
    void unknownRoleIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.changeRole(5L, Map.of("role", "SUPERUSER"), admin));
    }

    @Test
    void adminCannotChangeOwnRole() {
        when(userRepository.findById(1L)).thenReturn(
                Optional.of(user(1, "Admin", "admin@example.com", "ADMIN")));

        assertThrows(ConflictException.class,
                () -> controller.changeRole(1L, Map.of("role", "STUDENT"), admin));
    }

    @Test
    void roleChangeIsSavedAndAudited() {
        User student = user(5, "Student", "student@example.com", "STUDENT");
        when(userRepository.findById(5L)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);

        User saved = controller.changeRole(5L, Map.of("role", "guard"), admin);

        assertEquals("GUARD", saved.getRole());
        verify(auditService).log("USER_ROLE_CHANGED", "USER", 5L, "STUDENT -> GUARD");
    }
}
