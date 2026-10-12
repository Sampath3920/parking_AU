package com.smartparking.backend.controller;

import com.smartparking.backend.dto.ServiceReportRequest;
import com.smartparking.backend.model.ServiceReport;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.service.ServiceReportService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/** Plain unit tests: no database and no Spring needed. */
class ServiceReportControllerTest {

    private final ServiceReportService reportService = mock(ServiceReportService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ServiceReportController controller =
            new ServiceReportController(reportService, userRepository);

    private User student() {
        User user = new User();
        user.setId(7L);
        user.setEmail("student@example.com");
        return user;
    }

    private Authentication login(String role) {
        return new UsernamePasswordAuthenticationToken(
                "student@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test
    void reporterIsAlwaysTheLoggedInUser() {
        User me = student();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(me));
        ServiceReportRequest request = new ServiceReportRequest("COMPLAINT", "OTHER", "S", "D", null, null, null);
        ServiceReport saved = new ServiceReport();
        when(reportService.submit(me, request)).thenReturn(saved);

        assertSame(saved, controller.submit(request, login("STUDENT")));
    }

    @Test
    void mineUsesTheIdOfTheLoggedInUser() {
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student()));
        List<ServiceReport> reports = List.of(new ServiceReport());
        when(reportService.mine(7L, "DAMAGE")).thenReturn(reports);

        assertSame(reports, controller.mine("DAMAGE", login("STUDENT")));
    }

    @Test
    void onlyAdministratorsAreTreatedAsAdmins() {
        User me = student();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(me));
        ServiceReport report = new ServiceReport();
        when(reportService.getFor(5L, me, false)).thenReturn(report);

        assertSame(report, controller.one(5L, login("STUDENT")));
        verify(reportService).getFor(5L, me, false);
    }
}
