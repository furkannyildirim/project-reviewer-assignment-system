package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;

public record ProjectCompanyCandidateResponse(
        Long id,
        String name,
        CompanyType companyType
) {
}
