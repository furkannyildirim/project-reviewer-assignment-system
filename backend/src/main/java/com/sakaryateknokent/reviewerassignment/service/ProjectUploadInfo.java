package com.sakaryateknokent.reviewerassignment.service;

import java.time.Instant;
import java.util.UUID;

public record ProjectUploadInfo(
        UUID uploadId,
        String originalFileName,
        String contentType,
        long fileSize,
        Instant expiresAt
) {
}
