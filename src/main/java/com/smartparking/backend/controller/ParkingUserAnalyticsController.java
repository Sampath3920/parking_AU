package com.smartparking.backend.controller;

import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.BookingRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-user-analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingUserAnalyticsController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ParkingSessionRepository sessionRepository;

    public ParkingUserAnalyticsController(
            UserRepository userRepository,
            BookingRepository bookingRepository,
            ParkingSessionRepository sessionRepository) {

        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping
    public List<Map<String, Object>> getUserAnalytics() {

        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {

            Map<String, Object> analytics = new HashMap<>();

            int totalBookings =
                    bookingRepository.findByUserId(user.getId()).size();

            int completedBookings =
                    bookingRepository.findByUserId(user.getId())
                            .stream()
                            .filter(b -> "COMPLETED".equals(b.getStatus()))
                            .toList()
                            .size();

            int activeSessions = 0;

            for (var session : sessionRepository.findAll()) {

                if (session.getBooking() != null &&
                    session.getBooking().getUser() != null &&
                    session.getBooking().getUser().getId().equals(user.getId()) &&
                    "ACTIVE".equals(session.getStatus())) {

                    activeSessions++;
                }
            }

            analytics.put("userId", user.getId());
            analytics.put("name", user.getName());
            analytics.put("email", user.getEmail());
            analytics.put("role", user.getRole());
            analytics.put("totalBookings", totalBookings);
            analytics.put("completedBookings", completedBookings);
            analytics.put("activeSessions", activeSessions);

            result.add(analytics);
        }

        return result;
    }
}