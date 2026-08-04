package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianExcelImportParser;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcademicianImportPreviewServiceTest {

    private final AcademicianRepository academicianRepository =
            mock(AcademicianRepository.class);
    private final AcademicianImportPreviewService previewService =
            new AcademicianImportPreviewService(
                    new AcademicianExcelImportParser(),
                    new AcademicianImportClassifier(academicianRepository)
            );

    @Test
    void returnsCombinedNameMessageAsGeneralWarningAndIncreasesWarningCount() {
        AcademicianImportPreviewResponse response = previewService.preview(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "ADI SOYADI");
                    writeRow(sheet, 1, "Dr. Ayse Yilmaz");
                })
        );

        String combinedNameMessage =
                "Birleşik ad soyad alanları otomatik ayrıştırılmıştır. "
                        + "Gerekirse aktarımdan sonra düzenleyebilirsiniz.";

        assertEquals(1, response.warningCount());
        assertTrue(response.generalWarnings().contains(combinedNameMessage));
        assertFalse(response.generalInformationMessages()
                .contains(combinedNameMessage));
    }

    @Test
    void carriesSheetLevelParserWarningInPreviewResponse() {
        AcademicianImportPreviewResponse response = previewService.preview(
                workbookWithSheets("akademisyenler.xlsx", workbook -> {
                    Sheet valid = workbook.createSheet("Gecerli");
                    writeRow(valid, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(valid, 1, "Ayse", "Yilmaz", "ayse@example.com");

                    Sheet invalid = workbook.createSheet("Sheet 2");
                    writeRow(invalid, 0, "EMAIL", "TELEFON");
                    writeRow(invalid, 1, "skip@example.com", "0555");
                })
        );

        assertEquals(1, response.totalDetectedRows());
        assertEquals(1, response.validRowCount());
        assertEquals(0, response.invalidRowCount());
        assertEquals(1, response.warningCount());
        assertEquals(1, response.generalWarnings().size());
        assertTrue(response.generalWarnings().getFirst().contains("Sheet 2"));
        assertEquals("NEW", response.rows().getFirst().status());
    }

    @Test
    void keepsRowsValidWhenEmailCouldExistInDatabase() {
        AcademicianImportPreviewResponse response = previewService.preview(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "ayse@example.com");
                })
        );

        assertEquals(1, response.validRowCount());
        assertEquals(0, response.invalidRowCount());
        assertTrue(response.rows().getFirst().errors().isEmpty());
    }

    @Test
    void keepsNoEmailRowsValidWithoutDatabaseIdentityCheck() {
        AcademicianImportPreviewResponse response = previewService.preview(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.");
                })
        );

        assertEquals(1, response.validRowCount());
        assertEquals(0, response.invalidRowCount());
        assertTrue(response.rows().getFirst().errors().isEmpty());
    }

    @Test
    void marksSameEmailDifferentIdentitiesInsideFileAsConflict() {
        AcademicianImportPreviewResponse response = previewService.preview(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "same@example.com");
                    writeRow(sheet, 2, "Ali", "Demir", "SAME@example.com");
                })
        );

        assertEquals(0, response.validRowCount());
        assertEquals(2, response.conflictRowCount());
        assertEquals(0, response.invalidRowCount());
        assertEquals("CONFLICT", response.rows().getFirst().status());
        assertTrue(response.rows().getFirst().messages().getFirst().contains(
                "ayni e-posta"
        ));
    }

    @Test
    void keepsInvalidParserErrorsOutOfPreviewMessages() {
        AcademicianImportPreviewResponse response = previewService.preview(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "", "invalid-email");
                })
        );

        var row = response.rows().getFirst();

        assertEquals("INVALID", row.status());
        assertTrue(row.errors().contains("E-posta formati gecersiz"));
        assertTrue(row.errors().contains("Soyad zorunludur"));
        assertFalse(row.messages().contains("E-posta formati gecersiz"));
        assertFalse(row.messages().contains("Soyad zorunludur"));
    }

    @Test
    void classifiesPreviewRowsWithSameStatusesUsedByImport() {
        Academician updateMatch = academician("Ugur", "Guzel", "Doc. Dr.");
        Academician duplicateMatch = academician("Ali", "Demir", "Prof. Dr.");
        duplicateMatch.setEmail("ali@example.com");
        Academician conflictMatch = academician("Baska", "Kisi", "Prof. Dr.");
        conflictMatch.setEmail("conflict@example.com");

        when(academicianRepository.findByNormalizedEmail("ugur@example.com"))
                .thenReturn(List.of());
        when(academicianRepository.findByCanonicalIdentity(
                "ugur",
                "guzel",
                "doc. dr."
        )).thenReturn(List.of(updateMatch));
        when(academicianRepository.findByNormalizedEmail("ali@example.com"))
                .thenReturn(List.of(duplicateMatch));
        when(academicianRepository.findByNormalizedEmail("conflict@example.com"))
                .thenReturn(List.of(conflictMatch));

        AcademicianImportPreviewResponse response = previewService.preview(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "EMAIL");
                    writeRow(sheet, 1, "Yeni", "Kisi", "Doc. Dr.",
                            "new@example.com");
                    writeRow(sheet, 2, "Ugur", "Guzel", "Doc. Dr.",
                            "ugur@example.com");
                    writeRow(sheet, 3, "Ali", "Demir", "Prof. Dr.",
                            "ali@example.com");
                    writeRow(sheet, 4, "Ayse", "Yilmaz", "Doc. Dr.",
                            "conflict@example.com");
                    writeRow(sheet, 5, "", "Eksik", "Doc. Dr.",
                            "invalid@example.com");
                })
        );

        assertEquals(1, response.newRowCount());
        assertEquals(1, response.updateRowCount());
        assertEquals(1, response.duplicateRowCount());
        assertEquals(1, response.conflictRowCount());
        assertEquals(1, response.invalidRowCount());
        assertEquals(List.of(
                "INVALID",
                "NEW",
                "UPDATE",
                "DUPLICATE",
                "CONFLICT"
        ), response.rows().stream()
                .map(row -> row.status())
                .toList());
    }

    @Test
    void noEmailTurkishIdentityWithMultipleDatabaseMatchesIsConflict() {
        when(academicianRepository.findByCanonicalIdentity(
                "mahir",
                "\u0131\u015F\u0131k",
                "dr. \u00F6\u011Fr. \u00FCyesi"
        )).thenReturn(List.of(
                academician("Mahir", "I\u015F\u0131k", "Dr. \u00D6\u011Fr. \u00DCyesi"),
                academician("Mahir", "I\u015F\u0131k", "Dr. \u00D6\u011Fr. \u00DCyesi")
        ));

        AcademicianImportPreviewResponse response = previewService.preview(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "ADI SOYADI");
                    writeRow(sheet, 1,
                            "Dr.\u00D6\u011Fr.\u00DCyesi Mahir I\u015F\u0131k");
                })
        );

        assertEquals(0, response.newRowCount());
        assertEquals(1, response.conflictRowCount());
        assertEquals("CONFLICT", response.rows().getFirst().status());
    }

    private MockMultipartFile workbook(
            String fileName,
            WorkbookBuilder builder
    ) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            builder.build(workbook.createSheet("Akademisyenler"));
            workbook.write(outputStream);

            return new MockMultipartFile(
                    "file",
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private MockMultipartFile workbookWithSheets(
            String fileName,
            MultiSheetWorkbookBuilder builder
    ) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            builder.build(workbook);
            workbook.write(outputStream);

            return new MockMultipartFile(
                    "file",
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private Academician academician(
            String firstName,
            String lastName,
            String title
    ) {
        Academician academician = new Academician();
        academician.setFirstName(firstName);
        academician.setLastName(lastName);
        academician.setTitle(title);
        return academician;
    }

    @FunctionalInterface
    private interface WorkbookBuilder {
        void build(Sheet sheet);
    }

    @FunctionalInterface
    private interface MultiSheetWorkbookBuilder {
        void build(Workbook workbook);
    }
}
