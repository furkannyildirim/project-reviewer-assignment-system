package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianExcelImportParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicianImportPreviewServiceTest {

    private final AcademicianImportPreviewService previewService =
            new AcademicianImportPreviewService(
                    new AcademicianExcelImportParser()
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
    void marksDuplicateEmailsInsideFileInvalid() {
        AcademicianImportPreviewResponse response = previewService.preview(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "same@example.com");
                    writeRow(sheet, 2, "Ali", "Demir", "SAME@example.com");
                })
        );

        assertEquals(0, response.validRowCount());
        assertEquals(2, response.invalidRowCount());
        assertTrue(response.rows().getFirst().errors().contains(
                "Dosya icinde ayni e-posta birden fazla kullanilmis"
        ));
        assertTrue(response.rows().get(1).errors().contains(
                "Dosya icinde ayni e-posta birden fazla kullanilmis"
        ));
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

    private void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    @FunctionalInterface
    private interface WorkbookBuilder {
        void build(Sheet sheet);
    }
}
