package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

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
}
