package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;

public record CompanyListResponse(
        Long id,
        String name,
        String email,
        String phone,
        String activityArea,
        String logoUrl,
        CompanyType companyType,
        CompanyContactResponse primaryContact,
        int contactCount
) {
}
