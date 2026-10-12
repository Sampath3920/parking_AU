package com.smartparking.backend.security;

import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Makes "student", "Student" and "ROLE_STUDENT" all become STUDENT. */
    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "STUDENT";
        }
        String value = role.trim().toUpperCase();
        if (value.startsWith("ROLE_")) {
            value = value.substring(5);
        }
        return value;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(normalizeRole(user.getRole()))
                .disabled(!user.isActive())
                .build();
    }
}
