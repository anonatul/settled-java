package com.settled.dto.policy;

import com.settled.domain.PolicyType;

import java.math.BigDecimal;
import java.util.UUID;

public record PolicyTypeResponse(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal coverageAmount,
        BigDecimal premiumRate,
        boolean active
) {
    public static PolicyTypeResponse from(PolicyType type) {
        return new PolicyTypeResponse(
                type.getId(),
                type.getCode(),
                type.getName(),
                type.getDescription(),
                type.getCoverageAmount(),
                type.getPremiumRate(),
                type.isActive()
        );
    }
}