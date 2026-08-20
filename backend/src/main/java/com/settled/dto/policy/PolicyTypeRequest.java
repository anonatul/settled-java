package com.settled.dto.policy;

import com.settled.domain.PolicyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PolicyTypeRequest(
        @NotBlank(message = "Code is required")
        @Size(max = 30)
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 120)
        String name,

        @Size(max = 500)
        String description,

        @NotNull(message = "Coverage amount is required")
        @DecimalMin(value = "0.01", message = "Coverage amount must be positive")
        BigDecimal coverageAmount,

        @NotNull(message = "Premium rate is required")
        @Positive(message = "Premium rate must be positive")
        BigDecimal premiumRate,

        boolean active
) {
    public static PolicyType toEntity(PolicyTypeRequest request) {
        PolicyType type = new PolicyType();
        type.setCode(request.code().toUpperCase());
        type.setName(request.name());
        type.setDescription(request.description());
        type.setCoverageAmount(request.coverageAmount());
        type.setPremiumRate(request.premiumRate());
        type.setActive(request.active());
        return type;
    }
}