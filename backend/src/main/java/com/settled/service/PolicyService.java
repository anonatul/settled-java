package com.settled.service;

import com.settled.common.PageResponse;
import com.settled.domain.Customer;
import com.settled.domain.Policy;
import com.settled.domain.PolicyType;
import com.settled.domain.User;
import com.settled.domain.enums.AuditAction;
import com.settled.domain.enums.PolicyStatus;
import com.settled.domain.enums.Role;
import com.settled.dto.policy.PolicyRequest;
import com.settled.dto.policy.PolicyResponse;
import com.settled.exception.BadRequestException;
import com.settled.exception.ResourceNotFoundException;
import com.settled.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyTypeService policyTypeService;
    private final CustomerService customerService;
    private final AuditService auditService;
    private final NumberGenerator numberGenerator;

    @Transactional(readOnly = true)
    public PageResponse<PolicyResponse> listMine(UUID userId, Pageable pageable) {
        Customer customer = customerService.getCustomer(userId);
        return PageResponse.from(
                policyRepository.findByCustomerId(customer.getId(), pageable),
                PolicyResponse::from);
    }

    @Transactional(readOnly = true)
    public PolicyResponse getMine(UUID userId, UUID policyId) {
        Policy policy = getPolicy(policyId);
        Customer customer = customerService.getCustomer(userId);
        if (!policy.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Policy not found");
        }
        return PolicyResponse.from(policy);
    }

    @Transactional
    public PolicyResponse createForCustomer(UUID customerId, PolicyRequest request, User admin) {
        Customer customer = customerService.getCustomer(customerId);
        PolicyType type = policyTypeService.getEntity(request.policyTypeId());
        if (!type.isActive()) {
            throw new BadRequestException("Policy type is not active");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date must be after start date");
        }
        if (request.sumInsured().compareTo(type.getCoverageAmount()) > 0) {
            throw new BadRequestException("Sum insured cannot exceed policy type coverage amount");
        }

        Policy policy = new Policy();
        policy.setCustomer(customer);
        policy.setPolicyType(type);
        policy.setPolicyNumber(generateUniquePolicyNumber());
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(request.startDate());
        policy.setEndDate(request.endDate());
        policy.setPremium(request.premium());
        policy.setSumInsured(request.sumInsured());
        Policy saved = policyRepository.save(policy);

        auditService.log(admin, AuditAction.POLICY_CREATED, "Policy", saved.getId(),
                "Policy " + saved.getPolicyNumber() + " created for customer " + customer.getCustomerNumber(), null);
        return PolicyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<PolicyResponse> search(String q, String status, Pageable pageable) {
        PolicyStatus policyStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                policyStatus = PolicyStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid policy status: " + status);
            }
        }
        return PageResponse.from(policyRepository.search(q, policyStatus, pageable), PolicyResponse::from);
    }

    public Policy getPolicy(UUID id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Policy", id));
    }

    public Policy getActivePolicyForCustomer(UUID policyId, UUID userId) {
        Policy policy = getPolicy(policyId);
        Customer customer = customerService.getCustomer(userId);
        if (!policy.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Policy not found");
        }
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new BadRequestException("Policy is not active");
        }
        if (policy.getEndDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Policy has expired");
        }
        return policy;
    }

    private String generateUniquePolicyNumber() {
        String number;
        do {
            number = numberGenerator.policyNumber();
        } while (policyRepository.existsByPolicyNumber(number));
        return number;
    }
}