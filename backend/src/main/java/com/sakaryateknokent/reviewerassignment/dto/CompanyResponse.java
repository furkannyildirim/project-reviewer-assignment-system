package com.sakaryateknokent.reviewerassignment.dto;
public record CompanyResponse(
        Long id,
        String name,
        String email,
        String phone,
        String activityArea,
        String logoUrl
) {
}