package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.enums.ReviewerRecommendationLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ReviewerRecommendationMatcher {

    private static final int DIRECT_KEYWORD_SCORE = 5;
    private static final int STRUCTURED_FIELD_SCORE = 2;
    private static final int NARRATIVE_FIELD_SCORE = 1;
    private static final Comparator<String> ORIGINAL_KEYWORD_ORDER =
            String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    public ReviewerRecommendationMatch match(Project project, Academician academician) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(academician, "academician");

        Set<String> directProjectKeywords = normalizeProjectKeywords(project.getKeywords());
        List<String> structuredFields = List.of(
                valueOrEmpty(project.getName()),
                valueOrEmpty(project.getMainTechnologyArea()),
                valueOrEmpty(project.getCategory()),
                valueOrEmpty(project.getSector())
        );
        List<String> narrativeFields = List.of(
                valueOrEmpty(project.getSummary()),
                valueOrEmpty(project.getResearchAndDevelopmentNature()),
                valueOrEmpty(project.getInnovativeAspect()),
                valueOrEmpty(project.getProjectDetail())
        );

        Map<String, String> academicianKeywords = normalizeAcademicianKeywords(
                academician.getKeywords());
        List<MatchedKeyword> matches = new ArrayList<>();
        for (Map.Entry<String, String> keyword : academicianKeywords.entrySet()) {
            int score = strongestScore(keyword.getKey(), directProjectKeywords,
                    structuredFields, narrativeFields);
            if (score > 0) {
                matches.add(new MatchedKeyword(keyword.getKey(), keyword.getValue(), score,
                        score == DIRECT_KEYWORD_SCORE));
            }
        }

        int totalScore = matches.stream().mapToInt(MatchedKeyword::score).sum();
        int directCount = (int) matches.stream().filter(MatchedKeyword::direct).count();
        int matchedCount = matches.size();
        Optional<ReviewerRecommendationLevel> level = determineLevel(
                directCount, matchedCount);
        List<String> matchedKeywords = matches.stream()
                .sorted(Comparator.comparing(MatchedKeyword::canonical)
                        .thenComparing(MatchedKeyword::original, ORIGINAL_KEYWORD_ORDER))
                .map(MatchedKeyword::original)
                .toList();

        return new ReviewerRecommendationMatch(totalScore, level, directCount,
                matchedCount, matchedKeywords);
    }

    private int strongestScore(
            String keyword,
            Set<String> directProjectKeywords,
            List<String> structuredFields,
            List<String> narrativeFields
    ) {
        if (directProjectKeywords.contains(keyword)) {
            return DIRECT_KEYWORD_SCORE;
        }
        if (structuredFields.stream().anyMatch(field ->
                ReviewerRecommendationTextNormalizer.containsPhrase(field, keyword))) {
            return STRUCTURED_FIELD_SCORE;
        }
        if (narrativeFields.stream().anyMatch(field ->
                ReviewerRecommendationTextNormalizer.containsPhrase(field, keyword))) {
            return NARRATIVE_FIELD_SCORE;
        }
        return 0;
    }

    private Optional<ReviewerRecommendationLevel> determineLevel(
            int directCount,
            int matchedCount
    ) {
        if (directCount >= 2 || (directCount >= 1 && matchedCount >= 3)) {
            return Optional.of(ReviewerRecommendationLevel.HIGH);
        }
        if (directCount >= 1 || matchedCount >= 2) {
            return Optional.of(ReviewerRecommendationLevel.MEDIUM);
        }
        if (matchedCount == 1) {
            return Optional.of(ReviewerRecommendationLevel.LOW);
        }
        return Optional.empty();
    }

    private Set<String> normalizeProjectKeywords(Set<String> keywords) {
        if (keywords == null) {
            return Set.of();
        }
        return keywords.stream()
                .filter(Objects::nonNull)
                .map(ReviewerRecommendationTextNormalizer::normalizePhrase)
                .filter(keyword -> !keyword.isEmpty())
                .collect(Collectors.toSet());
    }

    private Map<String, String> normalizeAcademicianKeywords(Set<String> keywords) {
        Map<String, String> normalized = new TreeMap<>();
        if (keywords == null) {
            return normalized;
        }
        keywords.stream()
                .filter(Objects::nonNull)
                .filter(keyword -> !ReviewerRecommendationTextNormalizer
                        .normalizePhrase(keyword).isEmpty())
                .sorted(ORIGINAL_KEYWORD_ORDER)
                .forEach(keyword -> normalized.putIfAbsent(
                        ReviewerRecommendationTextNormalizer.normalizePhrase(keyword),
                        keyword));
        return normalized;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record MatchedKeyword(
            String canonical,
            String original,
            int score,
            boolean direct
    ) {
    }
}
