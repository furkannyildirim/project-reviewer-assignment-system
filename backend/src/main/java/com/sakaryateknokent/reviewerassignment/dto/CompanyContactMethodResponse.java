package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;

public record CompanyContactMethodResponse(
        Long id,
        CompanyContactMethodType type,
        String value,
        String label,
        boolean primary
) {
}
