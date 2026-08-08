package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewRowResponse;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.importing.CompanyExcelImportParser;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportPreview;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportRow;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyImportPreviewService {

    private static final String AMBIGUOUS_EXTERNAL_MATCH_MESSAGE =
            "Satir birden fazla mevcut dis sirketle eslesiyor; otomatik guncelleme yapilmadi.";
    private static final String WEAK_EXTERNAL_NAME_MATCH_MESSAGE =
            "Benzer sirket yalnizca sirket adina gore eslesti. Ayirt edici adres veya iletisim bilgisi yetersiz oldugu icin otomatik islem yapilmadi; manuel kontrol edin.";
    private static final String LOCATION_MANUAL_REVIEW_MESSAGE =
            "Mevcut sirket icin Excel dosyasinda farkli veya ek konum bilgisi bulundu. Otomatik guncelleme yapilmadi; manuel kontrol edin.";

    private final CompanyExcelImportParser parser;
    private final CompanyRepository companyRepository;
    private final CompanyLocationMergeClassifier locationMergeClassifier;

    public CompanyImportPreviewResponse preview(MultipartFile file) {
        return preview(file, CompanyType.TGB);
    }

    public CompanyImportPreviewResponse preview(
            MultipartFile file,
            CompanyType companyType
    ) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Excel dosyasi bos olamaz");
        }

        try {
            CompanyImportPreview preview = parser.parse(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    companyType
            );
            return toResponse(preview, companyType);
        } catch (IOException exception) {
            throw new InvalidRequestException("Excel dosyasi okunamadi");
        }
    }

    private CompanyImportPreviewResponse toResponse(
            CompanyImportPreview preview,
            CompanyType companyType
    ) {
        PreviewState state = previewState(
                companyType,
                preview.getRows()
        );
        CompanyFileDuplicateClassifier.FileDuplicateRows fileDuplicateRows =
                CompanyFileDuplicateClassifier.findDuplicates(
                        preview.getRows(),
                        companyType
                );
        List<CompanyImportPreviewRowResponse> rowResponses = new ArrayList<>();
        for (CompanyImportRow row : preview.getRows()) {
            rowResponses.add(toRowResponse(
                    row,
                    state,
                    fileDuplicateRows
            ));
        }

        int validRowCount = 0;
        int warningCount = preview.getGeneralWarnings().size()
                + rowResponses.stream()
                .mapToInt(row -> row.warnings().size())
                .sum();

        for (CompanyImportPreviewRowResponse row : rowResponses) {
            if (row.valid()) {
                validRowCount++;
            }
        }

        return new CompanyImportPreviewResponse(
                preview.getFileName(),
                preview.getSheetCount(),
                preview.getRows().size(),
                validRowCount,
                preview.getRows().size() - validRowCount,
                warningCount,
                List.copyOf(new LinkedHashSet<>(preview.getDetectedHeaders())),
                List.copyOf(new LinkedHashSet<>(preview.getIgnoredHeaders())),
                List.copyOf(preview.getGeneralWarnings()),
                List.copyOf(preview.getGeneralInformationMessages()),
                rowResponses
        );
    }

    private CompanyImportPreviewRowResponse toRowResponse(
            CompanyImportRow row,
            PreviewState state,
            CompanyFileDuplicateClassifier.FileDuplicateRows fileDuplicateRows
    ) {
        String status = CompanyLocationMergeClassifier.STATUS_INVALID;
        List<String> warnings = row.getWarnings();
        List<String> updateMessages = List.of();
        List<String> locationWarnings = List.of();
        List<String> errors = validationErrors(row);
        boolean valid = errors.isEmpty();

        if (valid) {
            if (fileDuplicateRows.duplicate(row)) {
                status = CompanyLocationMergeClassifier.STATUS_DUPLICATE;
                warnings = new ArrayList<>(row.getWarnings());
                warnings.add(fileDuplicateRows.message(row));
            } else {
                ExistingCompanyMatch companyMatch =
                        findExistingCompany(row, state);
                if (companyMatch.requiresReview()) {
                    status = CompanyLocationMergeClassifier.STATUS_WARNING;
                    warnings = new ArrayList<>(row.getWarnings());
                    warnings.add(companyMatch.reviewMessage());
                } else {
                    Optional<Company> existingCompany = companyMatch.company();
                    CompanyLocationMergeClassifier.LocationMergeDecision decision =
                            locationMergeClassifier.classify(
                                    row,
                                    existingCompany.orElse(null)
                            );
                    warnings = new ArrayList<>(row.getWarnings());
                    if (existingCompany.isPresent()
                            && (decision.update()
                            || !decision.warnings().isEmpty())) {
                        status = CompanyLocationMergeClassifier.STATUS_WARNING;
                        List<String> manualWarnings = new ArrayList<>();
                        manualWarnings.add(LOCATION_MANUAL_REVIEW_MESSAGE);
                        manualWarnings.addAll(decision.warnings());
                        locationWarnings = List.copyOf(manualWarnings);
                        warnings.addAll(manualWarnings);
                    } else {
                        status = decision.status();
                    }
                }
            }
        }

        return new CompanyImportPreviewRowResponse(
                row.getSheetName(),
                row.getRowNumber(),
                row.getName(),
                row.getActivityArea(),
                status,
                row.getCity(),
                row.getDistrict(),
                row.getOsb(),
                row.getAddress(),
                row.getBuilding(),
                List.copyOf(row.getCompanyPhones()),
                List.copyOf(row.getCompanyEmails()),
                row.getContactFullName(),
                row.getContactPosition(),
                row.getContactPhone(),
                row.getContactEmail(),
                List.copyOf(updateMessages),
                List.copyOf(locationWarnings),
                List.copyOf(errors),
                List.copyOf(warnings),
                valid
        );
    }

    private PreviewState previewState(
            CompanyType companyType,
            List<CompanyImportRow> rows
    ) {
        Map<String, Company> companiesByName = new LinkedHashMap<>();
        Map<Company, Company> virtualCompanies =
                new IdentityHashMap<>();
        List<Company> existingCompanies =
                companyRepository.findByCompanyType(companyType);
        Map<String, List<Company>> virtualExternalCompaniesByName =
                new LinkedHashMap<>();
        for (Company company : existingCompanies) {
            Company virtualCompany = virtualCompany(company, virtualCompanies);
            String canonicalCompanyName = canonicalName(company.getName());
            companiesByName.putIfAbsent(
                    canonicalCompanyName,
                    virtualCompany
            );
            if (CompanyType.EXTERNAL.equals(companyType)) {
                virtualExternalCompaniesByName
                        .computeIfAbsent(
                                canonicalCompanyName,
                                ignored -> new ArrayList<>()
                        )
                        .add(virtualCompany);
            }
        }

        return new PreviewState(
                companyType,
                companiesByName,
                immutableCompanyLists(virtualExternalCompaniesByName),
                virtualCompanies
        );
    }

    private ExistingCompanyMatch findExistingCompany(
            CompanyImportRow row,
            PreviewState state
    ) {
        if (CompanyType.EXTERNAL.equals(state.companyType())) {
            List<ExternalCompanyMatch> matches = state
                    .externalCompaniesByName()
                    .getOrDefault(
                            canonicalName(row.getName()),
                            List.of()
                    )
                    .stream()
                    .map(company -> new ExternalCompanyMatch(
                            company,
                            ExternalCompanyIdentityMatcher.evaluate(row, company)
                    ))
                    .filter(match -> match.strength().matches())
                    .toList();
            if (matches.size() > 1) {
                return ExistingCompanyMatch.review(
                        AMBIGUOUS_EXTERNAL_MATCH_MESSAGE
                );
            }
            Optional<ExternalCompanyMatch> identityMatch =
                    matches.stream().findFirst();
            if (identityMatch.isPresent()
                    && ExternalCompanyIdentityMatcher.MatchStrength.NAME_FALLBACK
                    .equals(identityMatch.get().strength())) {
                return ExistingCompanyMatch.review(
                        WEAK_EXTERNAL_NAME_MATCH_MESSAGE
                );
            }
            return ExistingCompanyMatch.of(
                    identityMatch.map(ExternalCompanyMatch::company)
            );
        }

        if (row.getName() == null) {
            return ExistingCompanyMatch.none();
        }

        return ExistingCompanyMatch.of(Optional.ofNullable(
                state.companiesByName().get(canonicalName(row.getName()))
        ));
    }

    private Company virtualCompany(
            Company company,
            Map<Company, Company> virtualCompanies
    ) {
        return virtualCompanies.computeIfAbsent(company, ignored -> {
            Company virtualCompany = new Company();
            virtualCompany.setName(company.getName());
            virtualCompany.setActivityArea(company.getActivityArea());
            virtualCompany.setCity(company.getCity());
            virtualCompany.setDistrict(company.getDistrict());
            virtualCompany.setOsb(company.getOsb());
            virtualCompany.setAddress(company.getAddress());
            virtualCompany.setBuilding(company.getBuilding());
            virtualCompany.setCompanyType(company.getCompanyType());
            for (CompanyContactMethod method : company.getContactMethods()) {
                CompanyContactMethod virtualMethod = new CompanyContactMethod();
                virtualMethod.setType(method.getType());
                virtualMethod.setValue(method.getValue());
                virtualMethod.setPrimary(method.isPrimary());
                virtualCompany.addContactMethod(virtualMethod);
            }
            return virtualCompany;
        });
    }

    private List<String> validationErrors(CompanyImportRow row) {
        return CompanyFileDuplicateClassifier.validationErrors(row);
    }

    private String canonicalName(String name) {
        return CompanyFileDuplicateClassifier.canonicalName(name);
    }

    private Map<String, List<Company>> immutableCompanyLists(
            Map<String, List<Company>> companiesByName
    ) {
        Map<String, List<Company>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, List<Company>> entry
                : companiesByName.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(immutable);
    }

    private record PreviewState(
            CompanyType companyType,
            Map<String, Company> companiesByName,
            Map<String, List<Company>> externalCompaniesByName,
            Map<Company, Company> virtualCompanies
    ) {
    }

    private record ExistingCompanyMatch(
            Optional<Company> company,
            String reviewMessage
    ) {

        private static ExistingCompanyMatch of(Optional<Company> company) {
            return new ExistingCompanyMatch(company, null);
        }

        private static ExistingCompanyMatch none() {
            return of(Optional.empty());
        }

        private static ExistingCompanyMatch review(String message) {
            return new ExistingCompanyMatch(Optional.empty(), message);
        }

        private boolean requiresReview() {
            return reviewMessage != null;
        }
    }

    private record ExternalCompanyMatch(
            Company company,
            ExternalCompanyIdentityMatcher.MatchStrength strength
    ) {
    }

}
