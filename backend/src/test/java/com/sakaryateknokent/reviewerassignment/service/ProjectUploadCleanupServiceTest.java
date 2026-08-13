package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.config.ProjectUploadCleanupProperties;
import com.sakaryateknokent.reviewerassignment.repository.ProjectUploadRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectUploadCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-13T10:00:00Z");

    private final ProjectUploadRepository uploadRepository = mock(ProjectUploadRepository.class);
    private final ProjectUploadService uploadService = mock(ProjectUploadService.class);
    private final ProjectFileStorageService fileStorageService = mock(ProjectFileStorageService.class);
    private final ProjectUploadCleanupService cleanupService = new ProjectUploadCleanupService(
            uploadRepository, uploadService, fileStorageService,
            new ProjectUploadCleanupProperties(Duration.ofHours(1), 25),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void deletesOnlyClaimedExpiredTemporaryFileThenDatabaseRecord() {
        UUID uploadId = UUID.randomUUID();
        ExpiredProjectUpload upload = new ExpiredProjectUpload(uploadId,
                "temporary/11111111-1111-1111-1111-111111111111.pdf");
        when(uploadRepository.findCleanupCandidateIds(eq(NOW), any()))
                .thenReturn(List.of(uploadId));
        when(uploadService.claimExpiredUploadForCleanup(uploadId, NOW))
                .thenReturn(Optional.of(upload));

        cleanupService.cleanupExpiredUploads();

        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(uploadRepository).findCleanupCandidateIds(eq(NOW), pageable.capture());
        assertEquals(25, pageable.getValue().getPageSize());
        verify(fileStorageService).deleteTemporary(upload.storageKey());
        verify(uploadService).deleteExpiredUpload(uploadId);
        verify(fileStorageService, never()).deleteProjectFile(any());
    }

    @Test
    void leavesExpiredRecordForRetryWhenTemporaryFileDeletionFails() {
        UUID uploadId = UUID.randomUUID();
        ExpiredProjectUpload upload = new ExpiredProjectUpload(uploadId,
                "temporary/11111111-1111-1111-1111-111111111111.pdf");
        when(uploadRepository.findCleanupCandidateIds(eq(NOW), any()))
                .thenReturn(List.of(uploadId));
        when(uploadService.claimExpiredUploadForCleanup(uploadId, NOW))
                .thenReturn(Optional.of(upload));
        doThrow(new IllegalStateException("storage unavailable"))
                .when(fileStorageService).deleteTemporary(upload.storageKey());

        cleanupService.cleanupExpiredUploads();

        verify(uploadService, never()).deleteExpiredUpload(any());
    }

    @Test
    void skipsCandidateThatCouldNotBeClaimedBecauseItWasConsumedConcurrently() {
        UUID uploadId = UUID.randomUUID();
        when(uploadRepository.findCleanupCandidateIds(eq(NOW), any()))
                .thenReturn(List.of(uploadId));
        when(uploadService.claimExpiredUploadForCleanup(uploadId, NOW))
                .thenReturn(Optional.empty());

        cleanupService.cleanupExpiredUploads();

        verify(fileStorageService, never()).deleteTemporary(any());
        verify(uploadService, never()).deleteExpiredUpload(any());
    }
}
