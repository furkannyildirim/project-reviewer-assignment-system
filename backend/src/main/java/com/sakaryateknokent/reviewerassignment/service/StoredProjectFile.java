package com.sakaryateknokent.reviewerassignment.service;

import java.util.UUID;

public record StoredProjectFile(
        UUID fileId,
        String storageKey,
        String originalFileName,
        String contentType,
        long fileSize
) {
}
