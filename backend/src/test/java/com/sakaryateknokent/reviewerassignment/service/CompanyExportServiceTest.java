package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContact;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactMethodRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyExportServiceTest {

    private final CompanyRepository companyRepository =
            mock(CompanyRepository.class);
    private final CompanyContactRepository companyContactRepository =
            mock(CompanyContactRepository.class);
    private final CompanyContactMethodRepository contactMethodRepository =
            mock(CompanyContactMethodRepository.class);
    private final CompanyExportService exportService =
            new CompanyExportService(
                    companyRepository,
                    companyContactRepository,
                    contactMethodRepository
            );

    @Test
    void exportsOnlyTgbCompaniesWithSearchInitialAndUnpagedQuery() {
        Company company = company(1L, "A Yazilim", CompanyType.TGB);
        when(companyRepository.search(
                eq(CompanyType.TGB),
                eq("yazilim"),
                eq("A"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(company)));
        when(companyContactRepository.findByCompanyIdIn(List.of(1L)))
                .thenReturn(List.of());
        when(contactMethodRepository.findByCompanyIdIn(List.of(1L)))
                .thenReturn(List.of());

        Sheet sheet = sheet(exportService.exportToExcel(
                CompanyType.TGB,
                " yazilim ",
                " A "
        ));

        assertEquals("Firma", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("A Yazilim", sheet.getRow(1).getCell(0).getStringCellValue());
        assertEquals("TGB", sheet.getRow(1).getCell(1).getStringCellValue());
        assertEquals(8, sheet.getRow(0).getLastCellNum() - 1);
        assertEquals("Yetkili E-posta",
                sheet.getRow(0).getCell(8).getStringCellValue());

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(companyRepository).search(
                eq(CompanyType.TGB),
                eq("yazilim"),
                eq("A"),
                pageableCaptor.capture()
        );
        assertTrue(pageableCaptor.getValue().isUnpaged());
    }

    @Test
    void exportsOnlyExternalCompaniesWithDisplayName() {
        Company company = company(2L, "Dis Firma", CompanyType.EXTERNAL);
        when(companyRepository.search(
                eq(CompanyType.EXTERNAL),
                eq(""),
                eq(""),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(company)));
        when(companyContactRepository.findByCompanyIdIn(List.of(2L)))
                .thenReturn(List.of());
        when(contactMethodRepository.findByCompanyIdIn(List.of(2L)))
                .thenReturn(List.of());

        Sheet sheet = sheet(exportService.exportToExcel(
                CompanyType.EXTERNAL,
                null,
                null
        ));

        assertEquals("Dis Firma", sheet.getRow(1).getCell(0).getStringCellValue());
        assertEquals(
                "D\u0131\u015F \u015Eirket",
                sheet.getRow(1).getCell(1).getStringCellValue()
        );
        verify(companyRepository).search(
                eq(CompanyType.EXTERNAL),
                eq(""),
                eq(""),
                any(Pageable.class)
        );
    }

    @Test
    void exportsHeaderOnlyWorkbookWhenFilterResultIsEmpty() {
        when(companyRepository.search(
                eq(CompanyType.TGB),
                eq("none"),
                eq("Z"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        Sheet sheet = sheet(exportService.exportToExcel(
                CompanyType.TGB,
                "none",
                "Z"
        ));

        assertEquals(0, sheet.getLastRowNum());
        assertEquals("Firma", sheet.getRow(0).getCell(0).getStringCellValue());
        verify(companyContactRepository, never()).findByCompanyIdIn(any());
        verify(contactMethodRepository, never()).findByCompanyIdIn(any());
    }

    @Test
    void exportsOnlySelectedCompaniesWithinCompanyTypeAndDeduplicatesIds() {
        Company selected = company(1L, "TGB Firma", CompanyType.TGB);
        when(companyRepository.findByCompanyTypeAndIdIn(
                eq(CompanyType.TGB),
                eq(new java.util.LinkedHashSet<>(List.of(1L, 9L))),
                any(org.springframework.data.domain.Sort.class)
        )).thenReturn(List.of(selected));
        when(companyContactRepository.findByCompanyIdIn(List.of(1L)))
                .thenReturn(List.of());
        when(contactMethodRepository.findByCompanyIdIn(List.of(1L)))
                .thenReturn(List.of());

        Sheet sheet = sheet(exportService.exportToExcel(
                CompanyType.TGB,
                List.of(1L, 9L, 1L)
        ));

        assertEquals("TGB Firma", sheet.getRow(1).getCell(0).getStringCellValue());
        assertEquals(1, sheet.getLastRowNum());
        verify(companyRepository).findByCompanyTypeAndIdIn(
                eq(CompanyType.TGB),
                eq(new java.util.LinkedHashSet<>(List.of(1L, 9L))),
                any(org.springframework.data.domain.Sort.class)
        );
    }

    @Test
    void selectedCompanyExportWithEmptyIdsWritesHeaderOnly() {
        Sheet sheet = sheet(exportService.exportToExcel(
                CompanyType.TGB,
                List.of()
        ));

        assertEquals(0, sheet.getLastRowNum());
        assertEquals("Firma", sheet.getRow(0).getCell(0).getStringCellValue());
        verify(companyRepository, never()).findByCompanyTypeAndIdIn(
                any(),
                any(),
                any(org.springframework.data.domain.Sort.class)
        );
        verify(companyContactRepository, never()).findByCompanyIdIn(any());
        verify(contactMethodRepository, never()).findByCompanyIdIn(any());
    }

    @Test
    void exportsContactMethodsAndContactsWithStableOrderingAndBlankSlots() {
        Company company = company(3L, "Delta", CompanyType.TGB);
        company.setEmail("legacy@example.com");
        company.setPhone("999");
        company.setSecondaryPhone("888");

        when(companyRepository.search(
                eq(CompanyType.TGB),
                eq(""),
                eq(""),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(company)));
        when(companyContactRepository.findByCompanyIdIn(List.of(3L)))
                .thenReturn(List.of(
                        contact(12L, company, false, "Ayse Kaya",
                                null, "ayse@example.com", "0544"),
                        contact(10L, company, true, "Ahmet Yilmaz",
                                "IK Muduru", "ahmet@example.com", "0532"),
                        contact(11L, company, false, "Can Demir",
                                "CTO", null, null)
                ));
        when(contactMethodRepository.findByCompanyIdIn(List.of(3L)))
                .thenReturn(List.of(
                        contactMethod(22L, company,
                                CompanyContactMethodType.PHONE, "0216", false),
                        contactMethod(20L, company,
                                CompanyContactMethodType.PHONE, "0264", true),
                        contactMethod(21L, company,
                                CompanyContactMethodType.PHONE, "0532", false),
                        contactMethod(32L, company,
                                CompanyContactMethodType.EMAIL,
                                "sales@example.com", false),
                        contactMethod(30L, company,
                                CompanyContactMethodType.EMAIL,
                                "info@example.com", true),
                        contactMethod(31L, company,
                                CompanyContactMethodType.EMAIL,
                                "contact@example.com", false)
                ));

        Sheet sheet = sheet(exportService.exportToExcel(
                CompanyType.TGB,
                "",
                ""
        ));

        Row row = sheet.getRow(1);
        assertEquals("Yazilim", row.getCell(2).getStringCellValue());
        assertEquals("0264\n0532\n0216", row.getCell(3).getStringCellValue());
        assertEquals(
                "info@example.com\ncontact@example.com\nsales@example.com",
                row.getCell(4).getStringCellValue()
        );
        assertEquals(
                "Ahmet Yilmaz\nCan Demir\nAyse Kaya",
                row.getCell(5).getStringCellValue()
        );
        assertEquals("IK Muduru\nCTO\n", row.getCell(6).getStringCellValue());
        assertEquals("0532\n\n0544", row.getCell(7).getStringCellValue());
        assertEquals(
                "ahmet@example.com\n\nayse@example.com",
                row.getCell(8).getStringCellValue()
        );
        assertTrue(row.getCell(3).getCellStyle().getWrapText());
        assertTrue(row.getCell(8).getCellStyle().getWrapText());
    }

    @Test
    void exportsAllPhoneRecordsInOneCellWithNewlines() {
        Company company = company(4L, "Telefonlu", CompanyType.TGB);

        Row row = exportRow(
                company,
                List.of(),
                List.of(
                        contactMethod(40L, company,
                                CompanyContactMethodType.PHONE, "111", true),
                        contactMethod(41L, company,
                                CompanyContactMethodType.PHONE, "222", false),
                        contactMethod(42L, company,
                                CompanyContactMethodType.PHONE, "333", false)
                )
        );

        assertEquals("111\n222\n333", row.getCell(3).getStringCellValue());
    }

    @Test
    void ordersPrimaryPhoneBeforeOtherPhonesById() {
        Company company = company(5L, "Telefon Sirali", CompanyType.TGB);

        Row row = exportRow(
                company,
                List.of(),
                List.of(
                        contactMethod(52L, company,
                                CompanyContactMethodType.PHONE, "ucuncu", false),
                        contactMethod(50L, company,
                                CompanyContactMethodType.PHONE, "birinci", false),
                        contactMethod(51L, company,
                                CompanyContactMethodType.PHONE, "ana", true)
                )
        );

        assertEquals("ana\nbirinci\nucuncu", row.getCell(3).getStringCellValue());
    }

    @Test
    void exportsAllEmailRecordsInOneCellWithNewlines() {
        Company company = company(6L, "Epostali", CompanyType.TGB);

        Row row = exportRow(
                company,
                List.of(),
                List.of(
                        contactMethod(60L, company,
                                CompanyContactMethodType.EMAIL,
                                "a@example.com", true),
                        contactMethod(61L, company,
                                CompanyContactMethodType.EMAIL,
                                "b@example.com", false)
                )
        );

        assertEquals(
                "a@example.com\nb@example.com",
                row.getCell(4).getStringCellValue()
        );
    }

    @Test
    void ordersPrimaryEmailBeforeOtherEmailsById() {
        Company company = company(7L, "Eposta Sirali", CompanyType.TGB);

        Row row = exportRow(
                company,
                List.of(),
                List.of(
                        contactMethod(72L, company,
                                CompanyContactMethodType.EMAIL,
                                "third@example.com", false),
                        contactMethod(70L, company,
                                CompanyContactMethodType.EMAIL,
                                "first@example.com", false),
                        contactMethod(71L, company,
                                CompanyContactMethodType.EMAIL,
                                "primary@example.com", true)
                )
        );

        assertEquals(
                "primary@example.com\nfirst@example.com\nthird@example.com",
                row.getCell(4).getStringCellValue()
        );
    }

    @Test
    void ordersContactsPrimaryFirstAndThenById() {
        Company company = company(8L, "Yetkili Sirali", CompanyType.TGB);

        Row row = exportRow(
                company,
                List.of(
                        contact(82L, company, false, "Ucuncu",
                                null, null, "3"),
                        contact(80L, company, false, "Birinci",
                                null, null, "1"),
                        contact(81L, company, true, "Ana",
                                null, null, "2")
                ),
                List.of()
        );

        assertEquals("Ana\nBirinci\nUcuncu", row.getCell(5).getStringCellValue());
        assertEquals("2\n1\n3", row.getCell(7).getStringCellValue());
    }

    @Test
    void contactColumnsAreGeneratedFromSameOrderedContactList() {
        Company company = company(9L, "Yetkili Hizali", CompanyType.TGB);

        Row row = exportRow(
                company,
                List.of(
                        contact(91L, company, true, "Ana",
                                "CEO", "ana@example.com", "1"),
                        contact(90L, company, false, "Diger",
                                "CTO", "diger@example.com", "2")
                ),
                List.of()
        );

        assertEquals("Ana\nDiger", row.getCell(5).getStringCellValue());
        assertEquals("CEO\nCTO", row.getCell(6).getStringCellValue());
        assertEquals("1\n2", row.getCell(7).getStringCellValue());
        assertEquals(
                "ana@example.com\ndiger@example.com",
                row.getCell(8).getStringCellValue()
        );
    }

    @Test
    void preservesBlankSlotsForMissingContactFields() {
        Company company = company(10L, "Eksik Yetkili", CompanyType.TGB);

        Row row = exportRow(
                company,
                List.of(
                        contact(100L, company, true, "Ahmet",
                                "Mudur", "ahmet@example.com", "1"),
                        contact(101L, company, false, "Ayse",
                                null, null, "2")
                ),
                List.of()
        );

        assertEquals("Ahmet\nAyse", row.getCell(5).getStringCellValue());
        assertEquals("Mudur\n", row.getCell(6).getStringCellValue());
        assertEquals("1\n2", row.getCell(7).getStringCellValue());
        assertEquals("ahmet@example.com\n", row.getCell(8).getStringCellValue());
    }

    @Test
    void doesNotUseLegacyCompanyPhoneOrEmailFieldsAsExportSource() {
        Company company = company(11L, "Legacy", CompanyType.TGB);
        company.setPhone("legacy-phone");
        company.setSecondaryPhone("legacy-secondary-phone");
        company.setEmail("legacy@example.com");

        Row row = exportRow(company, List.of(), List.of());

        assertEquals("", row.getCell(3).getStringCellValue());
        assertEquals("", row.getCell(4).getStringCellValue());
    }

    @Test
    void appliesWrapTextToMultiLineExportCells() {
        Company company = company(12L, "Wrap", CompanyType.TGB);

        Row row = exportRow(
                company,
                List.of(contact(120L, company, true, "Yetkili",
                        "Mudur", "mail@example.com", "555")),
                List.of(contactMethod(120L, company,
                        CompanyContactMethodType.PHONE, "111", true))
        );

        assertTrue(row.getCell(3).getCellStyle().getWrapText());
        assertTrue(row.getCell(4).getCellStyle().getWrapText());
        assertTrue(row.getCell(5).getCellStyle().getWrapText());
        assertTrue(row.getCell(6).getCellStyle().getWrapText());
        assertTrue(row.getCell(7).getCellStyle().getWrapText());
        assertTrue(row.getCell(8).getCellStyle().getWrapText());
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

    private Row exportRow(
            Company company,
            List<CompanyContact> contacts,
            List<CompanyContactMethod> contactMethods
    ) {
        when(companyRepository.search(
                eq(company.getCompanyType()),
                eq(""),
                eq(""),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(company)));
        when(companyContactRepository.findByCompanyIdIn(List.of(company.getId())))
                .thenReturn(contacts);
        when(contactMethodRepository.findByCompanyIdIn(List.of(company.getId())))
                .thenReturn(contactMethods);

        return sheet(exportService.exportToExcel(
                company.getCompanyType(),
                "",
                ""
        )).getRow(1);
    }

    private Company company(Long id, String name, CompanyType companyType) {
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        company.setActivityArea("Yazilim");
        company.setCompanyType(companyType);
        return company;
    }

    private CompanyContact contact(
            Long id,
            Company company,
            boolean primary,
            String fullName,
            String position,
            String email,
            String phone
    ) {
        CompanyContact contact = new CompanyContact();
        contact.setId(id);
        contact.setCompany(company);
        contact.setPrimary(primary);
        contact.setFullName(fullName);
        contact.setPosition(position);
        contact.setEmail(email);
        contact.setPhone(phone);
        return contact;
    }

    private CompanyContactMethod contactMethod(
            Long id,
            Company company,
            CompanyContactMethodType type,
            String value,
            boolean primary
    ) {
        CompanyContactMethod contactMethod = new CompanyContactMethod();
        contactMethod.setId(id);
        contactMethod.setCompany(company);
        contactMethod.setType(type);
        contactMethod.setValue(value);
        contactMethod.setPrimary(primary);
        return contactMethod;
    }
}
