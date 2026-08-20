package com.settled.repository;

import com.settled.domain.ClaimAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimAssignmentRepository extends JpaRepository<ClaimAssignment, UUID> {

    Optional<ClaimAssignment> findFirstByClaimIdAndActiveTrueOrderByAssignedAtDesc(UUID claimId);

    List<ClaimAssignment> findByClaimIdOrderByAssignedAtDesc(UUID claimId);

    boolean existsByClaimIdAndActiveTrue(UUID claimId);
}