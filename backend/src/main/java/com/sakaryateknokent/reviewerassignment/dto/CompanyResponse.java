package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record CompanyResponse(
        Long id,
        String name,
        String email,
        String phone,
        String activityArea,
        String logoUrl,
        List<CompanyContactResponse> contacts
) {
}
