package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyImportResultResponse;
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
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyImportServiceTest {

    private final CompanyRepository companyRepository =
            mock(CompanyRepository.class);
    private final CompanyContactMethodRepository companyContactMethodRepository =
            mock(CompanyContactMethodRepository.class);
    private final CompanyImportService companyImportService =
            new CompanyImportService(
                    new CompanyExcelImportParser(),
                    companyRepository,
                    companyContactMethodRepository,
                    new CompanyLocationMergeClassifier()
            );

    @BeforeEach
    void setUp() {
        lenient().when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of());
        lenient().when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of());
        lenient().when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void importsValidRowAsTgbCompany() {
        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(
                            sheet,
                            0,
                            "Firma Unvan\u0131",
                            "Telefon 1",
                            "Telefon 2",
                            "Email",
                            "Sektor",
                            "\u0130L",
                            "A\u00C7IK ADRES",
                            "B\u0130NA"
                    );
                    writeRow(
                            sheet,
                            1,
                            "ABC Teknoloji",
                            "111",
                            "222",
                            "INFO@EXAMPLE.COM",
                            "Yazilim",
                            "Sakarya",
                            "Teknokent Kampusu",
                            "A Blok"
                    );
                }));

        Company savedCompany = savedCompanies().getFirst();
        assertEquals(1, response.importedCount());
        assertEquals(0, response.duplicateCount());
        assertEquals(0, response.invalidCount());
        assertEquals("IMPORTED", response.rows().getFirst().status());
        assertEquals(CompanyType.TGB, savedCompany.getCompanyType());
        assertEquals("ABC Teknoloji", savedCompany.getName());
        assertEquals(List.of("info@example.com"), companyEmails(savedCompany));
        assertEquals(List.of("111", "222"), companyPhones(savedCompany));
        assertEquals("Yazilim", savedCompany.getActivityArea());
        assertEquals("Sakarya", savedCompany.getCity());
        assertEquals("Teknokent Kampusu", savedCompany.getAddress());
        assertEquals("A Blok", savedCompany.getBuilding());
    }

    @Test
    void importsMergedExcelRowAsTgbCompany() {
        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    mergeWithValue(sheet, 0, 3, 0, 4, "Firma Unvan\u0131");
                    mergeWithValue(sheet, 0, 5, 0, 6, "Sektor");
                    mergeWithValue(sheet, 0, 12, 0, 13, "Telefon 1");
                    mergeWithValue(sheet, 0, 14, 0, 15, "Telefon 2");
                    mergeWithValue(sheet, 0, 16, 0, 18, "Email");
                    mergeWithValue(sheet, 1, 2, 1, 3, "Anonim Teknoloji");
                    mergeWithValue(sheet, 1, 4, 1, 5, "Yazilim");
                    mergeWithValue(sheet, 1, 11, 1, 12, "111");
                    mergeWithValue(sheet, 1, 13, 1, 14, "222");
                    mergeWithValue(sheet, 1, 16, 1, 17, "INFO@EXAMPLE.COM");
                }));

        Company savedCompany = savedCompanies().getFirst();
        assertEquals(1, response.importedCount());
        assertEquals("Anonim Teknoloji", savedCompany.getName());
        assertEquals("Yazilim", savedCompany.getActivityArea());
        assertEquals(List.of("111", "222"), companyPhones(savedCompany));
        assertEquals(List.of("info@example.com"), companyEmails(savedCompany));
    }

    @Test
    void storesOptionalBlankFieldsAsNull() {
        companyImportService.importTgbCompanies(file(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(
                    sheet,
                    0,
                    "Firma Unvan\u0131",
                    "Telefon 1",
                    "Telefon 2",
                    "Email",
                    "Faaliyet Alani",
                    "\u0130L",
                    "\u0130L\u00C7E",
                    "OSB",
                    "A\u00C7IK ADRES",
                    "B\u0130NA"
            );
            writeRow(sheet, 1, "ABC Teknoloji", " ", " ", " ", " ",
                    " ", "\t", "", "  ", " ");
        }));

        Company savedCompany = savedCompanies().getFirst();
        assertTrue(savedCompany.getContactMethods().isEmpty());
        assertNull(savedCompany.getActivityArea());
        assertNull(savedCompany.getCity());
        assertNull(savedCompany.getDistrict());
        assertNull(savedCompany.getOsb());
        assertNull(savedCompany.getAddress());
        assertNull(savedCompany.getBuilding());
    }

    @Test
    void updatesExistingTgbMissingBuildingFromDuplicateName() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", "A Blok");
                }));

        assertEquals(0, response.importedCount());
        assertEquals(1, response.updatedCount());
        assertEquals(0, response.duplicateCount());
        assertEquals("UPDATED", response.rows().getFirst().status());
        assertEquals("A Blok", existingCompany.getBuilding());
        verify(companyRepository).saveAndFlush(existingCompany);
    }

    @Test
    void updatesExistingExternalMissingAddressFromDuplicateName() {
        Company existingCompany = company("Dis Teknoloji", CompanyType.EXTERNAL);
        when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of(existingCompany));

        CompanyImportResultResponse response = companyImportService
                .importExternalCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "F\u0130RMA", "A\u00C7IK ADRES");
                    writeRow(sheet, 1, "Dis Teknoloji", "Baris Mahallesi");
                }));

        assertEquals(1, response.updatedCount());
        assertEquals("UPDATED", response.rows().getFirst().status());
        assertEquals("Baris Mahallesi", existingCompany.getAddress());
    }

    @Test
    void keepsExistingSameBuildingAsDuplicateWithoutOverwrite() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        existingCompany.setBuilding("A Blok");
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", " A Blok ");
                }));

        assertEquals(0, response.updatedCount());
        assertEquals(1, response.duplicateCount());
        assertEquals("DUPLICATE", response.rows().getFirst().status());
        assertEquals("A Blok", existingCompany.getBuilding());
        verify(companyRepository, never()).saveAndFlush(existingCompany);
    }

    @Test
    void keepsExistingBuildingWhenExcelHasConflictingValue() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        existingCompany.setBuilding("A Blok");
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", "B Blok");
                }));

        assertEquals(0, response.updatedCount());
        assertEquals(1, response.duplicateCount());
        assertEquals("A Blok", existingCompany.getBuilding());
        assertTrue(response.rows().getFirst().messages().getFirst()
                .contains("Bina bilgisi"));
        verify(companyRepository, never()).saveAndFlush(existingCompany);
    }

    @Test
    void updatesMissingCityAndKeepsConflictingBuilding() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        existingCompany.setBuilding("A Blok");
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "\u0130L", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", "Sakarya", "B Blok");
                }));

        assertEquals(1, response.updatedCount());
        assertEquals("UPDATED", response.rows().getFirst().status());
        assertEquals("Sakarya", existingCompany.getCity());
        assertEquals("A Blok", existingCompany.getBuilding());
        assertTrue(response.rows().getFirst().messages().getFirst()
                .contains("Bina bilgisi"));
    }

    @Test
    void blankExcelLocationDoesNotClearExistingValue() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        existingCompany.setBuilding("A Blok");
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", " ");
                }));

        assertEquals(1, response.duplicateCount());
        assertEquals("A Blok", existingCompany.getBuilding());
        verify(companyRepository, never()).saveAndFlush(existingCompany);
    }

    @Test
    void fillsMultipleMissingLocationFieldsWithoutChangingOtherCompanyData() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        existingCompany.setActivityArea("Eski Faaliyet");
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        companyImportService.importTgbCompanies(file(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(
                    sheet,
                    0,
                    "Firma Unvan\u0131",
                    "Faaliyet Alani",
                    "Telefon 1",
                    "\u0130L",
                    "\u0130L\u00C7E",
                    "OSB",
                    "A\u00C7IK ADRES",
                    "B\u0130NA"
            );
            writeRow(
                    sheet,
                    1,
                    "ABC Teknoloji",
                    "Yeni Faaliyet",
                    "555",
                    "Sakarya",
                    "Serdivan",
                    "1. OSB",
                    "Adres",
                    "A Blok"
            );
        }));

        assertEquals("Sakarya", existingCompany.getCity());
        assertEquals("Serdivan", existingCompany.getDistrict());
        assertEquals("1. OSB", existingCompany.getOsb());
        assertEquals("Adres", existingCompany.getAddress());
        assertEquals("A Blok", existingCompany.getBuilding());
        assertEquals("Eski Faaliyet", existingCompany.getActivityArea());
        assertTrue(existingCompany.getContactMethods().isEmpty());
    }

    @Test
    void reportsAllLocationConflictsWithoutOverwriting() {
        Company existingCompany = company("ABC Teknoloji", CompanyType.TGB);
        existingCompany.setCity("Kocaeli");
        existingCompany.setBuilding("A Blok");
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(existingCompany));

        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "\u0130L", "B\u0130NA");
                    writeRow(sheet, 1, "ABC Teknoloji", "Sakarya", "B Blok");
                }));

        assertEquals(2, response.rows().getFirst().messages().size());
        assertEquals("Kocaeli", existingCompany.getCity());
        assertEquals("A Blok", existingCompany.getBuilding());
    }

    @Test
    void importsWithoutCreatingContacts() {
        companyImportService.importTgbCompanies(file(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvan\u0131");
            writeRow(sheet, 1, "ABC Teknoloji");
        }));

        assertTrue(savedCompanies().getFirst().getContacts().isEmpty());
    }

    @Test
    void doesNotSaveValidationInvalidRow() {
        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Telefon 1");
                    writeRow(sheet, 1, "", "111");
                }));

        assertEquals(0, response.importedCount());
        assertEquals(1, response.invalidCount());
        assertEquals("INVALID", response.rows().getFirst().status());
        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    void marksExistingTgbEmailAsDuplicate() {
        when(companyContactMethodRepository.findByTypeAndValueIgnoreCaseOrderByIdAsc(
                CompanyContactMethodType.EMAIL,
                "info@example.com"
        )).thenReturn(List.of(emailMethod(
                company("Existing", CompanyType.TGB),
                "info@example.com"
        )));

        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email");
                    writeRow(sheet, 1, "ABC Teknoloji", "INFO@EXAMPLE.COM");
                }));

        assertDuplicate(response, "Bu e-posta adresine sahip bir sirket");
    }

    @Test
    void marksExternalEmailAsDuplicateBecauseEmailIsGloballyUnique() {
        when(companyContactMethodRepository.findByTypeAndValueIgnoreCaseOrderByIdAsc(
                CompanyContactMethodType.EMAIL,
                "info@example.com"
        ))
                .thenReturn(List.of(emailMethod(
                        company("Existing", CompanyType.EXTERNAL),
                        "info@example.com"
                )));

        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email");
                    writeRow(sheet, 1, "ABC Teknoloji", "INFO@EXAMPLE.COM");
                }));

        assertDuplicate(response, "Bu e-posta adresine sahip bir sirket");
    }

    @Test
    void marksNoEmailExistingTgbNameAsDuplicate() {
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(company(
                        "  sakarya   teknoloji a.\u015f. ",
                        CompanyType.TGB
                )));

        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131");
                    writeRow(sheet, 1, "Sakarya Teknoloji A.\u015e.");
                }));

        assertDuplicate(response, "Bu sirket adiyla bir TGB sirketi");
    }

    @Test
    void allowsNoEmailTgbImportWhenOnlyExternalCompanyHasSameName() {
        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131");
                    writeRow(sheet, 1, "ABC Teknoloji");
                }));

        assertEquals(1, response.importedCount());
        assertEquals(0, response.duplicateCount());
    }

    @Test
    void allowsRowsWithSameNameWhenEmailsAreDifferent() {
        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email");
                    writeRow(sheet, 1, "ABC Teknoloji", "a@example.com");
                    writeRow(sheet, 2, "ABC Teknoloji", "b@example.com");
                }));

        assertEquals(2, response.importedCount());
        verify(companyRepository, times(2)).saveAndFlush(any(Company.class));
    }

    @Test
    void marksBothFileDuplicateEmailsAsDuplicate() {
        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email");
                    writeRow(sheet, 1, "ABC Teknoloji", "INFO@EXAMPLE.COM");
                    writeRow(sheet, 2, "XYZ Teknoloji", "info@example.com");
                }));

        assertEquals(0, response.importedCount());
        assertEquals(2, response.duplicateCount());
        assertEquals(List.of("DUPLICATE", "DUPLICATE"),
                response.rows().stream().map(row -> row.status()).toList());
        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    void ignoresInvalidRowsWhenClassifyingFileDuplicateEmails() {
        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email");
                    writeRow(sheet, 1, "", "info@example.com");
                    writeRow(sheet, 2, "ABC Teknoloji", "INFO@EXAMPLE.COM");
                }));

        assertEquals(1, response.importedCount());
        assertEquals(0, response.duplicateCount());
        assertEquals(1, response.invalidCount());
        assertEquals(List.of("INVALID", "IMPORTED"),
                response.rows().stream().map(row -> row.status()).toList());
        assertTrue(response.rows().getFirst().messages().contains(
                "Firma unvani zorunludur"
        ));
        assertEquals(
                List.of("info@example.com"),
                companyEmails(savedCompanies().getFirst())
        );
    }

    @Test
    void marksBothFileDuplicateNoEmailNamesAsDuplicate() {
        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131");
                    writeRow(sheet, 1, "  Sakarya   Teknoloji A.\u015e.  ");
                    writeRow(sheet, 2, "sakarya teknoloji a.\u015f.");
                }));

        assertEquals(0, response.importedCount());
        assertEquals(2, response.duplicateCount());
        assertTrue(response.rows().stream().allMatch(
                row -> row.messages().getFirst().contains("ayni sirket adi")
        ));
        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    void invalidOrDuplicateRowsDoNotBlockOtherValidRowsAndCountsAreCorrect() {
        when(companyContactMethodRepository.findByTypeAndValueIgnoreCaseOrderByIdAsc(
                CompanyContactMethodType.EMAIL,
                "duplicate@example.com"
        ))
                .thenReturn(List.of(emailMethod(
                        company("Duplicate", CompanyType.TGB),
                        "duplicate@example.com"
                )));

        CompanyImportResultResponse response = companyImportService
                .importTgbCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Sirketler");
                    writeRow(sheet, 0, "Firma Unvan\u0131", "Email");
                    writeRow(sheet, 1, "ABC Teknoloji", "valid@example.com");
                    writeRow(sheet, 2, "", "invalid@example.com");
                    writeRow(sheet, 3, "XYZ Teknoloji", "duplicate@example.com");
                }));

        assertEquals(3, response.totalRowCount());
        assertEquals(1, response.importedCount());
        assertEquals(1, response.duplicateCount());
        assertEquals(1, response.invalidCount());
        verify(companyRepository, times(1)).saveAndFlush(any(Company.class));
    }

    @Test
    void alwaysAssignsTgbCompanyTypeDuringImport() {
        companyImportService.importTgbCompanies(file(workbook -> {
            Sheet sheet = workbook.createSheet("Sirketler");
            writeRow(sheet, 0, "Firma Unvan\u0131");
            writeRow(sheet, 1, "ABC Teknoloji");
        }));

        assertEquals(CompanyType.TGB, savedCompanies().getFirst().getCompanyType());
    }

    @Test
    void importsExternalCompanyWithContactAsPrimary() {
        CompanyImportResultResponse response = companyImportService
                .importExternalCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(
                            sheet,
                            0,
                            "FİRMA",
                            "TELEFON",
                            "E-POSTA",
                            "FAALİYET ALANI",
                            "\u0130L",
                            "\u0130L\u00C7E",
                            "OSB",
                            "A\u00C7IK ADRES",
                            "B\u0130NA",
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
                            "Makine",
                            "Kocaeli",
                            "Gebze",
                            "GOSB",
                            "Baris Mahallesi",
                            "C Blok",
                            "Ayse Yilmaz",
                            "Mudur",
                            "222",
                            "AYSE@EXAMPLE.COM"
                    );
                }));

        Company savedCompany = savedCompanies().getFirst();
        assertEquals(1, response.importedCount());
        assertEquals(CompanyType.EXTERNAL, savedCompany.getCompanyType());
        assertEquals("Dis Teknoloji", savedCompany.getName());
        assertEquals(List.of("111"), companyPhones(savedCompany));
        assertEquals(List.of("info@example.com"), companyEmails(savedCompany));
        assertEquals("Makine", savedCompany.getActivityArea());
        assertEquals("Kocaeli", savedCompany.getCity());
        assertEquals("Gebze", savedCompany.getDistrict());
        assertEquals("GOSB", savedCompany.getOsb());
        assertEquals("Baris Mahallesi", savedCompany.getAddress());
        assertEquals("C Blok", savedCompany.getBuilding());
        assertEquals(1, savedCompany.getContacts().size());
        assertEquals("Ayse Yilmaz", savedCompany.getContacts().getFirst().getFullName());
        assertEquals("Mudur", savedCompany.getContacts().getFirst().getPosition());
        assertEquals("222", savedCompany.getContacts().getFirst().getPhone());
        assertEquals("ayse@example.com", savedCompany.getContacts().getFirst().getEmail());
        assertTrue(savedCompany.getContacts().getFirst().isPrimary());
    }

    @Test
    void importsExternalCompanyWithLongActivityArea() {
        String activityArea = repeated("A", 1500);

        CompanyImportResultResponse response = companyImportService
                .importExternalCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FİRMA", "FAALİYET ALANI");
                    writeRow(sheet, 1, "Dis Teknoloji", activityArea);
                }));

        Company savedCompany = savedCompanies().getFirst();
        assertEquals(1, response.importedCount());
        assertEquals(activityArea, savedCompany.getActivityArea());
    }

    @Test
    void importsExternalCompanyWithoutContactWhenContactNameIsBlank() {
        CompanyImportResultResponse response = companyImportService
                .importExternalCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FİRMA", "YETKİLİ TELEFON");
                    writeRow(sheet, 1, "Dis Teknoloji", "222");
                }));

        Company savedCompany = savedCompanies().getFirst();
        assertEquals(1, response.importedCount());
        assertTrue(savedCompany.getContacts().isEmpty());
        assertEquals(1, response.rows().getFirst().messages().size());
    }

    @Test
    void importsExternalCompanyWithoutContactOrWarningWhenContactFieldsArePlaceholders() {
        CompanyImportResultResponse response = companyImportService
                .importExternalCompanies(file(workbook -> {
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
                }));

        Company savedCompany = savedCompanies().getFirst();
        assertEquals(1, response.importedCount());
        assertTrue(savedCompany.getContacts().isEmpty());
        assertTrue(response.rows().getFirst().messages().isEmpty());
    }

    @Test
    void marksExternalEmailDuplicateGlobally() {
        when(companyContactMethodRepository.findByTypeAndValueIgnoreCaseOrderByIdAsc(
                CompanyContactMethodType.EMAIL,
                "info@example.com"
        ))
                .thenReturn(List.of(emailMethod(
                        company("Existing", CompanyType.EXTERNAL),
                        "info@example.com"
                )));

        CompanyImportResultResponse response = companyImportService
                .importExternalCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FİRMA", "E-POSTA");
                    writeRow(sheet, 1, "Dis Teknoloji", "INFO@EXAMPLE.COM");
                }));

        assertDuplicate(response, "Bu e-posta adresine sahip bir sirket");
    }

    @Test
    void marksNoEmailExternalNameDuplicateOnlyAgainstExternalCompanies() {
        when(companyRepository.findByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(List.of(company("dis teknoloji", CompanyType.EXTERNAL)));

        CompanyImportResultResponse response = companyImportService
                .importExternalCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FİRMA");
                    writeRow(sheet, 1, "Dis Teknoloji");
                }));

        assertDuplicate(response, "Bu sirket adiyla bir dis sirket");
    }

    @Test
    void allowsNoEmailExternalNameWhenOnlyTgbCompanyHasSameName() {
        when(companyRepository.findByCompanyType(CompanyType.TGB))
                .thenReturn(List.of(company("Dis Teknoloji", CompanyType.TGB)));

        CompanyImportResultResponse response = companyImportService
                .importExternalCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FİRMA");
                    writeRow(sheet, 1, "Dis Teknoloji");
                }));

        assertEquals(1, response.importedCount());
        assertEquals(0, response.duplicateCount());
    }

    @Test
    void marksExternalFileDuplicatesUsingEmailOrNoEmailNameRules() {
        CompanyImportResultResponse response = companyImportService
                .importExternalCompanies(file(workbook -> {
                    Sheet sheet = workbook.createSheet("Dis Sirketler");
                    writeRow(sheet, 0, "FİRMA", "E-POSTA");
                    writeRow(sheet, 1, "Bir", "INFO@EXAMPLE.COM");
                    writeRow(sheet, 2, "Iki", "info@example.com");
                    writeRow(sheet, 3, "Uc", " ");
                    writeRow(sheet, 4, " uc ", " ");
                }));

        assertEquals(4, response.duplicateCount());
        assertTrue(response.rows().stream().allMatch(
                row -> row.status().equals("DUPLICATE")
        ));
    }

    private void assertDuplicate(
            CompanyImportResultResponse response,
            String expectedMessagePart
    ) {
        assertEquals(0, response.importedCount());
        assertEquals(1, response.duplicateCount());
        assertEquals("DUPLICATE", response.rows().getFirst().status());
        assertTrue(response.rows().getFirst().messages().getFirst()
                .contains(expectedMessagePart));
        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    private List<Company> savedCompanies() {
        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository, atLeastOnce()).saveAndFlush(captor.capture());
        return captor.getAllValues();
    }

    private List<String> companyPhones(Company company) {
        return company.getContactMethods()
                .stream()
                .filter(contactMethod ->
                        contactMethod.getType() == CompanyContactMethodType.PHONE)
                .map(contactMethod -> contactMethod.getValue())
                .toList();
    }

    private List<String> companyEmails(Company company) {
        return company.getContactMethods()
                .stream()
                .filter(contactMethod ->
                        contactMethod.getType() == CompanyContactMethodType.EMAIL)
                .map(contactMethod -> contactMethod.getValue())
                .toList();
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
