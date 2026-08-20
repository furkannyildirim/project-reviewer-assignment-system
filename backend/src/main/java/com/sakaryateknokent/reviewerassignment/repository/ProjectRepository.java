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
            SELECT project.company.id AS companyId,
                   COUNT(DISTINCT project.id) AS totalProjectCount,
                   COUNT(DISTINCT CASE
                       WHEN project.status = :draftStatus
                       THEN project.id ELSE NULL END
                   ) AS draftProjectCount,
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
                   ) AS completedProjectCount,
                   COUNT(DISTINCT CASE
                       WHEN project.status = :cancelledStatus
                       THEN project.id ELSE NULL END
                   ) AS cancelledProjectCount
            FROM Project project
            WHERE project.company.id = :companyId
            GROUP BY project.company.id
            """)
    Optional<CompanyProjectSummary> findCompanyProjectSummary(
            @Param("companyId") Long companyId,
            @Param("draftStatus") ProjectStatus draftStatus,
            @Param("awaitingReviewerStatus") ProjectStatus awaitingReviewerStatus,
            @Param("activeStatus") ProjectStatus activeStatus,
            @Param("completedStatus") ProjectStatus completedStatus,
            @Param("cancelledStatus") ProjectStatus cancelledStatus
    );
}
