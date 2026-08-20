package com.settled.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "settlements", uniqueConstraints = @UniqueConstraint(columnNames = "settlement_number"))
public class Settlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private Claim claim;

    @Column(nullable = false, length = 30)
    private String settlementNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal settledAmount;

    @Column(nullable = false)
    private LocalDate settlementDate;

    @Column(length = 100)
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processed_by", nullable = false)
    private User processedBy;

    @Column(nullable = false)
    private Instant createdAt;
}