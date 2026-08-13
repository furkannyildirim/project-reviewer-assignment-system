package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ProjectListResponse(
        Long id, String name, Long companyId, String companyName,
        CompanyType companyType, String category, String sector,
        LocalDate startDate, LocalDate endDate, ProjectStatus status,
        long activeReviewerCount, Instant createdAt
) {
}
