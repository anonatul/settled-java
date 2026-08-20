package com.settled.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 120, message = "Email must be at most 120 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotBlank(message = "First name is required")
        @Size(max = 80, message = "First name must be at most 80 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80, message = "Last name must be at most 80 characters")
        String lastName,

        @Pattern(regexp = "^[0-9+\\-() ]{6,20}$", message = "Invalid phone number")
        String phone,

        LocalDate dateOfBirth,

        @Size(max = 120)
        String address,

        @Size(max = 80)
        String city,

        @Size(max = 40)
        String state,

        @Size(max = 20)
        String postalCode,

        @Size(max = 40)
        String country
) {
}