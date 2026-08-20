package com.settled.controller;

import com.settled.common.ApiResponse;
import com.settled.common.CurrentUser;
import com.settled.dto.customer.CustomerProfileRequest;
import com.settled.dto.customer.CustomerResponse;
import com.settled.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer profile management")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    @Operation(summary = "Get my customer profile")
    public ApiResponse<CustomerResponse> me(@CurrentUser UUID userId) {
        return ApiResponse.ok(customerService.getMyProfile(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my customer profile")
    public ApiResponse<CustomerResponse> update(@CurrentUser UUID userId,
                                                @Valid @RequestBody CustomerProfileRequest request) {
        return ApiResponse.ok(customerService.updateMyProfile(userId, request));
    }
}