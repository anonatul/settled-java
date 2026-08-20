package com.settled.repository;

import com.settled.domain.ClaimNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimNoteRepository extends JpaRepository<ClaimNote, UUID> {

    List<ClaimNote> findByClaimIdOrderByCreatedAtDesc(UUID claimId);
}