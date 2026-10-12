package com.smartparking.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * What an administrator can change. Every field is optional; only the ones that are sent are applied.
 * assignment: "ME" (take the report), "NONE" (remove the handler).
 * slotAction: "FLAG" (mark the slot as under maintenance so nobody can book it) or "CLEAR" (open it again).
 */
public record ServiceReportDecisionRequest(
        String status,

        @Size(max = 1000, message = "Response must be 1000 characters or fewer")
        String response,

        String assignment,

        @Size(max = 500, message = "Note must be 500 characters or fewer")
        String note,

        String slotAction) {
}
