package com.smartparking.backend.controller;

import com.smartparking.backend.model.ParkingPass;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.ParkingPassRepository;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/parking-passes")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingPassController {

    private final ParkingPassRepository passRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public ParkingPassController(
            ParkingPassRepository passRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository) {

        this.passRepository = passRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @PostMapping("/create")
    public ParkingPass createPass(
            @RequestParam Long userId,
            @RequestParam Long vehicleId,
            @RequestBody ParkingPass pass) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new RuntimeException("Vehicle not found"));

        pass.setUser(user);
        pass.setVehicle(vehicle);

        if (pass.getStartDate() == null) {
            pass.setStartDate(LocalDate.now());
        }

        if (pass.getStatus() == null) {
            pass.setStatus("ACTIVE");
        }

        return passRepository.save(pass);
    }

    @GetMapping
    public List<ParkingPass> getAllPasses() {
        return passRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<ParkingPass> getUserPasses(
            @PathVariable Long userId) {

        return passRepository.findByUserId(userId);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<ParkingPass> getVehiclePasses(
            @PathVariable Long vehicleId) {

        return passRepository.findByVehicleId(vehicleId);
    }

    @GetMapping("/{id}")
    public ParkingPass getPass(@PathVariable Long id) {

        return passRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Parking pass not found"));
    }

    @PutMapping("/{id}")
    public ParkingPass updatePass(
            @PathVariable Long id,
            @RequestBody ParkingPass updatedPass) {

        ParkingPass pass = passRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Parking pass not found"));

        pass.setPassNumber(updatedPass.getPassNumber());
        pass.setPassType(updatedPass.getPassType());
        pass.setStartDate(updatedPass.getStartDate());
        pass.setEndDate(updatedPass.getEndDate());
        pass.setStatus(updatedPass.getStatus());

        return passRepository.save(pass);
    }

    @DeleteMapping("/{id}")
    public String deletePass(@PathVariable Long id) {

        passRepository.deleteById(id);

        return "Parking pass deleted successfully";
    }
}