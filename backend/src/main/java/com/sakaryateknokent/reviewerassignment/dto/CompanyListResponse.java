package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;

import java.util.List;

public record CompanyListResponse(
        Long id,
        String name,
        String activityArea,
        String logoUrl,
        CompanyType companyType,
        List<CompanyContactMethodResponse> contactMethods,
        CompanyContactResponse primaryContact,
        int contactCount
) {
}
