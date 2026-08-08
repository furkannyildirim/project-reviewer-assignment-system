package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyImportResultResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyImportResultRowResponse;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContact;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContactMethod;
import com.sakaryateknokent.reviewerassignment.enums.CompanyContactMethodType;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.importing.CompanyExcelImportParser;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportPreview;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportRow;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyImportService {

    private static final String STATUS_IMPORTED = "IMPORTED";
    private static final String STATUS_DUPLICATE = "DUPLICATE";
    private static final String STATUS_INVALID = "INVALID";
    private static final String EXISTING_EMAIL_MESSAGE =
            "Bu e-posta adresine sahip bir sirket zaten mevcut.";
    private static final String EXISTING_TGB_NAME_MESSAGE =
            "Bu sirket adiyla bir TGB sirketi zaten mevcut.";
    private static final String EXISTING_EXTERNAL_NAME_MESSAGE =
            "Bu sirket adiyla bir dis sirket zaten mevcut.";
    private static final String DATA_INTEGRITY_MESSAGE =
            "Kayit sirasinda veri butunlugu hatasi olustu.";
    private static final String AMBIGUOUS_EXTERNAL_MATCH_MESSAGE =
            "Satir birden fazla mevcut dis sirketle eslesiyor; otomatik guncelleme yapilmadi.";
    private static final String WEAK_EXTERNAL_NAME_MATCH_MESSAGE =
            "Benzer sirket yalnizca sirket adina gore eslesti. Ayirt edici adres veya iletisim bilgisi yetersiz oldugu icin otomatik islem yapilmadi; manuel kontrol edin.";
    private static final String LOCATION_MANUAL_REVIEW_MESSAGE =
            "Mevcut sirket icin Excel dosyasinda farkli veya ek konum bilgisi bulundu. Otomatik guncelleme yapilmadi; manuel kontrol edin.";

    private final CompanyExcelImportParser parser;
    private final CompanyRepository companyRepository;
    private final CompanyLocationMergeClassifier locationMergeClassifier;

    @Transactional
    public CompanyImportResultResponse importTgbCompanies(MultipartFile file) {
        return importCompanies(file, CompanyType.TGB);
    }

    @Transactional
    public CompanyImportResultResponse importExternalCompanies(
            MultipartFile file
    ) {
        return importCompanies(file, CompanyType.EXTERNAL);
    }

    private CompanyImportResultResponse importCompanies(
            MultipartFile file,
            CompanyType companyType
    ) {
        CompanyImportPreview preview = parse(file, companyType);
        CompanyFileDuplicateClassifier.FileDuplicateRows fileDuplicateRows =
                CompanyFileDuplicateClassifier.findDuplicates(
                        preview.getRows(),
                        companyType
                );
        ImportState state = importState(
                companyType,
                preview.getRows()
        );

        List<CompanyImportResultRowResponse> resultRows = new ArrayList<>();
        int importedCount = 0;
        int duplicateCount = 0;
        int invalidCount = 0;

        for (CompanyImportRow row : preview.getRows()) {
            List<String> validationErrors = validationErrors(row);
            if (!validationErrors.isEmpty()) {
                invalidCount++;
                resultRows.add(toResultRow(
                        row,
                        STATUS_INVALID,
                        validationErrors
                ));
                continue;
            }

            if (fileDuplicateRows.duplicate(row)) {
                duplicateCount++;
                resultRows.add(toResultRow(
                        row,
                        STATUS_DUPLICATE,
                        List.of(fileDuplicateRows.message(row))
                ));
                continue;
            }

            ExistingCompanyMatch companyMatch =
                    findExistingCompany(row, state);
            if (companyMatch.requiresReview()) {
                duplicateCount++;
                resultRows.add(toResultRow(
                        row,
                        STATUS_DUPLICATE,
                        List.of(companyMatch.reviewMessage())
                ));
                continue;
            }
            Optional<Company> existingCompany = companyMatch.company();

            try {
                if (existingCompany.isPresent()) {
                    Company company = existingCompany.get();
                    CompanyLocationMergeClassifier.LocationMergeDecision
                            mergeDecision = locationMergeClassifier.classify(
                            row,
                            company
                    );

                    duplicateCount++;
                    List<String> messages = mergeDecision.update()
                            || !mergeDecision.warnings().isEmpty()
                            ? manualLocationReviewMessages(mergeDecision)
                            : duplicateMessages(row, companyType, List.of());
                    resultRows.add(toResultRow(
                            row,
                            STATUS_DUPLICATE,
                            messages
                    ));
                    continue;
                }

                companyRepository.saveAndFlush(
                        toCompany(row, companyType)
                );
                importedCount++;
                resultRows.add(toResultRow(
                        row,
                        STATUS_IMPORTED,
                        row.getWarnings()
                ));
            } catch (DataIntegrityViolationException exception) {
                invalidCount++;
                resultRows.add(toResultRow(
                        row,
                        STATUS_INVALID,
                        List.of(DATA_INTEGRITY_MESSAGE)
                ));
            }
        }

        return new CompanyImportResultResponse(
                preview.getFileName(),
                preview.getRows().size(),
                importedCount,
                0,
                duplicateCount,
                invalidCount,
                List.copyOf(resultRows)
        );
    }

    private CompanyImportPreview parse(
            MultipartFile file,
            CompanyType companyType
    ) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Excel dosyasi bos olamaz");
        }

        try (InputStream inputStream = file.getInputStream()) {
            return parser.parse(
                    file.getOriginalFilename(),
                    inputStream,
                    companyType
            );
        } catch (IOException exception) {
            throw new InvalidRequestException("Excel dosyasi okunamadi");
        }
    }

    private ImportState importState(
            CompanyType companyType,
            List<CompanyImportRow> rows
    ) {
        Map<String, Company> companiesByName = new LinkedHashMap<>();
        Map<String, List<Company>> externalCompaniesByName =
                new LinkedHashMap<>();
        List<Company> existingCompanies =
                companyRepository.findByCompanyType(companyType);
        for (Company company : existingCompanies) {
            String canonicalCompanyName = canonicalName(company.getName());
            companiesByName.putIfAbsent(
                    canonicalCompanyName,
                    company
            );
            if (CompanyType.EXTERNAL.equals(companyType)) {
                externalCompaniesByName
                        .computeIfAbsent(
                                canonicalCompanyName,
                                ignored -> new ArrayList<>()
                        )
                        .add(company);
            }
        }

        return new ImportState(
                companyType,
                companiesByName,
                immutableCompanyLists(externalCompaniesByName)
        );
    }

    private List<String> validationErrors(CompanyImportRow row) {
        return CompanyFileDuplicateClassifier.validationErrors(row);
    }

    private ExistingCompanyMatch findExistingCompany(
            CompanyImportRow row,
            ImportState state
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

    private List<String> duplicateMessages(
            CompanyImportRow row,
            CompanyType companyType,
            List<String> warnings
    ) {
        if (!warnings.isEmpty()) {
            return warnings;
        }

        if (CompanyType.EXTERNAL.equals(companyType)
                && !row.getCompanyEmails().isEmpty()) {
            return List.of(EXISTING_EMAIL_MESSAGE);
        }

        return List.of(existingNameMessage(companyType));
    }

    private List<String> manualLocationReviewMessages(
            CompanyLocationMergeClassifier.LocationMergeDecision decision
    ) {
        List<String> messages = new ArrayList<>();
        messages.add(LOCATION_MANUAL_REVIEW_MESSAGE);
        messages.addAll(decision.warnings());
        return List.copyOf(messages);
    }

    private String existingNameMessage(CompanyType companyType) {
        return CompanyType.EXTERNAL.equals(companyType)
                ? EXISTING_EXTERNAL_NAME_MESSAGE
                : EXISTING_TGB_NAME_MESSAGE;
    }

    private Company toCompany(CompanyImportRow row, CompanyType companyType) {
        Company company = new Company();
        company.setName(row.getName());
        company.setActivityArea(row.getActivityArea());
        company.setCity(row.getCity());
        company.setDistrict(row.getDistrict());
        company.setOsb(row.getOsb());
        company.setAddress(row.getAddress());
        company.setBuilding(row.getBuilding());
        company.setCompanyType(companyType);
        addContactMethods(company, row);

        if (CompanyType.EXTERNAL.equals(companyType)
                && row.getContactFullName() != null) {
            CompanyContact contact = new CompanyContact();
            contact.setFullName(row.getContactFullName());
            contact.setPosition(row.getContactPosition());
            contact.setPhone(row.getContactPhone());
            contact.setEmail(row.getContactEmail());
            contact.setPrimary(true);
            company.addContact(contact);
        }

        return company;
    }

    private CompanyImportResultRowResponse toResultRow(
            CompanyImportRow row,
            String status,
            List<String> messages
    ) {
        return new CompanyImportResultRowResponse(
                row.getSheetName(),
                row.getRowNumber(),
                row.getName(),
                row.getActivityArea(),
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
                status,
                List.copyOf(messages)
        );
    }

    private void addContactMethods(Company company, CompanyImportRow row) {
        for (int index = 0; index < row.getCompanyPhones().size(); index++) {
            CompanyContactMethod contactMethod = new CompanyContactMethod();
            contactMethod.setType(CompanyContactMethodType.PHONE);
            contactMethod.setValue(row.getCompanyPhones().get(index));
            contactMethod.setPrimary(index == 0);
            company.addContactMethod(contactMethod);
        }

        for (int index = 0; index < row.getCompanyEmails().size(); index++) {
            CompanyContactMethod contactMethod = new CompanyContactMethod();
            contactMethod.setType(CompanyContactMethodType.EMAIL);
            contactMethod.setValue(row.getCompanyEmails().get(index));
            contactMethod.setPrimary(index == 0);
            company.addContactMethod(contactMethod);
        }
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

    private record ImportState(
            CompanyType companyType,
            Map<String, Company> companiesByName,
            Map<String, List<Company>> externalCompaniesByName
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
