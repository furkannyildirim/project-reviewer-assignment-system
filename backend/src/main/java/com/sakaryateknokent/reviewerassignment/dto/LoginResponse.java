package com.sakaryateknokent.reviewerassignment.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}