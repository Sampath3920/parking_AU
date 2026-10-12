package com.smartparking.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be 150 characters or fewer")
        String title,

        @NotBlank(message = "Message is required")
        @Size(max = 1500, message = "Message must be 1500 characters or fewer")
        String message,

        @NotBlank(message = "Choose who should receive the announcement")
        String audience) {
}
