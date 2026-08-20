package com.settled.dto.claim;

import com.settled.domain.ClaimDocument;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String fileName,
        String contentType,
        long size,
        String uploadedByName,
        Instant uploadedAt
) {
    public static DocumentResponse from(ClaimDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getContentType(),
                document.getSize(),
                document.getUploadedBy().getFullName(),
                document.getUploadedAt()
        );
    }
}