package com.sakaryateknokent.reviewerassignment.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReviewerRecommendationTextNormalizer {

    private static final Locale TURKISH_LOCALE = Locale.forLanguageTag("tr");

    private ReviewerRecommendationTextNormalizer() {
    }

    public static String normalizePhrase(String value) {
        return String.join(" ", tokenize(value));
    }

    public static List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        String compatible = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(TURKISH_LOCALE);
        int[] codePoints = compatible.codePoints().toArray();
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int index = 0; index < codePoints.length; index++) {
            int codePoint = codePoints[index];
            if (Character.isLetterOrDigit(codePoint)) {
                current.appendCodePoint(codePoint);
            } else if ((codePoint == '+' || codePoint == '#')
                    && !current.isEmpty()) {
                current.appendCodePoint(codePoint);
            } else if (codePoint == '.'
                    && current.isEmpty()
                    && index + 1 < codePoints.length
                    && Character.isLetterOrDigit(codePoints[index + 1])) {
                current.append('.');
            } else {
                addToken(tokens, current);
            }
        }
        addToken(tokens, current);
        return List.copyOf(tokens);
    }

    public static boolean containsPhrase(String text, String phrase) {
        List<String> textTokens = tokenize(text);
        List<String> phraseTokens = tokenize(phrase);
        if (phraseTokens.isEmpty() || phraseTokens.size() > textTokens.size()) {
            return false;
        }

        for (int start = 0; start <= textTokens.size() - phraseTokens.size(); start++) {
            boolean matches = true;
            for (int offset = 0; offset < phraseTokens.size(); offset++) {
                if (!textTokens.get(start + offset).equals(phraseTokens.get(offset))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private static void addToken(List<String> tokens, StringBuilder current) {
        if (!current.isEmpty()) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }
}
