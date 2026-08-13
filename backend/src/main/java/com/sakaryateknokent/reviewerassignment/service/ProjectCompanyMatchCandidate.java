package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;

record ProjectCompanyMatchCandidate(
        Long id,
        String name,
        CompanyType companyType
) {
}
