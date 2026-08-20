package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.ProjectReviewerAssignment;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectReviewerAssignmentRepository
        extends JpaRepository<ProjectReviewerAssignment, Long> {

    long countByProjectIdAndStatus(Long projectId, AssignmentStatus status);

    boolean existsByProjectIdAndAcademicianIdAndStatus(
            Long projectId,
            Long academicianId,
            AssignmentStatus status
    );

    boolean existsByProjectIdAndStatus(Long projectId, AssignmentStatus status);

    @Modifying
    @Query("DELETE FROM ProjectReviewerAssignment assignment "
            + "WHERE assignment.project.id = :projectId")
    int deleteAllByProjectId(@Param("projectId") Long projectId);

    @Query("""
            SELECT assignment
            FROM ProjectReviewerAssignment assignment
            JOIN FETCH assignment.academician
            JOIN FETCH assignment.assignedBy
            WHERE assignment.project.id = :projectId
              AND assignment.status = :status
            ORDER BY assignment.assignedAt ASC, assignment.id ASC
            """)
    List<ProjectReviewerAssignment> findByProjectIdAndStatusWithPeople(
            @Param("projectId") Long projectId,
            @Param("status") AssignmentStatus status
    );

    @Query("""
            SELECT assignment
            FROM ProjectReviewerAssignment assignment
            JOIN FETCH assignment.academician
            JOIN FETCH assignment.assignedBy
            WHERE assignment.project.id = :projectId
              AND assignment.status IN :statuses
            ORDER BY assignment.endedAt DESC, assignment.id DESC
            """)
    List<ProjectReviewerAssignment> findHistoryByProjectIdWithPeople(
            @Param("projectId") Long projectId,
            @Param("statuses") Collection<AssignmentStatus> statuses
    );

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

    @Query("""
            SELECT assignment.academician.id AS academicianId,
                   COUNT(DISTINCT CASE
                       WHEN assignment.status = :activeStatus
                       THEN assignment.project.id ELSE NULL END
                   ) AS activeProjectCount,
                   COUNT(DISTINCT CASE
                       WHEN assignment.status IN (:activeStatus, :completedStatus)
                       THEN assignment.project.id ELSE NULL END
                   ) AS totalProjectCount,
                   COUNT(DISTINCT CASE
                       WHEN assignment.status = :activeStatus
                        AND assignment.project.id = :projectId
                       THEN assignment.project.id ELSE NULL END
                   ) AS currentProjectActiveAssignmentCount
            FROM ProjectReviewerAssignment assignment
            WHERE assignment.academician.id IN :academicianIds
            GROUP BY assignment.academician.id
            """)
    List<AcademicianProjectWorkload> findWorkloads(
            @Param("academicianIds") Collection<Long> academicianIds,
            @Param("projectId") Long projectId,
            @Param("activeStatus") AssignmentStatus activeStatus,
            @Param("completedStatus") AssignmentStatus completedStatus
    );

    @Query("""
            SELECT assignment.academician.id AS academicianId,
                   COUNT(DISTINCT CASE
                       WHEN assignment.status = :activeStatus
                       THEN assignment.project.id ELSE NULL END
                   ) AS activeProjectCount,
                   COUNT(DISTINCT CASE
                       WHEN assignment.status = :completedStatus
                       THEN assignment.project.id ELSE NULL END
                   ) AS completedProjectCount,
                   COUNT(DISTINCT CASE
                       WHEN assignment.status IN (:activeStatus, :completedStatus)
                       THEN assignment.project.id ELSE NULL END
                   ) AS totalProjectCount
            FROM ProjectReviewerAssignment assignment
            WHERE assignment.academician.id = :academicianId
              AND assignment.status IN (:activeStatus, :completedStatus)
            GROUP BY assignment.academician.id
            """)
    Optional<AcademicianProjectSummary> findProjectSummary(
            @Param("academicianId") Long academicianId,
            @Param("activeStatus") AssignmentStatus activeStatus,
            @Param("completedStatus") AssignmentStatus completedStatus
    );
}
