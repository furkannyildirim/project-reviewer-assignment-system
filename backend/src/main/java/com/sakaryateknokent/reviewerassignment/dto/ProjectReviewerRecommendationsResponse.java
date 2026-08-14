package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record ProjectReviewerRecommendationsResponse(
        Long projectId,
        boolean strongRecommendationAvailable,
        List<ReviewerRecommendationResponse> recommendations
) {
    public ProjectReviewerRecommendationsResponse {
        recommendations = List.copyOf(recommendations);
    }
}
