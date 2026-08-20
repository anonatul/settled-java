package com.settled.dto.claim;

import com.settled.domain.ClaimNote;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        String note,
        boolean internal,
        String authorName,
        Instant createdAt
) {
    public static NoteResponse from(ClaimNote note) {
        return new NoteResponse(
                note.getId(),
                note.getNote(),
                note.isInternal(),
                note.getAuthor().getFullName(),
                note.getCreatedAt()
        );
    }
}