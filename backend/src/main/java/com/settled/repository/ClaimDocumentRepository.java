package com.settled.repository;

import com.settled.domain.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, UUID> {

    List<ClaimDocument> findByClaimIdOrderByUploadedAtDesc(UUID claimId);

    long countByClaimId(UUID claimId);
}