package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.config.ProjectFileStorageProperties;
import com.sakaryateknokent.reviewerassignment.entity.ProjectUpload;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.ProjectUploadStatus;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.ProjectUploadRepository;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectUploadService {

    private static final String UNUSABLE_UPLOAD_MESSAGE =
            "Gecici yukleme kullanilamiyor";

    private final ProjectUploadRepository projectUploadRepository;
    private final UserRepository userRepository;
    private final ProjectFileStorageService fileStorageService;
    private final ProjectFileStorageProperties storageProperties;
    private final Clock clock;

    public ProjectUploadInfo createTemporaryUpload(
            MultipartFile file,
            String currentUserEmail
    ) {
        if (file == null) {
            throw new InvalidRequestException("Dosya zorunludur");
        }
        try {
            return createTemporaryUpload(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    currentUserEmail
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("Dosya okunamadi", exception);
        }
    }

    public ProjectUploadInfo createTemporaryUpload(
            String originalFileName,
            InputStream file,
            String currentUserEmail
    ) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanici bulunamadi"));
        StoredProjectFile storedFile = fileStorageService.storeTemporary(
                originalFileName, file);
        Instant now = clock.instant();

        ProjectUpload upload = new ProjectUpload();
        upload.setId(UUID.randomUUID());
        upload.setUploadedBy(user);
        upload.setStorageKey(storedFile.storageKey());
        upload.setOriginalFileName(storedFile.originalFileName());
        upload.setContentType(storedFile.contentType());
        upload.setFileSize(storedFile.fileSize());
        upload.setStatus(ProjectUploadStatus.PENDING);
        upload.setCreatedAt(now);
        upload.setExpiresAt(now.plus(storageProperties.uploadTtl()));

        try {
            return toInfo(projectUploadRepository.saveAndFlush(upload));
        } catch (RuntimeException exception) {
            try {
                fileStorageService.delete(storedFile.storageKey());
            } catch (RuntimeException ignored) {
                // Preserve the database error; cleanup is best effort.
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ProjectUploadInfo getUsableUpload(
            UUID uploadId,
            String currentUserEmail
    ) {
        return toInfo(findUsableUpload(uploadId, currentUserEmail));
    }

    @Transactional(readOnly = true)
    public OpenedProjectUpload openUsableUpload(
            UUID uploadId,
            String currentUserEmail
    ) {
        ProjectUpload upload = findUsableUpload(uploadId, currentUserEmail);
        InputStream inputStream = fileStorageService.loadTemporary(upload.getStorageKey());
        return new OpenedProjectUpload(toInfo(upload), inputStream);
    }

    public ProjectUploadInfo markConsumed(UUID uploadId, String currentUserEmail) {
        ProjectUpload upload = findUsableUpload(uploadId, currentUserEmail);
        upload.setStatus(ProjectUploadStatus.CONSUMED);
        upload.setConsumedAt(clock.instant());
        return toInfo(projectUploadRepository.saveAndFlush(upload));
    }

    private ProjectUpload findUsableUpload(UUID uploadId, String currentUserEmail) {
        ProjectUpload upload = projectUploadRepository
                .findByIdAndUploadedByEmail(uploadId, currentUserEmail)
                .orElseThrow(() -> new InvalidRequestException(UNUSABLE_UPLOAD_MESSAGE));
        if (!ProjectUploadStatus.PENDING.equals(upload.getStatus())
                || !upload.getExpiresAt().isAfter(clock.instant())) {
            throw new InvalidRequestException(UNUSABLE_UPLOAD_MESSAGE);
        }
        return upload;
    }

    private ProjectUploadInfo toInfo(ProjectUpload upload) {
        return new ProjectUploadInfo(upload.getId(), upload.getOriginalFileName(),
                upload.getContentType(), upload.getFileSize(), upload.getExpiresAt());
    }
}
