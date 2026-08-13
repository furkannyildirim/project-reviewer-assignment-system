package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.ProjectReviewerAssignment;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProjectReviewerAssignmentRepository
        extends JpaRepository<ProjectReviewerAssignment, Long> {

    long countByProjectIdAndStatus(Long projectId, AssignmentStatus status);

    boolean existsByProjectIdAndStatus(Long projectId, AssignmentStatus status);

    long countByAcademicianIdAndStatus(Long academicianId, AssignmentStatus status);

    long countByAcademicianIdAndStatusIn(
            Long academicianId,
            Collection<AssignmentStatus> statuses
    );

    @Query("""
            SELECT assignment.project.id AS projectId,
                   COUNT(assignment) AS activeReviewerCount
            FROM ProjectReviewerAssignment assignment
            WHERE assignment.project.id IN :projectIds
              AND assignment.status = :status
            GROUP BY assignment.project.id
            """)
    List<ProjectActiveReviewerCount> countActiveByProjectIds(
            @Param("projectIds") Collection<Long> projectIds,
            @Param("status") AssignmentStatus status
    );
}
