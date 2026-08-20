package com.sakaryateknokent.reviewerassignment.repository;

public interface CompanyProjectSummary {

    Long getCompanyId();

    long getTotalProjectCount();

    long getDraftProjectCount();

    long getAwaitingReviewerProjectCount();

    long getActiveProjectCount();

    long getCompletedProjectCount();

    long getCancelledProjectCount();
}
