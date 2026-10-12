package com.smartparking.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** What a user sends to report a complaint or damage. The reporter is always the logged-in user. */
public record ServiceReportRequest(
        @NotBlank(message = "Choose the type of report")
        String type,

        @NotBlank(message = "Choose a category")
        @Size(max = 40, message = "Category is too long")
        String category,

        @NotBlank(message = "Subject is required")
        @Size(max = 150, message = "Subject must be 150 characters or fewer")
        String subject,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must be 2000 characters or fewer")
        String description,

        LocalDate incidentDate,
        Long vehicleId,
        Long slotId) {
}
