package com.settled.controller;

import com.settled.common.ApiResponse;
import com.settled.common.CurrentUser;
import com.settled.dto.auth.AuthResponse;
import com.settled.dto.auth.LoginRequest;
import com.settled.dto.auth.RegisterRequest;
import com.settled.dto.user.UserResponse;
import com.settled.repository.UserRepository;
import com.settled.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login and current user")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new customer account")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                              HttpServletRequest httpRequest) {
        return ApiResponse.ok("Registration successful", authService.register(request, httpRequest));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT token")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest httpRequest) {
        return ApiResponse.ok("Login successful", authService.login(request, httpRequest));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user")
    public ApiResponse<UserResponse> me(@CurrentUser UUID userId) {
        return ApiResponse.ok(authService.me(userRepository.findById(userId).orElseThrow()));
    }
}