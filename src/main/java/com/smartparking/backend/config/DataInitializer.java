package com.smartparking.backend.config;

import com.smartparking.backend.model.ParkingLocation;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.ParkingLocationRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;
import com.smartparking.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates DEMO data on first start (only if it is missing):
 * one admin, one guard and one sample parking location with slots.
 * No real personal information. Turn off with app.seed.enabled=false.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ParkingLocationRepository locationRepository;
    private final ParkingSlotRepository slotRepository;
    private final PasswordEncoder passwordEncoder;

    private final boolean enabled;
    private final String demoPassword;

    public DataInitializer(UserRepository userRepository,
                           ParkingLocationRepository locationRepository,
                           ParkingSlotRepository slotRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.seed.enabled:true}") boolean enabled,
                           @Value("${app.seed.password:ChangeMe@123}") String demoPassword) {
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.slotRepository = slotRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        createUserIfMissing("admin@college.test", "Demo Admin", "ADMIN");
        createUserIfMissing("guard@college.test", "Demo Guard", "GUARD");

        if (locationRepository.count() == 0) {
            ParkingLocation location = new ParkingLocation();
            location.setName("Main Block Parking");
            location.setLocation("Near the main gate");
            location.setStatus("ACTIVE");
            location.setTotalSlots(10);
            location.setAvailableSlots(10);
            location = locationRepository.save(location);

            for (int i = 1; i <= 5; i++) {
                createSlot(location, "C" + i, "4-WHEELER");
                createSlot(location, "B" + i, "2-WHEELER");
            }
        }
    }

    private void createUserIfMissing(String email, String name, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setPhone("0000000000");
        user.setPassword(passwordEncoder.encode(demoPassword));
        userRepository.save(user);
    }

    private void createSlot(ParkingLocation location, String number, String type) {
        ParkingSlot slot = new ParkingSlot();
        slot.setSlotNumber(number);
        slot.setVehicleType(type);
        slot.setStatus("AVAILABLE");
        slot.setParkingLocation(location);
        slotRepository.save(slot);
    }
}
