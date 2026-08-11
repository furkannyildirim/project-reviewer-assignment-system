package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AcademicianExportService {

    private static final String[] HEADERS = {
            "Ad",
            "Soyad",
            "E-posta",
            "Telefon",
            "Ünvan",
            "Anahtar Kelimeler"
    };

    public byte[] exportToExcel(List<AcademicianResponse> academicians) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            CellStyle headerStyle = createHeaderStyle(workbook);

            writeHeader(sheet, headerStyle);
            writeRows(sheet, academicians);
            applyColumnWidths(sheet);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Akademisyen Excel dosyası üretilemedi", exception);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);

        for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(HEADERS[columnIndex]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRows(
            Sheet sheet,
            List<AcademicianResponse> academicians
    ) {
        for (int index = 0; index < academicians.size(); index++) {
            AcademicianResponse academician = academicians.get(index);
            Row row = sheet.createRow(index + 1);

            row.createCell(0).setCellValue(nullToBlank(academician.firstName()));
            row.createCell(1).setCellValue(nullToBlank(academician.lastName()));
            row.createCell(2).setCellValue(nullToBlank(academician.email()));
            row.createCell(3).setCellValue(nullToBlank(academician.phone()));
            row.createCell(4).setCellValue(nullToBlank(academician.title()));
            row.createCell(5).setCellValue(joinKeywords(academician));
        }
    }

    private void applyColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 24 * 256);
        sheet.setColumnWidth(2, 34 * 256);
        sheet.setColumnWidth(3, 18 * 256);
        sheet.setColumnWidth(4, 24 * 256);
        sheet.setColumnWidth(5, 48 * 256);
    }

    private String nullToBlank(String value) {
        return value == null
                ? ""
                : value;
    }

    private String joinKeywords(AcademicianResponse academician) {
        if (academician.keywords() == null || academician.keywords().isEmpty()) {
            return "";
        }

        return academician.keywords()
                .stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));
    }
}
