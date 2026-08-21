package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.config.ProjectUploadCleanupProperties;
import com.sakaryateknokent.reviewerassignment.repository.ProjectUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectUploadCleanupService {

    private final ProjectUploadRepository projectUploadRepository;
    private final ProjectUploadService projectUploadService;
    private final ProjectFileStorageService fileStorageService;
    private final ProjectUploadCleanupProperties cleanupProperties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${project.upload.cleanup.interval:1h}")
    public void cleanupExpiredUploads() {
        Instant now = clock.instant();
        for (UUID uploadId : projectUploadRepository.findCleanupCandidateIds(
                now, PageRequest.of(0, cleanupProperties.batchSize()))) {
            cleanupOne(uploadId, now);
        }
    }

    private void cleanupOne(UUID uploadId, Instant now) {
        try {
            projectUploadService.claimExpiredUploadForCleanup(uploadId, now)
                    .ifPresent(this::deleteExpiredUpload);
        } catch (RuntimeException exception) {
            log.warn("Expired project upload cleanup deferred for uploadId={}", uploadId);
        }
    }

    private void deleteExpiredUpload(ExpiredProjectUpload upload) {
        try {
            fileStorageService.deleteTemporary(upload.storageKey());
        } catch (RuntimeException exception) {
            log.warn("Expired project upload file cleanup deferred for uploadId={}",
                    upload.uploadId());
            return;
        }
        projectUploadService.deleteExpiredUpload(upload.uploadId());
    }
}
