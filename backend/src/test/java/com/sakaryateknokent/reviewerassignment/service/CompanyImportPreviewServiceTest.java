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

        assertEquals("EXISTING", row.status());
        assertTrue(row.updateMessages().isEmpty());
        assertTrue(row.locationWarnings().isEmpty());
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
        assertEquals(row.warnings(), row.locationWarnings());
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
        assertEquals(row.warnings(), row.locationWarnings());
        assertEquals(1, response.warningCount());
    }

    @Test
    void previewsExternalLocationUpdateFromNameFallback() {
        Company existingCompany = company("Dis Teknoloji", CompanyType.EXTERNAL);
        when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of(existingCompany));

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "IL", "ILCE", "OSB", "BINA");
                    writeRow(sheet, 1, "Dis Teknoloji", "Sakarya",
                            "Adapazari", "1. OSB", "A Blok");
                }), CompanyType.EXTERNAL)
                .rows()
                .getFirst();

        assertEquals("UPDATE", row.status());
        assertEquals(List.of(
                "Il bilgisi eklenecek: Sakarya",
                "Ilce bilgisi eklenecek: Adapazari",
                "OSB bilgisi eklenecek: 1. OSB",
                "Bina bilgisi eklenecek: A Blok"
        ), row.updateMessages());
        assertTrue(row.warnings().isEmpty());
    }

    @Test
    void previewMarksSameFileNoEmailNameDuplicatesLikeImport() {
        CompanyImportPreviewResponse response = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131");
                    writeRow(sheet, 1, "ABC Teknoloji");
                    writeRow(sheet, 2, " abc teknoloji ");
                }), CompanyType.TGB);

        assertEquals(List.of("DUPLICATE", "DUPLICATE"),
                response.rows()
                        .stream()
                        .map(CompanyImportPreviewRowResponse::status)
                        .toList());
        assertTrue(response.rows()
                .stream()
                .allMatch(row -> row.locationWarnings().isEmpty()));
        assertTrue(response.rows()
                .stream()
                .allMatch(row -> row.warnings().getFirst()
                        .contains("Dosya icinde ayni sirket adi")));
        assertEquals(2, response.validRowCount());
        assertEquals(0, response.invalidRowCount());
    }

    @Test
    void previewUsesVirtualUpdatedLocationStateForLaterRows() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        CompanyContactMethod firstEmail = emailMethod(
                existingCompany,
                "first@example.com"
        );
        CompanyContactMethod secondEmail = emailMethod(
                existingCompany,
                "second@example.com"
        );
        when(contactMethodRepository.findByTypeAndValueIgnoreCaseOrderByIdAsc(
                CompanyContactMethodType.EMAIL,
                "first@example.com"
        )).thenReturn(List.of(firstEmail));
        when(contactMethodRepository.findByTypeAndValueIgnoreCaseOrderByIdAsc(
                CompanyContactMethodType.EMAIL,
                "second@example.com"
        )).thenReturn(List.of(secondEmail));

        CompanyImportPreviewResponse response = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email", "\u0130L");
                    writeRow(sheet, 1, "ABC Teknoloji",
                            "first@example.com", "Sakarya");
                    writeRow(sheet, 2, "ABC Teknoloji",
                            "second@example.com", "Sakarya");
                }), CompanyType.TGB);

        assertEquals("UPDATE", response.rows().get(0).status());
        assertEquals("EXISTING", response.rows().get(1).status());
        assertEquals(List.of("Il bilgisi eklenecek: Sakarya"),
                response.rows().get(0).updateMessages());
        assertTrue(response.rows().get(1).updateMessages().isEmpty());
        assertEquals(null, existingCompany.getCity());
    }

    @Test
    void previewUsesExternalNameAddressDuplicateRule() {
        Company existingCompany = company("Dis Teknoloji", CompanyType.EXTERNAL);
        existingCompany.setAddress("Baris Mahallesi");
        when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of(existingCompany));

        CompanyImportPreviewResponse response = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "Dis Teknoloji", "Baris Mahallesi");
                    writeRow(sheet, 2, "Dis Teknoloji", "Farkli Adres");
                }), CompanyType.EXTERNAL);

        assertEquals(List.of("EXISTING", "NEW"),
                response.rows()
                        .stream()
                        .map(CompanyImportPreviewRowResponse::status)
                        .toList());
    }

    @Test
    void previewUsesExternalSameFileDuplicateRules() {
        CompanyImportPreviewResponse response = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "E-POSTA", "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "Bir", "INFO@EXAMPLE.COM", "Adres A");
                    writeRow(sheet, 2, "Iki", "info@example.com", "Adres B");
                    writeRow(sheet, 3, "Uc", " ", "Adres A");
                    writeRow(sheet, 4, " uc ", " ", " adres   a ");
                    writeRow(sheet, 5, "Uc", " ", "Adres B");
                    writeRow(sheet, 6, "Dort", " ", "Adres A");
                    writeRow(sheet, 7, "Bes", " ", " ");
                    writeRow(sheet, 8, " bes ", " ", " ");
                }), CompanyType.EXTERNAL);

        assertEquals(List.of(
                "DUPLICATE",
                "DUPLICATE",
                "DUPLICATE",
                "DUPLICATE",
                "NEW",
                "NEW",
                "DUPLICATE",
                "DUPLICATE"
        ), response.rows()
                .stream()
                .map(CompanyImportPreviewRowResponse::status)
                .toList());
    }

    private Company company(String name, CompanyType companyType) {
        Company company = new Company();
        company.setName(name);
        company.setCompanyType(companyType);
        return company;
    }

    private CompanyContactMethod emailMethod(Company company, String email) {
        CompanyContactMethod contactMethod = new CompanyContactMethod();
        contactMethod.setCompany(company);
        contactMethod.setType(CompanyContactMethodType.EMAIL);
        contactMethod.setValue(email);
        return contactMethod;
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
