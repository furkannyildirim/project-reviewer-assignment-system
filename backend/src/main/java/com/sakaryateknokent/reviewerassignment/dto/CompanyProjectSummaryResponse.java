package com.sakaryateknokent.reviewerassignment.dto;

public record CompanyProjectSummaryResponse(
        Long companyId,
        long totalProjectCount,
        long awaitingReviewerProjectCount,
        long activeProjectCount,
        long completedProjectCount
) {
}
