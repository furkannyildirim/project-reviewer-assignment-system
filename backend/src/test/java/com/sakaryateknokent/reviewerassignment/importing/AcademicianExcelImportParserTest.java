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
        assertFalse(preview.getGeneralWarnings().isEmpty());
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

    @FunctionalInterface
    private interface WorkbookBuilder {
        void build(Workbook workbook);
    }
}
