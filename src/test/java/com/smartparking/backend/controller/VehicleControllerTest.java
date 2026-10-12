package com.smartparking.backend.controller;

import com.smartparking.backend.exception.ConflictException;
import com.smartparking.backend.model.User;
import com.smartparking.backend.model.Vehicle;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.repository.VehicleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleControllerTest {

    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final VehicleController controller =
            new VehicleController(vehicleRepository, userRepository);

    @Test
    void fourthVehicleIsRejectedBeforeSaving() {
        when(userRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(new User()));
        when(vehicleRepository.findByUserId(3L))
                .thenReturn(List.of(new Vehicle(), new Vehicle(), new Vehicle()));

        assertThrows(ConflictException.class,
                () -> controller.addVehicle(3L, new Vehicle()));
        verify(vehicleRepository, never()).save(any());
    }
}
