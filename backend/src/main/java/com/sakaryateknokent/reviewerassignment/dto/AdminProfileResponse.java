package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.Role;

public record AdminProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        boolean active
) {
}
