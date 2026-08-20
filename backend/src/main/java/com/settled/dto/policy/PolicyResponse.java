package com.settled.dto.policy;

import com.settled.domain.Policy;
import com.settled.domain.enums.PolicyStatus;
import com.settled.dto.customer.CustomerResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        String policyNumber,
        PolicyStatus status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal premium,
        BigDecimal sumInsured,
        UUID customerId,
        String customerNumber,
        String customerName,
        UUID policyTypeId,
        String policyTypeCode,
        String policyTypeName
) {
    public static PolicyResponse from(Policy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getPolicyNumber(),
                policy.getStatus(),
                policy.getStartDate(),
                policy.getEndDate(),
                policy.getPremium(),
                policy.getSumInsured(),
                policy.getCustomer().getId(),
                policy.getCustomer().getCustomerNumber(),
                policy.getCustomer().getUser().getFullName(),
                policy.getPolicyType().getId(),
                policy.getPolicyType().getCode(),
                policy.getPolicyType().getName()
        );
    }
}