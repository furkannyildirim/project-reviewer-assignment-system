package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.config.ProjectFileStorageProperties;
import com.sakaryateknokent.reviewerassignment.entity.ProjectUpload;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.ProjectUploadStatus;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.ProjectUploadRepository;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-13T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ProjectUploadRepository uploadRepository = mock(ProjectUploadRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProjectFileStorageService storageService = mock(ProjectFileStorageService.class);
    private final ProjectUploadService uploadService = new ProjectUploadService(
            uploadRepository, userRepository, storageService,
            new ProjectFileStorageProperties(Path.of("storage"), DataSize.ofMegabytes(20),
                    Duration.ofHours(24)), CLOCK);

    @Test
    void createsPendingUploadForAuthenticatedUserWithConfiguredTtl() {
        User user = user("owner@example.com");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(storageService.storeTemporary(eq("project.pdf"), any()))
                .thenReturn(storedFile());
        when(uploadRepository.saveAndFlush(any(ProjectUpload.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectUploadInfo info = uploadService.createTemporaryUpload("project.pdf",
                new ByteArrayInputStream(new byte[]{1}), "owner@example.com");

        ProjectUpload saved = savedUpload();
        assertNotNull(saved.getId());
        assertEquals(ProjectUploadStatus.PENDING, saved.getStatus());
        assertEquals(user, saved.getUploadedBy());
        assertEquals(NOW.plus(Duration.ofHours(24)), saved.getExpiresAt());
        assertEquals(saved.getId(), info.uploadId());
    }

    @Test
    void removesTemporaryFileWhenDatabaseSaveFails() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user("owner@example.com")));
        when(storageService.storeTemporary(eq("project.pdf"), any())).thenReturn(storedFile());
        when(uploadRepository.saveAndFlush(any(ProjectUpload.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> uploadService.createTemporaryUpload(
                "project.pdf", new ByteArrayInputStream(new byte[]{1}), "owner@example.com"));

        verify(storageService).delete("temporary/file.pdf");
    }

    @Test
    void ownerCanReadUsablePendingUpload() {
        UUID uploadId = UUID.randomUUID();
        ProjectUpload upload = upload(uploadId, "owner@example.com", ProjectUploadStatus.PENDING,
                NOW.plusSeconds(1));
        when(uploadRepository.findByIdAndUploadedByEmail(uploadId, "owner@example.com"))
                .thenReturn(Optional.of(upload));

        ProjectUploadInfo info = uploadService.getUsableUpload(uploadId, "owner@example.com");

        assertEquals(uploadId, info.uploadId());
    }

    @Test
    void hidesOtherUsersUploadBehindGenericUnusableError() {
        UUID uploadId = UUID.randomUUID();
        when(uploadRepository.findByIdAndUploadedByEmail(uploadId, "other@example.com"))
                .thenReturn(Optional.empty());

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> uploadService.getUsableUpload(uploadId, "other@example.com"));

        assertEquals("Gecici yukleme kullanilamiyor", exception.getMessage());
    }

    @Test
    void rejectsExpiredAndConsumedUploads() {
        UUID expiredId = UUID.randomUUID();
        UUID consumedId = UUID.randomUUID();
        when(uploadRepository.findByIdAndUploadedByEmail(expiredId, "owner@example.com"))
                .thenReturn(Optional.of(upload(expiredId, "owner@example.com",
                        ProjectUploadStatus.PENDING, NOW)));
        when(uploadRepository.findByIdAndUploadedByEmail(consumedId, "owner@example.com"))
                .thenReturn(Optional.of(upload(consumedId, "owner@example.com",
                        ProjectUploadStatus.CONSUMED, NOW.plusSeconds(1))));

        assertThrows(InvalidRequestException.class,
                () -> uploadService.getUsableUpload(expiredId, "owner@example.com"));
        assertThrows(InvalidRequestException.class,
                () -> uploadService.getUsableUpload(consumedId, "owner@example.com"));
    }

    @Test
    void markConsumedSetsStatusAndTimestampAndPreventsReuse() {
        UUID uploadId = UUID.randomUUID();
        ProjectUpload upload = upload(uploadId, "owner@example.com", ProjectUploadStatus.PENDING,
                NOW.plusSeconds(1));
        when(uploadRepository.findByIdAndUploadedByEmail(uploadId, "owner@example.com"))
                .thenReturn(Optional.of(upload));
        when(uploadRepository.saveAndFlush(upload)).thenReturn(upload);

        uploadService.markConsumed(uploadId, "owner@example.com");

        assertEquals(ProjectUploadStatus.CONSUMED, upload.getStatus());
        assertEquals(NOW, upload.getConsumedAt());
        assertThrows(InvalidRequestException.class,
                () -> uploadService.getUsableUpload(uploadId, "owner@example.com"));
    }

    @Test
    void claimsOnlyExpiredPendingUploadForCleanupUsingOptimisticVersionedSave() {
        UUID uploadId = UUID.randomUUID();
        ProjectUpload upload = upload(uploadId, "owner@example.com",
                ProjectUploadStatus.PENDING, NOW.minusSeconds(1));
        when(uploadRepository.findById(uploadId)).thenReturn(Optional.of(upload));
        when(uploadRepository.saveAndFlush(upload)).thenReturn(upload);

        Optional<ExpiredProjectUpload> claimed = uploadService.claimExpiredUploadForCleanup(
                uploadId, NOW);

        assertEquals(ProjectUploadStatus.EXPIRED, upload.getStatus());
        assertEquals(uploadId, claimed.orElseThrow().uploadId());
        assertEquals("temporary/file.pdf", claimed.orElseThrow().storageKey());
        verify(uploadRepository).saveAndFlush(upload);
    }

    @Test
    void doesNotClaimConsumedOrNotYetExpiredUploadForCleanup() {
        UUID consumedId = UUID.randomUUID();
        UUID validId = UUID.randomUUID();
        when(uploadRepository.findById(consumedId)).thenReturn(Optional.of(upload(
                consumedId, "owner@example.com", ProjectUploadStatus.CONSUMED,
                NOW.minusSeconds(1))));
        when(uploadRepository.findById(validId)).thenReturn(Optional.of(upload(
                validId, "owner@example.com", ProjectUploadStatus.PENDING,
                NOW.plusSeconds(1))));

        assertEquals(Optional.empty(), uploadService.claimExpiredUploadForCleanup(consumedId, NOW));
        assertEquals(Optional.empty(), uploadService.claimExpiredUploadForCleanup(validId, NOW));
        verify(uploadRepository, never()).saveAndFlush(any(ProjectUpload.class));
    }

    @Test
    void doesNotStoreFileForUnknownUser() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> uploadService.createTemporaryUpload(
                "project.pdf", new ByteArrayInputStream(new byte[]{1}), "missing@example.com"));

        verify(storageService, never()).storeTemporary(any(), any());
    }

    @Test
    void doesNotCreateDatabaseRecordWhenStorageValidationFails() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user("owner@example.com")));
        when(storageService.storeTemporary(eq("project.pdf"), any()))
                .thenThrow(new InvalidRequestException("PDF dosyasi gecersiz"));

        assertThrows(InvalidRequestException.class, () -> uploadService.createTemporaryUpload(
                "project.pdf", new ByteArrayInputStream(new byte[]{1}), "owner@example.com"));

        verify(uploadRepository, never()).saveAndFlush(any(ProjectUpload.class));
    }

    private StoredProjectFile storedFile() {
        return new StoredProjectFile(UUID.randomUUID(), "temporary/file.pdf",
                "project.pdf", "application/pdf", 10);
    }

    private ProjectUpload upload(
            UUID id,
            String email,
            ProjectUploadStatus status,
            Instant expiresAt
    ) {
        ProjectUpload upload = new ProjectUpload();
        upload.setId(id);
        upload.setUploadedBy(user(email));
        upload.setStorageKey("temporary/file.pdf");
        upload.setOriginalFileName("project.pdf");
        upload.setContentType("application/pdf");
        upload.setFileSize(10L);
        upload.setStatus(status);
        upload.setExpiresAt(expiresAt);
        upload.setCreatedAt(NOW.minusSeconds(1));
        return upload;
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }

    private ProjectUpload savedUpload() {
        ArgumentCaptor<ProjectUpload> captor = ArgumentCaptor.forClass(ProjectUpload.class);
        verify(uploadRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
