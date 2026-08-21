package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;

public interface CompanyMatchCandidateProjection {

    Long getId();

    String getName();

    CompanyType getCompanyType();
}
