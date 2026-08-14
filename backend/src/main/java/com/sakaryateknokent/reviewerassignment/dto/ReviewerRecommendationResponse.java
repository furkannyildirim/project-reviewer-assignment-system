package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.ReviewerRecommendationLevel;

import java.util.List;

public record ReviewerRecommendationResponse(
        Long academicianId,
        String firstName,
        String lastName,
        String title,
        ReviewerRecommendationLevel level,
        List<String> matchedKeywords,
        long activeProjectCount,
        long totalProjectCount
) {
    public ReviewerRecommendationResponse {
        matchedKeywords = List.copyOf(matchedKeywords);
    }
}
