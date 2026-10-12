package com.smartparking.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentReviewRequest(
        @NotBlank String status,
        String reason) {
}
