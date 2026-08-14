package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.enums.ReviewerRecommendationLevel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewerRecommendationMatcherTest {

    private final ReviewerRecommendationMatcher matcher =
            new ReviewerRecommendationMatcher();

    @Test
    void twoDirectKeywordMatchesAreHigh() {
        Project project = project(Set.of("Yapay Zeka", "Makine Öğrenmesi"));

        ReviewerRecommendationMatch result = match(project,
                Set.of("yapay zeka", "makine öğrenmesi"));

        assertMatch(result, 10, ReviewerRecommendationLevel.HIGH, 2, 2);
    }

    @Test
    void oneDirectAndTwoDifferentHelperMatchesAreHigh() {
        Project project = project(Set.of("Yapay Zeka"));
        project.setMainTechnologyArea("Görüntü işleme");
        project.setSummary("Doğal dil işleme çözümü");

        ReviewerRecommendationMatch result = match(project,
                Set.of("Yapay Zeka", "Görüntü İşleme", "Doğal Dil İşleme"));

        assertMatch(result, 8, ReviewerRecommendationLevel.HIGH, 1, 3);
    }

    @Test
    void oneDirectKeywordMatchIsMedium() {
        ReviewerRecommendationMatch result = match(
                project(Set.of("Robotik")), Set.of("robotik"));

        assertMatch(result, 5, ReviewerRecommendationLevel.MEDIUM, 1, 1);
    }

    @Test
    void twoHelperMatchesAreMedium() {
        Project project = project(Set.of());
        project.setName("Enerji depolama projesi");
        project.setProjectDetail("Batarya yönetimi geliştirilecektir");

        ReviewerRecommendationMatch result = match(project,
                Set.of("enerji depolama", "batarya yönetimi"));

        assertMatch(result, 3, ReviewerRecommendationLevel.MEDIUM, 0, 2);
    }

    @Test
    void oneHelperMatchIsLow() {
        Project project = project(Set.of());
        project.setCategory("Akıllı ulaşım");

        ReviewerRecommendationMatch result = match(project,
                Set.of("akıllı ulaşım"));

        assertMatch(result, 2, ReviewerRecommendationLevel.LOW, 0, 1);
    }

    @Test
    void noMatchExplicitlyRepresentsNoRecommendation() {
        ReviewerRecommendationMatch result = match(
                project(Set.of("Robotik")), Set.of("hukuk"));

        assertEquals(0, result.totalScore());
        assertTrue(result.level().isEmpty());
        assertFalse(result.isRecommendation());
        assertTrue(result.matchedKeywords().isEmpty());
    }

    @Test
    void sameKeywordInMultipleFieldsScoresOnlyOnceAtStrongestLevel() {
        Project project = project(Set.of("Veri Analizi"));
        project.setName("Veri analizi projesi");
        project.setSummary("Veri analizi yapılacaktır");

        ReviewerRecommendationMatch result = match(project,
                Set.of("Veri Analizi"));

        assertMatch(result, 5, ReviewerRecommendationLevel.MEDIUM, 1, 1);
        assertEquals(List.of("Veri Analizi"), result.matchedKeywords());
    }

    @Test
    void multiWordPhraseMustBeContiguousAndSubstringDoesNotMatch() {
        Project project = project(Set.of());
        project.setSummary("Makine ile öğrenmesi ve veritabanı çalışmaları");

        ReviewerRecommendationMatch result = match(project,
                Set.of("makine öğrenmesi", "veri"));

        assertFalse(result.isRecommendation());
    }

    @Test
    void matchesTurkishCaseVariantsAndTechnicalExpressions() {
        Project project = project(Set.of());
        project.setMainTechnologyArea("IŞIK tabanlı C++ ve C# uygulaması");
        project.setSummary(".NET ile İPEK üretim takibi");

        ReviewerRecommendationMatch result = match(project,
                Set.of("ışık", "c++", "C#", ".net", "ipek"));

        assertMatch(result, 8, ReviewerRecommendationLevel.MEDIUM, 0, 5);
        assertEquals(List.of(".net", "C#", "c++", "ipek", "ışık"),
                result.matchedKeywords());
    }

    @Test
    void safelyIgnoresNullAndBlankProjectFieldsAndKeywords() {
        Project project = project(null);
        project.setName(null);
        Academician academician = academician(new LinkedHashSet<>(List.of("", "   ")));

        ReviewerRecommendationMatch result = matcher.match(project, academician);

        assertFalse(result.isRecommendation());
        assertEquals(0, result.totalScore());
    }

    @Test
    void differentSetOrdersProduceSameDeterministicResult() {
        Project project = project(new LinkedHashSet<>(List.of("Robotik", "Yapay Zeka")));
        Academician first = academician(new LinkedHashSet<>(
                List.of("Robotik", "Yapay Zeka", "YAPAY ZEKA")));
        Academician second = academician(new LinkedHashSet<>(
                List.of("YAPAY ZEKA", "Yapay Zeka", "Robotik")));

        ReviewerRecommendationMatch firstResult = matcher.match(project, first);
        ReviewerRecommendationMatch secondResult = matcher.match(project, second);

        assertEquals(firstResult, secondResult);
        assertEquals(List.of("Robotik", "YAPAY ZEKA"), firstResult.matchedKeywords());
    }

    private ReviewerRecommendationMatch match(Project project, Set<String> keywords) {
        return matcher.match(project, academician(keywords));
    }

    private Project project(Set<String> keywords) {
        Project project = new Project();
        project.setKeywords(keywords);
        return project;
    }

    private Academician academician(Set<String> keywords) {
        Academician academician = new Academician();
        academician.setKeywords(keywords);
        return academician;
    }

    private void assertMatch(
            ReviewerRecommendationMatch result,
            int score,
            ReviewerRecommendationLevel level,
            int directCount,
            int matchedCount
    ) {
        assertEquals(score, result.totalScore());
        assertEquals(level, result.level().orElseThrow());
        assertEquals(directCount, result.directKeywordMatchCount());
        assertEquals(matchedCount, result.matchedKeywordCount());
        assertTrue(result.isRecommendation());
    }
}
