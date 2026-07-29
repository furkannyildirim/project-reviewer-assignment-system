package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportResultResponse;
import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportResultRowResponse;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianExcelImportParser;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportPreview;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportRow;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianIdentityProjection;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AcademicianImportService {

    private static final Locale TURKISH_LOCALE =
            Locale.forLanguageTag("tr");
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_SKIPPED_DUPLICATE =
            "SKIPPED_DUPLICATE";

    private final AcademicianExcelImportParser parser;
    private final AcademicianRepository academicianRepository;

    @Transactional
    public AcademicianImportResultResponse importCreateOnly(
            MultipartFile file
    ) {
        AcademicianImportPreview preview = parse(file);
        rejectInvalidRows(preview.getRows());

        Set<String> emailLookup = collectEmails(preview.getRows());
        Set<String> existingEmails = emailLookup.isEmpty()
                ? Set.of()
                : academicianRepository.findExistingEmails(emailLookup);
        Set<CanonicalIdentity> existingIdentities =
                loadExistingIdentities(preview.getRows());

        Set<String> seenEmails = new LinkedHashSet<>();
        Set<CanonicalIdentity> seenNoEmailIdentities = new LinkedHashSet<>();
        List<Academician> academiciansToCreate = new ArrayList<>();
        List<AcademicianImportResultRowResponse> createdRows =
                new ArrayList<>();
        List<AcademicianImportResultRowResponse> skippedRows =
                new ArrayList<>();

        for (AcademicianImportRow row : preview.getRows()) {
            DuplicateCheck duplicateCheck = duplicateCheck(
                    row,
                    existingEmails,
                    existingIdentities,
                    seenEmails,
                    seenNoEmailIdentities
            );

            if (duplicateCheck.duplicate()) {
                skippedRows.add(toResultRow(
                        row,
                        STATUS_SKIPPED_DUPLICATE,
                        List.of(duplicateCheck.message())
                ));
                continue;
            }

            seen(row, seenEmails, seenNoEmailIdentities);
            academiciansToCreate.add(toAcademician(row));
            createdRows.add(toResultRow(
                    row,
                    STATUS_CREATED,
                    row.getWarnings()
            ));
        }

        try {
            if (!academiciansToCreate.isEmpty()) {
                academicianRepository.saveAllAndFlush(academiciansToCreate);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException(
                    "Import sirasinda cakisan akademisyen kaydi bulundu. "
                            + "Lutfen dosyayi tekrar kontrol edin."
            );
        }

        int warningCount = preview.getGeneralWarnings().size()
                + preview.getRows().stream()
                .mapToInt(row -> row.getWarnings().size())
                .sum();

        return new AcademicianImportResultResponse(
                preview.getFileName(),
                preview.getRows().size(),
                createdRows.size(),
                skippedRows.size(),
                warningCount,
                List.copyOf(createdRows),
                List.copyOf(skippedRows),
                List.copyOf(preview.getGeneralWarnings())
        );
    }

    private AcademicianImportPreview parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException(
                    "Excel dosyasi bos olamaz"
            );
        }

        try (InputStream inputStream = file.getInputStream()) {
            return parser.parse(file.getOriginalFilename(), inputStream);
        } catch (IOException exception) {
            throw new InvalidRequestException(
                    "Excel dosyasi okunamadi"
            );
        }
    }

    private void rejectInvalidRows(List<AcademicianImportRow> rows) {
        List<String> messages = rows.stream()
                .filter(row -> !row.isValid())
                .flatMap(row -> row.getErrors().stream()
                        .map(error -> "Sayfa '" + row.getSheetName()
                                + "', Excel satiri " + row.getRowNumber()
                                + ": " + error))
                .toList();

        if (!messages.isEmpty()) {
            throw new InvalidRequestException(
                    "Excel dosyasinda hatali satirlar var. "
                            + String.join(" | ", messages)
            );
        }
    }

    private Set<String> collectEmails(List<AcademicianImportRow> rows) {
        Set<String> emails = new LinkedHashSet<>();

        for (AcademicianImportRow row : rows) {
            if (row.getEmail() != null) {
                emails.add(row.getEmail());
            }
        }

        return emails;
    }

    private Set<CanonicalIdentity> loadExistingIdentities(
            List<AcademicianImportRow> rows
    ) {
        boolean hasNoEmailCandidate = rows.stream()
                .anyMatch(row -> row.isValid() && row.getEmail() == null);

        if (!hasNoEmailCandidate) {
            return Set.of();
        }

        Set<CanonicalIdentity> identities = new LinkedHashSet<>();
        for (AcademicianIdentityProjection projection
                : academicianRepository.findIdentities()) {
            identities.add(canonicalIdentity(
                    projection.getFirstName(),
                    projection.getLastName(),
                    projection.getTitle()
            ));
        }

        return identities;
    }

    private DuplicateCheck duplicateCheck(
            AcademicianImportRow row,
            Set<String> existingEmails,
            Set<CanonicalIdentity> existingIdentities,
            Set<String> seenEmails,
            Set<CanonicalIdentity> seenNoEmailIdentities
    ) {
        String email = row.getEmail();
        if (email != null) {
            if (seenEmails.contains(email)) {
                return DuplicateCheck.duplicate(
                        "Dosya icinde ayni e-posta daha once yer aliyor"
                );
            }

            if (existingEmails.contains(email)) {
                return DuplicateCheck.duplicate(
                        "Bu e-posta adresiyle kayitli bir akademisyen zaten var"
                );
            }

            return DuplicateCheck.unique();
        }

        CanonicalIdentity identity = canonicalIdentity(row);
        if (seenNoEmailIdentities.contains(identity)) {
            return DuplicateCheck.duplicate(
                    "Dosya icinde ayni ad, soyad ve unvan daha once yer aliyor"
            );
        }

        if (existingIdentities.contains(identity)) {
            return DuplicateCheck.duplicate(
                    "Bu ad, soyad ve unvan ile kayitli bir akademisyen zaten var"
            );
        }

        return DuplicateCheck.unique();
    }

    private void seen(
            AcademicianImportRow row,
            Set<String> seenEmails,
            Set<CanonicalIdentity> seenNoEmailIdentities
    ) {
        if (row.getEmail() != null) {
            seenEmails.add(row.getEmail());
            return;
        }

        seenNoEmailIdentities.add(canonicalIdentity(row));
    }

    private Academician toAcademician(AcademicianImportRow row) {
        Academician academician = new Academician();
        academician.setFirstName(normalizeRequiredText(row.getFirstName()));
        academician.setLastName(normalizeRequiredText(row.getLastName()));
        academician.setEmail(AcademicianTextNormalizer.normalizeEmail(
                row.getEmail()
        ));
        academician.setPhone(AcademicianTextNormalizer.normalizeOptionalText(
                row.getPhone()
        ));
        academician.setTitle(AcademicianTextNormalizer.normalizeOptionalText(
                row.getTitle()
        ));
        academician.getKeywords().addAll(
                AcademicianTextNormalizer.normalizeKeywords(
                        row.getKeywords()
                )
        );

        return academician;
    }

    private AcademicianImportResultRowResponse toResultRow(
            AcademicianImportRow row,
            String status,
            List<String> messages
    ) {
        return new AcademicianImportResultRowResponse(
                row.getSheetName(),
                row.getRowNumber(),
                row.getFirstName(),
                row.getLastName(),
                row.getEmail(),
                row.getTitle(),
                status,
                List.copyOf(messages)
        );
    }

    private CanonicalIdentity canonicalIdentity(AcademicianImportRow row) {
        return canonicalIdentity(
                row.getFirstName(),
                row.getLastName(),
                row.getTitle()
        );
    }

    private CanonicalIdentity canonicalIdentity(
            String firstName,
            String lastName,
            String title
    ) {
        return new CanonicalIdentity(
                canonicalValue(firstName),
                canonicalValue(lastName),
                canonicalValue(title)
        );
    }

    private String canonicalValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return AcademicianTextNormalizer.normalizeWhitespace(value)
                .toLowerCase(TURKISH_LOCALE);
    }

    private String normalizeRequiredText(String value) {
        return AcademicianTextNormalizer.normalizeWhitespace(value);
    }

    private record CanonicalIdentity(
            String firstName,
            String lastName,
            String title
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
