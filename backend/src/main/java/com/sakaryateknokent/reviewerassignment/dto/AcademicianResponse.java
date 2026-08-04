package com.sakaryateknokent.reviewerassignment.dto;

import java.util.Set;

public record AcademicianResponse(
        Long id,
        Long version,
        String firstName,
        String lastName,
        String email,
        String phone,
        String title,
        Set<String> keywords
) {
}
