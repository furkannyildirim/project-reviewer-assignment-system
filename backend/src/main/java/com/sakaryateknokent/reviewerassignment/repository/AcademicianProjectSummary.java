package com.sakaryateknokent.reviewerassignment.repository;

public interface AcademicianProjectSummary {

    Long getAcademicianId();

    long getActiveProjectCount();

    long getCompletedProjectCount();

    long getTotalProjectCount();
}
