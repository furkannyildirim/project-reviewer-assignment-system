package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CreateProjectFromUploadRequest;
import com.sakaryateknokent.reviewerassignment.dto.ProjectResponse;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectCreationFromUploadServiceTest {

    private final ProjectUploadService uploadService = mock(ProjectUploadService.class);
    private final ProjectFileStorageService storageService = mock(ProjectFileStorageService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectCreationFromUploadService creationService =
            new ProjectCreationFromUploadService(uploadService, storageService, projectService);

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createsFromVerifiedUploadAndCleansTemporaryFileAfterCommit() {
        UUID uploadId = UUID.randomUUID();
        CreateProjectFromUploadRequest request = request(uploadId);
        UsableProjectUpload upload = upload(uploadId);
        StoredProjectFile permanent = permanentFile();
        ProjectResponse expected = response();
        when(uploadService.getUsableUploadForCreation(uploadId, "user@example.com"))
                .thenReturn(upload);
        when(projectService.findCompanyForProject(5L)).thenReturn(company());
        when(projectService.findUserForProject("user@example.com")).thenReturn(user());
        when(storageService.copyTemporaryToProjects(any())).thenReturn(permanent);
        when(projectService.createProjectFromUpload(any(), any(), any(), any()))
                .thenReturn(expected);
        TransactionSynchronizationManager.initSynchronization();

        ProjectResponse result = creationService.create(request, "user@example.com");

        assertEquals(expected, result);
        ArgumentCaptor<StoredProjectFile> temporary = ArgumentCaptor.forClass(StoredProjectFile.class);
        verify(storageService).copyTemporaryToProjects(temporary.capture());
        assertEquals(upload.storageKey(), temporary.getValue().storageKey());
        assertEquals(upload.originalFileName(), temporary.getValue().originalFileName());
        assertEquals(upload.contentType(), temporary.getValue().contentType());
        assertEquals(upload.fileSize(), temporary.getValue().fileSize());
        verify(uploadService).markConsumed(uploadId, "user@example.com");
        verify(storageService, never()).deleteTemporary(any());

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(storageService).deleteTemporary(upload.storageKey());
        verify(storageService, never()).deleteProjectFile(permanent.storageKey());
    }

    @Test
    void doesNotCopyFileWhenCompanyDoesNotExist() {
        UUID uploadId = UUID.randomUUID();
        when(uploadService.getUsableUploadForCreation(uploadId, "user@example.com"))
                .thenReturn(upload(uploadId));
        when(projectService.findCompanyForProject(5L))
                .thenThrow(new ResourceNotFoundException("Sirket bulunamadi"));

        assertThrows(ResourceNotFoundException.class,
                () -> creationService.create(request(uploadId), "user@example.com"));

        verify(storageService, never()).copyTemporaryToProjects(any());
        verify(uploadService, never()).markConsumed(any(), any());
    }

    @Test
    void doesNotSaveProjectOrConsumeTokenWhenCopyFails() {
        UUID uploadId = UUID.randomUUID();
        prepareVerifiedInput(uploadId);
        when(storageService.copyTemporaryToProjects(any()))
                .thenThrow(new IllegalStateException("storage unavailable"));

        assertThrows(IllegalStateException.class,
                () -> creationService.create(request(uploadId), "user@example.com"));

        verify(projectService, never()).createProjectFromUpload(any(), any(), any(), any());
        verify(uploadService, never()).markConsumed(any(), any());
    }

    @Test
    void removesPermanentCopyAfterProjectSaveFailureAndKeepsTemporaryFile() {
        UUID uploadId = UUID.randomUUID();
        StoredProjectFile permanent = permanentFile();
        prepareVerifiedInput(uploadId);
        when(storageService.copyTemporaryToProjects(any())).thenReturn(permanent);
        when(projectService.createProjectFromUpload(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("database failure"));
        TransactionSynchronizationManager.initSynchronization();

        assertThrows(IllegalStateException.class,
                () -> creationService.create(request(uploadId), "user@example.com"));
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storageService).deleteProjectFile(permanent.storageKey());
        verify(storageService, never()).deleteTemporary(any());
        verify(uploadService, never()).markConsumed(any(), any());
    }

    @Test
    void removesPermanentCopyWhenConsumingUploadFails() {
        UUID uploadId = UUID.randomUUID();
        StoredProjectFile permanent = permanentFile();
        prepareVerifiedInput(uploadId);
        when(storageService.copyTemporaryToProjects(any())).thenReturn(permanent);
        when(projectService.createProjectFromUpload(any(), any(), any(), any()))
                .thenReturn(response());
        doThrow(new InvalidRequestException("Gecici yukleme kullanilamiyor"))
                .when(uploadService).markConsumed(uploadId, "user@example.com");
        TransactionSynchronizationManager.initSynchronization();

        assertThrows(InvalidRequestException.class,
                () -> creationService.create(request(uploadId), "user@example.com"));
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storageService).deleteProjectFile(permanent.storageKey());
        verify(storageService, never()).deleteTemporary(any());
    }

    @Test
    void rejectsUnusableOrOtherUsersUploadBeforeStorageChanges() {
        UUID uploadId = UUID.randomUUID();
        when(uploadService.getUsableUploadForCreation(uploadId, "other@example.com"))
                .thenThrow(new InvalidRequestException("Gecici yukleme kullanilamiyor"));

        assertThrows(InvalidRequestException.class,
                () -> creationService.create(request(uploadId), "other@example.com"));

        verify(storageService, never()).copyTemporaryToProjects(any());
    }

    private void prepareVerifiedInput(UUID uploadId) {
        when(uploadService.getUsableUploadForCreation(uploadId, "user@example.com"))
                .thenReturn(upload(uploadId));
        when(projectService.findCompanyForProject(5L)).thenReturn(company());
        when(projectService.findUserForProject("user@example.com")).thenReturn(user());
    }

    private void complete(int status) {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(sync -> sync.afterCompletion(status));
    }

    private CreateProjectFromUploadRequest request(UUID uploadId) {
        return new CreateProjectFromUploadRequest(uploadId, 5L, "Onayli Proje", "Yonetici",
                LocalDate.of(2026, 2, 1), 6, "Ar-Ge", "Yazilim", "Ozet",
                "Nitelik", "Yenilik", "Yapay Zeka", "Detay", Set.of("AI"));
    }

    private UsableProjectUpload upload(UUID uploadId) {
        return new UsableProjectUpload(uploadId,
                "temporary/11111111-1111-1111-1111-111111111111.pdf",
                "kaynak.pdf", "application/pdf", 1024,
                Instant.parse("2026-08-14T10:00:00Z"));
    }

    private StoredProjectFile permanentFile() {
        return new StoredProjectFile(UUID.randomUUID(),
                "projects/22222222-2222-2222-2222-222222222222.pdf",
                "kaynak.pdf", "application/pdf", 1024);
    }

    private Company company() {
        Company company = new Company();
        company.setId(5L);
        company.setName("Sirket");
        company.setCompanyType(CompanyType.TGB);
        return company;
    }

    private User user() {
        User user = new User();
        user.setId(3L);
        user.setFirstName("Kullanici");
        user.setLastName("Adi");
        return user;
    }

    private ProjectResponse response() {
        return new ProjectResponse(1L, 5L, "Sirket", CompanyType.TGB, 3L,
                "Kullanici Adi", "Onayli Proje", "Yonetici",
                LocalDate.of(2026, 2, 1), 6, LocalDate.of(2026, 8, 1),
                "Ar-Ge", "Yazilim", "Ozet", "Nitelik", "Yenilik", "Yapay Zeka",
                "Detay", Set.of("AI"), ProjectStatus.AWAITING_REVIEWER,
                UUID.randomUUID(), "kaynak.pdf", "application/pdf", 1024L,
                Instant.now(), Instant.now());
    }
}
