package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectCancellationQueryTest {

    @Test
    void academicianSummaryCountsOnlyActiveAndCompletedAssignments()
            throws Exception {
        Method method = ProjectReviewerAssignmentRepository.class.getMethod(
                "findProjectSummary",
                Long.class,
                AssignmentStatus.class,
                AssignmentStatus.class
        );
        String query = normalize(method.getAnnotation(Query.class).value());

        assertTrue(query.contains(
                "assignment.status IN (:activeStatus, :completedStatus)"
        ));
        assertFalse(query.contains("CANCELLED"));
    }

    @Test
    void recommendationWorkloadCountsOnlyActiveAndCompletedAssignments()
            throws Exception {
        Method method = ProjectReviewerAssignmentRepository.class.getMethod(
                "findWorkloads",
                Collection.class,
                Long.class,
                AssignmentStatus.class,
                AssignmentStatus.class
        );
        String query = normalize(method.getAnnotation(Query.class).value());

        assertTrue(query.contains(
                "assignment.status IN (:activeStatus, :completedStatus)"
        ));
        assertFalse(query.contains("CANCELLED"));
    }

    @Test
    void companySummaryKeepsTotalAndCountsCancelledStatusSeparately()
            throws Exception {
        Method method = ProjectRepository.class.getMethod(
                "findCompanyProjectSummary",
                Long.class,
                ProjectStatus.class,
                ProjectStatus.class,
                ProjectStatus.class,
                ProjectStatus.class,
                ProjectStatus.class
        );
        String query = normalize(method.getAnnotation(Query.class).value());

        assertTrue(query.contains("COUNT(DISTINCT project.id) AS totalProjectCount"));
        assertTrue(query.contains("project.status = :cancelledStatus"));
        assertTrue(query.contains(") AS cancelledProjectCount"));
    }

    private String normalize(String query) {
        return query.replaceAll("\\s+", " ").trim();
    }
}
