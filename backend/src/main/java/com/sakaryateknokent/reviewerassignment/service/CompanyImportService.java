package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyImportResultResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyImportResultRowResponse;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.CompanyContact;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.importing.CompanyExcelImportParser;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportPreview;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportRow;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyImportService {

    private static final Locale TURKISH_LOCALE =
            Locale.forLanguageTag("tr");
    private static final String STATUS_IMPORTED = "IMPORTED";
    private static final String STATUS_DUPLICATE = "DUPLICATE";
    private static final String STATUS_INVALID = "INVALID";
    private static final String EXISTING_EMAIL_MESSAGE =
            "Bu e-posta adresine sahip bir sirket zaten mevcut.";
    private static final String EXISTING_TGB_NAME_MESSAGE =
            "Bu sirket adiyla bir TGB sirketi zaten mevcut.";
    private static final String EXISTING_EXTERNAL_NAME_MESSAGE =
            "Bu sirket adiyla bir dis sirket zaten mevcut.";
    private static final String FILE_EMAIL_MESSAGE =
            "Dosya icinde ayni e-posta birden fazla kullanilmis.";
    private static final String PARSER_FILE_EMAIL_MESSAGE =
            "Dosya icinde ayni e-posta birden fazla kullanilmis";
    private static final String FILE_NAME_MESSAGE =
            "Dosya icinde ayni sirket adi birden fazla kullanilmis.";
    private static final String DATA_INTEGRITY_MESSAGE =
            "Kayit sirasinda veri butunlugu hatasi olustu.";

    private final CompanyExcelImportParser parser;
    private final CompanyRepository companyRepository;

    public CompanyImportResultResponse importTgbCompanies(MultipartFile file) {
        return importCompanies(file, CompanyType.TGB);
    }

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
        FileDuplicateKeys fileDuplicateKeys =
                findFileDuplicateKeys(preview.getRows());
        Set<String> existingNames = loadExistingNames(companyType);

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

            DuplicateCheck fileDuplicateCheck =
                    fileDuplicateCheck(row, fileDuplicateKeys);
            if (fileDuplicateCheck.duplicate()) {
                duplicateCount++;
                resultRows.add(toResultRow(
                        row,
                        STATUS_DUPLICATE,
                        List.of(fileDuplicateCheck.message())
                ));
                continue;
            }

            DuplicateCheck databaseDuplicateCheck =
                    databaseDuplicateCheck(row, existingNames, companyType);
            if (databaseDuplicateCheck.duplicate()) {
                duplicateCount++;
                resultRows.add(toResultRow(
                        row,
                        STATUS_DUPLICATE,
                        List.of(databaseDuplicateCheck.message())
                ));
                continue;
            }

            try {
                companyRepository.saveAndFlush(toCompany(row, companyType));
                rememberSavedRow(row, existingNames);
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

    private Set<String> loadExistingNames(CompanyType companyType) {
        Set<String> names = new LinkedHashSet<>();
        for (String name : companyRepository.findNamesByCompanyType(
                companyType
        )) {
            names.add(canonicalName(name));
        }

        return names;
    }

    private FileDuplicateKeys findFileDuplicateKeys(
            List<CompanyImportRow> rows
    ) {
        Map<String, Integer> emailCounts = new LinkedHashMap<>();
        Map<String, Integer> noEmailNameCounts = new LinkedHashMap<>();

        for (CompanyImportRow row : rows) {
            if (!validationErrors(row).isEmpty()) {
                continue;
            }

            if (row.getEmail() != null) {
                increment(emailCounts, canonicalEmail(row.getEmail()));
                continue;
            }

            if (row.getName() != null) {
                increment(noEmailNameCounts, canonicalName(row.getName()));
            }
        }

        return new FileDuplicateKeys(
                duplicateKeys(emailCounts),
                duplicateKeys(noEmailNameCounts)
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
            FileDuplicateKeys duplicateKeys
    ) {
        if (row.getEmail() != null
                && duplicateKeys.emails().contains(canonicalEmail(row.getEmail()))) {
            return DuplicateCheck.duplicate(FILE_EMAIL_MESSAGE);
        }

        if (row.getEmail() == null
                && row.getName() != null
                && duplicateKeys.noEmailNames().contains(
                canonicalName(row.getName())
        )) {
            return DuplicateCheck.duplicate(FILE_NAME_MESSAGE);
        }

        return DuplicateCheck.unique();
    }

    private DuplicateCheck databaseDuplicateCheck(
            CompanyImportRow row,
            Set<String> existingNames,
            CompanyType companyType
    ) {
        if (row.getEmail() != null) {
            String email = canonicalEmail(row.getEmail());
            boolean emailExists = CompanyType.TGB.equals(companyType)
                    && companyRepository.existsByCompanyTypeAndEmailIgnoreCase(
                    CompanyType.TGB,
                    email
            );
            if (emailExists || companyRepository.existsByEmailIgnoreCase(email)) {
                return DuplicateCheck.duplicate(EXISTING_EMAIL_MESSAGE);
            }

            return DuplicateCheck.unique();
        }

        if (existingNames.contains(canonicalName(row.getName()))) {
            return DuplicateCheck.duplicate(existingNameMessage(companyType));
        }

        return DuplicateCheck.unique();
    }

    private String existingNameMessage(CompanyType companyType) {
        return CompanyType.EXTERNAL.equals(companyType)
                ? EXISTING_EXTERNAL_NAME_MESSAGE
                : EXISTING_TGB_NAME_MESSAGE;
    }

    private void rememberSavedRow(
            CompanyImportRow row,
            Set<String> existingNames
    ) {
        if (row.getEmail() == null) {
            existingNames.add(canonicalName(row.getName()));
        }
    }

    private Company toCompany(CompanyImportRow row, CompanyType companyType) {
        Company company = new Company();
        company.setName(row.getName());
        company.setEmail(row.getEmail());
        company.setPhone(row.getPhone());
        company.setSecondaryPhone(row.getSecondaryPhone());
        company.setActivityArea(row.getActivityArea());
        company.setCompanyType(companyType);

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
                row.getPhone(),
                row.getSecondaryPhone(),
                row.getEmail(),
                row.getContactFullName(),
                row.getContactPosition(),
                row.getContactPhone(),
                row.getContactEmail(),
                status,
                List.copyOf(messages)
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
                .toLowerCase(TURKISH_LOCALE);
    }

    private record FileDuplicateKeys(
            Set<String> emails,
            Set<String> noEmailNames
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
