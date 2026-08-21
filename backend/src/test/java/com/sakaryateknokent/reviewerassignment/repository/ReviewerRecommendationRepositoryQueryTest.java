package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewerRecommendationRepositoryQueryTest {

    @Test
    void projectQueryFetchesKeywordsInOneRepositoryCall() throws Exception {
        EntityGraph graph = ProjectRepository.class
                .getMethod("findForReviewerRecommendationsById", Long.class)
                .getAnnotation(EntityGraph.class);

        assertArrayEquals(new String[]{"keywords"}, graph.attributePaths());
    }

    @Test
    void academicianQueryFetchesAllKeywordsWithoutPerAcademicianLookup() throws Exception {
        Query query = AcademicianRepository.class.getMethod("findAllWithKeywords")
                .getAnnotation(Query.class);
        String normalized = normalize(query.value());

        assertTrue(normalized.contains("SELECT DISTINCT academician"));
        assertTrue(normalized.contains("LEFT JOIN FETCH academician.keywords"));
    }

    @Test
    void workloadQueryUsesDistinctProjectCountsAndCurrentProjectInOneAggregation()
            throws Exception {
        Method method = ProjectReviewerAssignmentRepository.class.getMethod(
                "findWorkloads",
                Collection.class,
                Long.class,
                AssignmentStatus.class,
                AssignmentStatus.class);
        String normalized = normalize(method.getAnnotation(Query.class).value());

        assertTrue(normalized.contains("COUNT(DISTINCT CASE"));
        assertTrue(normalized.contains(
                "assignment.status IN (:activeStatus, :completedStatus)"));
        assertTrue(normalized.contains("assignment.project.id = :projectId"));
        assertTrue(normalized.contains("assignment.academician.id IN :academicianIds"));
        assertTrue(normalized.contains("GROUP BY assignment.academician.id"));
    }

    @Test
    void projectSummaryQueryCountsDistinctActiveAndCompletedProjectsOnly()
            throws Exception {
        Method method = ProjectReviewerAssignmentRepository.class.getMethod(
                "findProjectSummary",
                Long.class,
                AssignmentStatus.class,
                AssignmentStatus.class);
        String normalized = normalize(method.getAnnotation(Query.class).value());

        assertTrue(normalized.contains(
                "assignment.status = :activeStatus"));
        assertTrue(normalized.contains(
                "assignment.status = :completedStatus"));
        assertTrue(normalized.contains(
                "assignment.status IN (:activeStatus, :completedStatus)"));
        assertTrue(normalized.contains("COUNT(DISTINCT CASE"));
        assertTrue(normalized.contains(
                "WHERE assignment.academician.id = :academicianId"));
        assertTrue(normalized.contains(
                "GROUP BY assignment.academician.id"));
        assertTrue(method.getReturnType().equals(Optional.class));
    }

    private String normalize(String query) {
        return query.replaceAll("\\s+", " ").trim();
    }
}
