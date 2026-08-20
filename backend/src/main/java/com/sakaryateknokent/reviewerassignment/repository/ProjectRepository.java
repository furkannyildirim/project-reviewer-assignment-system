package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @EntityGraph(attributePaths = {"company", "createdBy", "keywords"})
    @Query("SELECT project FROM Project project WHERE project.id = :id")
    Optional<Project> findDetailById(Long id);

    @EntityGraph(attributePaths = "keywords")
    @Query("SELECT project FROM Project project WHERE project.id = :id")
    Optional<Project> findForReviewerRecommendationsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT project FROM Project project WHERE project.id = :id")
    Optional<Project> findByIdForReviewerAssignment(Long id);

    @Override
    @EntityGraph(attributePaths = "company")
    Page<Project> findAll(Pageable pageable);

    long countByCompanyId(Long companyId);

    @Query("""
            SELECT project.id
            FROM Project project
            WHERE project.status IN :statuses
              AND project.endDate IS NOT NULL
              AND project.endDate <= :today
            ORDER BY project.endDate ASC, project.id ASC
            """)
    List<Long> findCompletionCandidateIds(
            @Param("statuses") Collection<ProjectStatus> statuses,
            @Param("today") LocalDate today
    );

    @Query("""
            SELECT project.company.id AS companyId,
                   COUNT(DISTINCT project.id) AS totalProjectCount,
                   COUNT(DISTINCT CASE
                       WHEN project.status = :awaitingReviewerStatus
                       THEN project.id ELSE NULL END
                   ) AS awaitingReviewerProjectCount,
                   COUNT(DISTINCT CASE
                       WHEN project.status = :activeStatus
                       THEN project.id ELSE NULL END
                   ) AS activeProjectCount,
                   COUNT(DISTINCT CASE
                       WHEN project.status = :completedStatus
                       THEN project.id ELSE NULL END
                   ) AS completedProjectCount
            FROM Project project
            WHERE project.company.id = :companyId
            GROUP BY project.company.id
            """)
    Optional<CompanyProjectSummary> findCompanyProjectSummary(
            @Param("companyId") Long companyId,
            @Param("awaitingReviewerStatus") ProjectStatus awaitingReviewerStatus,
            @Param("activeStatus") ProjectStatus activeStatus,
            @Param("completedStatus") ProjectStatus completedStatus
    );
}
