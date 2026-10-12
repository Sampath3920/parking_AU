package com.smartparking.backend.controller;

import com.smartparking.backend.model.ParkingEvent;
import com.smartparking.backend.model.ParkingLocation;
import com.smartparking.backend.repository.ParkingEventRepository;
import com.smartparking.backend.repository.ParkingLocationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking-events")
@CrossOrigin(origins = "http://localhost:5173")
public class ParkingEventController {

    private final ParkingEventRepository eventRepository;
    private final ParkingLocationRepository locationRepository;

    public ParkingEventController(
            ParkingEventRepository eventRepository,
            ParkingLocationRepository locationRepository) {
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
    }

    @PostMapping("/create")
    public ParkingEvent createEvent(
            @RequestParam Long locationId,
            @RequestBody ParkingEvent event) {

        ParkingLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Parking location not found"));

        event.setParkingLocation(location);

        if (event.getStatus() == null) {
            event.setStatus("UPCOMING");
        }

        return eventRepository.save(event);
    }

    @GetMapping
    public List<ParkingEvent> getAllEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/{id}")
    public ParkingEvent getEvent(@PathVariable Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking event not found"));
    }

    @GetMapping("/date/{date}")
    public List<ParkingEvent> getEventsByDate(
            @PathVariable String date) {

        return eventRepository.findByEventDate(
                java.time.LocalDate.parse(date));
    }

    @GetMapping("/status/{status}")
    public List<ParkingEvent> getEventsByStatus(
            @PathVariable String status) {

        return eventRepository.findByStatus(status);
    }

    @GetMapping("/location/{locationId}")
    public List<ParkingEvent> getEventsByLocation(
            @PathVariable Long locationId) {

        return eventRepository.findByParkingLocationId(locationId);
    }

    @PutMapping("/{id}")
    public ParkingEvent updateEvent(
            @PathVariable Long id,
            @RequestBody ParkingEvent updatedEvent) {

        ParkingEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking event not found"));

        event.setEventName(updatedEvent.getEventName());
        event.setEventDate(updatedEvent.getEventDate());
        event.setVehicleType(updatedEvent.getVehicleType());
        event.setMaxCapacity(updatedEvent.getMaxCapacity());
        event.setStatus(updatedEvent.getStatus());

        return eventRepository.save(event);
    }

    @PutMapping("/{id}/status")
    public ParkingEvent updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        ParkingEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking event not found"));

        if (!status.equals("UPCOMING")
                && !status.equals("ACTIVE")
                && !status.equals("COMPLETED")
                && !status.equals("CANCELLED")) {
            throw new RuntimeException("Invalid event status");
        }

        event.setStatus(status);

        return eventRepository.save(event);
    }

    @DeleteMapping("/{id}")
    public String deleteEvent(@PathVariable Long id) {

        eventRepository.deleteById(id);

        return "Parking event deleted successfully";
    }
}