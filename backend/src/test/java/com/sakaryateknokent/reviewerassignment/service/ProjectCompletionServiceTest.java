package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectReviewerAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectCompletionServiceTest {

    private static final Long PROJECT_ID = 11L;
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 8, 20);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Istanbul");

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectReviewerAssignmentRepository assignmentRepository =
            mock(ProjectReviewerAssignmentRepository.class);
    private final ProjectCompletionService service = new ProjectCompletionService(
            projectRepository,
            assignmentRepository
    );

    @Test
    void eachProjectRunsInANewTransaction() throws Exception {
        Transactional transactional = ProjectCompletionService.class
                .getMethod(
                        "completeIfDue",
                        Long.class,
                        LocalDate.class,
                        ZoneId.class
                )
                .getAnnotation(Transactional.class);

        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    @Test
    void dueActiveProjectAndOnlyActiveAssignmentsAreCompletedAtProjectMidnight() {
        Project project = project(ProjectStatus.ACTIVE, BUSINESS_DATE);
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project));

        assertTrue(service.completeIfDue(PROJECT_ID, BUSINESS_DATE, BUSINESS_ZONE));

        assertEquals(ProjectStatus.COMPLETED, project.getStatus());
        verify(assignmentRepository).completeActiveAssignments(
                PROJECT_ID,
                AssignmentStatus.ACTIVE,
                AssignmentStatus.COMPLETED,
                Instant.parse("2026-08-19T21:00:00Z")
        );
        verify(projectRepository).saveAndFlush(project);
    }

    @Test
    void dueAwaitingReviewerProjectWithoutAssignmentsIsCompleted() {
        Project project = project(ProjectStatus.AWAITING_REVIEWER, BUSINESS_DATE.minusDays(2));
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(assignmentRepository.completeActiveAssignments(
                any(), any(), any(), any()))
                .thenReturn(0);

        assertTrue(service.completeIfDue(PROJECT_ID, BUSINESS_DATE, BUSINESS_ZONE));

        assertEquals(ProjectStatus.COMPLETED, project.getStatus());
        verify(projectRepository).saveAndFlush(project);
    }

    @Test
    void futureEndDateIsNoOp() {
        assertNoOp(project(ProjectStatus.ACTIVE, BUSINESS_DATE.plusDays(1)));
    }

    @Test
    void nullEndDateIsNoOp() {
        assertNoOp(project(ProjectStatus.ACTIVE, null));
    }

    @Test
    void alreadyCompletedProjectIsNoOp() {
        assertNoOp(project(ProjectStatus.COMPLETED, BUSINESS_DATE));
    }

    @Test
    void processingSameProjectAgainIsIdempotent() {
        Project project = project(ProjectStatus.ACTIVE, BUSINESS_DATE);
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project));

        assertTrue(service.completeIfDue(PROJECT_ID, BUSINESS_DATE, BUSINESS_ZONE));
        assertFalse(service.completeIfDue(PROJECT_ID, BUSINESS_DATE, BUSINESS_ZONE));

        verify(assignmentRepository, times(1)).completeActiveAssignments(
                any(), any(), any(), any());
        verify(projectRepository, times(1)).saveAndFlush(project);
    }

    private void assertNoOp(Project project) {
        when(projectRepository.findByIdForReviewerAssignment(PROJECT_ID))
                .thenReturn(Optional.of(project));

        assertFalse(service.completeIfDue(PROJECT_ID, BUSINESS_DATE, BUSINESS_ZONE));

        verifyNoInteractions(assignmentRepository);
        verify(projectRepository, never()).saveAndFlush(any());
    }

    private Project project(ProjectStatus status, LocalDate endDate) {
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setStatus(status);
        project.setEndDate(endDate);
        return project;
    }
}
