package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;

import java.util.List;

public record CompanyResponse(
        Long id,
        String name,
        String activityArea,
        String city,
        String district,
        String osb,
        String address,
        String building,
        CompanyType companyType,
        List<CompanyContactMethodResponse> contactMethods,
        List<CompanyContactResponse> contacts
) {
}
