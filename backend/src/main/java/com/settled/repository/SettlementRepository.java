package com.settled.repository;

import com.settled.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    Optional<Settlement> findByClaimId(UUID claimId);

    boolean existsByClaimId(UUID claimId);

    boolean existsBySettlementNumber(String settlementNumber);
}