package com.sakaryateknokent.reviewerassignment.service;

import java.time.Instant;
import java.util.UUID;

record UsableProjectUpload(
        UUID uploadId,
        String storageKey,
        String originalFileName,
        String contentType,
        long fileSize,
        Instant expiresAt
) {
}
