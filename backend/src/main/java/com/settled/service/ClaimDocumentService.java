package com.settled.service;

import com.settled.domain.Claim;
import com.settled.domain.ClaimDocument;
import com.settled.domain.enums.ClaimStatus;
import com.settled.domain.enums.AuditAction;
import com.settled.exception.BadRequestException;
import com.settled.exception.ResourceNotFoundException;
import com.settled.repository.ClaimDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClaimDocumentService {

    private final ClaimDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    @Transactional
    public ClaimDocument upload(Claim claim, MultipartFile file, com.settled.domain.User uploader) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("File too large. Maximum allowed size is 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !isAllowed(contentType)) {
            throw new BadRequestException("Only PDF, image and document files are allowed");
        }
        if (claim.getStatus().isTerminal()) {
            throw new BadRequestException("Cannot upload documents to a " + claim.getStatus() + " claim");
        }

        FileStorageService.StoredFile stored = fileStorageService.store(file, claim.getId());
        ClaimDocument document = new ClaimDocument();
        document.setClaim(claim);
        document.setUploadedBy(uploader);
        document.setFileName(stored.originalName());
        document.setContentType(stored.contentType());
        document.setSize(stored.size());
        document.setStoragePath(stored.storagePath());
        document.setUploadedAt(Instant.now());
        ClaimDocument saved = documentRepository.save(document);

        auditService.log(uploader, AuditAction.DOCUMENT_UPLOADED, "ClaimDocument", saved.getId(),
                "Document '" + saved.getFileName() + "' uploaded for claim " + claim.getClaimNumber(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ClaimDocument> list(Claim claim) {
        return documentRepository.findByClaimIdOrderByUploadedAtDesc(claim.getId());
    }

    @Transactional(readOnly = true)
    public ClaimDocument get(UUID claimId, UUID documentId) {
        return documentRepository.findById(documentId)
                .filter(doc -> doc.getClaim().getId().equals(claimId))
                .orElseThrow(() -> ResourceNotFoundException.of("ClaimDocument", documentId));
    }

    private boolean isAllowed(String contentType) {
        return contentType.startsWith("image/")
                || contentType.equals("application/pdf")
                || contentType.contains("word")
                || contentType.contains("officedocument")
                || contentType.equals("text/plain")
                || contentType.equals("application/octet-stream");
    }
}