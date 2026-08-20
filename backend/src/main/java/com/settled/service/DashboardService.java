package com.settled.service;

import com.settled.domain.Claim;
import com.settled.domain.enums.ClaimStatus;
import com.settled.domain.Customer;
import com.settled.dto.dashboard.ClaimSummary;
import com.settled.dto.dashboard.CustomerDashboard;
import com.settled.dto.dashboard.OfficerDashboard;
import com.settled.repository.ClaimRepository;
import com.settled.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final CustomerService customerService;

    @Cacheable(cacheNames = "customerDashboard", key = "#userId")
    @Transactional(readOnly = true)
    public CustomerDashboard customerDashboard(UUID userId) {
        Customer customer = customerService.getCustomer(userId);
        long policies = policyRepository.countByCustomerId(customer.getId());
        long totalClaims = claimRepository.countByCustomerId(customer.getId());
        long pending = claimRepository.countByCustomerIdAndStatus(customer.getId(), ClaimStatus.SUBMITTED)
                + claimRepository.countByCustomerIdAndStatus(customer.getId(), ClaimStatus.UNDER_REVIEW)
                + claimRepository.countByCustomerIdAndStatus(customer.getId(), ClaimStatus.ADDITIONAL_INFO_REQUIRED);
        long approved = claimRepository.countByCustomerIdAndStatus(customer.getId(), ClaimStatus.APPROVED)
                + claimRepository.countByCustomerIdAndStatus(customer.getId(), ClaimStatus.SETTLED);
        List<Claim> recent = claimRepository.findByCustomerId(
                customer.getId(), PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "submittedAt"))).getContent();

        Map<ClaimStatus, Long> byStatus = new LinkedHashMap<>();
        for (ClaimStatus status : ClaimStatus.values()) {
            byStatus.put(status, claimRepository.countByCustomerIdAndStatus(customer.getId(), status));
        }
        return new CustomerDashboard(policies, totalClaims, pending, approved, byStatus,
                recent.stream().map(ClaimSummary::from).toList());
    }

    @Cacheable(cacheNames = "officerDashboard", key = "#officerId")
    @Transactional(readOnly = true)
    public OfficerDashboard officerDashboard(UUID officerId) {
        long assigned = claimRepository.findByActiveAssignmentOfficer(officerId, null, PageRequest.of(0, 1)).getTotalElements();
        long pendingReview = claimRepository.findByActiveAssignmentOfficer(officerId, ClaimStatus.UNDER_REVIEW, PageRequest.of(0, 1)).getTotalElements();
        long awaitingInfo = claimRepository.findByActiveAssignmentOfficer(officerId, ClaimStatus.ADDITIONAL_INFO_REQUIRED, PageRequest.of(0, 1)).getTotalElements();
        List<Claim> recent = claimRepository.findByActiveAssignmentOfficer(
                officerId, null, PageRequest.of(0, 5)).getContent();

        Map<ClaimStatus, Long> byStatus = new LinkedHashMap<>();
        for (ClaimStatus status : ClaimStatus.values()) {
            byStatus.put(status, claimRepository.findByActiveAssignmentOfficer(officerId, status, PageRequest.of(0, 1)).getTotalElements());
        }
        return new OfficerDashboard(assigned, pendingReview, awaitingInfo, byStatus,
                recent.stream().map(ClaimSummary::from).toList());
    }
}