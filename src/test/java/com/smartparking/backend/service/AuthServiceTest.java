package com.smartparking.backend.service;

import com.smartparking.backend.dto.LoginRequest;
import com.smartparking.backend.dto.LoginResponse;
import com.smartparking.backend.dto.RegisterRequest;
import com.smartparking.backend.exception.AccountDisabledException;
import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Plain unit tests: no database and no Spring needed. */
class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtService jwtService =
            new JwtService("UnitTestSecretKeyUnitTestSecretKey123456", 60000);
    private final AuthService authService =
            new AuthService(userRepository, encoder, jwtService);

    private User existingUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Test Student");
        user.setEmail("test.student@example.com");
        user.setRole("STUDENT");
        user.setPassword(encoder.encode("Test@12345"));
        return user;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private RegisterRequest registerRequest(String role) {
        RegisterRequest request = new RegisterRequest();
        request.setName("New Person");
        request.setEmail("New.Person@Example.com");
        request.setPassword("Secret@123");
        request.setPhone("9999999999");
        request.setRole(role);
        return request;
    }

    @Test
    void validLoginReturnsTokenAndRole() {
        when(userRepository.findByEmail("test.student@example.com"))
                .thenReturn(Optional.of(existingUser()));

        LoginResponse response =
                authService.login(loginRequest("test.student@example.com", "Test@12345"));

        assertNotNull(response.getToken());
        assertEquals("STUDENT", response.getRole());
        assertEquals("test.student@example.com",
                jwtService.extractEmail(response.getToken()));
    }

    @Test
    void wrongPasswordIsRejectedWithBadCredentials() {
        when(userRepository.findByEmail("test.student@example.com"))
                .thenReturn(Optional.of(existingUser()));

        assertThrows(BadCredentialsException.class, () ->
                authService.login(loginRequest("test.student@example.com", "wrong")));
    }

    @Test
    void unknownEmailIsRejectedWithBadCredentials() {
        when(userRepository.findByEmail("nobody@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () ->
                authService.login(loginRequest("nobody@example.com", "anything")));
    }

    @Test
    void registerStoresHashedPasswordAndLowercaseEmail() {
        when(userRepository.findByEmail("new.person@example.com"))
                .thenReturn(Optional.empty());

        authService.register(registerRequest("faculty"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("new.person@example.com", saved.getEmail());
        assertEquals("FACULTY", saved.getRole());
        assertNotEquals("Secret@123", saved.getPassword());
        assertTrue(encoder.matches("Secret@123", saved.getPassword()));
    }

    @Test
    void adminRoleCannotBeSelfRegistered() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register(registerRequest("ADMIN")));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deactivatedAccountCannotLogInEvenWithTheRightPassword() {
        User user = existingUser();
        user.setActive(false);
        when(userRepository.findByEmail("test.student@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(AccountDisabledException.class, () ->
                authService.login(loginRequest("test.student@example.com", "Test@12345")));
    }

    @Test
    void registerStoresTheCreationTime() {
        when(userRepository.findByEmail("new.person@example.com"))
                .thenReturn(Optional.empty());

        authService.register(registerRequest("student"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNotNull(captor.getValue().getCreatedAt());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userRepository.findByEmail("new.person@example.com"))
                .thenReturn(Optional.of(existingUser()));

        assertThrows(ConflictException.class, () ->
                authService.register(registerRequest("STUDENT")));
    }
}
