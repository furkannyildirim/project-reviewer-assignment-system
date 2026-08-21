package com.sakaryateknokent.reviewerassignment.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectUploadResponse(
        UUID uploadId,
        String originalFileName,
        String contentType,
        long fileSize,
        Instant expiresAt
) {
}
