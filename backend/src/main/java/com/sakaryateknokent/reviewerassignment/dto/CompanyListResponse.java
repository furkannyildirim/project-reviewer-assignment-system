package com.sakaryateknokent.reviewerassignment.dto;

public record CompanyListResponse(
        Long id,
        String name,
        String email,
        String phone,
        String activityArea,
        String logoUrl,
        CompanyContactResponse primaryContact,
        int contactCount
) {
}
