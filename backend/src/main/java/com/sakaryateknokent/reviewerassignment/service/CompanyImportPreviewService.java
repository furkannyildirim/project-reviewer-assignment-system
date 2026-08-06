package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewRowResponse;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.importing.CompanyExcelImportParser;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportPreview;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportRow;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactMethodRepository;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyImportPreviewService {

    private static final String FILE_EMAIL_MESSAGE =
            "Dosya icinde ayni e-posta birden fazla kullanilmis.";
    private static final String FILE_NAME_MESSAGE =
            "Dosya icinde ayni sirket adi birden fazla kullanilmis.";
    private static final String PARSER_FILE_EMAIL_MESSAGE =
            "Dosya icinde ayni e-posta birden fazla kullanilmis";

    private final CompanyExcelImportParser parser;
    private final CompanyRepository companyRepository;
    private final CompanyContactMethodRepository contactMethodRepository;
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
        PreviewState state = previewState(companyType);
        FileDuplicateKeys fileDuplicateKeys =
                findFileDuplicateKeys(preview.getRows(), companyType);
        List<CompanyImportPreviewRowResponse> rowResponses = new ArrayList<>();
        for (CompanyImportRow row : preview.getRows()) {
            rowResponses.add(toRowResponse(row, state, fileDuplicateKeys));
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
            FileDuplicateKeys fileDuplicateKeys
    ) {
        String status = CompanyLocationMergeClassifier.STATUS_INVALID;
        List<String> warnings = row.getWarnings();
        List<String> updateMessages = List.of();
        List<String> locationWarnings = List.of();
        List<String> errors = validationErrors(row);
        boolean valid = errors.isEmpty();

        if (valid) {
            DuplicateCheck fileDuplicateCheck =
                    fileDuplicateCheck(row, fileDuplicateKeys, state.companyType());
            if (fileDuplicateCheck.duplicate()) {
                status = CompanyLocationMergeClassifier.STATUS_DUPLICATE;
                warnings = new ArrayList<>(row.getWarnings());
                warnings.add(fileDuplicateCheck.message());
            } else {
                Optional<Company> existingCompany =
                        findExistingCompany(row, state);
                CompanyLocationMergeClassifier.LocationMergeDecision decision =
                        locationMergeClassifier.classify(
                                row,
                                existingCompany.orElse(null)
                        );
                status = decision.status();
                updateMessages = decision.updateMessages();
                locationWarnings = decision.warnings();
                warnings = new ArrayList<>(row.getWarnings());
                warnings.addAll(decision.warnings());
                if (decision.update() && existingCompany.isPresent()) {
                    locationMergeClassifier.applyUpdates(
                            row,
                            existingCompany.get()
                    );
                } else if (existingCompany.isEmpty()) {
                    rememberNewRow(row, state);
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
            CompanyType companyType
    ) {
        Map<String, Company> companiesByName = new LinkedHashMap<>();
        Map<String, Company> externalCompaniesByNameAndAddress =
                new LinkedHashMap<>();
        Map<Company, Company> virtualCompanies =
                new IdentityHashMap<>();
        for (Company company : companyRepository.findByCompanyType(companyType)) {
            Company virtualCompany = virtualCompany(company, virtualCompanies);
            companiesByName.putIfAbsent(
                    canonicalName(company.getName()),
                    virtualCompany
            );
            if (CompanyType.EXTERNAL.equals(companyType)
                    && company.getAddress() != null) {
                externalCompaniesByNameAndAddress.putIfAbsent(
                        canonicalNameAddress(company.getName(), company.getAddress()),
                        virtualCompany
                );
            }
        }

        return new PreviewState(
                companyType,
                companiesByName,
                externalCompaniesByNameAndAddress,
                virtualCompanies
        );
    }

    private Optional<Company> findExistingCompany(
            CompanyImportRow row,
            PreviewState state
    ) {
        if (!row.getCompanyEmails().isEmpty()) {
            for (String email : row.getCompanyEmails()) {
                List<CompanyContactMethod> matches = contactMethodRepository
                        .findByTypeAndValueIgnoreCaseOrderByIdAsc(
                                CompanyContactMethodType.EMAIL,
                                canonicalEmail(email)
                        );
                if (!matches.isEmpty()) {
                    return Optional.of(virtualCompany(
                            matches.getFirst().getCompany(),
                            state.virtualCompanies()
                    ));
                }
            }

            return Optional.empty();
        }

        if (row.getName() == null) {
            return Optional.empty();
        }

        if (CompanyType.EXTERNAL.equals(state.companyType())
                && row.getAddress() != null) {
            return Optional.ofNullable(
                    state.externalCompaniesByNameAndAddress().get(
                            canonicalNameAddress(row.getName(), row.getAddress())
                    )
            );
        }

        return Optional.ofNullable(
                state.companiesByName().get(canonicalName(row.getName()))
        );
    }

    private void rememberNewRow(
            CompanyImportRow row,
            PreviewState state
    ) {
        if (row.getCompanyEmails().isEmpty() && row.getName() != null) {
            Company virtualCompany = virtualCompany(row);
            if (CompanyType.EXTERNAL.equals(state.companyType())
                    && row.getAddress() != null) {
                state.externalCompaniesByNameAndAddress().putIfAbsent(
                        canonicalNameAddress(row.getName(), row.getAddress()),
                        virtualCompany
                );
            } else {
                state.companiesByName().putIfAbsent(
                        canonicalName(row.getName()),
                        virtualCompany
                );
            }
        }
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
            return virtualCompany;
        });
    }

    private Company virtualCompany(CompanyImportRow row) {
        Company company = new Company();
        company.setName(row.getName());
        company.setActivityArea(row.getActivityArea());
        company.setCity(row.getCity());
        company.setDistrict(row.getDistrict());
        company.setOsb(row.getOsb());
        company.setAddress(row.getAddress());
        company.setBuilding(row.getBuilding());
        return company;
    }

    private FileDuplicateKeys findFileDuplicateKeys(
            List<CompanyImportRow> rows,
            CompanyType companyType
    ) {
        Map<String, Integer> emailCounts = new LinkedHashMap<>();
        Map<String, Integer> noEmailNameCounts = new LinkedHashMap<>();
        Map<String, Integer> noEmailNameAddressCounts = new LinkedHashMap<>();

        for (CompanyImportRow row : rows) {
            if (!validationErrors(row).isEmpty()) {
                continue;
            }

            if (!row.getCompanyEmails().isEmpty()) {
                for (String email : row.getCompanyEmails()) {
                    increment(emailCounts, canonicalEmail(email));
                }
                continue;
            }

            if (CompanyType.EXTERNAL.equals(companyType)
                    && row.getAddress() != null
                    && row.getName() != null) {
                increment(
                        noEmailNameAddressCounts,
                        canonicalNameAddress(row.getName(), row.getAddress())
                );
                continue;
            }

            if (row.getName() != null) {
                increment(noEmailNameCounts, canonicalName(row.getName()));
            }
        }

        return new FileDuplicateKeys(
                duplicateKeys(emailCounts),
                duplicateKeys(noEmailNameCounts),
                duplicateKeys(noEmailNameAddressCounts)
        );
    }

    private List<String> validationErrors(CompanyImportRow row) {
        return row.getErrors()
                .stream()
                .filter(error -> !PARSER_FILE_EMAIL_MESSAGE.equals(error))
                .toList();
    }

    private void increment(Map<String, Integer> counts, String key) {
        counts.put(key, counts.getOrDefault(key, 0) + 1);
    }

    private Set<String> duplicateKeys(Map<String, Integer> counts) {
        Set<String> duplicates = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add(entry.getKey());
            }
        }

        return duplicates;
    }

    private DuplicateCheck fileDuplicateCheck(
            CompanyImportRow row,
            FileDuplicateKeys duplicateKeys,
            CompanyType companyType
    ) {
        if (!row.getCompanyEmails().isEmpty()
                && row.getCompanyEmails()
                .stream()
                .map(this::canonicalEmail)
                .anyMatch(duplicateKeys.emails()::contains)) {
            return DuplicateCheck.duplicate(FILE_EMAIL_MESSAGE);
        }

        if (CompanyType.EXTERNAL.equals(companyType)
                && row.getCompanyEmails().isEmpty()
                && row.getAddress() != null
                && row.getName() != null
                && duplicateKeys.noEmailNameAddresses().contains(
                canonicalNameAddress(row.getName(), row.getAddress())
        )) {
            return DuplicateCheck.duplicate(FILE_NAME_MESSAGE);
        }

        if (row.getCompanyEmails().isEmpty()
                && row.getName() != null
                && duplicateKeys.noEmailNames().contains(
                canonicalName(row.getName())
        )) {
            return DuplicateCheck.duplicate(FILE_NAME_MESSAGE);
        }

        return DuplicateCheck.unique();
    }

    private String canonicalEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String canonicalName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        return AcademicianTextNormalizer.normalizeWhitespace(name)
                .toLowerCase(Locale.forLanguageTag("tr"));
    }

    private String canonicalAddress(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }

        return AcademicianTextNormalizer.normalizeWhitespace(address)
                .toLowerCase(Locale.forLanguageTag("tr"));
    }

    private String canonicalNameAddress(String name, String address) {
        return canonicalName(name) + "|" + canonicalAddress(address);
    }

    private record PreviewState(
            CompanyType companyType,
            Map<String, Company> companiesByName,
            Map<String, Company> externalCompaniesByNameAndAddress,
            Map<Company, Company> virtualCompanies
    ) {
    }

    private record FileDuplicateKeys(
            Set<String> emails,
            Set<String> noEmailNames,
            Set<String> noEmailNameAddresses
    ) {
    }

    private record DuplicateCheck(
            boolean duplicate,
            String message
    ) {

        private static DuplicateCheck unique() {
            return new DuplicateCheck(false, "");
        }

        private static DuplicateCheck duplicate(String message) {
            return new DuplicateCheck(true, message);
        }
    }
}
