package com.sakaryateknokent.reviewerassignment.repository;

public interface CompanyProjectSummary {

    Long getCompanyId();

    long getTotalProjectCount();

    long getAwaitingReviewerProjectCount();

    long getActiveProjectCount();

    long getCompletedProjectCount();
}
