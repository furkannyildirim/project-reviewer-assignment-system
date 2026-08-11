package com.sakaryateknokent.reviewerassignment.importing;

import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicianExcelImportParserTest {

    private final AcademicianExcelImportParser parser =
            new AcademicianExcelImportParser();

    @Test
    void parsesKeywordHeaderWithTurkishCapitalICombiningDot() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 1, "ADI SOYADI", "ANAHTAR KELİMELER");
            writeRow(sheet, 2, "Dr. Ayse Yilmaz", "yapay zeka");
        });

        assertTrue(preview.getDetectedHeaders().contains("ANAHTAR KELİMELER"));
        assertIterableEquals(
                List.of("yapay zeka"),
                List.copyOf(preview.getRows().getFirst().getKeywords())
        );
    }

    @Test
    void parsesKeywordHeaderVariantWithPossessiveSuffix() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "ADI SOYADI", "ANAHTAR KELİMELERİ");
            writeRow(sheet, 1, "Dr. Ayse Yilmaz", "robotik");
        });

        assertIterableEquals(
                List.of("robotik"),
                List.copyOf(preview.getRows().getFirst().getKeywords())
        );
    }

    @Test
    void parsesCombinedFullNameAndAddsGeneralWarning() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "ADI SOYADI");
            writeRow(sheet, 1, "Prof. Dr. Ali Veli Demir");
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertEquals("Ali Veli", row.getFirstName());
        assertEquals("Demir", row.getLastName());
        assertEquals("Prof. Dr.", row.getTitle());
        assertTrue(preview.getGeneralWarnings().contains(
                "Birleşik ad soyad alanları otomatik ayrıştırılmıştır. "
                        + "Gerekirse aktarımdan sonra düzenleyebilirsiniz."
        ));
        assertFalse(preview.getGeneralInformationMessages().contains(
                "Birleşik ad soyad alanları otomatik ayrıştırılmıştır. "
                        + "Gerekirse aktarımdan sonra düzenleyebilirsiniz."
        ));
    }

    @Test
    void parsesTitlePrefixWithNonBreakingSpaces() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "ADI SOYADI");
            writeRow(sheet, 1, "Prof.\u00A0Dr. Ali Yilmaz");
            writeRow(sheet, 2, "Dr.\u202F\u00D6\u011Fr.\u202F\u00DCyesi Ayse Demir");
        });

        assertParsedName(
                preview.getRows().get(0),
                "Prof. Dr.",
                "Ali",
                "Yilmaz"
        );
        assertParsedName(
                preview.getRows().get(1),
                "Dr. \u00D6\u011Fr. \u00DCyesi",
                "Ayse",
                "Demir"
        );
    }

    @Test
    void recognizesEmailHeadersWithUnicodeWhitespace() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "E-POSTA\u00A0", "E-\u202FPOSTA 2");
            writeRow(sheet, 1, "Ayse", "Yilmaz", "", "AYSE@EXAMPLE.COM");
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertEquals("ayse@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void keepsEmailWithInnerUnicodeWhitespaceInvalid() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
            writeRow(sheet, 1, "Ayse", "Yilmaz", "ali\u00A0@example.com");
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertNull(row.getEmail());
        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains("E-posta formati gecersiz"));
    }

    @Test
    void treatsNbspOnlyOptionalCellAsBlank() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "TELEFON");
            writeRow(sheet, 1, "Ayse", "Yilmaz", "\u00A0");
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertNull(row.getPhone());
        assertTrue(row.isValid());
    }

    @Test
    void keepsUnparseableCombinedNameInvalid() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "ADI SOYADI");
            writeRow(sheet, 1, "Prof. Dr. Selin");
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains("Ad soyad bilgisi ayrisitirilamadi"));
    }

    @Test
    void separateNameAndTitleColumnsHavePriorityOverCombinedName() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "ADI SOYADI", "AD", "SOYAD", "ÜNVAN");
            writeRow(
                    sheet,
                    1,
                    "Prof. Dr. Yanlis Kisi",
                    "Dogru",
                    "Soyad",
                    "Doç. Dr."
            );
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertEquals("Dogru", row.getFirstName());
        assertEquals("Soyad", row.getLastName());
        assertEquals("Doç. Dr.", row.getTitle());
    }

    @Test
    void stripsTitlePrefixWithoutDroppingFirstNameCharacters() {
        List<String> names = List.of(
                "Dr.Öğr.Üyesi Elif Dülger",
                "Dr. Öğr. Üyesi Elif Dülger",
                "Prof.Dr. Ali Demir",
                "Prof. Dr. Ali Demir",
                "Doç.Dr. Ayşe Yılmaz",
                "Doç. Dr. Ayşe Yılmaz",
                "Arş.Gör. Şeyma Şimşek",
                "Arş.Gör. Sezer Tan",
                "Arş.Gör. Yunus Emre Tanış",
                "Dr.Öğr.Üyesi Alper Göksu",
                "Dr.Öğr.Üyesi Elif Eker Kahveci"
        );

        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "ADI SOYADI");
            for (int i = 0; i < names.size(); i++) {
                writeRow(sheet, i + 1, names.get(i));
            }
        });

        assertParsedName(
                preview.getRows().get(0),
                "Dr. Öğr. Üyesi",
                "Elif",
                "Dülger"
        );
        assertParsedName(
                preview.getRows().get(1),
                "Dr. Öğr. Üyesi",
                "Elif",
                "Dülger"
        );
        assertParsedName(
                preview.getRows().get(2),
                "Prof. Dr.",
                "Ali",
                "Demir"
        );
        assertParsedName(
                preview.getRows().get(3),
                "Prof. Dr.",
                "Ali",
                "Demir"
        );
        assertParsedName(
                preview.getRows().get(4),
                "Doç. Dr.",
                "Ayşe",
                "Yılmaz"
        );
        assertParsedName(
                preview.getRows().get(5),
                "Doç. Dr.",
                "Ayşe",
                "Yılmaz"
        );
        assertParsedName(
                preview.getRows().get(6),
                "Arş. Gör.",
                "Şeyma",
                "Şimşek"
        );
        assertParsedName(
                preview.getRows().get(7),
                "Arş. Gör.",
                "Sezer",
                "Tan"
        );
        assertParsedName(
                preview.getRows().get(8),
                "Arş. Gör.",
                "Yunus Emre",
                "Tanış"
        );
        assertParsedName(
                preview.getRows().get(9),
                "Dr. Öğr. Üyesi",
                "Alper",
                "Göksu"
        );
        assertParsedName(
                preview.getRows().get(10),
                "Dr. Öğr. Üyesi",
                "Elif Eker",
                "Kahveci"
        );
    }

    @Test
    void stripsEveryConfiguredTitleAliasWithoutDroppingNameCharacters()
            throws ReflectiveOperationException {
        List<ExpectedTitleAlias> aliases = configuredTitleAliases();

        for (ExpectedTitleAlias alias : aliases) {
            AcademicianImportPreview preview = parse(workbook -> {
                Sheet sheet = workbook.createSheet("Akademisyenler");
                writeRow(sheet, 0, "ADI SOYADI");
                writeRow(sheet, 1, alias.source() + " Test Ad Soyad");
            });

            assertParsedName(
                    preview.getRows().getFirst(),
                    alias.standardTitle(),
                    "Test Ad",
                    "Soyad"
            );
        }
    }

    @Test
    void recognizesDifferentTitleSpellingsAtTextStart() {
        List<String> names = List.of(
                "Prof.Dr. Ali Demir",
                "Doç.Dr. Ayse Yilmaz",
                "Dr. Öğr. Üyesi Mehmet Kaya",
                "Dr.Öğr.Üyesi Fatma Celik",
                "Öğr. Gör. Dr. Hasan Can",
                "Arş. Gör. Dr. Zeynep Ak",
                "Öğr. Gör. Murat Tas",
                "Arş. Gör. Elif Su",
                "Dr. Ahmet Kos"
        );

        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "ADI SOYADI");
            for (int i = 0; i < names.size(); i++) {
                writeRow(sheet, i + 1, names.get(i));
            }
        });

        assertIterableEquals(
                List.of(
                        "Prof. Dr.",
                        "Doç. Dr.",
                        "Dr. Öğr. Üyesi",
                        "Dr. Öğr. Üyesi",
                        "Öğr. Gör. Dr.",
                        "Arş. Gör. Dr.",
                        "Öğr. Gör.",
                        "Arş. Gör.",
                        "Dr."
                ),
                preview.getRows().stream()
                        .map(AcademicianImportRow::getTitle)
                        .toList()
        );
        assertIterableEquals(
                List.of(
                        "Ali",
                        "Ayse",
                        "Mehmet",
                        "Fatma",
                        "Hasan",
                        "Zeynep",
                        "Murat",
                        "Elif",
                        "Ahmet"
                ),
                preview.getRows().stream()
                        .map(AcademicianImportRow::getFirstName)
                        .toList()
        );
        assertIterableEquals(
                List.of(
                        "Demir",
                        "Yilmaz",
                        "Kaya",
                        "Celik",
                        "Can",
                        "Ak",
                        "Tas",
                        "Su",
                        "Kos"
                ),
                preview.getRows().stream()
                        .map(AcademicianImportRow::getLastName)
                        .toList()
        );
    }

    @Test
    void splitsAndDeduplicatesKeywords() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "ADI SOYADI", "KEYWORDS");
            writeRow(
                    sheet,
                    1,
                    "Dr. Ayse Yilmaz",
                    "Yapay   Zeka, robotik; yapay zeka\nVeri"
            );
        });

        assertIterableEquals(
                List.of("Yapay Zeka", "robotik", "Veri"),
                List.copyOf(preview.getRows().getFirst().getKeywords())
        );
    }

    @Test
    void doesNotDependOnColumnOrder() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "EMAIL", "SOYADI", "ANAHTAR KELİMELER", "ADI");
            writeRow(sheet, 1, "ALI@EXAMPLE.COM", "Demir", "ai", "Ali");
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertEquals("Ali", row.getFirstName());
        assertEquals("Demir", row.getLastName());
        assertEquals("ali@example.com", row.getEmail());
    }

    @Test
    void ignoresUnknownAndProjectsColumnsWithoutReturningProjectContent() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "ADI SOYADI", "PROJELERİ", "Bilinmeyen");
            writeRow(sheet, 1, "Dr. Ayse Yilmaz", "Gizli proje", "not");
        });

        assertTrue(preview.getDetectedHeaders().contains("PROJELERİ"));
        assertTrue(preview.getIgnoredHeaders().contains("PROJELERİ"));
        assertTrue(preview.getIgnoredHeaders().contains("Bilinmeyen"));
        assertEquals(1, preview.getRows().size());
        assertFalse(preview.getRows().getFirst().getFirstName().contains("Gizli"));
    }

    @Test
    void acceptsMissingEmailPhoneAndTitleColumns() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD");
            writeRow(sheet, 1, "Ayse", "Yilmaz");
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertNull(row.getEmail());
        assertNull(row.getPhone());
        assertNull(row.getTitle());
        assertTrue(row.isValid());
    }

    @Test
    void keepsSeparatePhoneAndEmailColumnsWorking() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "TELEFON", "E-POSTA");
            writeRow(
                    sheet,
                    1,
                    "Ayse",
                    "Yilmaz",
                    "0532 111 22 33",
                    "AYSE@EXAMPLE.COM"
            );
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertEquals("0532 111 22 33", row.getPhone());
        assertEquals("ayse@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void parsesCommunicationCellWithPhoneNewlineEmail() {
        AcademicianImportRow row = parseCommunication("0532 111 22 33\nINFO@EXAMPLE.COM");

        assertEquals("0532 111 22 33", row.getPhone());
        assertEquals("info@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void parsesCommunicationCellWithOnlyEmail() {
        AcademicianImportRow row = parseCommunication("INFO@EXAMPLE.COM");

        assertNull(row.getPhone());
        assertEquals("info@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void parsesCommunicationCellWithOnlyPhone() {
        AcademicianImportRow row = parseCommunication("0532 111 22 33");

        assertEquals("0532 111 22 33", row.getPhone());
        assertNull(row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void keepsBlankCommunicationCellAsNullContactValues() {
        AcademicianImportRow row = parseCommunication("");

        assertNull(row.getPhone());
        assertNull(row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void parsesCommunicationCellWithSlashSeparator() {
        AcademicianImportRow row = parseCommunication("0532 111 22 33 / info@example.com");

        assertEquals("0532 111 22 33", row.getPhone());
        assertEquals("info@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void parsesCommunicationCellWithSemicolonSeparator() {
        AcademicianImportRow row = parseCommunication("0532 111 22 33; info@example.com");

        assertEquals("0532 111 22 33", row.getPhone());
        assertEquals("info@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void parsesCommunicationCellWithAmpersandSeparator() {
        AcademicianImportRow row = parseCommunication("0532 111 22 33 & info@example.com");

        assertEquals("0532 111 22 33", row.getPhone());
        assertEquals("info@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void ignoresCommunicationPlaceholderValues() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "ILETISIM");
            writeRow(sheet, 1, "Ayse", "Yilmaz", "Bilgi Yok");
            writeRow(sheet, 2, "Ali", "Demir", "--");
        });

        assertNull(preview.getRows().get(0).getPhone());
        assertNull(preview.getRows().get(0).getEmail());
        assertNull(preview.getRows().get(1).getPhone());
        assertNull(preview.getRows().get(1).getEmail());
    }

    @Test
    void keepsExplicitPhoneAndEmailOverCommunicationFallbacks() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "TELEFON", "E-POSTA", "ILETISIM");
            writeRow(
                    sheet,
                    1,
                    "Ayse",
                    "Yilmaz",
                    "0555 000 00 00",
                    "explicit@example.com",
                    "0532 111 22 33 / fallback@example.com"
            );
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertEquals("0555 000 00 00", row.getPhone());
        assertEquals("explicit@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void usesCommunicationFallbackWhenExplicitPhoneOrEmailIsBlank() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "TELEFON", "E-POSTA", "ILETISIM");
            writeRow(
                    sheet,
                    1,
                    "Ayse",
                    "Yilmaz",
                    "",
                    "",
                    "0532 111 22 33 / fallback@example.com"
            );
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertEquals("0532 111 22 33", row.getPhone());
        assertEquals("fallback@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void usesSecondEmailColumnWhenFirstEmailColumnIsBlank() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "",
                "ALI@UNI.EDU.TR",
                "",
                "",
                ""
        );

        assertEquals("ali@uni.edu.tr", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void usesSecondEmailColumnWhenFirstEmailColumnIsPlaceholder() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "Bilgi Yok",
                "ali@uni.edu.tr",
                "",
                "",
                ""
        );

        assertEquals("ali@uni.edu.tr", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void usesFirstEmailColumnWhenSecondEmailColumnIsBlank() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "ali@uni.edu.tr",
                "",
                "",
                "",
                ""
        );

        assertEquals("ali@uni.edu.tr", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void keepsDuplicateEmailColumnsValidWhenNormalizedValuesMatch() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "ali@uni.edu.tr",
                " ALI@UNI.EDU.TR ",
                "",
                "",
                ""
        );

        assertEquals("ali@uni.edu.tr", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void marksRowInvalidWhenDuplicateEmailColumnsHaveDifferentValues() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "ali@uni.edu.tr",
                "ali2@uni.edu.tr",
                "",
                "",
                ""
        );

        assertNull(row.getEmail());
        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains(
                "Ayn\u0131 sat\u0131rda birden fazla farkl\u0131 e-posta de\u011feri bulundu. "
                        + "E-posta s\u00fctunlar\u0131n\u0131 kontrol edin."
        ));
    }

    @Test
    void usesSecondPhoneColumnWhenFirstPhoneColumnIsBlank() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "",
                "",
                "",
                "0532 111 22 33",
                ""
        );

        assertEquals("0532 111 22 33", row.getPhone());
        assertTrue(row.isValid());
    }

    @Test
    void keepsDuplicatePhoneColumnsValidWhenValuesMatch() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "",
                "",
                "0532 111 22 33",
                "0532 111 22 33",
                ""
        );

        assertEquals("0532 111 22 33", row.getPhone());
        assertTrue(row.isValid());
    }

    @Test
    void marksRowInvalidWhenDuplicatePhoneColumnsHaveDifferentValues() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "",
                "",
                "0532 111 22 33",
                "0544 222 33 44",
                ""
        );

        assertNull(row.getPhone());
        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains(
                "Ayn\u0131 sat\u0131rda birden fazla farkl\u0131 telefon de\u011feri bulundu. "
                        + "Telefon s\u00fctunlar\u0131n\u0131 kontrol edin."
        ));
    }

    @Test
    void explicitSecondEmailColumnHasPriorityOverCommunicationEmail() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "",
                "explicit@example.com",
                "",
                "",
                "fallback@example.com"
        );

        assertEquals("explicit@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void communicationEmailIsUsedWhenAllExplicitEmailColumnsAreBlank() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "",
                "-",
                "",
                "",
                "fallback@example.com"
        );

        assertEquals("fallback@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void duplicatePhoneColumnsUseExplicitFallbackBeforeCommunicationPhone() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "",
                "",
                "",
                "0532 111 22 33",
                "0544 222 33 44"
        );

        assertEquals("0532 111 22 33", row.getPhone());
        assertTrue(row.isValid());
    }

    @Test
    void communicationPhoneIsUsedWhenAllExplicitPhoneColumnsAreBlank() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "",
                "",
                "",
                "--",
                "0532 111 22 33"
        );

        assertEquals("0532 111 22 33", row.getPhone());
        assertTrue(row.isValid());
    }

    @Test
    void keepsSingleEmailAndPhoneColumnsWorkingWithDuplicateColumnSupport() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "EMAIL", "TELEFON");
            writeRow(sheet, 1, "Ayse", "Yilmaz", "AYSE@EXAMPLE.COM",
                    "0532 111 22 33");
        });

        AcademicianImportRow row = preview.getRows().getFirst();
        assertEquals("ayse@example.com", row.getEmail());
        assertEquals("0532 111 22 33", row.getPhone());
        assertTrue(row.isValid());
    }

    @Test
    void keepsMixedCommunicationWorkingWithDuplicateColumnSupport() {
        AcademicianImportRow row = parseDuplicateContactColumns(
                "",
                "",
                "",
                "",
                "0532 111 22 33 / INFO@EXAMPLE.COM"
        );

        assertEquals("0532 111 22 33", row.getPhone());
        assertEquals("info@example.com", row.getEmail());
        assertTrue(row.isValid());
    }

    @Test
    void marksInvalidCommunicationPartWithRowError() {
        AcademicianImportRow row = parseCommunication("abcxyz");

        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains(
                "Iletisim parcasi gecersiz: abcxyz"
        ));
    }

    @Test
    void doesNotGuessCommunicationValueWithoutExplicitSeparator() {
        AcademicianImportRow row = parseCommunication("info@example.com 05321112233");

        assertNull(row.getPhone());
        assertNull(row.getEmail());
        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains(
                "Iletisim parcasi gecersiz: info@example.com 05321112233"
        ));
    }

    @Test
    void warnsAndUsesFirstValueWhenCommunicationHasMultipleContacts() {
        AcademicianImportRow row = parseCommunication(
                "0532 111 22 33 / 0544 222 33 44 / a@example.com / b@example.com"
        );

        assertEquals("0532 111 22 33", row.getPhone());
        assertEquals("a@example.com", row.getEmail());
        assertTrue(row.getWarnings().contains(
                "Birden fazla telefon bulundu, ilk deger kullanilacak"
        ));
        assertTrue(row.getWarnings().contains(
                "Birden fazla e-posta bulundu, ilk deger kullanilacak"
        ));
        assertTrue(row.isValid());
    }

    @Test
    void recognizesCommunicationHeaderAliases() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "ILETISIM BILGISI");
            writeRow(sheet, 1, "Ayse", "Yilmaz", "info@example.com");
        });

        assertTrue(preview.getDetectedHeaders().contains("ILETISIM BILGISI"));
        assertEquals("info@example.com", preview.getRows().getFirst().getEmail());
    }

    @Test
    void parsesMultipleVisibleSheetsAndTrimsSheetNames() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet first = workbook.createSheet(" Bir ");
            writeRow(first, 0, "AD", "SOYAD");
            writeRow(first, 1, "Ayse", "Yilmaz");

            Sheet second = workbook.createSheet("Iki");
            writeRow(second, 0, "AD", "SOYAD");
            writeRow(second, 1, "Ali", "Demir");

            Sheet hidden = workbook.createSheet("Gizli");
            writeRow(hidden, 0, "AD", "SOYAD");
            writeRow(hidden, 1, "Gizli", "Kisi");
            workbook.setSheetHidden(2, true);
        });

        assertEquals(2, preview.getSheetCount());
        assertEquals(2, preview.getRows().size());
        assertEquals("Bir", preview.getRows().getFirst().getSheetName());
    }

    @Test
    void warnsAndContinuesWhenVisibleSheetHasNoSupportedNameStructure() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet first = workbook.createSheet("Sheet 1");
            writeRow(first, 0, "AD", "SOYAD", "EMAIL");
            writeRow(first, 1, "Ayse", "Yilmaz", "ayse@example.com");

            Sheet invalid = workbook.createSheet("Sheet 2");
            writeRow(invalid, 0, "EMAIL", "TELEFON");
            writeRow(invalid, 1, "skip@example.com", "0555");

            Sheet third = workbook.createSheet("Sheet 3");
            writeRow(third, 0, "AD", "SOYAD", "EMAIL");
            writeRow(third, 1, "Ali", "Demir", "ali@example.com");
        });

        assertEquals(2, preview.getRows().size());
        assertIterableEquals(
                List.of("Sheet 1", "Sheet 3"),
                preview.getRows().stream()
                        .map(AcademicianImportRow::getSheetName)
                        .toList()
        );
        assertEquals(1, preview.getGeneralWarnings().size());
        assertTrue(preview.getGeneralWarnings().getFirst().contains("Sheet 2"));
        assertTrue(preview.getGeneralWarnings().getFirst()
                .contains("akademisyen ad/soyad başlıkları bulunamadı"));
    }

    @Test
    void doesNotWarnForSingleValidSheet() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD");
            writeRow(sheet, 1, "Ayse", "Yilmaz");
        });

        assertTrue(preview.getGeneralWarnings().isEmpty());
    }

    @Test
    void doesNotWarnForEmptyVisibleSheetWhenValidSheetExists() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet valid = workbook.createSheet("Akademisyenler");
            writeRow(valid, 0, "AD", "SOYAD");
            writeRow(valid, 1, "Ayse", "Yilmaz");

            workbook.createSheet("Bos");
        });

        assertEquals(1, preview.getRows().size());
        assertTrue(preview.getGeneralWarnings().isEmpty());
    }

    @Test
    void keepsHiddenSheetSilentWhenItHasInvalidStructure() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet visible = workbook.createSheet("Akademisyenler");
            writeRow(visible, 0, "AD", "SOYAD");
            writeRow(visible, 1, "Ayse", "Yilmaz");

            Sheet hidden = workbook.createSheet("Gizli");
            writeRow(hidden, 0, "EMAIL", "TELEFON");
            writeRow(hidden, 1, "skip@example.com", "0555");
            workbook.setSheetHidden(1, true);
        });

        assertEquals(1, preview.getSheetCount());
        assertTrue(preview.getGeneralWarnings().isEmpty());
    }

    @Test
    void sheetLevelWarningDoesNotMakeValidRowsInvalid() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet valid = workbook.createSheet("Akademisyenler");
            writeRow(valid, 0, "AD", "SOYAD");
            writeRow(valid, 1, "Ayse", "Yilmaz");

            Sheet invalid = workbook.createSheet("Eksik Baslik");
            writeRow(invalid, 0, "EMAIL", "TELEFON");
            writeRow(invalid, 1, "skip@example.com", "0555");
        });

        assertEquals(1, preview.getGeneralWarnings().size());
        assertTrue(preview.getRows().getFirst().isValid());
        assertTrue(preview.getRows().getFirst().getErrors().isEmpty());
    }

    @Test
    void readsMergedNameAndKeywordAnchorAndSkipsContinuationRow() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "ADI SOYADI", "ANAHTAR KELİMELER");
            writeRow(sheet, 1, "Dr. Ayse Yilmaz", "ai");
            writeRow(sheet, 2, "", "");
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 1, 1));
        });

        assertEquals(1, preview.getRows().size());
        assertIterableEquals(
                List.of("ai"),
                List.copyOf(preview.getRows().getFirst().getKeywords())
        );
    }

    @Test
    void findsHeaderRowWithinFirstTenRows() {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "Rapor");
            writeRow(sheet, 4, "ADI SOYADI");
            writeRow(sheet, 5, "Dr. Ayse Yilmaz");
        });

        assertEquals(6, preview.getRows().getFirst().getRowNumber());
    }

    @Test
    void rejectsFileWithoutNameHeader() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> parse(workbook -> {
                    Sheet sheet = workbook.createSheet("Akademisyenler");
                    writeRow(sheet, 0, "EMAIL", "TELEFON");
                    writeRow(sheet, 1, "a@example.com", "555");
                })
        );

        assertTrue(exception.getMessage().contains("adi ve soyadi"));
    }

    @Test
    void rejectsCorruptOrNonXlsxFiles() {
        assertThrows(
                InvalidRequestException.class,
                () -> parser.parse(
                        "akademisyenler.xlsx",
                        new ByteArrayInputStream("bozuk".getBytes())
                )
        );
        assertThrows(
                InvalidRequestException.class,
                () -> parser.parse(
                        "akademisyenler.xls",
                        new ByteArrayInputStream("bozuk".getBytes())
                )
        );
    }

    private AcademicianImportPreview parse(WorkbookBuilder builder) {
        try (Workbook workbook = new XSSFWorkbook()) {
            builder.build(workbook);
            return parser.parse(
                    "akademisyenler.xlsx",
                    new ByteArrayInputStream(toBytes(workbook))
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private AcademicianImportRow parseCommunication(String communication) {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "ILETISIM");
            writeRow(sheet, 1, "Ayse", "Yilmaz", communication);
        });

        return preview.getRows().getFirst();
    }

    private AcademicianImportRow parseDuplicateContactColumns(
            String email1,
            String email2,
            String phone1,
            String phone2,
            String communication
    ) {
        AcademicianImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(
                    sheet,
                    0,
                    "AD",
                    "SOYAD",
                    "E-POSTA 1",
                    "EMAIL 2",
                    "TELEFON 1",
                    "PHONE 2",
                    "ILETISIM"
            );
            writeRow(
                    sheet,
                    1,
                    "Ayse",
                    "Yilmaz",
                    email1,
                    email2,
                    phone1,
                    phone2,
                    communication
            );
        });

        return preview.getRows().getFirst();
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private void assertParsedName(
            AcademicianImportRow row,
            String title,
            String firstName,
            String lastName
    ) {
        assertEquals(title, row.getTitle());
        assertEquals(firstName, row.getFirstName());
        assertEquals(lastName, row.getLastName());
    }

    private List<ExpectedTitleAlias> configuredTitleAliases()
            throws ReflectiveOperationException {
        Field aliasesField = AcademicianExcelImportParser.class
                .getDeclaredField("TITLE_ALIASES");
        aliasesField.setAccessible(true);

        List<?> aliases = (List<?>) aliasesField.get(null);
        List<ExpectedTitleAlias> expectedAliases = new ArrayList<>();

        for (Object alias : aliases) {
            Method sourceMethod = alias.getClass().getDeclaredMethod("source");
            Method standardTitleMethod = alias.getClass()
                    .getDeclaredMethod("standardTitle");
            sourceMethod.setAccessible(true);
            standardTitleMethod.setAccessible(true);

            expectedAliases.add(new ExpectedTitleAlias(
                    (String) sourceMethod.invoke(alias),
                    (String) standardTitleMethod.invoke(alias)
            ));
        }

        return expectedAliases;
    }

    private record ExpectedTitleAlias(
            String source,
            String standardTitle
    ) {
    }

    @FunctionalInterface
    private interface WorkbookBuilder {
        void build(Workbook workbook);
    }
}
