package com.sakaryateknokent.reviewerassignment.repository;

public interface AcademicianProjectWorkload {

    Long getAcademicianId();

    long getActiveProjectCount();

    long getTotalProjectCount();

    long getCurrentProjectActiveAssignmentCount();
}
