package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportPreview;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportRow;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AcademicianImportClassifier {

    private static final String MANUAL_REVIEW_MESSAGE =
            "Mevcut akademisyen için farklı veya ek bilgiler bulundu. "
                    + "Otomatik güncelleme yapılmadı; manuel kontrol edin.";

    private final AcademicianRepository academicianRepository;

    @Transactional(readOnly = true)
    public AcademicianImportPlan classify(AcademicianImportPreview preview) {
        List<AcademicianImportDecision> decisions = new ArrayList<>();
        List<AcademicianImportRow> validRows = new ArrayList<>();

        for (AcademicianImportRow row : preview.getRows()) {
            if (row.isValid()) {
                validRows.add(row);
            } else {
                decisions.add(new AcademicianImportDecision(
                        row,
                        ImportStatus.INVALID,
                        null,
                        List.copyOf(row.getErrors()),
                        List.of(),
                        Set.of()
                ));
            }
        }

        for (LogicalImportRow logicalRow : analyzeSameFile(validRows)) {
            if (!logicalRow.conflictMessages().isEmpty()) {
                decisions.add(logicalRow.toDecision(
                        ImportStatus.CONFLICT,
                        null,
                        logicalRow.conflictMessages()
                ));
                continue;
            }

            decisions.add(classifyAgainstDatabase(logicalRow));
        }

        return new AcademicianImportPlan(preview, List.copyOf(decisions));
    }

    private List<LogicalImportRow> analyzeSameFile(
            List<AcademicianImportRow> validRows
    ) {
        Map<SameFileKey, SameFileGroup> groups = new LinkedHashMap<>();

        for (AcademicianImportRow row : validRows) {
            CanonicalIdentity identity = canonicalIdentity(row);
            SameFileKey key = SameFileKey.of(row.getEmail(), identity);
            groups.computeIfAbsent(key, SameFileGroup::new)
                    .add(row, identity);
        }

        Map<CanonicalIdentity, List<SameFileGroup>> groupsByIdentity =
                new LinkedHashMap<>();
        for (SameFileGroup group : groups.values()) {
            for (CanonicalIdentity identity : group.identities()) {
                groupsByIdentity
                        .computeIfAbsent(identity, ignored -> new ArrayList<>())
                        .add(group);
            }
        }

        for (List<SameFileGroup> identityGroups : groupsByIdentity.values()) {
            boolean hasEmail = identityGroups.stream()
                    .anyMatch(SameFileGroup::hasEmail);
            boolean hasNoEmail = identityGroups.stream()
                    .anyMatch(group -> !group.hasEmail());

            if (hasEmail && hasNoEmail) {
                identityGroups.forEach(group -> group.addConflict(
                        "Dosya içinde aynı canonical kimlik için e-posta yalnız "
                                + "bazı satırlarda bulundu; manuel kontrol edin."
                ));
            }
        }

        return groups.values().stream()
                .map(SameFileGroup::toLogicalRow)
                .toList();
    }

    private AcademicianImportDecision classifyAgainstDatabase(
            LogicalImportRow logicalRow
    ) {
        AcademicianImportRow row = logicalRow.row();
        String incomingEmail = row.getEmail();
        List<Academician> emailMatches = incomingEmail == null
                ? List.of()
                : nullSafe(academicianRepository.findByNormalizedEmail(
                        incomingEmail
                ));
        List<Academician> identityMatches = nullSafe(
                academicianRepository.findByCanonicalIdentity(
                        logicalRow.identity().firstName(),
                        logicalRow.identity().lastName(),
                        logicalRow.identity().title()
                )
        );

        Set<Academician> candidates = new LinkedHashSet<>(emailMatches);
        for (Academician identityMatch : identityMatches) {
            String existingEmail = AcademicianTextNormalizer.normalizeEmail(
                    identityMatch.getEmail()
            );
            if (incomingEmail == null
                    || existingEmail == null
                    || incomingEmail.equals(existingEmail)) {
                candidates.add(identityMatch);
            }
        }

        if (candidates.isEmpty()) {
            return logicalRow.toDecision(
                    ImportStatus.NEW,
                    null,
                    List.of("Yeni akademisyen oluşturulacak.")
            );
        }

        if (candidates.size() > 1) {
            return logicalRow.toDecision(
                    ImportStatus.CONFLICT,
                    null,
                    List.of(
                            MANUAL_REVIEW_MESSAGE,
                            "Birden fazla olası mevcut akademisyen bulundu."
                    )
            );
        }

        Academician existing = candidates.iterator().next();
        CanonicalIdentity existingIdentity = canonicalIdentity(existing);
        String existingEmail = AcademicianTextNormalizer.normalizeEmail(
                existing.getEmail()
        );

        if (incomingEmail != null && existingEmail != null) {
            if (!incomingEmail.equals(existingEmail)) {
                return logicalRow.toDecision(
                        ImportStatus.NEW,
                        null,
                        List.of("Yeni akademisyen oluşturulacak.")
                );
            }

            if (!logicalRow.identity().equals(existingIdentity)) {
                return logicalRow.toDecision(
                        ImportStatus.CONFLICT,
                        existing,
                        List.of(
                                MANUAL_REVIEW_MESSAGE,
                                "Aynı e-posta mevcut ancak canonical ad, soyad "
                                        + "veya ünvan bilgileri eşleşmiyor."
                        )
                );
            }

            return evaluateExistingInformation(logicalRow, existing);
        }

        if (incomingEmail != null || existingEmail != null) {
            return logicalRow.toDecision(
                    ImportStatus.CONFLICT,
                    existing,
                    List.of(
                            MANUAL_REVIEW_MESSAGE,
                            "Canonical kimlik eşleşiyor ancak e-posta yalnız "
                                    + "bir kayıtta mevcut."
                    )
            );
        }

        return evaluateExistingInformation(logicalRow, existing);
    }

    private AcademicianImportDecision evaluateExistingInformation(
            LogicalImportRow logicalRow,
            Academician existing
    ) {
        AcademicianImportRow row = logicalRow.row();
        List<String> differences = new ArrayList<>();

        String existingPhone = AcademicianTextNormalizer.normalizeOptionalText(
                existing.getPhone()
        );
        String incomingPhone = AcademicianTextNormalizer.normalizeOptionalText(
                row.getPhone()
        );
        if (incomingPhone != null && existingPhone == null) {
            differences.add("Excel satırında ek telefon bilgisi bulundu.");
        } else if (incomingPhone != null
                && !incomingPhone.equals(existingPhone)) {
            differences.add("Mevcut telefon ile Excel telefonu farklı.");
        }

        Set<String> existingKeywords = canonicalKeywords(
                existing.getKeywords()
        );
        long newKeywordCount = canonicalKeywords(row.getKeywords()).stream()
                .filter(keyword -> !existingKeywords.contains(keyword))
                .count();
        if (newKeywordCount > 0) {
            differences.add(
                    "Excel satırında " + newKeywordCount
                            + " ek anahtar kelime bulundu."
            );
        }

        if (differences.isEmpty()) {
            return logicalRow.toDecision(
                    ImportStatus.DUPLICATE,
                    existing,
                    List.of("Mevcut akademisyenle aynı; kayıt atlandı.")
            );
        }

        List<String> messages = new ArrayList<>();
        messages.add(MANUAL_REVIEW_MESSAGE);
        messages.addAll(differences);
        return logicalRow.toDecision(
                ImportStatus.CONFLICT,
                existing,
                List.copyOf(messages)
        );
    }

    private Set<String> canonicalKeywords(Set<String> keywords) {
        Set<String> canonicalKeywords = new LinkedHashSet<>();
        for (String keyword : AcademicianTextNormalizer.normalizeKeywords(
                keywords
        )) {
            canonicalKeywords.add(
                    AcademicianTextNormalizer.canonicalizeIdentityPart(keyword)
            );
        }
        return canonicalKeywords;
    }

    private List<Academician> nullSafe(List<Academician> academicians) {
        return academicians == null ? List.of() : academicians;
    }

    private CanonicalIdentity canonicalIdentity(AcademicianImportRow row) {
        return new CanonicalIdentity(
                canonicalValue(row.getFirstName()),
                canonicalValue(row.getLastName()),
                canonicalValue(row.getTitle())
        );
    }

    private CanonicalIdentity canonicalIdentity(Academician academician) {
        return new CanonicalIdentity(
                canonicalValue(academician.getFirstName()),
                canonicalValue(academician.getLastName()),
                canonicalValue(academician.getTitle())
        );
    }

    private String canonicalValue(String value) {
        return AcademicianTextNormalizer.canonicalizeIdentityPart(value);
    }

    public enum ImportStatus {
        NEW,
        DUPLICATE,
        CONFLICT,
        INVALID
    }

    public record AcademicianImportPlan(
            AcademicianImportPreview preview,
            List<AcademicianImportDecision> decisions
    ) {
    }

    public record AcademicianImportDecision(
            AcademicianImportRow row,
            ImportStatus status,
            Academician existingAcademician,
            List<String> messages,
            List<String> changes,
            Set<String> mergedKeywords
    ) {
    }

    private record CanonicalIdentity(
            String firstName,
            String lastName,
            String title
    ) {
    }

    private record SameFileKey(
            String email,
            CanonicalIdentity noEmailIdentity
    ) {

        private static SameFileKey of(
                String email,
                CanonicalIdentity identity
        ) {
            return email == null
                    ? new SameFileKey(null, identity)
                    : new SameFileKey(email, null);
        }
    }

    private record LogicalImportRow(
            CanonicalIdentity identity,
            AcademicianImportRow row,
            List<String> conflictMessages
    ) {

        private AcademicianImportDecision toDecision(
                ImportStatus status,
                Academician existingAcademician,
                List<String> messages
        ) {
            return new AcademicianImportDecision(
                    row,
                    status,
                    existingAcademician,
                    List.copyOf(messages),
                    List.of(),
                    Set.of()
            );
        }
    }

    private static class SameFileGroup {

        private final SameFileKey key;
        private final List<AcademicianImportRow> rows = new ArrayList<>();
        private final Set<CanonicalIdentity> identities =
                new LinkedHashSet<>();
        private final Set<String> phones = new LinkedHashSet<>();
        private final Set<String> keywords = new LinkedHashSet<>();
        private final Set<String> conflicts = new LinkedHashSet<>();

        private SameFileGroup(SameFileKey key) {
            this.key = key;
        }

        private void add(
                AcademicianImportRow row,
                CanonicalIdentity identity
        ) {
            rows.add(row);
            identities.add(identity);
            if (row.getPhone() != null) {
                phones.add(row.getPhone());
            }
            keywords.addAll(AcademicianTextNormalizer.normalizeKeywords(
                    row.getKeywords()
            ));
        }

        private boolean hasEmail() {
            return key.email() != null;
        }

        private Set<CanonicalIdentity> identities() {
            return identities;
        }

        private void addConflict(String message) {
            conflicts.add(message);
        }

        private LogicalImportRow toLogicalRow() {
            if (identities.size() > 1) {
                conflicts.add(
                        "Dosya içinde aynı e-posta farklı canonical "
                                + "kimliklerle kullanılmış; manuel kontrol edin."
                );
            }
            if (phones.size() > 1) {
                conflicts.add(
                        "Dosya içinde aynı kişi için farklı telefon "
                                + "değerleri bulundu; manuel kontrol edin."
                );
            }

            AcademicianImportRow first = rows.getFirst();
            AcademicianImportRow row = new AcademicianImportRow(
                    first.getSheetName(),
                    first.getRowNumber()
            );
            row.setFirstName(first.getFirstName());
            row.setLastName(first.getLastName());
            row.setTitle(first.getTitle());
            row.setEmail(key.email());
            row.setPhone(phones.stream().findFirst().orElse(null));
            row.setKeywords(new LinkedHashSet<>(keywords));

            for (AcademicianImportRow sourceRow : rows) {
                row.getWarnings().addAll(sourceRow.getWarnings());
            }

            return new LogicalImportRow(
                    identities.iterator().next(),
                    row,
                    List.copyOf(conflicts)
            );
        }
    }
}
