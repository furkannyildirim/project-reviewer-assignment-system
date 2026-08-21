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
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectDeletionServiceTest {

    private static final Long PROJECT_ID = 11L;

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectReviewerAssignmentRepository assignmentRepository =
            mock(ProjectReviewerAssignmentRepository.class);
    private final ProjectService projectService = new ProjectService(
            projectRepository,
            mock(CompanyRepository.class),
            mock(UserRepository.class),
            assignmentRepository,
            mock(AcademicianRepository.class)
    );

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
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project(ProjectStatus.COMPLETED)));

        assertThrows(
                DuplicateResourceException.class,
                () -> projectService.deleteProject(PROJECT_ID)
        );

        verify(assignmentRepository, never()).deleteAllByProjectId(any());
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void deletesAllAssignmentsBeforeProject() {
        Project project = project(ProjectStatus.ACTIVE);
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project));

        projectService.deleteProject(PROJECT_ID);

        var deletionOrder = inOrder(assignmentRepository, projectRepository);
        deletionOrder.verify(assignmentRepository).deleteAllByProjectId(PROJECT_ID);
        deletionOrder.verify(projectRepository).delete(project);
    }

    private void assertStatusDeleted(ProjectStatus status) {
        Project project = project(status);
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project));

        projectService.deleteProject(PROJECT_ID);

        verify(assignmentRepository).deleteAllByProjectId(PROJECT_ID);
        verify(projectRepository).delete(project);
    }

    private Project project(ProjectStatus status) {
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setStatus(status);
        return project;
    }
}
