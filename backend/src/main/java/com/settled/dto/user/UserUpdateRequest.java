package com.settled.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 80)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80)
        String lastName,

        @Size(max = 20)
        String phone,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 120)
        String email
) {
}