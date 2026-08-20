package com.settled.domain.enums;

public enum ClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    ADDITIONAL_INFO_REQUIRED,
    APPROVED,
    REJECTED,
    SETTLED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == SETTLED;
    }
}