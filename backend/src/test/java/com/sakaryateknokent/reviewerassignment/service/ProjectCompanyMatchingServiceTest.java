package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.enums.ProjectCompanyMatchStatus;
import com.sakaryateknokent.reviewerassignment.repository.CompanyMatchCandidateProjection;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectCompanyMatchingServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final ProjectCompanyMatchingService matchingService =
            new ProjectCompanyMatchingService(companyRepository);

    @Test
    void returnsUniqueMatchUsingExistingCanonicalNameNormalization() {
        when(companyRepository.findAllMatchCandidates()).thenReturn(List.of(
                company(7L, "Sakarya   Yazılım", CompanyType.TGB)));

        ProjectCompanyMatchResult result = matchingService.match(" sakarya yazılım ");

        assertEquals(ProjectCompanyMatchStatus.UNIQUE_MATCH, result.status());
        assertEquals(7L, result.matchedCompany().id());
        assertEquals("Sakarya   Yazılım", result.matchedCompany().name());
        assertEquals(CompanyType.TGB, result.matchedCompany().companyType());
        assertEquals(List.of(), result.candidates());
        assertEquals("Firma sistemdeki bir şirketle eşleşti.", result.message());
        verify(companyRepository).findAllMatchCandidates();
    }

    @Test
    void returnsAmbiguousCandidatesAcrossCompanyTypesInDeterministicOrder() {
        when(companyRepository.findAllMatchCandidates()).thenReturn(List.of(
                company(9L, "Ada Teknoloji", CompanyType.TGB),
                company(4L, "Ada Teknoloji", CompanyType.EXTERNAL),
                company(2L, "Ada Teknoloji", CompanyType.EXTERNAL),
                company(11L, "Başka Şirket", CompanyType.TGB)));

        ProjectCompanyMatchResult result = matchingService.match("ADA TEKNOLOJİ");

        assertEquals(ProjectCompanyMatchStatus.AMBIGUOUS, result.status());
        assertNull(result.matchedCompany());
        assertEquals(List.of(9L, 2L, 4L), result.candidates().stream()
                .map(ProjectCompanyMatchCandidate::id).toList());
        assertEquals("Aynı adla birden fazla şirket bulundu; seçim yapmanız gerekiyor.",
                result.message());
    }

    @Test
    void returnsNotFoundWhenNoCanonicalNameMatches() {
        when(companyRepository.findAllMatchCandidates()).thenReturn(List.of(
                company(1L, "Kayıtlı Şirket", CompanyType.EXTERNAL)));

        ProjectCompanyMatchResult result = matchingService.match("Yeni Şirket");

        assertEquals(ProjectCompanyMatchStatus.NOT_FOUND, result.status());
        assertNull(result.matchedCompany());
        assertEquals(List.of(), result.candidates());
        assertEquals("Firma sistemde bulunamadı; önce şirket kaydı oluşturmanız gerekiyor.",
                result.message());
    }

    @Test
    void returnsMissingWithoutQueryingCompanies() {
        ProjectCompanyMatchResult result = matchingService.match("  \t");

        assertEquals(ProjectCompanyMatchStatus.COMPANY_NAME_MISSING, result.status());
        assertNull(result.matchedCompany());
        assertEquals(List.of(), result.candidates());
        assertEquals("Belgeden firma adı çıkarılamadı; şirket seçmeniz gerekiyor.",
                result.message());
        verify(companyRepository, never()).findAllMatchCandidates();
    }

    private CompanyMatchCandidateProjection company(
            Long id,
            String name,
            CompanyType companyType
    ) {
        return new CompanyMatchCandidateProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public CompanyType getCompanyType() {
                return companyType;
            }
        };
    }
}
