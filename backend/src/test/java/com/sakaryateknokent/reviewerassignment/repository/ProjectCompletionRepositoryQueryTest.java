package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectCompletionRepositoryQueryTest {

    @Test
    void projectProcessingReusesPessimisticWriteLock() throws Exception {
        Lock lock = ProjectRepository.class
                .getMethod("findByIdForReviewerAssignment", Long.class)
                .getAnnotation(Lock.class);

        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }

    @Test
    void candidateQuerySelectsOnlyDueNonNullProjectsInEligibleStatuses()
            throws Exception {
        Method method = ProjectRepository.class.getMethod(
                "findCompletionCandidateIds",
                Collection.class,
                LocalDate.class
        );
        String query = normalize(method.getAnnotation(Query.class).value());

        assertTrue(query.contains("project.status IN :statuses"));
        assertTrue(query.contains("project.endDate IS NOT NULL"));
        assertTrue(query.contains("project.endDate <= :today"));
    }

    @Test
    void assignmentUpdateChangesOnlyActiveRowsAndSetsEffectiveEndTime()
            throws Exception {
        Method method = ProjectReviewerAssignmentRepository.class.getMethod(
                "completeActiveAssignments",
                Long.class,
                AssignmentStatus.class,
                AssignmentStatus.class,
                Instant.class
        );
        String query = normalize(method.getAnnotation(Query.class).value());

        assertTrue(query.contains("SET assignment.status = :completedStatus"));
        assertTrue(query.contains("assignment.endedAt = :endedAt"));
        assertTrue(query.contains("assignment.project.id = :projectId"));
        assertTrue(query.contains("assignment.status = :activeStatus"));
    }

    private String normalize(String query) {
        return query.replaceAll("\\s+", " ").trim();
    }
}
