package com.smartparking.backend.service;

import com.smartparking.backend.dto.LoginRequest;
import com.smartparking.backend.dto.LoginResponse;
import com.smartparking.backend.dto.RegisterRequest;
import com.smartparking.backend.exception.AccountDisabledException;
import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.security.CustomUserDetailsService;
import com.smartparking.backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthService {

    /** Roles a person may choose for themselves. ADMIN and GUARD are given by an administrator. */
    private static final Set<String> SELF_REGISTER_ROLES =
            Set.of("STUDENT", "FACULTY", "STAFF", "VISITOR");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        String role = CustomUserDetailsService.normalizeRole(request.getRole());

        if (!SELF_REGISTER_ROLES.contains(role)) {
            throw new IllegalArgumentException("This role cannot be selected during registration");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        return "Registration successful";
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Checked after the password so that strangers cannot find out which accounts are deactivated.
        if (!user.isActive()) {
            throw new AccountDisabledException(
                    "Your account has been deactivated. Please contact the administrator.");
        }

        String token = jwtService.generateToken(user.getEmail());

        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                CustomUserDetailsService.normalizeRole(user.getRole()),
                token,
                "Login successful"
        );
    }
}
