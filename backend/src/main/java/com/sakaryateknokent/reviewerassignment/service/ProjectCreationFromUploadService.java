package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CreateProjectFromUploadRequest;
import com.sakaryateknokent.reviewerassignment.dto.ProjectResponse;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectCreationFromUploadService {

    private final ProjectUploadService projectUploadService;
    private final ProjectFileStorageService fileStorageService;
    private final ProjectService projectService;

    @Transactional
    public ProjectResponse create(
            CreateProjectFromUploadRequest request,
            String currentUserEmail
    ) {
        UsableProjectUpload upload = projectUploadService.getUsableUploadForCreation(
                request.uploadId(), currentUserEmail);
        Company company = projectService.findCompanyForProject(request.companyId());
        User user = projectService.findUserForProject(currentUserEmail);
        StoredProjectFile temporaryFile = new StoredProjectFile(upload.uploadId(),
                upload.storageKey(), upload.originalFileName(), upload.contentType(),
                upload.fileSize());
        StoredProjectFile permanentFile = fileStorageService.copyTemporaryToProjects(
                temporaryFile);
        registerFileCompensation(temporaryFile.storageKey(), permanentFile.storageKey());

        ProjectResponse response = projectService.createProjectFromUpload(
                request, company, user, permanentFile);
        projectUploadService.markConsumed(request.uploadId(), currentUserEmail);
        return response;
    }

    private void registerFileCompensation(
            String temporaryStorageKey,
            String permanentStorageKey
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Dosya telafisi icin transaction gerekli");
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            deleteAfterCommit(temporaryStorageKey);
                        } else {
                            deleteAfterRollback(permanentStorageKey);
                        }
                    }
                });
    }

    private void deleteAfterCommit(String temporaryStorageKey) {
        try {
            fileStorageService.deleteTemporary(temporaryStorageKey);
        } catch (RuntimeException exception) {
            log.warn("Committed project upload temporary file could not be deleted", exception);
        }
    }

    private void deleteAfterRollback(String permanentStorageKey) {
        try {
            fileStorageService.deleteProjectFile(permanentStorageKey);
        } catch (RuntimeException exception) {
            log.warn("Rolled back project permanent file could not be deleted", exception);
        }
    }
}
