package com.settled.dto.claim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank(message = "Note is required")
        @Size(max = 2000)
        String note,

        Boolean internal
) {
}