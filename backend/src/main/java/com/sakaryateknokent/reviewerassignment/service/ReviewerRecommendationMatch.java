package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.ReviewerRecommendationLevel;

import java.util.List;
import java.util.Optional;

public record ReviewerRecommendationMatch(
        int totalScore,
        Optional<ReviewerRecommendationLevel> level,
        int directKeywordMatchCount,
        int matchedKeywordCount,
        List<String> matchedKeywords
) {
    public ReviewerRecommendationMatch {
        level = level == null ? Optional.empty() : level;
        matchedKeywords = List.copyOf(matchedKeywords);
    }

    public boolean isRecommendation() {
        return level.isPresent();
    }
}
