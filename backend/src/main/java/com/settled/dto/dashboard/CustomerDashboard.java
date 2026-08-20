package com.settled.dto.dashboard;

import com.settled.domain.enums.ClaimStatus;

import java.util.List;
import java.util.Map;

public record CustomerDashboard(
        long policies,
        long totalClaims,
        long pendingClaims,
        long approvedClaims,
        Map<ClaimStatus, Long> claimsByStatus,
        List<ClaimSummary> recentClaims
) {
}