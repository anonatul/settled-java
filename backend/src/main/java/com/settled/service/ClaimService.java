package com.settled.service;

import com.settled.common.PageResponse;
import com.settled.domain.*;
import com.settled.domain.enums.AuditAction;
import com.settled.domain.enums.ClaimStatus;
import com.settled.domain.enums.Role;
import com.settled.dto.claim.*;
import com.settled.exception.BadRequestException;
import com.settled.exception.ConflictException;
import com.settled.exception.ResourceNotFoundException;
import com.settled.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimAssignmentRepository assignmentRepository;
    private final ClaimStatusHistoryRepository historyRepository;
    private final ClaimNoteRepository noteRepository;
    private final SettlementRepository settlementRepository;
    private final ClaimStateMachine stateMachine;
    private final CustomerService customerService;
    private final PolicyService policyService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NumberGenerator numberGenerator;
    private final RateLimitService rateLimitService;

    @Value("${app.rate-limit.claim-max:10}")
    int claimRateMax;

    @Value("${app.rate-limit.claim-window-minutes:60}")
    long claimRateWindowMinutes;

    @Transactional
    public ClaimResponse submit(UUID userId, ClaimRequest request, HttpServletRequest httpRequest) {
        String rateKey = "rl:claim:" + userId;
        if (!rateLimitService.tryConsume(rateKey, claimRateMax, Duration.ofMinutes(claimRateWindowMinutes))) {
            throw new com.settled.exception.RateLimitExceededException(
                    "Claim submission limit reached (" + claimRateMax + " per " + claimRateWindowMinutes + " minutes). Please try again later.");
        }

        Policy policy = policyService.getActivePolicyForCustomer(request.policyId(), userId);
        if (request.amountRequested().compareTo(policy.getSumInsured()) > 0) {
            throw new BadRequestException("Requested amount cannot exceed policy sum insured");
        }

        Claim claim = new Claim();
        claim.setCustomer(policy.getCustomer());
        claim.setPolicy(policy);
        claim.setClaimNumber(generateUniqueClaimNumber());
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setIncidentDate(request.incidentDate());
        claim.setIncidentType(request.incidentType().trim());
        claim.setDescription(request.description().trim());
        claim.setAmountRequested(request.amountRequested());
        claim.setSubmittedAt(Instant.now());
        Claim saved = claimRepository.save(claim);

        ClaimStatusHistory history = new ClaimStatusHistory();
        history.setClaim(saved);
        history.setFromStatus(null);
        history.setToStatus(ClaimStatus.SUBMITTED);
        history.setChangedBy(policy.getCustomer().getUser());
        history.setChangedAt(Instant.now());
        historyRepository.save(history);

        User customerUser = policy.getCustomer().getUser();
        auditService.log(customerUser, AuditAction.CLAIM_SUBMITTED, "Claim", saved.getId(),
                "Claim " + saved.getClaimNumber() + " submitted by " + customerUser.getEmail(), clientIp(httpRequest));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listMine(UUID userId, String status, Pageable pageable) {
        Customer customer = customerService.getCustomer(userId);
        ClaimStatus claimStatus = parseStatus(status);
        Page<Claim> page = claimStatus == null
                ? claimRepository.findByCustomerId(customer.getId(), pageable)
                : claimRepository.findByCustomerIdAndStatus(customer.getId(), claimStatus, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getMine(UUID userId, UUID claimId) {
        Customer customer = customerService.getCustomer(userId);
        Claim claim = getClaim(claimId);
        if (!claim.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Claim not found");
        }
        return toResponse(claim);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listForOfficer(UUID officerId, String status, Pageable pageable) {
        ClaimStatus claimStatus = parseStatus(status);
        return PageResponse.from(
                claimRepository.findByActiveAssignmentOfficer(officerId, claimStatus, pageable),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getForOfficer(UUID officerId, UUID claimId) {
        Claim claim = getClaim(claimId);
        assignmentRepository.findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(claimId)
                .filter(a -> a.getOfficer().getId().equals(officerId))
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found or not assigned to you"));
        return toResponse(claim);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listAll(String q, String status, Pageable pageable) {
        return PageResponse.from(claimRepository.search(q, parseStatus(status), pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getAny(UUID claimId) {
        return toResponse(getClaim(claimId));
    }

    @Transactional
    public ClaimResponse assign(UUID claimId, ClaimAssignRequest request, User admin) {
        Claim claim = getClaim(claimId);
        if (claim.getStatus().isTerminal()) {
            throw new ConflictException("Cannot assign a " + claim.getStatus() + " claim");
        }
        User officer = userRepository.findById(request.officerId())
                .filter(u -> u.getRole() == Role.CLAIM_OFFICER)
                .orElseThrow(() -> new BadRequestException("Officer not found"));

        assignmentRepository.findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(claimId)
                .ifPresent(existing -> existing.setActive(false));

        ClaimAssignment assignment = new ClaimAssignment();
        assignment.setClaim(claim);
        assignment.setOfficer(officer);
        assignment.setAssignedBy(admin);
        assignment.setAssignedAt(Instant.now());
        assignment.setActive(true);
        assignmentRepository.save(assignment);

        if (claim.getStatus() == ClaimStatus.SUBMITTED) {
            stateMachine.transition(claim, ClaimStatus.UNDER_REVIEW, admin, "Claim assigned for review");
        }

        auditService.log(admin, AuditAction.CLAIM_ASSIGNED, "Claim", claim.getId(),
                "Claim " + claim.getClaimNumber() + " assigned to " + officer.getEmail(), null);
        return toResponse(claim);
    }

    @Transactional
    public ClaimResponse requestAdditionalInfo(UUID claimId, ClaimInfoRequest request, User officer) {
        Claim claim = getClaim(claimId);
        assertOfficerAssigned(claim, officer);
        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new ConflictException("Only claims under review can request additional information");
        }
        stateMachine.transition(claim, ClaimStatus.ADDITIONAL_INFO_REQUIRED, officer, request.note());
        addInternalNote(claim, officer, request.note(), true);
        audit(claim, AuditAction.CLAIM_STATUS_CHANGED, officer,
                "Additional information requested: " + request.note());
        return toResponse(claim);
    }

    @Transactional
    public ClaimResponse respondToInfoRequest(UUID userId, UUID claimId, ClaimInfoRequest request) {
        Customer customer = customerService.getCustomer(userId);
        Claim claim = getClaim(claimId);
        if (!claim.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Claim not found");
        }
        if (claim.getStatus() != ClaimStatus.ADDITIONAL_INFO_REQUIRED) {
            throw new ConflictException("Claim is not awaiting additional information");
        }
        stateMachine.transition(claim, ClaimStatus.UNDER_REVIEW, customer.getUser(),
                "Customer provided additional information");
        addInternalNote(claim, customer.getUser(), request.note(), false);
        audit(claim, AuditAction.CLAIM_STATUS_CHANGED, customer.getUser(),
                "Customer provided additional information");
        return toResponse(claim);
    }

    @Transactional
    public ClaimResponse approve(UUID claimId, ClaimDecisionRequest request, User officer) {
        Claim claim = getClaim(claimId);
        assertOfficerAssigned(claim, officer);
        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new ConflictException("Only claims under review can be approved");
        }
        if (request.amountApproved().compareTo(claim.getAmountRequested()) > 0) {
            throw new BadRequestException("Approved amount cannot exceed requested amount");
        }
        claim.setAmountApproved(request.amountApproved());
        claim.setDecidedAt(Instant.now());
        stateMachine.transition(claim, ClaimStatus.APPROVED, officer, request.note());
        if (request.note() != null && !request.note().isBlank()) {
            addInternalNote(claim, officer, request.note(), true);
        }
        audit(claim, AuditAction.CLAIM_APPROVED, officer,
                "Claim approved with amount " + request.amountApproved());
        return toResponse(claim);
    }

    @Transactional
    public ClaimResponse reject(UUID claimId, ClaimRejectRequest request, User officer) {
        Claim claim = getClaim(claimId);
        assertOfficerAssigned(claim, officer);
        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new ConflictException("Only claims under review can be rejected");
        }
        claim.setDecidedAt(Instant.now());
        stateMachine.transition(claim, ClaimStatus.REJECTED, officer, request.reason());
        addInternalNote(claim, officer, request.reason(), true);
        audit(claim, AuditAction.CLAIM_REJECTED, officer, "Claim rejected: " + request.reason());
        return toResponse(claim);
    }

    @Transactional
    public ClaimResponse settle(UUID claimId, SettlementRequest request, User officer) {
        Claim claim = getClaim(claimId);
        assertOfficerAssigned(claim, officer);
        if (claim.getStatus() != ClaimStatus.APPROVED) {
            throw new ConflictException("Only approved claims can be settled");
        }
        if (settlementRepository.existsByClaimId(claimId)) {
            throw new ConflictException("Claim already has a settlement");
        }
        if (request.settledAmount().compareTo(claim.getAmountApproved()) > 0) {
            throw new BadRequestException("Settled amount cannot exceed approved amount ("
                    + claim.getAmountApproved() + ")");
        }

        Settlement settlement = new Settlement();
        settlement.setClaim(claim);
        settlement.setSettlementNumber(generateUniqueSettlementNumber());
        settlement.setApprovedAmount(claim.getAmountApproved());
        settlement.setSettledAmount(request.settledAmount());
        settlement.setSettlementDate(request.settlementDate());
        settlement.setPaymentReference(request.paymentReference());
        settlement.setProcessedBy(officer);
        settlement.setCreatedAt(Instant.now());
        settlementRepository.save(settlement);

        claim.setSettledAt(Instant.now());
        stateMachine.transition(claim, ClaimStatus.SETTLED, officer,
                "Settled amount " + request.settledAmount() + (request.paymentReference() != null
                        ? " ref " + request.paymentReference() : ""));
        audit(claim, AuditAction.CLAIM_SETTLED, officer,
                "Claim settled with amount " + request.settledAmount()
                        + (request.paymentReference() != null ? ", reference " + request.paymentReference() : ""));
        return toResponse(claim);
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(UUID claimId) {
        Settlement settlement = settlementRepository.findByClaimId(claimId)
                .orElseThrow(() -> ResourceNotFoundException.of("Settlement for claim", claimId));
        return SettlementResponse.from(settlement);
    }

    @Transactional(readOnly = true)
    public java.util.List<StatusHistoryResponse> getHistory(UUID claimId) {
        return historyRepository.findByClaimIdOrderByChangedAtAsc(claimId).stream()
                .map(StatusHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<NoteResponse> getNotes(UUID claimId) {
        return noteRepository.findByClaimIdOrderByCreatedAtDesc(claimId).stream()
                .map(NoteResponse::from)
                .toList();
    }

    @Transactional
    public NoteResponse addNote(UUID claimId, NoteRequest request, User author) {
        Claim claim = getClaim(claimId);
        boolean internal = request.internal() == null || request.internal();
        if (internal && author.getRole() == Role.CUSTOMER) {
            internal = false;
        }
        return NoteResponse.from(addInternalNote(claim, author, request.note(), internal));
    }

    private ClaimNote addInternalNote(Claim claim, User author, String note, boolean internal) {
        ClaimNote claimNote = new ClaimNote();
        claimNote.setClaim(claim);
        claimNote.setAuthor(author);
        claimNote.setNote(note);
        claimNote.setInternal(internal);
        return noteRepository.save(claimNote);
    }

    private void audit(Claim claim, AuditAction action, User actor, String details) {
        auditService.log(actor, action, "Claim", claim.getId(), details, null);
    }

    private ClaimResponse toResponse(Claim claim) {
        ClaimAssignment assignment = assignmentRepository
                .findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(claim.getId())
                .orElse(null);
        Settlement settlement = settlementRepository.findByClaimId(claim.getId()).orElse(null);
        return ClaimResponse.from(claim, assignment, settlement);
    }

    public Claim getClaim(UUID id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Claim", id));
    }

    private void assertOfficerAssigned(Claim claim, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        assignmentRepository.findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(claim.getId())
                .filter(a -> a.getOfficer().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Claim not found or not assigned to you"));
    }

    private ClaimStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ClaimStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid claim status: " + status);
        }
    }

    private String generateUniqueClaimNumber() {
        String number;
        do {
            number = numberGenerator.claimNumber();
        } while (claimRepository.existsByClaimNumber(number));
        return number;
    }

    private String generateUniqueSettlementNumber() {
        String number;
        do {
            number = numberGenerator.settlementNumber();
        } while (settlementRepository.existsBySettlementNumber(number));
        return number;
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}