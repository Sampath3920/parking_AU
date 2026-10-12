package com.smartparking.backend.controller;

import com.smartparking.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-role-analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingRoleAnalyticsController {

    private final UserRepository userRepository;

    public ParkingRoleAnalyticsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public Map<String, Object> getRoleAnalytics() {

        Map<String, Object> analytics = new HashMap<>();

        analytics.put("totalUsers", userRepository.count());
        analytics.put("students",
                userRepository.findAll().stream()
                        .filter(u -> "STUDENT".equals(u.getRole()))
                        .count());
        analytics.put("faculty",
                userRepository.findAll().stream()
                        .filter(u -> "FACULTY".equals(u.getRole()))
                        .count());
        analytics.put("staff",
                userRepository.findAll().stream()
                        .filter(u -> "STAFF".equals(u.getRole()))
                        .count());
        analytics.put("admins",
                userRepository.findAll().stream()
                        .filter(u -> "ADMIN".equals(u.getRole()))
                        .count());

        return analytics;
    }
}