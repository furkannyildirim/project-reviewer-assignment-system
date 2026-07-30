package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyListResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyResponse;
import com.sakaryateknokent.reviewerassignment.dto.CreateCompanyContactRequest;
import com.sakaryateknokent.reviewerassignment.dto.CreateCompanyRequest;
import com.sakaryateknokent.reviewerassignment.dto.PageResponse;
import com.sakaryateknokent.reviewerassignment.dto.UpdateCompanyContactRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateCompanyRequest;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContact;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    private final CompanyRepository companyRepository =
            mock(CompanyRepository.class);
    private final CompanyContactRepository companyContactRepository =
            mock(CompanyContactRepository.class);
    private final CompanyService companyService =
            new CompanyService(companyRepository, companyContactRepository);

    @Test
    void createsTgbCompanyWithCompanyType() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponse response = companyService.createCompany(
                createRequest(CompanyType.TGB, "TGB Sirket", " 222 ")
        );

        Company savedCompany = savedCompany();
        assertEquals(CompanyType.TGB, savedCompany.getCompanyType());
        assertEquals("222", savedCompany.getSecondaryPhone());
        assertEquals(CompanyType.TGB, response.companyType());
        assertEquals("222", response.secondaryPhone());
        assertEquals("contact@example.com",
                savedCompany.getContacts().getFirst().getEmail());
    }

    @Test
    void createsExternalCompanyWithCompanyType() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponse response = companyService.createCompany(
                createRequest(CompanyType.EXTERNAL, "Dis Sirket", null)
        );

        Company savedCompany = savedCompany();
        assertEquals(CompanyType.EXTERNAL, savedCompany.getCompanyType());
        assertEquals(CompanyType.EXTERNAL, response.companyType());
    }

    @Test
    void createsCompanyWithNullSecondaryPhoneWhenBlank() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponse response = companyService.createCompany(
                createRequest(CompanyType.TGB, "TGB Sirket", "   ")
        );

        Company savedCompany = savedCompany();
        assertEquals(CompanyType.TGB, savedCompany.getCompanyType());
        assertNull(savedCompany.getSecondaryPhone());
        assertNull(response.secondaryPhone());
    }

    @Test
    void listsCompaniesByCompanyTypeAndReturnsCompanyType() {
        Company company = company(1L, "Dis Sirket", CompanyType.EXTERNAL);
        company.setSecondaryPhone("222");
        CompanyContact contact = contact(10L, company, true);

        when(companyRepository.search(
                eq(CompanyType.EXTERNAL),
                eq("term"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(company)));
        when(companyContactRepository.findByCompanyIdIn(List.of(1L)))
                .thenReturn(List.of(contact));

        PageResponse<CompanyListResponse> response =
                companyService.getCompanies(
                        CompanyType.EXTERNAL,
                        " term ",
                        0,
                        10
                );

        CompanyListResponse companyResponse = response.content().getFirst();
        assertEquals(CompanyType.EXTERNAL, companyResponse.companyType());
        assertEquals("222", companyResponse.secondaryPhone());
        assertEquals(1, companyResponse.contactCount());
        assertEquals("Yetkili Kisi", companyResponse.primaryContact().fullName());
        verify(companyRepository).search(
                eq(CompanyType.EXTERNAL),
                eq("term"),
                any(Pageable.class)
        );
    }

    @Test
    void updateKeepsExistingCompanyType() {
        Company company = company(1L, "Eski Sirket", CompanyType.EXTERNAL);
        CompanyContact contact = contact(10L, company, true);
        company.addContact(contact);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        CompanyResponse response = companyService.updateCompany(
                1L,
                new UpdateCompanyRequest(
                        "Yeni Sirket",
                        "new@example.com",
                        "555",
                        " 444 ",
                        "Yazilim",
                        null,
                        List.of(new UpdateCompanyContactRequest(
                                10L,
                                "Guncel Yetkili",
                                "Mudur",
                                "updated-contact@example.com",
                                null,
                                true
                        ))
                )
        );

        assertEquals(CompanyType.EXTERNAL, company.getCompanyType());
        assertEquals(CompanyType.EXTERNAL, response.companyType());
        assertEquals("new@example.com", company.getEmail());
        assertEquals("444", company.getSecondaryPhone());
        assertEquals("444", response.secondaryPhone());
        assertEquals("Guncel Yetkili",
                company.getContacts().getFirst().getFullName());
    }

    private CreateCompanyRequest createRequest(
            CompanyType companyType,
            String name,
            String secondaryPhone
    ) {
        return new CreateCompanyRequest(
                name,
                "company@example.com",
                "555",
                secondaryPhone,
                "Yazilim",
                null,
                companyType,
                List.of(new CreateCompanyContactRequest(
                        "Yetkili Kisi",
                        "Mudur",
                        "CONTACT@EXAMPLE.COM",
                        null,
                        true
                ))
        );
    }

    private Company company(Long id, String name, CompanyType companyType) {
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        company.setEmail("company" + id + "@example.com");
        company.setPhone("555");
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

    private Company savedCompany() {
        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
