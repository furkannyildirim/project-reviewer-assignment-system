package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.ProjectFormField;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProjectDocumentPreviewResponse(
        UUID uploadId,
        String originalFileName,
        String contentType,
        long fileSize,
        Instant expiresAt,
        String templateCode,
        String companyName,
        String name,
        String projectManager,
        LocalDate startDate,
        Integer durationMonths,
        LocalDate endDate,
        String category,
        String sector,
        String summary,
        String researchAndDevelopmentNature,
        String innovativeAspect,
        String mainTechnologyArea,
        String projectDetail,
        List<String> keywords,
        List<ProjectFormField> missingFields,
        List<ProjectFormField> reviewRequiredFields,
        List<String> warnings,
        ProjectCompanyMatchResponse companyMatch
) {
}
