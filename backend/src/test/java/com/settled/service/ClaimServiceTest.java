package com.settled.service;

import com.settled.domain.Claim;
import com.settled.domain.Customer;
import com.settled.domain.Policy;
import com.settled.domain.PolicyType;
import com.settled.domain.User;
import com.settled.domain.enums.ClaimStatus;
import com.settled.domain.enums.Role;
import com.settled.exception.BadRequestException;
import com.settled.exception.ConflictException;
import com.settled.exception.RateLimitExceededException;
import com.settled.exception.ResourceNotFoundException;
import com.settled.repository.*;
import com.settled.dto.claim.ClaimDecisionRequest;
import com.settled.dto.claim.ClaimRejectRequest;
import com.settled.dto.claim.SettlementRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private ClaimAssignmentRepository assignmentRepository;
    @Mock private ClaimStatusHistoryRepository historyRepository;
    @Mock private ClaimNoteRepository noteRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private ClaimStateMachine stateMachine;
    @Mock private CustomerService customerService;
    @Mock private PolicyService policyService;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private NumberGenerator numberGenerator;
    @Mock private RateLimitService rateLimitService;

    private ClaimService claimService;
    private User customerUser;
    private User officer;
    private Customer customer;
    private Policy policy;
    private Claim claim;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(claimRepository, assignmentRepository, historyRepository,
                noteRepository, settlementRepository, stateMachine, customerService, policyService,
                userRepository, auditService, numberGenerator, rateLimitService);
        claimService.claimRateMax = 10;
        claimService.claimRateWindowMinutes = 60;

        customerUser = new User();
        customerUser.setId(UUID.randomUUID());
        customerUser.setEmail("customer@test.io");
        customerUser.setRole(Role.CUSTOMER);

        officer = new User();
        officer.setId(UUID.randomUUID());
        officer.setEmail("officer@test.io");
        officer.setRole(Role.CLAIM_OFFICER);

        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setUser(customerUser);
        customer.setCustomerNumber("CUS-00000001");

        PolicyType type = new PolicyType();
        type.setId(UUID.randomUUID());
        type.setName("Health Insurance");
        type.setCoverageAmount(new BigDecimal("500000"));

        policy = new Policy();
        policy.setId(UUID.randomUUID());
        policy.setCustomer(customer);
        policy.setPolicyType(type);
        policy.setPolicyNumber("POL-TEST-001");
        policy.setSumInsured(new BigDecimal("500000"));
        policy.setEndDate(LocalDate.now().plusMonths(6));

        claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setCustomer(customer);
        claim.setPolicy(policy);
        claim.setClaimNumber("CLM-TEST-001");
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setAmountRequested(new BigDecimal("100000"));
    }

    @Test
    void submitRejectsWhenRateLimitExceeded() {
        when(rateLimitService.tryConsume(any(), anyInt(), any())).thenReturn(false);
        assertThrows(RateLimitExceededException.class,
                () -> claimService.submit(customerUser.getId(), null, null));
    }

    @Test
    void submitRejectsAmountAboveSumInsured() {
        when(rateLimitService.tryConsume(any(), anyInt(), any())).thenReturn(true);
        when(policyService.getActivePolicyForCustomer(any(), any())).thenReturn(policy);
        var request = new com.settled.dto.claim.ClaimRequest(policy.getId(), LocalDate.now().minusDays(1),
                "Fire", "damage", new BigDecimal("999999"));
        assertThrows(BadRequestException.class, () -> claimService.submit(customerUser.getId(), request, null));
    }

    @Test
    void rejectRequiresUnderReview() {
        claim.setStatus(ClaimStatus.SUBMITTED);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(assignmentRepository.findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(claim.getId()))
                .thenReturn(Optional.of(assignedTo(officer)));
        assertThrows(ConflictException.class,
                () -> claimService.reject(claim.getId(), new ClaimRejectRequest("no"), officer));
    }

    @Test
    void unassignedOfficerCannotDecide() {
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(assignmentRepository.findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(claim.getId()))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> claimService.approve(claim.getId(), new ClaimDecisionRequest(new BigDecimal("50000"), null), officer));
    }

    @Test
    void approveRejectsAmountAboveRequested() {
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(assignmentRepository.findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(claim.getId()))
                .thenReturn(Optional.of(assignedTo(officer)));
        assertThrows(BadRequestException.class,
                () -> claimService.approve(claim.getId(),
                        new ClaimDecisionRequest(new BigDecimal("200000"), null), officer));
    }

    @Test
    void settleRejectsAmountAboveApproved() {
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setAmountApproved(new BigDecimal("80000"));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(assignmentRepository.findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(claim.getId()))
                .thenReturn(Optional.of(assignedTo(officer)));
        when(settlementRepository.existsByClaimId(claim.getId())).thenReturn(false);
        assertThrows(BadRequestException.class,
                () -> claimService.settle(claim.getId(),
                        new SettlementRequest(new BigDecimal("90000"), "REF", LocalDate.now()), officer));
    }

    @Test
    void settleRejectsSecondSettlement() {
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setAmountApproved(new BigDecimal("80000"));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(assignmentRepository.findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(claim.getId()))
                .thenReturn(Optional.of(assignedTo(officer)));
        when(settlementRepository.existsByClaimId(claim.getId())).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> claimService.settle(claim.getId(),
                        new SettlementRequest(new BigDecimal("50000"), "REF", LocalDate.now()), officer));
    }

    @Test
    void settleSucceedsOnApprovedClaim() {
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setAmountApproved(new BigDecimal("80000"));
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(assignmentRepository.findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(claim.getId()))
                .thenReturn(Optional.of(assignedTo(officer)));
        when(settlementRepository.existsByClaimId(claim.getId())).thenReturn(false);
        when(settlementRepository.existsBySettlementNumber(any())).thenReturn(false);
        when(numberGenerator.settlementNumber()).thenReturn("STL-TEST-001");
        when(stateMachine.transition(any(), any(), any(), any())).thenReturn(null);
        when(settlementRepository.findByClaimId(claim.getId())).thenReturn(Optional.empty());

        var result = claimService.settle(claim.getId(),
                new SettlementRequest(new BigDecimal("75000"), "REF-001", LocalDate.now()), officer);

        assertNotNull(result);
        verify(settlementRepository).save(any());
        verify(stateMachine).transition(eq(claim), eq(ClaimStatus.SETTLED), eq(officer), any());
    }

    @Test
    void officerCanListOnlyAssignedClaims() {
        Page<Claim> page = new PageImpl<>(List.of(claim));
        when(claimRepository.findByActiveAssignmentOfficer(officer.getId(), null, PageRequest.of(0, 10)))
                .thenReturn(page);

        var result = claimService.listForOfficer(officer.getId(), null, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
    }

    private com.settled.domain.ClaimAssignment assignedTo(User officer) {
        com.settled.domain.ClaimAssignment assignment = new com.settled.domain.ClaimAssignment();
        assignment.setClaim(claim);
        assignment.setOfficer(officer);
        assignment.setActive(true);
        return assignment;
    }
}