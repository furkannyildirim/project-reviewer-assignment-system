package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProjectResponse(
        Long id, Long companyId, String companyName, CompanyType companyType,
        Long createdById, String createdByName, String name, String projectManager,
        LocalDate startDate, Integer durationMonths, LocalDate endDate, String category,
        String sector, String summary, String researchAndDevelopmentNature,
        String innovativeAspect, String mainTechnologyArea, String projectDetail,
        Set<String> keywords, ProjectStatus status, UUID sourceFileId,
        String originalFileName, String contentType, Long fileSize,
        Instant createdAt, Instant updatedAt
) {
}
