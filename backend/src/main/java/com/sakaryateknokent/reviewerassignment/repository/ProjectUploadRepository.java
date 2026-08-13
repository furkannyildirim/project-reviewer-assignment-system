package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.ProjectUpload;
import com.sakaryateknokent.reviewerassignment.enums.ProjectUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectUploadRepository extends JpaRepository<ProjectUpload, UUID> {

    Optional<ProjectUpload> findByIdAndUploadedByEmail(UUID id, String email);

    List<ProjectUpload> findByStatusAndExpiresAtBefore(
            ProjectUploadStatus status,
            Instant expiresAt
    );
}
