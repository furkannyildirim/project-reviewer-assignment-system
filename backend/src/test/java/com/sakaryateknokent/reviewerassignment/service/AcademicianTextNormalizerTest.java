package com.sakaryateknokent.reviewerassignment.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AcademicianTextNormalizerTest {

    @Test
    void normalizeWhitespaceCollapsesNonBreakingSpace() {
        assertEquals(
                "Ali Yilmaz",
                AcademicianTextNormalizer.normalizeWhitespace(
                        "  Ali\u00A0\u00A0Yilmaz  "
                )
        );
    }

    @Test
    void normalizeWhitespaceCollapsesNarrowNonBreakingSpace() {
        assertEquals(
                "Ali Yilmaz",
                AcademicianTextNormalizer.normalizeWhitespace(
                        "Ali\u202F\u202FYilmaz"
                )
        );
    }

    @Test
    void normalizeWhitespaceCollapsesUnicodeSpacesTabsAndNewlines() {
        assertEquals(
                "Ali Yilmaz Test",
                AcademicianTextNormalizer.normalizeWhitespace(
                        "\tAli\u2002Yilmaz\u2003\nTest"
                )
        );
    }

    @Test
    void normalizeEmailTrimsUnicodeWhitespaceOnlyAtEdges() {
        assertEquals(
                "ali@example.com",
                AcademicianTextNormalizer.normalizeEmail(
                        "\u00A0ALI@example.com\u00A0"
                )
        );
    }

    @Test
    void normalizeEmailDoesNotRemoveInnerWhitespace() {
        String normalized =
                AcademicianTextNormalizer.normalizeEmail(
                        "ali\u00A0@example.com"
                );

        assertEquals("ali @example.com", normalized);
        assertNotEquals("ali@example.com", normalized);
    }

    @Test
    void normalizeKeywordsDeduplicatesUnicodeWhitespaceVariants() {
        assertEquals(
                Set.of("Yapay Zeka"),
                AcademicianTextNormalizer.normalizeKeywords(Set.of(
                        "Yapay Zeka",
                        "Yapay\u00A0Zeka",
                        "Yapay\u202FZeka"
                ))
        );
    }

    @Test
    void normalizeOptionalTextTreatsUnicodeWhitespaceOnlyValuesAsBlank() {
        assertNull(AcademicianTextNormalizer.normalizeOptionalText("\u00A0"));
        assertNull(AcademicianTextNormalizer.normalizeOptionalText("\u202F"));
        assertNull(AcademicianTextNormalizer.normalizeOptionalText("\u2002"));
    }

    @Test
    void canonicalizeIdentityPartUsesTurkishLowercase() {
        assertEquals(
                "\u0131\u015F\u0131k",
                AcademicianTextNormalizer.canonicalizeIdentityPart("I\u015F\u0131k")
        );
        assertEquals(
                "ipek",
                AcademicianTextNormalizer.canonicalizeIdentityPart("\u0130pek")
        );
        assertEquals(
                "\u0131s\u0131k",
                AcademicianTextNormalizer.canonicalizeIdentityPart("ISIK")
        );
        assertEquals(
                "isim",
                AcademicianTextNormalizer.canonicalizeIdentityPart("\u0130S\u0130M")
        );
    }

    @Test
    void canonicalizeIdentityPartCollapsesUnicodeWhitespaceAndBlankToEmpty() {
        assertEquals(
                "\u0131\u015F\u0131k",
                AcademicianTextNormalizer.canonicalizeIdentityPart(
                        "I\u015F\u0131k\u00A0"
                )
        );
        assertEquals(
                "dr. \u00F6\u011Fr. \u00FCyesi",
                AcademicianTextNormalizer.canonicalizeIdentityPart(
                        "Dr.\u00A0\u00D6\u011Fr.\u00A0\u00DCyesi"
                )
        );
        assertEquals(
                "",
                AcademicianTextNormalizer.canonicalizeIdentityPart("\u00A0")
        );
    }
}
