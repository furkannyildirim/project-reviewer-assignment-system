package com.sakaryateknokent.reviewerassignment.dto;

public record CompanyContactResponse(
        Long id,
        String fullName,
        String position,
        String email,
        String phone,
        boolean primary
) {
}