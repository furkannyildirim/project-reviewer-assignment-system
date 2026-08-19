package com.sakaryateknokent.reviewerassignment.dto;

public record AcademicianProjectSummaryResponse(
        Long academicianId,
        long activeProjectCount,
        long completedProjectCount,
        long totalProjectCount
) {
}
