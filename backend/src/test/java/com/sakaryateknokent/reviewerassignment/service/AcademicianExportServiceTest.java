package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcademicianExportServiceTest {

    private final AcademicianExportService exportService =
            new AcademicianExportService();

    @Test
    void exportsHeadersAndAcademicianRowsToWorkbook() {
        Sheet sheet = sheet(exportService.exportToExcel(List.of(
                new AcademicianResponse(
                        1L,
                        0L,
                        "Ayşe",
                        "Yılmaz",
                        "ayse@example.com",
                        "555 111 2233",
                        "Doç. Dr.",
                        orderedSet("robotik", "yapay zeka")
                )
        )));

        Row header = sheet.getRow(0);
        assertEquals("Ad", header.getCell(0).getStringCellValue());
        assertEquals("Soyad", header.getCell(1).getStringCellValue());
        assertEquals("E-posta", header.getCell(2).getStringCellValue());
        assertEquals("Telefon", header.getCell(3).getStringCellValue());
        assertEquals("Ünvan", header.getCell(4).getStringCellValue());
        assertEquals(
                "Anahtar Kelimeler",
                header.getCell(5).getStringCellValue()
        );

        Row row = sheet.getRow(1);
        assertEquals("Ayşe", row.getCell(0).getStringCellValue());
        assertEquals("Yılmaz", row.getCell(1).getStringCellValue());
        assertEquals("ayse@example.com", row.getCell(2).getStringCellValue());
        assertEquals("555 111 2233", row.getCell(3).getStringCellValue());
        assertEquals("Doç. Dr.", row.getCell(4).getStringCellValue());
        assertEquals(
                "robotik, yapay zeka",
                row.getCell(5).getStringCellValue()
        );
    }

    @Test
    void exportsOnlyHeaderWhenResultIsEmpty() {
        Sheet sheet = sheet(exportService.exportToExcel(List.of()));

        assertEquals(0, sheet.getLastRowNum());
        assertEquals("Ad", sheet.getRow(0).getCell(0).getStringCellValue());
    }

    @Test
    void writesBlankCellsForNullValuesAndSortsKeywords() {
        Sheet sheet = sheet(exportService.exportToExcel(List.of(
                new AcademicianResponse(
                        1L,
                        0L,
                        "Ali",
                        "Demir",
                        null,
                        null,
                        null,
                        orderedSet("veri", "algoritma")
                )
        )));

        Row row = sheet.getRow(1);
        assertEquals("", row.getCell(2).getStringCellValue());
        assertEquals("", row.getCell(3).getStringCellValue());
        assertEquals("", row.getCell(4).getStringCellValue());
        assertEquals("algoritma, veri", row.getCell(5).getStringCellValue());
    }

    private Sheet sheet(byte[] workbookBytes) {
        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(workbookBytes)
        )) {
            return workbook.getSheetAt(0);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Set<String> orderedSet(String... values) {
        return new LinkedHashSet<>(List.of(values));
    }
}
