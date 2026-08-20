package com.settled.dto.auth;

import com.settled.dto.customer.CustomerResponse;
import com.settled.dto.user.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user,
        CustomerResponse customer
) {
    public static AuthResponse of(String token, long expiresIn, UserResponse user, CustomerResponse customer) {
        return new AuthResponse(token, "Bearer", expiresIn, user, customer);
    }
}