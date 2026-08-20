package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectReviewerAssignmentRepository;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectDeletionServiceTest {

    private static final Long PROJECT_ID = 11L;
    private static final String PERMANENT_STORAGE_KEY =
            "projects/00000000-0000-0000-0000-000000000011.pdf";

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectReviewerAssignmentRepository assignmentRepository =
            mock(ProjectReviewerAssignmentRepository.class);
    private final ProjectFileStorageService fileStorageService =
            mock(ProjectFileStorageService.class);
    private final ProjectService projectService = new ProjectService(
            projectRepository,
            mock(CompanyRepository.class),
            mock(UserRepository.class),
            assignmentRepository,
            mock(AcademicianRepository.class),
            fileStorageService
    );

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletesAwaitingReviewerProject() {
        assertStatusDeleted(ProjectStatus.AWAITING_REVIEWER);
    }

    @Test
    void deletesActiveProject() {
        assertStatusDeleted(ProjectStatus.ACTIVE);
    }

    @Test
    void missingProjectReturnsNotFound() {
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> projectService.deleteProject(PROJECT_ID)
        );

        verify(assignmentRepository, never()).deleteAllByProjectId(any());
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void completedProjectIsRejected() {
        assertStatusRejected(ProjectStatus.COMPLETED);
    }

    @Test
    void deletesAllAssignmentsBeforeProject() {
        Project project = project(ProjectStatus.ACTIVE, null);
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project));

        projectService.deleteProject(PROJECT_ID);

        var deletionOrder = inOrder(assignmentRepository, projectRepository);
        deletionOrder.verify(assignmentRepository).deleteAllByProjectId(PROJECT_ID);
        deletionOrder.verify(projectRepository).delete(project);
    }

    @Test
    void permanentFileIsDeletedOnlyAfterCommit() {
        beginSynchronization();
        Project project = deletableProjectWithPermanentFile();

        projectService.deleteProject(PROJECT_ID);

        verify(projectRepository).delete(project);
        verify(fileStorageService, never()).deleteProjectFile(any());

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(fileStorageService).deleteProjectFile(PERMANENT_STORAGE_KEY);
    }

    @Test
    void permanentFileIsPreservedAfterRollback() {
        beginSynchronization();
        deletableProjectWithPermanentFile();

        projectService.deleteProject(PROJECT_ID);
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorageService, never()).deleteProjectFile(any());
    }

    @Test
    void fileCleanupFailureAfterCommitDoesNotFailCompletedDeletion() {
        beginSynchronization();
        deletableProjectWithPermanentFile();
        doThrow(new RuntimeException("storage failure"))
                .when(fileStorageService)
                .deleteProjectFile(PERMANENT_STORAGE_KEY);

        projectService.deleteProject(PROJECT_ID);

        assertDoesNotThrow(() -> complete(TransactionSynchronization.STATUS_COMMITTED));
        verify(projectRepository).delete(any(Project.class));
    }

    private void assertStatusRejected(ProjectStatus status) {
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project(status, null)));

        assertThrows(
                DuplicateResourceException.class,
                () -> projectService.deleteProject(PROJECT_ID)
        );

        verify(assignmentRepository, never()).deleteAllByProjectId(PROJECT_ID);
        verify(projectRepository, never()).delete(any());
    }

    private void assertStatusDeleted(ProjectStatus status) {
        Project project = project(status, null);
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project));

        projectService.deleteProject(PROJECT_ID);

        verify(assignmentRepository).deleteAllByProjectId(PROJECT_ID);
        verify(projectRepository).delete(project);
    }

    private Project deletableProjectWithPermanentFile() {
        Project project = project(
                ProjectStatus.AWAITING_REVIEWER,
                PERMANENT_STORAGE_KEY
        );
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project));
        return project;
    }

    private Project project(ProjectStatus status, String storageKey) {
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setStatus(status);
        project.setStorageKey(storageKey);
        return project;
    }

    private void beginSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    private void complete(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(status));
        TransactionSynchronizationManager.clearSynchronization();
    }
}
