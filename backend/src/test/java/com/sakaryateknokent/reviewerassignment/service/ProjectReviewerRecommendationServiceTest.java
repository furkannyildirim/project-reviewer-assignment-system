package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerRecommendationsResponse;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.enums.ReviewerRecommendationLevel;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianProjectWorkload;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectReviewerAssignmentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectReviewerRecommendationServiceTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final AcademicianRepository academicianRepository = mock(AcademicianRepository.class);
    private final ProjectReviewerAssignmentRepository assignmentRepository =
            mock(ProjectReviewerAssignmentRepository.class);
    private final ReviewerRecommendationMatcher matcher = mock(ReviewerRecommendationMatcher.class);
    private final ProjectReviewerRecommendationService service =
            new ProjectReviewerRecommendationService(projectRepository,
                    academicianRepository, assignmentRepository, matcher);

    @Test
    void returnsOrderedRecommendationsWithWorkloadsAndExcludesCurrentActiveReviewer() {
        Project project = project(10L);
        Academician medium = academician(2L, "Ayşe", "Zengin");
        Academician highSecond = academician(3L, "Cem", "Akın");
        Academician assignedHigh = academician(4L, "Deniz", "Kaya");
        Academician highFirst = academician(1L, "Bora", "Akın");
        when(projectRepository.findForReviewerRecommendationsById(10L))
                .thenReturn(Optional.of(project));
        when(academicianRepository.findAllWithKeywords())
                .thenReturn(List.of(medium, highSecond, assignedHigh, highFirst));
        when(matcher.match(project, medium)).thenReturn(match(5,
                ReviewerRecommendationLevel.MEDIUM, 1, 1, "Robotik"));
        when(matcher.match(project, highSecond)).thenReturn(match(10,
                ReviewerRecommendationLevel.HIGH, 2, 2, "AI", "Robotik"));
        when(matcher.match(project, assignedHigh)).thenReturn(match(12,
                ReviewerRecommendationLevel.HIGH, 2, 3, "AI", "Robotik", "IoT"));
        when(matcher.match(project, highFirst)).thenReturn(match(10,
                ReviewerRecommendationLevel.HIGH, 2, 2, "AI", "Robotik"));
        List<AcademicianProjectWorkload> workloads = List.of(
                workload(1L, 8, 20, 0),
                workload(2L, 1, 2, 0),
                workload(3L, 0, 0, 0),
                workload(4L, 3, 7, 1));
        when(assignmentRepository.findWorkloads(
                List.of(2L, 3L, 4L, 1L), 10L, AssignmentStatus.ACTIVE))
                .thenReturn(workloads);

        ProjectReviewerRecommendationsResponse response =
                service.getRecommendations(10L);

        assertTrue(response.strongRecommendationAvailable());
        assertEquals(List.of(1L, 3L, 2L), response.recommendations().stream()
                .map(recommendation -> recommendation.academicianId()).toList());
        assertEquals(8, response.recommendations().getFirst().activeProjectCount());
        assertEquals(20, response.recommendations().getFirst().totalProjectCount());
        assertEquals(List.of("AI", "Robotik"),
                response.recommendations().getFirst().matchedKeywords());
        verify(projectRepository).findForReviewerRecommendationsById(10L);
        verify(academicianRepository).findAllWithKeywords();
        verify(assignmentRepository).findWorkloads(
                List.of(2L, 3L, 4L, 1L), 10L, AssignmentStatus.ACTIVE);
    }

    @Test
    void skipsWorkloadQueryWhenNoAcademicianMatches() {
        Project project = project(10L);
        Academician academician = academician(1L, "Ali", "Yılmaz");
        when(projectRepository.findForReviewerRecommendationsById(10L))
                .thenReturn(Optional.of(project));
        when(academicianRepository.findAllWithKeywords()).thenReturn(List.of(academician));
        when(matcher.match(project, academician)).thenReturn(
                new ReviewerRecommendationMatch(0, Optional.empty(), 0, 0, List.of()));

        ProjectReviewerRecommendationsResponse response =
                service.getRecommendations(10L);

        assertFalse(response.strongRecommendationAvailable());
        assertTrue(response.recommendations().isEmpty());
        verify(assignmentRepository, never()).findWorkloads(any(), any(), any());
    }

    @Test
    void returnsNotFoundWithoutLoadingAcademiciansWhenProjectDoesNotExist() {
        when(projectRepository.findForReviewerRecommendationsById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getRecommendations(99L));
        verify(academicianRepository, never()).findAllWithKeywords();
        verify(assignmentRepository, never()).findWorkloads(any(), any(), any());
    }

    @Test
    void missingWorkloadProjectionProducesZeroCounts() {
        Project project = project(10L);
        Academician academician = academician(1L, "Ali", "Yılmaz");
        when(projectRepository.findForReviewerRecommendationsById(10L))
                .thenReturn(Optional.of(project));
        when(academicianRepository.findAllWithKeywords()).thenReturn(List.of(academician));
        when(matcher.match(project, academician)).thenReturn(match(2,
                ReviewerRecommendationLevel.LOW, 0, 1, "Robotik"));
        when(assignmentRepository.findWorkloads(
                List.of(1L), 10L, AssignmentStatus.ACTIVE)).thenReturn(List.of());

        var recommendation = service.getRecommendations(10L)
                .recommendations().getFirst();

        assertEquals(0, recommendation.activeProjectCount());
        assertEquals(0, recommendation.totalProjectCount());
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setKeywords(Set.of("Robotik"));
        return project;
    }

    private Academician academician(Long id, String firstName, String lastName) {
        Academician academician = new Academician();
        academician.setId(id);
        academician.setFirstName(firstName);
        academician.setLastName(lastName);
        academician.setTitle("Dr. Öğr. Üyesi");
        academician.setKeywords(Set.of("Robotik"));
        return academician;
    }

    private ReviewerRecommendationMatch match(
            int score,
            ReviewerRecommendationLevel level,
            int directCount,
            int matchedCount,
            String... keywords
    ) {
        return new ReviewerRecommendationMatch(score, Optional.of(level), directCount,
                matchedCount, List.of(keywords));
    }

    private AcademicianProjectWorkload workload(
            Long academicianId,
            long activeCount,
            long totalCount,
            long currentCount
    ) {
        AcademicianProjectWorkload workload = mock(AcademicianProjectWorkload.class);
        when(workload.getAcademicianId()).thenReturn(academicianId);
        when(workload.getActiveProjectCount()).thenReturn(activeCount);
        when(workload.getTotalProjectCount()).thenReturn(totalCount);
        when(workload.getCurrentProjectActiveAssignmentCount()).thenReturn(currentCount);
        return workload;
    }
}
