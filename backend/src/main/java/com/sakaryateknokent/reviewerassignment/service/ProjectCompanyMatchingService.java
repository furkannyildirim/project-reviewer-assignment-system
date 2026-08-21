package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.ProjectCompanyMatchStatus;
import com.sakaryateknokent.reviewerassignment.repository.CompanyMatchCandidateProjection;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectCompanyMatchingService {

    private static final String UNIQUE_MATCH_MESSAGE =
            "Firma sistemdeki bir şirketle eşleşti.";
    private static final String AMBIGUOUS_MATCH_MESSAGE =
            "Aynı adla birden fazla şirket bulundu; seçim yapmanız gerekiyor.";
    private static final String NOT_FOUND_MESSAGE =
            "Firma sistemde bulunamadı; önce şirket kaydı oluşturmanız gerekiyor.";
    private static final String COMPANY_NAME_MISSING_MESSAGE =
            "Belgeden firma adı çıkarılamadı; şirket seçmeniz gerekiyor.";

    private final CompanyRepository companyRepository;

    public ProjectCompanyMatchResult match(String parsedCompanyName) {
        if (CompanyFileDuplicateClassifier.canonicalName(parsedCompanyName).isEmpty()) {
            return new ProjectCompanyMatchResult(
                    ProjectCompanyMatchStatus.COMPANY_NAME_MISSING,
                    null, List.of(), COMPANY_NAME_MISSING_MESSAGE);
        }

        String canonicalName = CompanyFileDuplicateClassifier.canonicalName(parsedCompanyName);
        List<ProjectCompanyMatchCandidate> candidates = companyRepository.findAllMatchCandidates()
                .stream()
                .filter(company -> canonicalName.equals(
                        CompanyFileDuplicateClassifier.canonicalName(company.getName())))
                .map(this::toCandidate)
                .sorted(Comparator
                        .comparing(ProjectCompanyMatchCandidate::companyType)
                        .thenComparing(ProjectCompanyMatchCandidate::name)
                        .thenComparing(ProjectCompanyMatchCandidate::id))
                .toList();

        if (candidates.isEmpty()) {
            return new ProjectCompanyMatchResult(
                    ProjectCompanyMatchStatus.NOT_FOUND,
                    null, List.of(), NOT_FOUND_MESSAGE);
        }
        if (candidates.size() == 1) {
            return new ProjectCompanyMatchResult(
                    ProjectCompanyMatchStatus.UNIQUE_MATCH,
                    candidates.getFirst(), List.of(), UNIQUE_MATCH_MESSAGE);
        }
        return new ProjectCompanyMatchResult(
                ProjectCompanyMatchStatus.AMBIGUOUS,
                null, candidates, AMBIGUOUS_MATCH_MESSAGE);
    }

    private ProjectCompanyMatchCandidate toCandidate(
            CompanyMatchCandidateProjection company
    ) {
        return new ProjectCompanyMatchCandidate(
                company.getId(), company.getName(), company.getCompanyType());
    }
}
