package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.ProjectUpload;
import com.sakaryateknokent.reviewerassignment.enums.ProjectUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT upload.id
            FROM ProjectUpload upload
            WHERE upload.status = com.sakaryateknokent.reviewerassignment.enums.ProjectUploadStatus.EXPIRED
               OR (upload.status = com.sakaryateknokent.reviewerassignment.enums.ProjectUploadStatus.PENDING
                   AND upload.expiresAt < :now)
            ORDER BY upload.expiresAt ASC, upload.id ASC
            """)
    List<UUID> findCleanupCandidateIds(
            @Param("now") Instant now,
            Pageable pageable
    );

    @Modifying
    @Query("""
            DELETE FROM ProjectUpload upload
            WHERE upload.id = :uploadId
              AND upload.status = com.sakaryateknokent.reviewerassignment.enums.ProjectUploadStatus.EXPIRED
            """)
    int deleteExpiredById(@Param("uploadId") UUID uploadId);
}
