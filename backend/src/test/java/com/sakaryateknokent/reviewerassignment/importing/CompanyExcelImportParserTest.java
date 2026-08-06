package com.sakaryateknokent.reviewerassignment.importing;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyExcelImportParserTest {

    private final CompanyExcelImportParser parser =
            new CompanyExcelImportParser();

    @Test
    void findsHeaderRowAfterReportRows() {
        CompanyImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "TGB Sirket Raporu");
            writeRow(sheet, 1, "");
            writeRow(sheet, 3, "Firma Unvanı", "Telefon 1");
            writeRow(sheet, 4, "ABC Teknoloji", "555");
        });

        assertEquals(5, preview.getRows().getFirst().getRowNumber());
        assertEquals("ABC Teknoloji", preview.getRows().getFirst().getName());
        assertFalse(preview.getDetectedHeaders().contains("TGB Sirket Raporu"));
        assertFalse(preview.getIgnoredHeaders().contains("TGB Sirket Raporu"));
    }

    @Test
    void keepsOnlyCompanyNameRowValidWithOtherFieldsNull() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı");
            writeRow(sheet, 1, "ABC Teknoloji");
        });

        assertEquals("ABC Teknoloji", row.getName());
        assertEquals(List.of(), row.getCompanyPhones());
        assertEquals(List.of(), row.getCompanyEmails());
        assertNull(row.getActivityArea());
        assertTrue(row.isValid());
    }

    @Test
    void marksRowInvalidWhenCompanyNameIsMissing() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "Telefon 1");
            writeRow(sheet, 1, "", "555");
        });

        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains("Firma unvani zorunludur"));
    }

    @Test
    void parsesSectorColumnAsActivityArea() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "Sektör");
            writeRow(sheet, 1, "ABC Teknoloji", "Yazilim");
        });

        assertEquals("Yazilim", row.getActivityArea());
        assertTrue(row.isValid());
    }

    @Test
    void parsesActivityAreaColumnAsActivityArea() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "Faaliyet Alanı");
            writeRow(sheet, 1, "ABC Teknoloji", "Ar-Ge");
        });

        assertEquals("Ar-Ge", row.getActivityArea());
        assertTrue(row.isValid());
    }

    @Test
    void prioritizesFilledActivityAreaWhenBothColumnsExist() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "Sektör", "Faaliyet Alanı");
            writeRow(sheet, 1, "ABC Teknoloji", "Yazilim", "Ar-Ge");
        });

        assertEquals("Ar-Ge", row.getActivityArea());
        assertTrue(row.isValid());
    }

    @Test
    void fallsBackToSectorWhenActivityAreaIsBlank() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "Sektör", "Faaliyet Alanı");
            writeRow(sheet, 1, "ABC Teknoloji", "Yazilim", "   ");
        });

        assertEquals("Yazilim", row.getActivityArea());
        assertTrue(row.isValid());
    }

    @Test
    void keepsRowValidAndActivityAreaNullWhenBothColumnsAreMissing() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "Telefon 1");
            writeRow(sheet, 1, "ABC Teknoloji", "555");
        });

        assertNull(row.getActivityArea());
        assertTrue(row.isValid());
    }

    @Test
    void mapsPhoneOneAndPhoneTwoToSeparateFields() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "Telefon 1", "Telefon 2");
            writeRow(sheet, 1, "ABC Teknoloji", "111", "222");
        });

        assertEquals(List.of("111", "222"), row.getCompanyPhones());
    }

    @Test
    void marksInvalidEmailAsInvalid() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "E-Posta");
            writeRow(sheet, 1, "ABC Teknoloji", "gecersiz");
        });

        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains(
                "E-posta parcasi formati gecersiz: gecersiz"
        ));
    }

    @Test
    void marksBothRowsInvalidWhenEmailIsDuplicatedInsideFile() {
        CompanyImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "Email");
            writeRow(sheet, 1, "ABC Teknoloji", " INFO@EXAMPLE.COM ");
            writeRow(sheet, 2, "XYZ Teknoloji", "info@example.com");
        });

        assertEquals(2, preview.getRows().size());
        assertEquals(
                List.of(false, false),
                preview.getRows().stream().map(CompanyImportRow::isValid).toList()
        );
        assertTrue(preview.getRows().getFirst().getErrors().contains(
                "Dosya icinde ayni e-posta birden fazla kullanilmis"
        ));
    }

    @Test
    void keepsNoBuildingAndWebInIgnoredHeaders() {
        CompanyImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "No", "Firma Unvanı", "Bina", "Web");
            writeRow(sheet, 1, "1", "ABC Teknoloji", "A Blok", "example.com");
        });

        assertTrue(preview.getIgnoredHeaders().contains("No"));
        assertTrue(preview.getIgnoredHeaders().contains("Bina"));
        assertTrue(preview.getIgnoredHeaders().contains("Web"));
        assertFalse(preview.getDetectedHeaders().contains("No"));
        assertFalse(preview.getDetectedHeaders().contains("Bina"));
        assertFalse(preview.getDetectedHeaders().contains("Web"));
        assertTrue(preview.getGeneralInformationMessages().contains(
                "No, Bina ve Web sutunlari bu aktarimda kullanilmayacaktir."
        ));
    }

    @Test
    void keepsUnknownColumnsFromSelectedHeaderRowInIgnoredHeaders() {
        CompanyImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "TGB Sirket Listesi Test Verisi");
            writeRow(sheet, 1, "Firma UnvanÄ±", "Aciklama");
            writeRow(sheet, 2, "ABC Teknoloji", "Not");
            writeRow(sheet, 3, "Firma Unvan\u0131", "Aciklama");
            writeRow(sheet, 4, "ABC Teknoloji", "Not");
        });

        assertTrue(preview.getIgnoredHeaders().contains("Aciklama"));
        assertFalse(preview.getDetectedHeaders().contains("Aciklama"));
        assertFalse(preview.getIgnoredHeaders().contains(
                "TGB Sirket Listesi Test Verisi"
        ));
    }

    @Test
    void skipsCompletelyBlankRows() {
        CompanyImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "Telefon 1");
            writeRow(sheet, 1, "", "");
            writeRow(sheet, 2, "ABC Teknoloji", "555");
        });

        assertEquals(1, preview.getRows().size());
        assertEquals("ABC Teknoloji", preview.getRows().getFirst().getName());
    }

    @Test
    void worksWhenColumnOrderChanges() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Email", "Telefon 2", "Firma Unvanı", "Telefon 1");
            writeRow(sheet, 1, "INFO@EXAMPLE.COM", "222", "ABC Teknoloji", "111");
        });

        assertEquals("ABC Teknoloji", row.getName());
        assertEquals(List.of("111", "222"), row.getCompanyPhones());
        assertEquals(List.of("info@example.com"), row.getCompanyEmails());
    }

    @Test
    void parsesMergedHeadersAndMergedDataCells() {
        CompanyImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            mergeWithValue(sheet, 0, 0, 0, 18, "TGB Sirket Raporu");
            mergeWithValue(sheet, 2, 3, 2, 4, "Firma Unvan\u0131");
            mergeWithValue(sheet, 2, 5, 2, 6, "Sektor");
            mergeWithValue(sheet, 2, 12, 2, 13, "Telefon 1");
            mergeWithValue(sheet, 2, 14, 2, 15, "Telefon 2");
            mergeWithValue(sheet, 2, 16, 2, 18, "Email");

            mergeWithValue(sheet, 3, 2, 3, 3, "Anonim Teknoloji");
            mergeWithValue(sheet, 3, 4, 3, 5, "Yazilim");
            mergeWithValue(sheet, 3, 11, 3, 12, "111");
            mergeWithValue(sheet, 3, 13, 3, 14, "222");
            mergeWithValue(sheet, 3, 16, 3, 17, "INFO@EXAMPLE.COM");
        });

        CompanyImportRow row = preview.getRows().getFirst();
        assertEquals(1, preview.getRows().size());
        assertEquals("Anonim Teknoloji", row.getName());
        assertEquals("Yazilim", row.getActivityArea());
        assertEquals(List.of("111", "222"), row.getCompanyPhones());
        assertEquals(List.of("info@example.com"), row.getCompanyEmails());
        assertEquals(
                List.of("Firma Unvan\u0131", "Sektor", "Telefon 1", "Telefon 2", "Email"),
                List.copyOf(preview.getDetectedHeaders())
        );
        assertFalse(preview.getDetectedHeaders().contains("TGB Sirket Raporu"));
        assertFalse(preview.getIgnoredHeaders().contains("TGB Sirket Raporu"));
        assertTrue(row.isValid());
    }

    @Test
    void resolvesDataMergeWhenHeaderAndDataRangesDoNotExactlyMatch() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            mergeWithValue(sheet, 0, 2, 0, 4, "Firma Unvan\u0131");
            mergeWithValue(sheet, 0, 6, 0, 7, "Email");
            mergeWithValue(sheet, 1, 1, 1, 2, "Aralik Kaymis Teknoloji");
            mergeWithValue(sheet, 1, 6, 1, 7, "kaymis@example.com");
        });

        assertEquals("Aralik Kaymis Teknoloji", row.getName());
        assertEquals(List.of("kaymis@example.com"), row.getCompanyEmails());
        assertTrue(row.isValid());
    }

    @Test
    void resolvesOverlappingDataMergeWhenTopLeftCellIsOutsideHeaderRange() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            mergeWithValue(sheet, 0, 2, 0, 4, "Firma Unvan\u0131");
            mergeWithValue(sheet, 0, 5, 0, 6, "Telefon 1");
            mergeWithValue(sheet, 1, 1, 1, 2, "Dis Anchor Teknoloji");
            mergeWithValue(sheet, 1, 5, 1, 6, "555");
        });

        assertEquals("Dis Anchor Teknoloji", row.getName());
        assertEquals(List.of("555"), row.getCompanyPhones());
        assertTrue(row.isValid());
    }

    @Test
    void skipsCompletelyBlankMergedRows() {
        CompanyImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            mergeWithValue(sheet, 0, 0, 0, 2, "Firma Unvan\u0131");
            mergeWithValue(sheet, 0, 3, 0, 4, "Telefon 1");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 3, 4));
            mergeWithValue(sheet, 2, 0, 2, 2, "Dolu Teknoloji");
            mergeWithValue(sheet, 2, 3, 2, 4, "555");
        });

        assertEquals(1, preview.getRows().size());
        assertEquals("Dolu Teknoloji", preview.getRows().getFirst().getName());
    }

    @Test
    void parsesMultipleVisibleSheets() {
        CompanyImportPreview preview = parse(workbook -> {
            Sheet first = workbook.createSheet("Bir");
            writeRow(first, 0, "Firma Unvanı");
            writeRow(first, 1, "ABC Teknoloji");

            Sheet second = workbook.createSheet("Iki");
            writeRow(second, 0, "Firma Unvanı");
            writeRow(second, 1, "XYZ Teknoloji");
        });

        assertEquals(2, preview.getSheetCount());
        assertEquals(2, preview.getRows().size());
        assertEquals(List.of("ABC Teknoloji", "XYZ Teknoloji"),
                preview.getRows().stream().map(CompanyImportRow::getName).toList());
    }

    @Test
    void parsesExternalCompanyAliasesAndIgnoresUnsupportedAddressColumns() {
        CompanyImportPreview preview = parse(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(
                    sheet,
                    0,
                    "İL",
                    "İLÇE",
                    "OSB",
                    "AÇIK ADRES",
                    "FİRMA",
                    "WEB SİTESİ",
                    "TELEFON",
                    "FAALİYET ALANI",
                    "E-POSTA",
                    "SEKTÖR"
            );
            writeRow(
                    sheet,
                    1,
                    "Sakarya",
                    "Serdivan",
                    "1. OSB",
                    "Adres",
                    "Dis Teknoloji",
                    "example.com",
                    "555",
                    "Makine",
                    "INFO@EXAMPLE.COM",
                    "Sanayi"
            );
        });

        CompanyImportRow row = preview.getRows().getFirst();
        assertEquals("Dis Teknoloji", row.getName());
        assertEquals(List.of("555"), row.getCompanyPhones());
        assertEquals(List.of("info@example.com"), row.getCompanyEmails());
        assertEquals("Makine", row.getActivityArea());
        assertTrue(preview.getIgnoredHeaders().contains("İL"));
        assertTrue(preview.getIgnoredHeaders().contains("İLÇE"));
        assertTrue(preview.getIgnoredHeaders().contains("OSB"));
        assertTrue(preview.getIgnoredHeaders().contains("AÇIK ADRES"));
        assertTrue(preview.getIgnoredHeaders().contains("WEB SİTESİ"));
        assertTrue(row.isValid());
    }

    @Test
    void fallsBackToExternalSectorWhenActivityAreaIsBlank() {
        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "FAALİYET ALANI", "SEKTÖR");
            writeRow(sheet, 1, "Dis Teknoloji", " ", "Enerji");
        });

        assertEquals("Enerji", row.getActivityArea());
    }

    @Test
    void parsesExternalCompanyPhoneEmailAliasesAndContactFields() {
        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(
                    sheet,
                    0,
                    "FİRMA",
                    "ŞİRKET TELEFON",
                    "ŞİRKET E-POSTA",
                    "YETKİLİ AD SOYAD",
                    "YETKİLİ ÜNVAN",
                    "YETKİLİ TELEFON",
                    "YETKİLİ E-POSTA"
            );
            writeRow(
                    sheet,
                    1,
                    "Dis Teknoloji",
                    "111",
                    "INFO@EXAMPLE.COM",
                    "Ayse Yilmaz",
                    "Mudur",
                    "222",
                    "AYSE@EXAMPLE.COM"
            );
        });

        assertEquals(List.of("111"), row.getCompanyPhones());
        assertEquals(List.of("info@example.com"), row.getCompanyEmails());
        assertEquals("Ayse Yilmaz", row.getContactFullName());
        assertEquals("Mudur", row.getContactPosition());
        assertEquals("222", row.getContactPhone());
        assertEquals("ayse@example.com", row.getContactEmail());
        assertTrue(row.isValid());
    }

    @Test
    void warnsAndKeepsExternalRowValidWhenContactNameIsBlank() {
        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "YETKİLİ AD SOYAD", "YETKİLİ TELEFON");
            writeRow(sheet, 1, "Dis Teknoloji", " ", "222");
        });

        assertNull(row.getContactFullName());
        assertTrue(row.isValid());
        assertEquals(1, row.getWarnings().size());
    }

    @Test
    void treatsBilgiYokEmailAsNullWithoutInvalidEmailOrDuplicateErrors() {
        CompanyImportPreview preview = parse(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "E-POSTA");
            writeRow(sheet, 1, "Bir Teknoloji", "Bilgi Yok");
            writeRow(sheet, 2, "Iki Teknoloji", " bilgi yok ");
        });

        assertEquals(2, preview.getRows().size());
        assertTrue(preview.getRows().stream().allMatch(
                row -> row.getCompanyEmails().isEmpty()
        ));
        assertTrue(preview.getRows().stream().allMatch(CompanyImportRow::isValid));
        assertTrue(preview.getRows().stream().allMatch(row -> row.getErrors().isEmpty()));
    }

    @Test
    void treatsPlaceholderPhoneAsNullWithoutLengthValidation() {
        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "TELEFON");
            writeRow(sheet, 1, "Dis Teknoloji", "Bilgi Yok");
        });

        assertEquals(List.of(), row.getCompanyPhones());
        assertTrue(row.isValid());
    }

    @Test
    void fallsBackToSectorWhenActivityAreaIsPlaceholder() {
        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "FAALİYET ALANI", "SEKTÖR");
            writeRow(sheet, 1, "Dis Teknoloji", "Bilgi Yok", "Enerji");
        });

        assertEquals("Enerji", row.getActivityArea());
    }

    @Test
    void keepsActivityAreaNullWhenActivityAreaAndSectorArePlaceholders() {
        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "FAALİYET ALANI", "SEKTÖR");
            writeRow(sheet, 1, "Dis Teknoloji", "Bilgi Yok", "--");
        });

        assertNull(row.getActivityArea());
        assertTrue(row.isValid());
    }

    @Test
    void keepsActivityAreaValidWhenLongerThan255AndShorterThan2000() {
        String activityArea = repeated("A", 1500);

        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "FAALİYET ALANI");
            writeRow(sheet, 1, "Dis Teknoloji", activityArea);
        });

        assertEquals(activityArea, row.getActivityArea());
        assertTrue(row.isValid());
    }

    @Test
    void acceptsActivityAreaWithExactly2000Characters() {
        String activityArea = repeated("A", 2000);

        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "FAALİYET ALANI");
            writeRow(sheet, 1, "Dis Teknoloji", activityArea);
        });

        assertEquals(activityArea, row.getActivityArea());
        assertTrue(row.isValid());
    }

    @Test
    void marksActivityAreaWith2001CharactersInvalid() {
        String activityArea = repeated("A", 2001);

        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "FAALİYET ALANI");
            writeRow(sheet, 1, "Dis Teknoloji", activityArea);
        });

        assertEquals(activityArea, row.getActivityArea());
        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains(
                "Faaliyet alanı en fazla 2000 karakter olabilir."
        ));
    }

    @Test
    void usesLongSectorFallbackWhenActivityAreaIsPlaceholder() {
        String sector = repeated("S", 1500);

        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "FAALİYET ALANI", "SEKTÖR");
            writeRow(sheet, 1, "Dis Teknoloji", "Bilgi Yok", sector);
        });

        assertEquals(sector, row.getActivityArea());
        assertTrue(row.isValid());
    }

    @Test
    void keepsContactFieldsNullAndDoesNotWarnWhenTheyArePlaceholders() {
        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(
                    sheet,
                    0,
                    "FİRMA",
                    "YETKİLİ AD SOYAD",
                    "YETKİLİ ÜNVAN",
                    "YETKİLİ TELEFON",
                    "YETKİLİ E-POSTA"
            );
            writeRow(
                    sheet,
                    1,
                    "Dis Teknoloji",
                    "Bilgi Yok",
                    "Yok",
                    "N/A",
                    "-"
            );
        });

        assertNull(row.getContactFullName());
        assertNull(row.getContactPosition());
        assertNull(row.getContactPhone());
        assertNull(row.getContactEmail());
        assertTrue(row.getWarnings().isEmpty());
        assertTrue(row.isValid());
    }

    @Test
    void treatsPlaceholderCompanyNameAsMissingAndInvalid() {
        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "TELEFON");
            writeRow(sheet, 1, "Bilgi Yok", "555");
        });

        assertNull(row.getName());
        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains("Firma unvani zorunludur"));
    }

    @Test
    void treatsPlaceholderVariantsAsNull() {
        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(
                    sheet,
                    0,
                    "FİRMA",
                    "TELEFON",
                    "E-POSTA",
                    "FAALİYET ALANI",
                    "YETKİLİ TELEFON"
            );
            writeRow(
                    sheet,
                    1,
                    "Dis Teknoloji",
                    "N/A",
                    "Yok",
                    "Bilinmiyor",
                    "-"
            );
        });

        assertEquals(List.of(), row.getCompanyPhones());
        assertEquals(List.of(), row.getCompanyEmails());
        assertNull(row.getActivityArea());
        assertNull(row.getContactPhone());
        assertTrue(row.isValid());
    }

    @Test
    void keepsRealInvalidEmailInvalid() {
        CompanyImportRow row = parseFirstRow(CompanyType.EXTERNAL, workbook -> {
            Sheet sheet = workbook.createSheet("Dis Sirketler");
            writeRow(sheet, 0, "FİRMA", "E-POSTA");
            writeRow(sheet, 1, "Dis Teknoloji", "gecersiz");
        });

        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains(
                "E-posta parcasi formati gecersiz: gecersiz"
        ));
    }

    @Test
    void splitsMultipleCompanyPhonesAndEmailsWithSupportedSeparators() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvan\u0131", "Telefon 1", "Email");
            writeRow(
                    sheet,
                    1,
                    "ABC Teknoloji",
                    "111 / 222; 333 & 444\n555",
                    "A@EXAMPLE.COM / b@example.com; a@example.com"
            );
        });

        assertEquals(
                List.of("111", "222", "333", "444", "555"),
                row.getCompanyPhones()
        );
        assertEquals(
                List.of("a@example.com", "b@example.com"),
                row.getCompanyEmails()
        );
        assertTrue(row.isValid());
    }

    @Test
    void splitsDashSeparatedPhonePairButKeepsHyphenatedEmailWhole() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvan\u0131", "Telefon 1", "Email");
            writeRow(
                    sheet,
                    1,
                    "ABC Teknoloji",
                    "111 - 222",
                    "info-sales@example.com"
            );
        });

        assertEquals(List.of("111", "222"), row.getCompanyPhones());
        assertEquals(List.of("info-sales@example.com"), row.getCompanyEmails());
        assertTrue(row.isValid());
    }

    @Test
    void splitsUnicodeDashSeparatedPhonePairs() {
        CompanyImportPreview preview = parse(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvan\u0131", "Telefon 1");
            writeRow(
                    sheet,
                    1,
                    "En Dash Teknoloji",
                    "0 262 658 86 65 – 0 262 658 94 92"
            );
            writeRow(
                    sheet,
                    2,
                    "Em Dash Teknoloji",
                    "0 262 658 86 65 — 0 262 658 94 92"
            );
        });

        assertEquals(
                List.of("0 262 658 86 65", "0 262 658 94 92"),
                preview.getRows().getFirst().getCompanyPhones()
        );
        assertEquals(
                List.of("0 262 658 86 65", "0 262 658 94 92"),
                preview.getRows().get(1).getCompanyPhones()
        );
        assertTrue(preview.getRows().stream().allMatch(CompanyImportRow::isValid));
    }

    @Test
    void keepsHyphenatedSinglePhoneWhole() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvan\u0131", "Telefon 1");
            writeRow(sheet, 1, "ABC Teknoloji", "0532-111-22-33");
        });

        assertEquals(List.of("0532-111-22-33"), row.getCompanyPhones());
        assertTrue(row.isValid());
    }

    @Test
    void keepsSpacedSinglePhoneWhole() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvan\u0131", "Telefon 1");
            writeRow(sheet, 1, "ABC Teknoloji", "0532 123 45 67");
        });

        assertEquals(List.of("0532 123 45 67"), row.getCompanyPhones());
        assertTrue(row.isValid());
    }

    @Test
    void marksInvalidCompanyContactPartsInvalid() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvan\u0131", "Telefon 1", "Email");
            writeRow(
                    sheet,
                    1,
                    "ABC Teknoloji",
                    "111 / abcxyz",
                    "ok@example.com / gecersiz"
            );
        });

        assertEquals(List.of("111"), row.getCompanyPhones());
        assertEquals(List.of("ok@example.com"), row.getCompanyEmails());
        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains("Telefon parcasi gecersiz: abcxyz"));
        assertTrue(row.getErrors().contains(
                "E-posta parcasi formati gecersiz: gecersiz"
        ));
    }

    @Test
    void mergesHeadersFromMultipleSheetsWithoutDuplicates() {
        CompanyImportPreview preview = parse(workbook -> {
            Sheet first = workbook.createSheet("Bir");
            writeRow(first, 0, "Firma Unvan\u0131", "Telefon 1", "Aciklama");
            writeRow(first, 1, "ABC Teknoloji", "111", "Not");

            Sheet second = workbook.createSheet("Iki");
            writeRow(
                    second,
                    0,
                    "Firma Unvan\u0131",
                    "Telefon 1",
                    "Telefon 2",
                    "Aciklama"
            );
            writeRow(second, 1, "XYZ Teknoloji", "222", "333", "Not");
        });

        assertEquals(
                List.of("Firma Unvan\u0131", "Telefon 1", "Telefon 2"),
                List.copyOf(preview.getDetectedHeaders())
        );
        assertEquals(List.of("Aciklama"), List.copyOf(preview.getIgnoredHeaders()));
    }

    private CompanyImportRow parseFirstRow(WorkbookBuilder builder) {
        return parse(builder).getRows().getFirst();
    }

    private CompanyImportRow parseFirstRow(
            CompanyType companyType,
            WorkbookBuilder builder
    ) {
        return parse(companyType, builder).getRows().getFirst();
    }

    private CompanyImportPreview parse(WorkbookBuilder builder) {
        return parse(CompanyType.TGB, builder);
    }

    private CompanyImportPreview parse(
            CompanyType companyType,
            WorkbookBuilder builder
    ) {
        try (Workbook workbook = new XSSFWorkbook()) {
            builder.build(workbook);
            return parser.parse(
                    "sirketler.xlsx",
                    new ByteArrayInputStream(toBytes(workbook)),
                    companyType
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private String repeated(String value, int count) {
        return value.repeat(count);
    }

    private void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private void mergeWithValue(
            Sheet sheet,
            int firstRow,
            int firstColumn,
            int lastRow,
            int lastColumn,
            String value
    ) {
        Row row = sheet.getRow(firstRow);
        if (row == null) {
            row = sheet.createRow(firstRow);
        }
        row.createCell(firstColumn).setCellValue(value);
        sheet.addMergedRegion(new CellRangeAddress(
                firstRow,
                lastRow,
                firstColumn,
                lastColumn
        ));
    }

    @FunctionalInterface
    private interface WorkbookBuilder {
        void build(Workbook workbook);
    }
}
