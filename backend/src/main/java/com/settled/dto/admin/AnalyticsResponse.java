package com.settled.dto.admin;

import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        long totalUsers,
        long totalCustomers,
        long totalOfficers,
        long totalPolicies,
        long totalClaims,
        long pendingClaims,
        long approvedClaims,
        long rejectedClaims,
        long settledClaims,
        java.math.BigDecimal totalSettledAmount,
        Map<String, Long> claimsByStatus,
        List<MonthlyClaims> monthlyClaims
) {
    public record MonthlyClaims(String month, long count) {
    }
}