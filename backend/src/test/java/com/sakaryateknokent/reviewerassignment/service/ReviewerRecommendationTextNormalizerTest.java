package com.sakaryateknokent.reviewerassignment.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewerRecommendationTextNormalizerTest {

    @Test
    void appliesNfkcTurkishCaseAndUnicodeWhitespaceNormalization() {
        assertEquals("ışık iletişim",
                ReviewerRecommendationTextNormalizer.normalizePhrase(
                        "  ＩŞIK\u00A0\u202Fİletişim  "));
        assertEquals("ışık",
                ReviewerRecommendationTextNormalizer.normalizePhrase("IŞIK"));
        assertEquals("ışık",
                ReviewerRecommendationTextNormalizer.normalizePhrase("ışık"));
        assertEquals("iletişim",
                ReviewerRecommendationTextNormalizer.normalizePhrase("İLETİŞİM"));
        assertEquals("iletişim",
                ReviewerRecommendationTextNormalizer.normalizePhrase("iletişim"));
        assertEquals("ipek",
                ReviewerRecommendationTextNormalizer.normalizePhrase("İPEK"));
        assertEquals("ipek",
                ReviewerRecommendationTextNormalizer.normalizePhrase("ipek"));
    }

    @Test
    void doesNotFoldTurkishCharactersToAscii() {
        assertFalse(ReviewerRecommendationTextNormalizer.containsPhrase(
                "şeker üretimi", "seker"));
        assertFalse(ReviewerRecommendationTextNormalizer.containsPhrase(
                "ölçüm sistemi", "olcum"));
        assertFalse(ReviewerRecommendationTextNormalizer.containsPhrase(
                "çığ önleme ve güçlü ölçüm", "cig onleme ve guclu olcum"));
    }

    @Test
    void treatsPunctuationAsBoundaryAndPreservesTechnicalTokens() {
        assertEquals(List.of("veri", "analizi", "c++", "c#", ".net"),
                ReviewerRecommendationTextNormalizer.tokenize(
                        "Veri, analizi; C++ / C# ve .NET".replace(" ve ", " ")));
        assertEquals("c++ c# .net",
                ReviewerRecommendationTextNormalizer.normalizePhrase("Ｃ＋＋ C# .NET"));
    }

    @Test
    void matchesWholeContiguousTokenSequencesOnly() {
        assertTrue(ReviewerRecommendationTextNormalizer.containsPhrase(
                "Makine öğrenmesi tabanlı sistem", "makine öğrenmesi"));
        assertFalse(ReviewerRecommendationTextNormalizer.containsPhrase(
                "Makine ile öğrenmesi", "makine öğrenmesi"));
        assertFalse(ReviewerRecommendationTextNormalizer.containsPhrase(
                "Veritabanı yönetimi", "veri"));
    }
}
