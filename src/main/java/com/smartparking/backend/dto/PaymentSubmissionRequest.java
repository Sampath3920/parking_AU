package com.smartparking.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentSubmissionRequest(
        @NotBlank String paymentMethod,
        @NotBlank String transactionReference,
        @AssertTrue(message = "You must confirm the payment details before submitting")
        @NotNull
        Boolean acknowledged) {
}
