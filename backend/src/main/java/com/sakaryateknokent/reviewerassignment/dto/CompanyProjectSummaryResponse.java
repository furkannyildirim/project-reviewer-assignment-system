package com.sakaryateknokent.reviewerassignment.dto;

public record CompanyProjectSummaryResponse(
        Long companyId,
        long totalProjectCount,
        long draftProjectCount,
        long awaitingReviewerProjectCount,
        long activeProjectCount,
        long completedProjectCount,
        long cancelledProjectCount
) {
}
