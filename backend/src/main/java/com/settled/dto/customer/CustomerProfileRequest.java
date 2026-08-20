package com.settled.dto.customer;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerProfileRequest(
        @Size(max = 120)
        String address,

        @Size(max = 80)
        String city,

        @Size(max = 40)
        String state,

        @Size(max = 20)
        String postalCode,

        @Size(max = 40)
        String country,

        @Pattern(regexp = "^[0-9+\\-() ]{6,20}$", message = "Invalid phone number")
        String phone
) {
}