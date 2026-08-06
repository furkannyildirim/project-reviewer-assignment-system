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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyImportPreviewService {

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
        Map<String, Company> existingCompaniesByName =
                existingCompaniesByName(companyType);
        List<CompanyImportPreviewRowResponse> rowResponses =
                preview.getRows()
                        .stream()
                        .map(row -> toRowResponse(row, existingCompaniesByName))
                        .toList();

        int validRowCount = 0;
        int warningCount = preview.getGeneralWarnings().size()
                + rowResponses.stream()
                .mapToInt(row -> row.warnings().size())
                .sum();

        for (CompanyImportRow row : preview.getRows()) {
            if (row.isValid()) {
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
            Map<String, Company> existingCompaniesByName
    ) {
        String status = CompanyLocationMergeClassifier.STATUS_INVALID;
        List<String> warnings = row.getWarnings();
        List<String> updateMessages = List.of();

        if (row.isValid()) {
            Optional<Company> existingCompany =
                    findExistingCompany(row, existingCompaniesByName);
            CompanyLocationMergeClassifier.LocationMergeDecision decision =
                    locationMergeClassifier.classify(
                            row,
                            existingCompany.orElse(null)
                    );
            status = decision.status();
            updateMessages = decision.updateMessages();
            warnings = new java.util.ArrayList<>(row.getWarnings());
            warnings.addAll(decision.warnings());
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
                List.copyOf(row.getErrors()),
                List.copyOf(warnings),
                row.isValid()
        );
    }

    private Map<String, Company> existingCompaniesByName(
            CompanyType companyType
    ) {
        Map<String, Company> companiesByName = new LinkedHashMap<>();
        for (Company company : companyRepository.findByCompanyType(companyType)) {
            companiesByName.putIfAbsent(
                    canonicalName(company.getName()),
                    company
            );
        }

        return companiesByName;
    }

    private Optional<Company> findExistingCompany(
            CompanyImportRow row,
            Map<String, Company> existingCompaniesByName
    ) {
        if (!row.getCompanyEmails().isEmpty()) {
            for (String email : row.getCompanyEmails()) {
                List<CompanyContactMethod> matches = contactMethodRepository
                        .findByTypeAndValueIgnoreCaseOrderByIdAsc(
                                CompanyContactMethodType.EMAIL,
                                canonicalEmail(email)
                        );
                if (!matches.isEmpty()) {
                    return Optional.of(matches.getFirst().getCompany());
                }
            }

            return Optional.empty();
        }

        if (row.getName() == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                existingCompaniesByName.get(canonicalName(row.getName()))
        );
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
}
