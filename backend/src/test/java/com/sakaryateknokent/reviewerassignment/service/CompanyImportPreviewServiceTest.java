package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewRowResponse;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.importing.CompanyExcelImportParser;
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
    private final CompanyImportPreviewService previewService =
            new CompanyImportPreviewService(
                    new CompanyExcelImportParser(),
                    companyRepository,
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
    void previewsWarningWithoutUpdatingWhenExistingLocationIsMissing() {
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

        assertEquals("WARNING", row.status());
        assertTrue(row.updateMessages().isEmpty());
        assertEquals(row.warnings(), row.locationWarnings());
        assertTrue(row.warnings().getFirst().contains("manuel kontrol"));
        assertEquals(null, existingCompany.getBuilding());
    }

    @Test
    void tgbPreviewIgnoresExistingTgbEmailWhenNameDiffers() {
        Company existingCompany = company("Existing TGB", CompanyType.TGB);
        CompanyContactMethod emailMethod = new CompanyContactMethod();
        emailMethod.setCompany(existingCompany);
        emailMethod.setType(CompanyContactMethodType.EMAIL);
        emailMethod.setValue("info@example.com");

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", "INFO@EXAMPLE.COM", "A Blok");
                }))
                .rows()
                .getFirst();

        assertEquals("NEW", row.status());
        assertTrue(row.updateMessages().isEmpty());
        assertTrue(row.warnings().isEmpty());
        assertEquals(null, existingCompany.getBuilding());
    }

    @Test
    void tgbPreviewIgnoresExternalWithSameCanonicalNameAndEmail() {
        Company externalCompany = company(
                "ABC Teknoloji",
                CompanyType.EXTERNAL
        );
        CompanyContactMethod emailMethod = emailMethod(
                externalCompany,
                "info@example.com"
        );

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email");
                    writeRow(sheet, 1, "ABC Teknoloji", "INFO@EXAMPLE.COM");
                }), CompanyType.TGB)
                .rows()
                .getFirst();

        assertEquals("NEW", row.status());
        assertTrue(row.warnings().isEmpty());
        assertTrue(row.updateMessages().isEmpty());
        assertTrue(row.locationWarnings().isEmpty());
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
        assertEquals(2, row.warnings().size());
        assertTrue(row.warnings().stream()
                .anyMatch(message -> message.contains("Bina bilgisi")));
        assertEquals("A Blok", existingCompany.getBuilding());
    }

    @Test
    void previewsManualReviewWhenSameRowHasEnrichmentAndConflict() {
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

        assertEquals("WARNING", row.status());
        assertTrue(row.updateMessages().isEmpty());
        assertEquals(2, row.warnings().size());
        assertEquals(row.warnings(), row.locationWarnings());
        assertEquals(2, response.warningCount());
        assertEquals(null, existingCompany.getCity());
        assertEquals("A Blok", existingCompany.getBuilding());
    }

    @Test
    void previewsExternalLocationManualReviewFromNameFallback() {
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

        assertEquals("WARNING", row.status());
        assertTrue(row.updateMessages().isEmpty());
        assertTrue(row.warnings().getFirst().contains("manuel kontrol"));
        assertEquals(null, existingCompany.getCity());
        assertEquals(null, existingCompany.getDistrict());
        assertEquals(null, existingCompany.getOsb());
        assertEquals(null, existingCompany.getBuilding());
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
    void previewMarksSameCanonicalTgbNamesDuplicateRegardlessOfEmails() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        CompanyContactMethod firstEmail = emailMethod(
                existingCompany,
                "first@example.com"
        );
        CompanyContactMethod secondEmail = emailMethod(
                existingCompany,
                "second@example.com"
        );

        CompanyImportPreviewResponse response = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email", "\u0130L");
                    writeRow(sheet, 1, "ABC Teknoloji",
                            "first@example.com", "Sakarya");
                    writeRow(sheet, 2, "ABC Teknoloji",
                            "second@example.com", "Sakarya");
                }), CompanyType.TGB);

        assertEquals("DUPLICATE", response.rows().get(0).status());
        assertEquals("DUPLICATE", response.rows().get(1).status());
        assertTrue(response.rows().get(0).updateMessages().isEmpty());
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
    void previewsWarningForWeakExternalNameFallback() {
        Company existingCompany = company("ABC", CompanyType.EXTERNAL);
        existingCompany.setAddress("Ankara");
        when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of(existingCompany));

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "ABC", " ");
                }), CompanyType.EXTERNAL)
                .rows()
                .getFirst();

        assertEquals("WARNING", row.status());
        assertTrue(row.warnings().getFirst()
                .contains("yalnizca sirket adina gore eslesti"));
        assertTrue(row.updateMessages().isEmpty());
        assertTrue(row.locationWarnings().isEmpty());
        assertEquals("Ankara", existingCompany.getAddress());
    }

    @Test
    void previewUsesExternalSameFileDuplicateRules() {
        CompanyImportPreviewResponse response = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "TELEFON", "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "Bir", "0532", "Adres A");
                    writeRow(sheet, 2, "Iki", "0532", "Adres B");
                    writeRow(sheet, 3, "Uc", " ", "Adres A");
                    writeRow(sheet, 4, " uc ", " ", " adres   a ");
                    writeRow(sheet, 5, "Uc", " ", "Adres B");
                    writeRow(sheet, 6, "Dort", " ", "Adres A");
                    writeRow(sheet, 7, "Bes", " ", " ");
                    writeRow(sheet, 8, " bes ", " ", " ");
                }), CompanyType.EXTERNAL);

        assertEquals(List.of(
                "NEW",
                "NEW",
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

    @Test
    void previewsBothExternalRowsAsNewWithDifferentAddressesAndSameEmail() {
        CompanyImportPreviewResponse response = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "E-POSTA", "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "ABC", "same@email.com", "Adres A");
                    writeRow(sheet, 2, "ABC", "same@email.com", "Adres B");
                }), CompanyType.EXTERNAL);

        assertEquals(List.of("NEW", "NEW"), response.rows()
                .stream()
                .map(CompanyImportPreviewRowResponse::status)
                .toList());
        assertTrue(response.rows().stream()
                .allMatch(row -> row.warnings().isEmpty()));
    }

    @Test
    void previewsBothSameAddressRowsAsNewWhenTheirPhonesDiffer() {
        CompanyImportPreviewResponse response = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "TELEFON", "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "ABC", "111", "Sakarya");
                    writeRow(sheet, 2, "ABC", "222", "Sakarya");
                }), CompanyType.EXTERNAL);

        assertEquals(List.of("NEW", "NEW"), response.rows()
                .stream()
                .map(CompanyImportPreviewRowResponse::status)
                .toList());
    }

    @Test
    void previewsWarningForPreExistingCompanyWithEmailAndMissingAddress() {
        Company existingCompany = company("ABC", CompanyType.EXTERNAL);
        emailMethod(existingCompany, "same@email.com");
        when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of(existingCompany));

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "E-POSTA", "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "ABC", "same@email.com", "Sakarya");
                }), CompanyType.EXTERNAL)
                .rows()
                .getFirst();

        assertEquals("WARNING", row.status());
        assertTrue(row.updateMessages().isEmpty());
        assertTrue(row.warnings().getFirst().contains("manuel kontrol"));
        assertEquals(null, existingCompany.getAddress());
    }

    @Test
    void previewsWarningForPreExistingCompanyWithPhoneAndMissingAddress() {
        Company existingCompany = company("ABC", CompanyType.EXTERNAL);
        phoneMethod(existingCompany, "555");
        when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of(existingCompany));

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "TELEFON", "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "ABC", "555", "Sakarya");
                }), CompanyType.EXTERNAL)
                .rows()
                .getFirst();

        assertEquals("WARNING", row.status());
        assertTrue(row.updateMessages().isEmpty());
        assertTrue(row.warnings().getFirst().contains("manuel kontrol"));
        assertEquals(null, existingCompany.getAddress());
    }

    @Test
    void keepsPreExistingLocationUnchangedForLaterNoEmailRowInPreview() {
        Company existingCompany = company("ABC", CompanyType.EXTERNAL);
        emailMethod(existingCompany, "same@email.com");
        phoneMethod(existingCompany, "555");
        when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of(existingCompany));

        CompanyImportPreviewResponse response = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "E-POSTA", "TELEFON",
                            "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "ABC", "same@email.com", " ",
                            "Sakarya");
                    writeRow(sheet, 2, "ABC", " ", "555", "Sakarya");
                }), CompanyType.EXTERNAL);

        assertEquals(List.of("WARNING", "WARNING"), response.rows()
                .stream()
                .map(CompanyImportPreviewRowResponse::status)
                .toList());
        assertEquals(null, existingCompany.getAddress());
    }

    @Test
    void previewsWarningWhenMultipleExternalCompaniesMatch() {
        Company first = company("ABC", CompanyType.EXTERNAL);
        Company second = company("ABC", CompanyType.EXTERNAL);
        emailMethod(first, "shared@email.com");
        emailMethod(second, "shared@email.com");
        when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of(first, second));

        CompanyImportPreviewRowResponse row = previewService
                .preview(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FIRMA", "E-POSTA", "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "ABC", "shared@email.com", "Sakarya");
                }), CompanyType.EXTERNAL)
                .rows()
                .getFirst();

        assertEquals("WARNING", row.status());
        assertTrue(row.warnings().getFirst()
                .contains("birden fazla mevcut dis sirketle"));
        assertEquals(null, first.getAddress());
        assertEquals(null, second.getAddress());
    }

    private Company company(String name, CompanyType companyType) {
        Company company = new Company();
        company.setName(name);
        company.setCompanyType(companyType);
        return company;
    }

    private CompanyContactMethod emailMethod(Company company, String email) {
        CompanyContactMethod contactMethod = new CompanyContactMethod();
        contactMethod.setType(CompanyContactMethodType.EMAIL);
        contactMethod.setValue(email);
        company.addContactMethod(contactMethod);
        return contactMethod;
    }

    private CompanyContactMethod phoneMethod(Company company, String phone) {
        CompanyContactMethod contactMethod = new CompanyContactMethod();
        contactMethod.setType(CompanyContactMethodType.PHONE);
        contactMethod.setValue(phone);
        company.addContactMethod(contactMethod);
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
