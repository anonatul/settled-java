package com.settled.service;

import com.settled.domain.Claim;
import com.settled.domain.enums.ClaimStatus;
import com.settled.domain.Settlement;
import com.settled.domain.enums.Role;
import com.settled.dto.admin.AnalyticsResponse;
import com.settled.repository.ClaimRepository;
import com.settled.repository.PolicyRepository;
import com.settled.repository.SettlementRepository;
import com.settled.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final SettlementRepository settlementRepository;

    @Cacheable(cacheNames = "adminDashboard")
    @Transactional(readOnly = true)
    public AnalyticsResponse analytics() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : claimRepository.countByStatusGrouped()) {
            byStatus.put(((ClaimStatus) row[0]).name(), (Long) row[1]);
        }
        for (ClaimStatus status : ClaimStatus.values()) {
            byStatus.putIfAbsent(status.name(), 0L);
        }

        List<Object[]> monthRows = claimRepository.countByMonth(Instant.now().minus(365, ChronoUnit.DAYS));
        List<AnalyticsResponse.MonthlyClaims> monthly = monthRows.stream()
                .map(row -> new AnalyticsResponse.MonthlyClaims((String) row[0], (Long) row[1]))
                .toList();

        BigDecimal totalSettled = settlementRepository.findAll().stream()
                .map(Settlement::getSettledAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pending = byStatus.getOrDefault("SUBMITTED", 0L)
                + byStatus.getOrDefault("UNDER_REVIEW", 0L)
                + byStatus.getOrDefault("ADDITIONAL_INFO_REQUIRED", 0L);
        return new AnalyticsResponse(
                userRepository.count(),
                userRepository.findByRole(Role.CUSTOMER).size(),
                userRepository.findByRole(Role.CLAIM_OFFICER).size(),
                policyRepository.count(),
                claimRepository.count(),
                pending,
                byStatus.getOrDefault("APPROVED", 0L),
                byStatus.getOrDefault("REJECTED", 0L),
                byStatus.getOrDefault("SETTLED", 0L),
                totalSettled,
                byStatus,
                monthly
        );
    }
}