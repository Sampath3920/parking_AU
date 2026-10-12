package com.smartparking.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record VehicleDecisionRequest(
        @NotBlank(message = "Decision is required") String status,
        String reason) {
}
