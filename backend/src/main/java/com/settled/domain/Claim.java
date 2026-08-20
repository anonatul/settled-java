package com.settled.domain;

import com.settled.domain.enums.ClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "claims", uniqueConstraints = @UniqueConstraint(columnNames = "claim_number"))
public class Claim extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false, length = 30)
    private String claimNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false, length = 80)
    private String incidentType;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountRequested;

    @Column(precision = 15, scale = 2)
    private BigDecimal amountApproved;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column
    private Instant decidedAt;

    @Column
    private Instant settledAt;
}