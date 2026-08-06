package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewRowResponse;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.importing.CompanyExcelImportParser;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactMethodRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompanyImportPreviewServiceTest {

    private final CompanyRepository companyRepository =
            mock(CompanyRepository.class);
    private final CompanyContactMethodRepository contactMethodRepository =
            mock(CompanyContactMethodRepository.class);
    private final CompanyImportPreviewService previewService =
            new CompanyImportPreviewService(
                    new CompanyExcelImportParser(),
                    companyRepository,
                    contactMethodRepository,
                    new CompanyLocationMergeClassifier()
            );

    @BeforeEach
    void setUp() {
        lenient().when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of());
        lenient().when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of());
    }

    @Test
    void previewsNewCompanyWhenNoDuplicateExists() {
        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", "A Blok");
                }), CompanyType.TGB)
                .rows()
                .getFirst();

        assertEquals("NEW", row.status());
        assertTrue(row.updateMessages().isEmpty());
        assertTrue(row.warnings().isEmpty());
    }

    @Test
    void previewsUpdateWhenExistingLocationIsMissing() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", "A Blok");
                }), CompanyType.TGB)
                .rows()
                .getFirst();

        assertEquals("UPDATE", row.status());
        assertEquals(List.of("Bina bilgisi eklenecek: A Blok"),
                row.updateMessages());
        assertTrue(row.warnings().isEmpty());
    }

    @Test
    void tgbPreviewOverloadClassifiesEmailDuplicateLocationUpdate() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        CompanyContactMethod emailMethod = new CompanyContactMethod();
        emailMethod.setCompany(existingCompany);
        emailMethod.setType(CompanyContactMethodType.EMAIL);
        emailMethod.setValue("info@example.com");
        when(contactMethodRepository.findByTypeAndValueIgnoreCaseOrderByIdAsc(
                CompanyContactMethodType.EMAIL,
                "info@example.com"
        )).thenReturn(List.of(emailMethod));

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", "INFO@EXAMPLE.COM", "A Blok");
                }))
                .rows()
                .getFirst();

        assertEquals("UPDATE", row.status());
        assertEquals(List.of("Bina bilgisi eklenecek: A Blok"),
                row.updateMessages());
        assertTrue(row.warnings().isEmpty());
    }

    @Test
    void previewsDuplicateWhenExistingLocationIsSame() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        existingCompany.setBuilding("A Blok");
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", " A Blok ");
                }), CompanyType.TGB)
                .rows()
                .getFirst();

        assertEquals("DUPLICATE", row.status());
        assertTrue(row.updateMessages().isEmpty());
        assertTrue(row.warnings().isEmpty());
    }

    @Test
    void previewsWarningWhenExistingLocationConflicts() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        existingCompany.setBuilding("A Blok");
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", "B Blok");
                }), CompanyType.TGB)
                .rows()
                .getFirst();

        assertEquals("WARNING", row.status());
        assertTrue(row.updateMessages().isEmpty());
        assertEquals(1, row.warnings().size());
        assertTrue(row.warnings().getFirst().contains("Bina bilgisi"));
    }

    @Test
    void previewsUpdateAndKeepsWarningWhenSameRowHasBoth() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        existingCompany.setBuilding("A Blok");
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        CompanyImportPreviewResponse response = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "\u0130L", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", "Sakarya", "B Blok");
                }), CompanyType.TGB);
        CompanyImportPreviewRowResponse row = response.rows().getFirst();

        assertEquals("UPDATE", row.status());
        assertEquals(List.of("Il bilgisi eklenecek: Sakarya"),
                row.updateMessages());
        assertEquals(1, row.warnings().size());
        assertEquals(1, response.warningCount());
    }

    private Company company(String name, CompanyType companyType) {
        Company company = new Company();
        company.setName(name);
        company.setCompanyType(companyType);
        return company;
    }

    private MockMultipartFile file(WorkbookBuilder builder) {
        try (Workbook workbook = new XSSFWorkbook()) {
            builder.build(workbook);
            return new MockMultipartFile(
                    "file",
                    "sirketler.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    toBytes(workbook)
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
        for (int index = 0; index < values.length; index++) {
            row.createCell(index).setCellValue(values[index]);
        }
    }

    @FunctionalInterface
    private interface WorkbookBuilder {
        void build(Workbook workbook);
    }
}
