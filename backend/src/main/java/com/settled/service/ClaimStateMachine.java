package com.settled.service;

import com.settled.domain.Claim;
import com.settled.domain.enums.ClaimStatus;
import com.settled.domain.ClaimStatusHistory;
import com.settled.domain.User;
import com.settled.exception.BadRequestException;
import com.settled.exception.ConflictException;
import com.settled.repository.ClaimStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClaimStateMachine {

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED = new EnumMap<>(ClaimStatus.class);

    static {
        ALLOWED.put(ClaimStatus.SUBMITTED, EnumSet.of(ClaimStatus.UNDER_REVIEW));
        ALLOWED.put(ClaimStatus.UNDER_REVIEW, EnumSet.of(
                ClaimStatus.ADDITIONAL_INFO_REQUIRED,
                ClaimStatus.APPROVED,
                ClaimStatus.REJECTED));
        ALLOWED.put(ClaimStatus.ADDITIONAL_INFO_REQUIRED, EnumSet.of(ClaimStatus.UNDER_REVIEW));
        ALLOWED.put(ClaimStatus.APPROVED, EnumSet.of(ClaimStatus.SETTLED));
        ALLOWED.put(ClaimStatus.REJECTED, EnumSet.noneOf(ClaimStatus.class));
        ALLOWED.put(ClaimStatus.SETTLED, EnumSet.noneOf(ClaimStatus.class));
    }

    private final ClaimStatusHistoryRepository historyRepository;

    @Transactional
    public ClaimStatusHistory transition(Claim claim, ClaimStatus target, User changedBy, String note) {
        ClaimStatus from = claim.getStatus();
        if (!ALLOWED.getOrDefault(from, EnumSet.noneOf(ClaimStatus.class)).contains(target)) {
            throw new ConflictException("Invalid claim status transition: " + from + " -> " + target);
        }
        claim.setStatus(target);

        ClaimStatusHistory history = new ClaimStatusHistory();
        history.setClaim(claim);
        history.setFromStatus(from);
        history.setToStatus(target);
        history.setChangedBy(changedBy);
        history.setNote(note);
        history.setChangedAt(Instant.now());
        return historyRepository.save(history);
    }
}