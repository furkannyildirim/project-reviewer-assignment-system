package com.sakaryateknokent.reviewerassignment.importing;

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
        assertNull(row.getPhone());
        assertNull(row.getSecondaryPhone());
        assertNull(row.getEmail());
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

        assertEquals("111", row.getPhone());
        assertEquals("222", row.getSecondaryPhone());
    }

    @Test
    void marksInvalidEmailAsInvalid() {
        CompanyImportRow row = parseFirstRow(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvanı", "E-Posta");
            writeRow(sheet, 1, "ABC Teknoloji", "gecersiz");
        });

        assertFalse(row.isValid());
        assertTrue(row.getErrors().contains("E-posta formati gecersiz"));
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
        assertEquals("111", row.getPhone());
        assertEquals("222", row.getSecondaryPhone());
        assertEquals("info@example.com", row.getEmail());
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
        assertEquals("111", row.getPhone());
        assertEquals("222", row.getSecondaryPhone());
        assertEquals("info@example.com", row.getEmail());
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
        assertEquals("kaymis@example.com", row.getEmail());
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
        assertEquals("555", row.getPhone());
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

    private CompanyImportPreview parse(WorkbookBuilder builder) {
        try (Workbook workbook = new XSSFWorkbook()) {
            builder.build(workbook);
            return parser.parse(
                    "sirketler.xlsx",
                    new ByteArrayInputStream(toBytes(workbook))
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
