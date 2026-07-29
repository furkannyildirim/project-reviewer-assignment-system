package com.sakaryateknokent.reviewerassignment.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AcademicianTextNormalizer {

    private static final Locale TURKISH_LOCALE =
            Locale.forLanguageTag("tr");

    private AcademicianTextNormalizer() {
    }

    public static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public static String normalizeEmail(String email) {
        String normalizedEmail = normalizeOptionalText(email);

        return normalizedEmail == null
                ? null
                : normalizedEmail.toLowerCase(Locale.ROOT);
    }

    public static Set<String> normalizeKeywords(Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new LinkedHashSet<>();
        }

        Map<String, String> uniqueKeywords = new LinkedHashMap<>();

        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }

            String cleanedKeyword = normalizeWhitespace(keyword);
            String comparisonValue =
                    cleanedKeyword.toLowerCase(TURKISH_LOCALE);

            uniqueKeywords.putIfAbsent(
                    comparisonValue,
                    cleanedKeyword
            );
        }

        return new LinkedHashSet<>(uniqueKeywords.values());
    }

    public static String normalizeWhitespace(String value) {
        return value
                .trim()
                .replaceAll("\\s+", " ");
    }
}
