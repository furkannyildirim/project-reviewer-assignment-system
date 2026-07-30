package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;

import java.util.List;

public record CompanyResponse(
        Long id,
        String name,
        String email,
        String phone,
        String secondaryPhone,
        String activityArea,
        String logoUrl,
        CompanyType companyType,
        List<CompanyContactResponse> contacts
) {
}
