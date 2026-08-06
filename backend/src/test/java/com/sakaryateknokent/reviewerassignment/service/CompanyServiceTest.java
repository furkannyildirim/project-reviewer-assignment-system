package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyContactMethodRequest;
import com.sakaryateknokent.reviewerassignment.dto.CompanyListResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyResponse;
import com.sakaryateknokent.reviewerassignment.dto.CreateCompanyContactRequest;
import com.sakaryateknokent.reviewerassignment.dto.CreateCompanyRequest;
import com.sakaryateknokent.reviewerassignment.dto.PageResponse;
import com.sakaryateknokent.reviewerassignment.dto.UpdateCompanyContactRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateCompanyRequest;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContact;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactMethodRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    private final CompanyRepository companyRepository =
            mock(CompanyRepository.class);
    private final CompanyContactRepository companyContactRepository =
            mock(CompanyContactRepository.class);
    private final CompanyContactMethodRepository contactMethodRepository =
            mock(CompanyContactMethodRepository.class);
    private final CompanyService companyService = new CompanyService(
            companyRepository,
            companyContactRepository,
            contactMethodRepository
    );
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createsCompanyWithoutContactMethods() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponse response = companyService.createCompany(
                createRequest(CompanyType.TGB, List.of())
        );

        Company savedCompany = savedCompany();
        assertTrue(savedCompany.getContactMethods().isEmpty());
        assertTrue(response.contactMethods().isEmpty());
        assertEquals("contact@example.com",
                savedCompany.getContacts().getFirst().getEmail());
    }

    @Test
    void createsCompanyWithLocationFieldsAndReturnsThem() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponse response = companyService.createCompany(
                createRequest(
                        CompanyType.TGB,
                        List.of(),
                        "Yazilim",
                        " Sakarya ",
                        " Serdivan ",
                        " Sakarya 1. OSB ",
                        " 1. Cadde No: 10 ",
                        " A Blok "
                )
        );

        Company savedCompany = savedCompany();
        assertEquals("Sakarya", savedCompany.getCity());
        assertEquals("Serdivan", savedCompany.getDistrict());
        assertEquals("Sakarya 1. OSB", savedCompany.getOsb());
        assertEquals("1. Cadde No: 10", savedCompany.getAddress());
        assertEquals("A Blok", savedCompany.getBuilding());
        assertEquals("Sakarya", response.city());
        assertEquals("Serdivan", response.district());
        assertEquals("Sakarya 1. OSB", response.osb());
        assertEquals("1. Cadde No: 10", response.address());
        assertEquals("A Blok", response.building());
    }

    @Test
    void createsCompanyWhenLocationFieldsAreNull() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponse response = companyService.createCompany(
                createRequest(CompanyType.EXTERNAL, List.of())
        );

        Company savedCompany = savedCompany();
        assertNull(savedCompany.getCity());
        assertNull(savedCompany.getDistrict());
        assertNull(savedCompany.getOsb());
        assertNull(savedCompany.getAddress());
        assertNull(savedCompany.getBuilding());
        assertNull(response.city());
        assertNull(response.district());
        assertNull(response.osb());
        assertNull(response.address());
        assertNull(response.building());
    }

    @Test
    void normalizesBlankLocationFieldsToNullWithoutChangingCase() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        companyService.createCompany(createRequest(
                CompanyType.TGB,
                List.of(),
                "Yazilim",
                "  ",
                "\t",
                "",
                "  ",
                " B Blok "
        ));

        Company savedCompany = savedCompany();
        assertNull(savedCompany.getCity());
        assertNull(savedCompany.getDistrict());
        assertNull(savedCompany.getOsb());
        assertNull(savedCompany.getAddress());
        assertEquals("B Blok", savedCompany.getBuilding());
    }

    @Test
    void createsCompanyWithSinglePhone() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        companyService.createCompany(createRequest(
                CompanyType.TGB,
                List.of(method(null, CompanyContactMethodType.PHONE, " 111 ", null, false))
        ));

        CompanyContactMethod savedMethod =
                savedCompany().getContactMethods().getFirst();
        assertEquals(CompanyContactMethodType.PHONE, savedMethod.getType());
        assertEquals("111", savedMethod.getValue());
        assertTrue(savedMethod.isPrimary());
    }

    @Test
    void createsCompanyWithMultiplePhones() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        companyService.createCompany(createRequest(
                CompanyType.TGB,
                List.of(
                        method(null, CompanyContactMethodType.PHONE, "111", "Santral", true),
                        method(null, CompanyContactMethodType.PHONE, "222", null, false)
                )
        ));

        List<CompanyContactMethod> savedMethods =
                savedCompany().getContactMethods();
        assertEquals(2, savedMethods.size());
        assertEquals("Santral", savedMethods.getFirst().getLabel());
        assertTrue(savedMethods.getFirst().isPrimary());
        assertFalse(savedMethods.get(1).isPrimary());
    }

    @Test
    void createsCompanyWithSingleEmailAndNormalizesLowercase() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        companyService.createCompany(createRequest(
                CompanyType.TGB,
                List.of(method(null, CompanyContactMethodType.EMAIL, " INFO@EXAMPLE.COM ", null, false))
        ));

        CompanyContactMethod savedMethod =
                savedCompany().getContactMethods().getFirst();
        assertEquals(CompanyContactMethodType.EMAIL, savedMethod.getType());
        assertEquals("info@example.com", savedMethod.getValue());
        assertTrue(savedMethod.isPrimary());
    }

    @Test
    void createsCompanyWithMultipleEmails() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        companyService.createCompany(createRequest(
                CompanyType.TGB,
                List.of(
                        method(null, CompanyContactMethodType.EMAIL, "a@example.com", null, true),
                        method(null, CompanyContactMethodType.EMAIL, "b@example.com", null, false)
                )
        ));

        List<CompanyContactMethod> savedMethods =
                savedCompany().getContactMethods();
        assertEquals(2, savedMethods.size());
        assertTrue(savedMethods.getFirst().isPrimary());
    }

    @Test
    void supportsPrimaryPhoneAndPrimaryEmailAtSameTime() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        companyService.createCompany(createRequest(
                CompanyType.EXTERNAL,
                List.of(
                        method(null, CompanyContactMethodType.PHONE, "111", null, true),
                        method(null, CompanyContactMethodType.EMAIL, "info@example.com", null, true)
                )
        ));

        List<CompanyContactMethod> savedMethods =
                savedCompany().getContactMethods();
        assertTrue(savedMethods.stream()
                .filter(method -> CompanyContactMethodType.PHONE.equals(method.getType()))
                .findFirst()
                .orElseThrow()
                .isPrimary());
        assertTrue(savedMethods.stream()
                .filter(method -> CompanyContactMethodType.EMAIL.equals(method.getType()))
                .findFirst()
                .orElseThrow()
                .isPrimary());
    }

    @Test
    void rejectsTwoPrimaryPhones() {
        assertThrows(InvalidRequestException.class, () ->
                companyService.createCompany(createRequest(
                        CompanyType.TGB,
                        List.of(
                                method(null, CompanyContactMethodType.PHONE, "111", null, true),
                                method(null, CompanyContactMethodType.PHONE, "222", null, true)
                        )
                )));
    }

    @Test
    void rejectsTwoPrimaryEmails() {
        assertThrows(InvalidRequestException.class, () ->
                companyService.createCompany(createRequest(
                        CompanyType.TGB,
                        List.of(
                                method(null, CompanyContactMethodType.EMAIL, "a@example.com", null, true),
                                method(null, CompanyContactMethodType.EMAIL, "b@example.com", null, true)
                        )
                )));
    }

    @Test
    void defaultsFirstMethodOfEachTypeToPrimaryWhenNoneSelected() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        companyService.createCompany(createRequest(
                CompanyType.TGB,
                List.of(
                        method(null, CompanyContactMethodType.PHONE, "111", null, false),
                        method(null, CompanyContactMethodType.PHONE, "222", null, false),
                        method(null, CompanyContactMethodType.EMAIL, "a@example.com", null, false),
                        method(null, CompanyContactMethodType.EMAIL, "b@example.com", null, false)
                )
        ));

        List<CompanyContactMethod> savedMethods =
                savedCompany().getContactMethods();
        assertTrue(savedMethods.get(0).isPrimary());
        assertFalse(savedMethods.get(1).isPrimary());
        assertTrue(savedMethods.get(2).isPrimary());
        assertFalse(savedMethods.get(3).isPrimary());
    }

    @Test
    void rejectsGlobalCaseInsensitiveEmailDuplicateOnCreate() {
        when(contactMethodRepository.existsByTypeAndValueIgnoreCase(
                CompanyContactMethodType.EMAIL,
                "duplicate@example.com"
        )).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                companyService.createCompany(createRequest(
                        CompanyType.TGB,
                        List.of(method(null, CompanyContactMethodType.EMAIL,
                                "DUPLICATE@EXAMPLE.COM", null, true))
                )));
    }

    @Test
    void updatesExistingMethodDeletesMissingMethodAndAddsNewMethod() {
        Company company = company(1L, "Eski Sirket", CompanyType.EXTERNAL);
        company.setCity("Eski Il");
        company.setDistrict("Eski Ilce");
        company.setOsb("Eski OSB");
        company.setAddress("Eski Adres");
        company.setBuilding("Eski Bina");
        CompanyContact contact = contact(10L, company, true);
        company.addContact(contact);
        company.addContactMethod(contactMethod(
                100L,
                company,
                CompanyContactMethodType.PHONE,
                "111",
                true
        ));
        company.addContactMethod(contactMethod(
                101L,
                company,
                CompanyContactMethodType.EMAIL,
                "old@example.com",
                true
        ));

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        CompanyResponse response = companyService.updateCompany(
                1L,
                updateRequest(
                        List.of(
                                method(100L, CompanyContactMethodType.PHONE,
                                        "222", "Yeni", false),
                                method(null, CompanyContactMethodType.EMAIL,
                                        "new@example.com", null, false)
                        )
                )
        );

        assertEquals("Yeni Sirket", company.getName());
        assertEquals("Kocaeli", company.getCity());
        assertEquals("Gebze", company.getDistrict());
        assertEquals("GOSB", company.getOsb());
        assertEquals("Yeni Adres", company.getAddress());
        assertEquals("Bina 2", company.getBuilding());
        assertEquals(2, company.getContactMethods().size());
        assertEquals("222", company.getContactMethods().getFirst().getValue());
        assertEquals("Yeni", company.getContactMethods().getFirst().getLabel());
        assertEquals("new@example.com",
                company.getContactMethods().get(1).getValue());
        assertEquals(2, response.contactMethods().size());
        assertEquals("Kocaeli", response.city());
        assertEquals("Gebze", response.district());
        assertEquals("GOSB", response.osb());
        assertEquals("Yeni Adres", response.address());
        assertEquals("Bina 2", response.building());
    }

    @Test
    void updateCanClearLocationFieldsWithBlankValues() {
        Company company = company(1L, "Eski Sirket", CompanyType.EXTERNAL);
        company.setCity("Kocaeli");
        company.setDistrict("Gebze");
        company.setOsb("GOSB");
        company.setAddress("Adres");
        company.setBuilding("Bina");
        CompanyContact contact = contact(10L, company, true);
        company.addContact(contact);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        CompanyResponse response = companyService.updateCompany(
                1L,
                updateRequest(
                        List.of(),
                        "Yazilim",
                        " ",
                        "",
                        "\t",
                        "  ",
                        null
                )
        );

        assertNull(company.getCity());
        assertNull(company.getDistrict());
        assertNull(company.getOsb());
        assertNull(company.getAddress());
        assertNull(company.getBuilding());
        assertNull(response.city());
        assertNull(response.district());
        assertNull(response.osb());
        assertNull(response.address());
        assertNull(response.building());
    }

    @Test
    void locationFieldsAreAvailableForTgbAndExternalCompanies() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponse tgbResponse = companyService.createCompany(
                createRequest(
                        CompanyType.TGB,
                        List.of(),
                        "Yazilim",
                        "Sakarya",
                        null,
                        null,
                        null,
                        null
                )
        );
        CompanyResponse externalResponse = companyService.createCompany(
                createRequest(
                        CompanyType.EXTERNAL,
                        List.of(),
                        "Danismanlik",
                        "Istanbul",
                        null,
                        null,
                        null,
                        null
                )
        );

        assertEquals("Sakarya", tgbResponse.city());
        assertEquals("Istanbul", externalResponse.city());
    }

    @Test
    void rejectsOtherCompanyContactMethodIdOnUpdate() {
        Company company = company(1L, "Eski Sirket", CompanyType.EXTERNAL);
        CompanyContact contact = contact(10L, company, true);
        company.addContact(contact);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        assertThrows(InvalidRequestException.class, () ->
                companyService.updateCompany(
                        1L,
                        updateRequest(List.of(method(
                                999L,
                                CompanyContactMethodType.PHONE,
                                "222",
                                null,
                                true
                        )))
                ));
    }

    @Test
    void rejectsDuplicateEmailOnUpdateWhenAnotherCompanyHasIt() {
        Company company = company(1L, "Eski Sirket", CompanyType.EXTERNAL);
        CompanyContact contact = contact(10L, company, true);
        company.addContact(contact);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(contactMethodRepository.existsByTypeAndValueIgnoreCaseAndCompanyIdNot(
                CompanyContactMethodType.EMAIL,
                "taken@example.com",
                1L
        )).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                companyService.updateCompany(
                        1L,
                        updateRequest(List.of(method(
                                null,
                                CompanyContactMethodType.EMAIL,
                                "TAKEN@EXAMPLE.COM",
                                null,
                                true
                        )))
                ));
    }

    @Test
    void listsCompaniesWithContactMethodsAndContactPersonRulesIntact() {
        Company company = company(1L, "Dis Sirket", CompanyType.EXTERNAL);
        CompanyContact contact = contact(10L, company, true);
        CompanyContactMethod contactMethod = contactMethod(
                100L,
                company,
                CompanyContactMethodType.EMAIL,
                "info@example.com",
                true
        );

        when(companyRepository.search(
                eq(CompanyType.EXTERNAL),
                eq("term"),
                eq(""),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(company)));
        when(companyContactRepository.findByCompanyIdIn(List.of(1L)))
                .thenReturn(List.of(contact));
        when(contactMethodRepository.findByCompanyIdIn(List.of(1L)))
                .thenReturn(List.of(contactMethod));

        PageResponse<CompanyListResponse> response =
                companyService.getCompanies(
                        CompanyType.EXTERNAL,
                        " term ",
                        null,
                        0,
                        10
                );

        CompanyListResponse companyResponse = response.content().getFirst();
        assertEquals(CompanyType.EXTERNAL, companyResponse.companyType());
        assertEquals("info@example.com",
                companyResponse.contactMethods().getFirst().value());
        assertEquals(1, companyResponse.contactCount());
        assertEquals("Yetkili Kisi", companyResponse.primaryContact().fullName());
        verify(companyRepository).search(
                eq(CompanyType.EXTERNAL),
                eq("term"),
                eq(""),
                any(Pageable.class)
        );
    }

    @Test
    void listsTgbCompaniesWithTrimmedInitialAndFilteredTotalElements() {
        Company beta = company(1L, "Beta Teknoloji", CompanyType.TGB);
        when(companyRepository.search(
                eq(CompanyType.TGB),
                eq("yazilim"),
                eq("B"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(beta), Pageable.ofSize(10), 18));
        when(companyContactRepository.findByCompanyIdIn(List.of(1L)))
                .thenReturn(List.of());
        when(contactMethodRepository.findByCompanyIdIn(List.of(1L)))
                .thenReturn(List.of());

        PageResponse<CompanyListResponse> response =
                companyService.getCompanies(
                        CompanyType.TGB,
                        " yazilim ",
                        " B ",
                        0,
                        10
                );

        assertEquals(18, response.totalElements());
        assertEquals("Beta Teknoloji", response.content().getFirst().name());
        verify(companyRepository).search(
                eq(CompanyType.TGB),
                eq("yazilim"),
                eq("B"),
                any(Pageable.class)
        );
    }

    @Test
    void listsExternalCompaniesWithLowercaseAndTurkishInitials() {
        Company sirket = company(2L, "\u015Eahin Yazilim", CompanyType.EXTERNAL);
        when(companyRepository.search(
                eq(CompanyType.EXTERNAL),
                eq(""),
                eq("\u015F"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(sirket), Pageable.ofSize(10), 1));
        when(companyContactRepository.findByCompanyIdIn(List.of(2L)))
                .thenReturn(List.of());
        when(contactMethodRepository.findByCompanyIdIn(List.of(2L)))
                .thenReturn(List.of());

        PageResponse<CompanyListResponse> response =
                companyService.getCompanies(
                        CompanyType.EXTERNAL,
                        "",
                        " \u015F ",
                        0,
                        10
                );

        assertEquals(1, response.totalElements());
        verify(companyRepository).search(
                eq(CompanyType.EXTERNAL),
                eq(""),
                eq("\u015F"),
                any(Pageable.class)
        );
    }

    @Test
    void countsCompaniesByCompanyTypeWithoutSearchOrInitialFilters() {
        when(companyRepository.countByCompanyType(CompanyType.TGB))
                .thenReturn(120L);
        when(companyRepository.countByCompanyType(CompanyType.EXTERNAL))
                .thenReturn(340L);

        assertEquals(120L, companyService.countCompanies(CompanyType.TGB));
        assertEquals(340L, companyService.countCompanies(CompanyType.EXTERNAL));
        verify(companyRepository).countByCompanyType(CompanyType.TGB);
        verify(companyRepository).countByCompanyType(CompanyType.EXTERNAL);
        verify(companyRepository, never()).search(
                any(CompanyType.class),
                anyString(),
                anyString(),
                any(Pageable.class)
        );
    }

    @Test
    void returnsFilteredCompanyIdsWithoutPagination() {
        Company first = company(1L, "Delta", CompanyType.TGB);
        Company second = company(2L, "Dora", CompanyType.TGB);
        when(companyRepository.search(
                eq(CompanyType.TGB),
                eq("yazilim"),
                eq("D"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(first, second)));

        List<Long> ids = companyService.getCompanyIds(
                CompanyType.TGB,
                " yazilim ",
                " D "
        );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        assertEquals(List.of(1L, 2L), ids);
        verify(companyRepository).search(
                eq(CompanyType.TGB),
                eq("yazilim"),
                eq("D"),
                pageableCaptor.capture()
        );
        assertFalse(pageableCaptor.getValue().isPaged());
    }

    @Test
    void createCompanyRequestAllowsActivityAreaWith2000Characters() {
        CreateCompanyRequest request = createRequest(
                CompanyType.EXTERNAL,
                List.of(),
                repeated("A", 2000)
        );

        assertEquals(0, validator.validate(request).size());
    }

    @Test
    void createCompanyRequestRejectsActivityAreaWith2001Characters() {
        CreateCompanyRequest request = createRequest(
                CompanyType.EXTERNAL,
                List.of(),
                repeated("A", 2001)
        );

        Set<String> messages = validator.validate(request)
                .stream()
                .map(violation -> violation.getMessage())
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("Faaliyet alani en fazla 2000 karakter olabilir"),
                messages);
    }

    @Test
    void updateCompanyRequestAllowsActivityAreaWith2000Characters() {
        UpdateCompanyRequest request = updateRequest(
                List.of(),
                repeated("A", 2000)
        );

        assertEquals(0, validator.validate(request).size());
    }

    @Test
    void updateCompanyRequestRejectsActivityAreaWith2001Characters() {
        UpdateCompanyRequest request = updateRequest(
                List.of(),
                repeated("A", 2001)
        );

        Set<String> messages = validator.validate(request)
                .stream()
                .map(violation -> violation.getMessage())
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("Faaliyet alani en fazla 2000 karakter olabilir"),
                messages);
    }

    private CreateCompanyRequest createRequest(
            CompanyType companyType,
            List<CompanyContactMethodRequest> contactMethods
    ) {
        return createRequest(companyType, contactMethods, "Yazilim");
    }

    private CreateCompanyRequest createRequest(
            CompanyType companyType,
            List<CompanyContactMethodRequest> contactMethods,
            String activityArea
    ) {
        return createRequest(
                companyType,
                contactMethods,
                activityArea,
                null,
                null,
                null,
                null,
                null
        );
    }

    private CreateCompanyRequest createRequest(
            CompanyType companyType,
            List<CompanyContactMethodRequest> contactMethods,
            String activityArea,
            String city,
            String district,
            String osb,
            String address,
            String building
    ) {
        return new CreateCompanyRequest(
                "TGB Sirket",
                activityArea,
                city,
                district,
                osb,
                address,
                building,
                null,
                companyType,
                contactMethods,
                List.of(new CreateCompanyContactRequest(
                        "Yetkili Kisi",
                        "Mudur",
                        "CONTACT@EXAMPLE.COM",
                        null,
                        true
                ))
        );
    }

    private UpdateCompanyRequest updateRequest(
            List<CompanyContactMethodRequest> contactMethods
    ) {
        return updateRequest(contactMethods, "Yazilim");
    }

    private UpdateCompanyRequest updateRequest(
            List<CompanyContactMethodRequest> contactMethods,
            String activityArea
    ) {
        return updateRequest(
                contactMethods,
                activityArea,
                "Kocaeli",
                "Gebze",
                "GOSB",
                "Yeni Adres",
                "Bina 2"
        );
    }

    private UpdateCompanyRequest updateRequest(
            List<CompanyContactMethodRequest> contactMethods,
            String activityArea,
            String city,
            String district,
            String osb,
            String address,
            String building
    ) {
        return new UpdateCompanyRequest(
                "Yeni Sirket",
                activityArea,
                city,
                district,
                osb,
                address,
                building,
                null,
                contactMethods,
                List.of(new UpdateCompanyContactRequest(
                        10L,
                        "Guncel Yetkili",
                        "Mudur",
                        "updated-contact@example.com",
                        null,
                        true
                ))
        );
    }

    private CompanyContactMethodRequest method(
            Long id,
            CompanyContactMethodType type,
            String value,
            String label,
            boolean primary
    ) {
        return new CompanyContactMethodRequest(
                id,
                type,
                value,
                label,
                primary
        );
    }

    private String repeated(String value, int count) {
        return value.repeat(count);
    }

    private Company company(Long id, String name, CompanyType companyType) {
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        company.setActivityArea("Yazilim");
        company.setLogoUrl(null);
        company.setCompanyType(companyType);
        return company;
    }

    private CompanyContact contact(
            Long id,
            Company company,
            boolean primary
    ) {
        CompanyContact contact = new CompanyContact();
        contact.setId(id);
        contact.setCompany(company);
        contact.setFullName("Yetkili Kisi");
        contact.setPosition("Mudur");
        contact.setEmail("contact@example.com");
        contact.setPhone(null);
        contact.setPrimary(primary);
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

    private Company savedCompany() {
        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
