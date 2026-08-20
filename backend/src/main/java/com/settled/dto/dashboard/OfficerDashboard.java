package com.settled.dto.dashboard;

import com.settled.domain.enums.ClaimStatus;

import java.util.List;
import java.util.Map;

public record OfficerDashboard(
        long assignedClaims,
        long pendingReview,
        long awaitingInfo,
        Map<ClaimStatus, Long> claimsByStatus,
        List<ClaimSummary> recentClaims
) {
}