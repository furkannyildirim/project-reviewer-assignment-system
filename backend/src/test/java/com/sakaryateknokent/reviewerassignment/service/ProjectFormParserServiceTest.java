package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.ProjectFormField;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectFormParserServiceTest {

    private final ProjectFormParserService parser = new ProjectFormParserService();

    @Test
    void parsesAllSupportedFieldsFromRepresentativeCompletedForm() {
        ParsedProjectForm result = parse(completedForm());

        assertEquals("TT-FR-003.10", result.templateCode());
        assertEquals("Örnek Teknoloji A.Ş.", result.companyName());
        assertEquals("Akıllı Üretim Platformu", result.name());
        assertEquals("Ayşe Şimşek", result.projectManager());
        assertEquals(LocalDate.of(2026, 2, 15), result.startDate());
        assertEquals(12, result.durationMonths());
        assertEquals("Ar-Ge, Yazılım", result.category());
        assertEquals("Yazılım", result.sector());
        assertEquals("Finans Teknolojileri", result.mainTechnologyArea());
        assertTrue(result.missingFields().isEmpty());
        assertTrue(result.reviewRequiredFields().isEmpty());
    }

    @Test
    void keepsMultilineSummaryAndDetailInsideTheirOwnSections() {
        ParsedProjectForm result = parse(completedForm());

        assertEquals("Birinci özet satırı.\nİkinci özet satırı.", result.summary());
        assertEquals("Birinci iş paketi.\nİkinci iş paketi.", result.projectDetail());
        assertFalse(result.summary().contains("Yazılım"));
        assertFalse(result.projectDetail().contains("Anahtar Kelimeler"));
    }

    @Test
    void preservesTurkishCharactersInLongTextValues() {
        ParsedProjectForm result = parse(completedForm());

        assertEquals("Özgün ölçüm yöntemi geliştirilecektir.",
                result.researchAndDevelopmentNature());
        assertEquals("Çözüm, mevcut ürünlerden yüksek doğrulukla ayrışır.",
                result.innovativeAspect());
    }

    @Test
    void parsesSupportedDateFormatsAndPositiveMonthDuration() {
        assertEquals(LocalDate.of(2026, 2, 15),
                parse(completedForm()).startDate());
        assertEquals(LocalDate.of(2026, 2, 15),
                parse(completedForm().replace("15.02.2026", "15/02/2026")).startDate());
        assertEquals(LocalDate.of(2026, 2, 15),
                parse(completedForm().replace("15.02.2026", "15-02-2026")).startDate());
        assertEquals(18,
                parse(completedForm().replace("12 ay", "18 Ay")).durationMonths());
    }

    @Test
    void normalizesKeywordsWithoutChangingFirstMeaningfulSpelling() {
        ParsedProjectForm result = parse(completedForm());

        assertEquals(Set.of("Yapay Zekâ", "Görüntü İşleme", "Robotik"),
                result.keywords());
    }

    @Test
    void emptyTemplateDoesNotInventValuesAndMarksFieldsMissing() {
        ParsedProjectForm result = parse(emptyForm());

        assertNull(result.companyName());
        assertNull(result.name());
        assertNull(result.summary());
        assertNull(result.category());
        assertNull(result.sector());
        assertTrue(result.keywords().isEmpty());
        assertTrue(result.missingFields().containsAll(Set.of(
                ProjectFormField.COMPANY_NAME,
                ProjectFormField.NAME,
                ProjectFormField.SUMMARY,
                ProjectFormField.CATEGORY,
                ProjectFormField.SECTOR,
                ProjectFormField.KEYWORDS
        )));
        assertTrue(result.missingFields().contains(ProjectFormField.START_DATE));
        assertFalse(result.reviewRequiredFields().contains(ProjectFormField.START_DATE));
    }

    @Test
    void readsExplicitlySelectedCategoryAndSectorOnly() {
        ParsedProjectForm result = parse(completedForm());

        assertEquals("Ar-Ge, Yazılım", result.category());
        assertEquals("Yazılım", result.sector());
        assertFalse(result.sector().contains("Ambalaj"));
    }

    @Test
    void marksSelectionAsReviewRequiredWhenCheckboxStateIsUnclear() {
        String ambiguous = completedForm()
                .replace("☒ Ar-Ge ☐ Tasarım ☑ Yazılım", "Ar-Ge / Tasarım / Yazılım")
                .replace("☐ Ambalaj\n☒ Yazılım", "Ambalaj / Yazılım");

        ParsedProjectForm result = parse(ambiguous);

        assertTrue(result.reviewRequiredFields().contains(ProjectFormField.CATEGORY));
        assertTrue(result.reviewRequiredFields().contains(ProjectFormField.SECTOR));
        assertEquals("Ar-Ge / Tasarım / Yazılım", result.category());
        assertEquals("Ambalaj / Yazılım", result.sector());
    }

    @Test
    void marksInvalidDateAndAmbiguousDurationForReviewWithoutGuessing() {
        ParsedProjectForm result = parse(completedForm()
                .replace("15.02.2026", "31.13.2026")
                .replace("12 ay", "1 yıl / 12 ay"));

        assertNull(result.startDate());
        assertNull(result.durationMonths());
        assertTrue(result.reviewRequiredFields().contains(ProjectFormField.START_DATE));
        assertTrue(result.reviewRequiredFields().contains(ProjectFormField.DURATION_MONTHS));
        assertFalse(result.missingFields().contains(ProjectFormField.START_DATE));
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("31.13.2026")));
    }

    @Test
    void returnsRemainingValuesWhenSomeFieldsAreMissing() {
        ParsedProjectForm result = parse(completedForm()
                .replace("Proje Yöneticisi\nAyşe Şimşek", "Proje Yöneticisi")
                .replace("Anahtar Kelimeler\nProjenin ilgili olduğu sektör ve konu alanıyla ilişkilendirilebilen temel kavram ve kelimeleri belirtiniz\nYapay Zekâ; Görüntü İşleme\nyapay zekâ, Robotik",
                        "Anahtar Kelimeler"));

        assertEquals("Akıllı Üretim Platformu", result.name());
        assertNull(result.projectManager());
        assertTrue(result.keywords().isEmpty());
        assertTrue(result.missingFields().contains(ProjectFormField.PROJECT_MANAGER));
        assertTrue(result.missingFields().contains(ProjectFormField.KEYWORDS));
    }

    @Test
    void rejectsUnrelatedDocumentInsteadOfGuessingFields() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> parse("Genel rapor\nBaşlık\nİçerik")
        );

        assertEquals("Desteklenen proje formu taninamadi", exception.getMessage());
    }

    @Test
    void recognizesTurkishCaseWhitespaceAndColonVariationsInHeadings() {
        String varied = completedForm()
                .replace("Firma Unvanı\nÖrnek Teknoloji A.Ş.",
                        "  FİRMA   UNVANI : Örnek Teknoloji A.Ş.")
                .replace("Proje Adı\nAkıllı Üretim Platformu",
                        "PROJE   ADI: Akıllı Üretim Platformu")
                .replace("Proje Özeti", "  PROJE   ÖZETİ :")
                .replace("Anahtar Kelimeler", "ANAHTAR KELİMELER:");

        ParsedProjectForm result = parse(varied);

        assertEquals("Örnek Teknoloji A.Ş.", result.companyName());
        assertEquals("Akıllı Üretim Platformu", result.name());
        assertEquals("Birinci özet satırı.\nİkinci özet satırı.", result.summary());
        assertEquals(Set.of("Yapay Zekâ", "Görüntü İşleme", "Robotik"),
                result.keywords());
    }

    private ParsedProjectForm parse(String text) {
        return parser.parse(new ExtractedProjectDocumentText(
                ProjectDocumentTextExtractionService.DOCX_CONTENT_TYPE,
                text
        ));
    }

    private String completedForm() {
        return """
                PROJE BİLGİ FORMU
                TT-FR-003.10
                Firma Unvanı
                Örnek Teknoloji A.Ş.
                Proje Adı
                Akıllı Üretim Platformu
                Proje Yöneticisi
                Ayşe Şimşek
                Proje Süresi
                12 ay
                Proje Başlangıç Tarihi
                15.02.2026
                NACE Kodu\u00A0
                62.01
                Proje Kategorisi
                ☒ Ar-Ge ☐ Tasarım ☑ Yazılım
                Proje Özeti
                “Bu bölümde, proje önerinizle ilgili olarak gizlilik gerektirmeyen bilgiler sunulması beklenmektedir.”
                Birinci özet satırı.
                İkinci özet satırı.
                Projenin Sektörü
                ☐ Ambalaj
                ☒ Yazılım
                Proje Hedefleri
                Ar-Ge Niteliği
                Projenizin araştırma ve geliştirme yönünü açıklayınız.
                Özgün ölçüm yöntemi geliştirilecektir.
                Yenilikçi Yönü
                Bu bölümde, projenizin mevcut çözümlerden nasıl ayrıştığını belirtiniz.
                Çözüm, mevcut ürünlerden yüksek doğrulukla ayrışır.
                Proje İş Birlikleri
                Proje Teknoloji Alanı
                Ana Teknoloji Alanını seçiniz.
                Finans Teknolojileri
                Teknopark Teknik Hizmetleri
                Proje Detayı
                Proje kapsamında gerçekleştirilecek çalışmaları ayrıntılı şekilde açıklayınız.
                Birinci iş paketi.
                İkinci iş paketi.
                Anahtar Kelimeler
                Projenin ilgili olduğu sektör ve konu alanıyla ilişkilendirilebilen temel kavram ve kelimeleri belirtiniz
                Yapay Zekâ; Görüntü İşleme
                yapay zekâ, Robotik
                Proje Ekibi
                """;
    }

    private String emptyForm() {
        return """
                PROJE BİLGİ FORMU
                TT-FR-003.10
                Firma Unvanı
                Proje Adı
                Proje Yöneticisi
                Proje Süresi
                Proje Başlangıç Tarihi
                NACE Kodu\u00A0
                Proje Kategorisi
                ☐ Ar-Ge ☐ Tasarım ☐ Yazılım
                Proje Özeti
                “Bu bölümde, proje önerinizle ilgili olarak gizlilik gerektirmeyen bilgiler sunulması beklenmektedir.”
                Projenin Sektörü
                ☐ Ambalaj
                ☐ Yazılım
                Proje Hedefleri
                Ar-Ge Niteliği
                Projenizin araştırma ve geliştirme yönünü açıklayınız.
                Yenilikçi Yönü
                Bu bölümde, projenizin mevcut çözümlerden nasıl ayrıştığını belirtiniz.
                Proje İş Birlikleri
                Proje Teknoloji Alanı
                Ana Teknoloji Alanını seçiniz.
                Bir öğe seçin.
                Teknopark Teknik Hizmetleri
                Proje Detayı
                Proje kapsamında gerçekleştirilecek çalışmaları ayrıntılı şekilde açıklayınız.
                Anahtar Kelimeler
                Projenin ilgili olduğu sektör ve konu alanıyla ilişkilendirilebilen temel kavram ve kelimeleri belirtiniz
                Proje Ekibi
                """;
    }
}
