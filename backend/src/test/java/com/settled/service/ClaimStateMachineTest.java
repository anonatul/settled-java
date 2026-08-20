package com.settled.service;

import com.settled.domain.Claim;
import com.settled.domain.enums.ClaimStatus;
import com.settled.domain.ClaimStatusHistory;
import com.settled.domain.User;
import com.settled.exception.ConflictException;
import com.settled.repository.ClaimStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimStateMachineTest {

    @Mock
    private ClaimStatusHistoryRepository historyRepository;

    private ClaimStateMachine stateMachine;
    private Claim claim;
    private User user;

    @BeforeEach
    void setUp() {
        stateMachine = new ClaimStateMachine(historyRepository);
        claim = new Claim();
        user = new User();
        lenient().when(historyRepository.save(any(ClaimStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void submittedCanMoveToUnderReview() {
        claim.setStatus(ClaimStatus.SUBMITTED);
        ClaimStatusHistory history = stateMachine.transition(claim, ClaimStatus.UNDER_REVIEW, user, "review");
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
        assertEquals(ClaimStatus.SUBMITTED, history.getFromStatus());
        verify(historyRepository).save(any(ClaimStatusHistory.class));
    }

    @Test
    void underReviewCanMoveToAdditionalInfoApprovedOrRejected() {
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        stateMachine.transition(claim, ClaimStatus.ADDITIONAL_INFO_REQUIRED, user, null);
        assertEquals(ClaimStatus.ADDITIONAL_INFO_REQUIRED, claim.getStatus());

        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        stateMachine.transition(claim, ClaimStatus.APPROVED, user, null);
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());

        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        stateMachine.transition(claim, ClaimStatus.REJECTED, user, null);
        assertEquals(ClaimStatus.REJECTED, claim.getStatus());
    }

    @Test
    void additionalInfoCanMoveBackToUnderReview() {
        claim.setStatus(ClaimStatus.ADDITIONAL_INFO_REQUIRED);
        stateMachine.transition(claim, ClaimStatus.UNDER_REVIEW, user, "info provided");
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
    }

    @Test
    void approvedCanMoveToSettled() {
        claim.setStatus(ClaimStatus.APPROVED);
        stateMachine.transition(claim, ClaimStatus.SETTLED, user, "settled");
        assertEquals(ClaimStatus.SETTLED, claim.getStatus());
    }

    @Test
    void terminalStatesRejectAllTransitions() {
        claim.setStatus(ClaimStatus.APPROVED);
        assertThrows(ConflictException.class,
                () -> stateMachine.transition(claim, ClaimStatus.UNDER_REVIEW, user, null));
        assertThrows(ConflictException.class,
                () -> stateMachine.transition(claim, ClaimStatus.REJECTED, user, null));

        claim.setStatus(ClaimStatus.REJECTED);
        assertThrows(ConflictException.class,
                () -> stateMachine.transition(claim, ClaimStatus.UNDER_REVIEW, user, null));
        assertThrows(ConflictException.class,
                () -> stateMachine.transition(claim, ClaimStatus.APPROVED, user, null));

        claim.setStatus(ClaimStatus.SETTLED);
        assertThrows(ConflictException.class,
                () -> stateMachine.transition(claim, ClaimStatus.APPROVED, user, null));
    }

    @Test
    void submittedCannotJumpToApproved() {
        claim.setStatus(ClaimStatus.SUBMITTED);
        assertThrows(ConflictException.class,
                () -> stateMachine.transition(claim, ClaimStatus.APPROVED, user, null));
    }
}