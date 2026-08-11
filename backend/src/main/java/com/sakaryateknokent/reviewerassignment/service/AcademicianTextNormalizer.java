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
        if (value == null) {
            return null;
        }

        String normalized = normalizeWhitespace(value);

        return normalized.isEmpty() ? null : normalized;
    }

    public static String normalizeEmail(String email) {
        String normalizedEmail = normalizeOptionalText(email);

        return normalizedEmail == null
                ? null
                : normalizedEmail.toLowerCase(Locale.ROOT);
    }

    public static String canonicalizeIdentityPart(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return normalizeWhitespace(value).toLowerCase(TURKISH_LOCALE);
    }

    public static Set<String> normalizeKeywords(Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new LinkedHashSet<>();
        }

        Map<String, String> uniqueKeywords = new LinkedHashMap<>();

        for (String keyword : keywords) {
            if (keyword == null) {
                continue;
            }

            String cleanedKeyword = normalizeWhitespace(keyword);
            if (cleanedKeyword.isEmpty()) {
                continue;
            }
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
        StringBuilder normalized = new StringBuilder();
        boolean pendingSpace = false;

        for (int index = 0; index < value.length(); ) {
            int codePoint = value.codePointAt(index);
            if (isNormalizableWhitespace(codePoint)) {
                if (!normalized.isEmpty()) {
                    pendingSpace = true;
                }
            } else {
                if (pendingSpace) {
                    normalized.append(' ');
                    pendingSpace = false;
                }
                normalized.appendCodePoint(codePoint);
            }
            index += Character.charCount(codePoint);
        }

        return normalized.toString();
    }

    private static boolean isNormalizableWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint);
    }
}
