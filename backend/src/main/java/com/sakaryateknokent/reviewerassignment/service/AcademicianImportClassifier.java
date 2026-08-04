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

        SameFileAnalysis sameFileAnalysis = analyzeSameFile(validRows);
        for (LogicalImportRow logicalRow : sameFileAnalysis.rows()) {
            List<String> conflictMessages =
                    sameFileAnalysis.conflictsByIdentity()
                            .get(logicalRow.identity());
            if (conflictMessages != null && !conflictMessages.isEmpty()) {
                decisions.add(logicalRow.toDecision(
                        ImportStatus.CONFLICT,
                        null,
                        conflictMessages,
                        List.of(),
                        Set.of()
                ));
                continue;
            }

            decisions.add(classifyAgainstDatabase(logicalRow));
        }

        return new AcademicianImportPlan(
                preview,
                List.copyOf(decisions)
        );
    }

    private SameFileAnalysis analyzeSameFile(
            List<AcademicianImportRow> validRows
    ) {
        Map<CanonicalIdentity, SameFileGroup> groups = new LinkedHashMap<>();
        Map<String, Set<CanonicalIdentity>> identitiesByEmail =
                new LinkedHashMap<>();

        for (AcademicianImportRow row : validRows) {
            CanonicalIdentity identity = canonicalIdentity(row);
            groups.computeIfAbsent(identity, SameFileGroup::new).add(row);

            if (row.getEmail() != null) {
                identitiesByEmail
                        .computeIfAbsent(row.getEmail(), ignored ->
                                new LinkedHashSet<>())
                        .add(identity);
            }
        }

        Map<CanonicalIdentity, List<String>> conflictsByIdentity =
                new LinkedHashMap<>();
        for (SameFileGroup group : groups.values()) {
            group.conflictMessages().forEach(message ->
                    addConflict(conflictsByIdentity, group.identity(), message));
        }

        for (Map.Entry<String, Set<CanonicalIdentity>> entry
                : identitiesByEmail.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }

            for (CanonicalIdentity identity : entry.getValue()) {
                addConflict(
                        conflictsByIdentity,
                        identity,
                        "Dosya icinde ayni e-posta farkli akademisyen bilgileriyle kullanilmis: "
                                + entry.getKey()
                );
            }
        }

        List<LogicalImportRow> logicalRows = groups.values().stream()
                .map(SameFileGroup::toLogicalRow)
                .toList();

        return new SameFileAnalysis(
                logicalRows,
                Collections.unmodifiableMap(conflictsByIdentity)
        );
    }

    private AcademicianImportDecision classifyAgainstDatabase(
            LogicalImportRow logicalRow
    ) {
        AcademicianImportRow row = logicalRow.row();

        if (row.getEmail() != null) {
            List<Academician> emailMatches =
                    nullSafe(academicianRepository.findByNormalizedEmail(
                            row.getEmail()
                    ));
            if (emailMatches.size() == 1) {
                Academician existing = emailMatches.getFirst();
                if (!canonicalIdentity(existing).equals(logicalRow.identity())) {
                    return logicalRow.toDecision(
                            ImportStatus.CONFLICT,
                            existing,
                            List.of(
                                    "Ayni e-posta mevcut ancak ad, soyad veya unvan bilgileri eslesmiyor."
                            ),
                            List.of(),
                            Set.of()
                    );
                }

                return evaluateMerge(logicalRow, existing);
            }

            if (emailMatches.size() > 1) {
                return logicalRow.toDecision(
                        ImportStatus.CONFLICT,
                        null,
                        List.of("Ayni e-posta ile birden fazla mevcut kayit bulundu."),
                        List.of(),
                        Set.of()
                );
            }
        }

        List<Academician> identityMatches =
                nullSafe(academicianRepository.findByCanonicalIdentity(
                        logicalRow.identity().firstName(),
                        logicalRow.identity().lastName(),
                        logicalRow.identity().title()
                ));
        if (identityMatches.isEmpty()) {
            return logicalRow.toDecision(
                    ImportStatus.NEW,
                    null,
                    List.of("Yeni akademisyen olusturulacak."),
                    List.of(),
                    Set.of()
            );
        }

        if (identityMatches.size() > 1) {
            return logicalRow.toDecision(
                    ImportStatus.CONFLICT,
                    null,
                    List.of(
                            "Ayni ad, soyad ve unvan ile birden fazla mevcut kayit bulundu."
                    ),
                    List.of(),
                    Set.of()
            );
        }

        return evaluateMerge(logicalRow, identityMatches.getFirst());
    }

    private AcademicianImportDecision evaluateMerge(
            LogicalImportRow logicalRow,
            Academician existing
    ) {
        AcademicianImportRow row = logicalRow.row();
        List<String> conflicts = new ArrayList<>();
        List<String> changes = new ArrayList<>();

        String existingEmail = AcademicianTextNormalizer.normalizeEmail(
                existing.getEmail()
        );
        String incomingEmail = row.getEmail();
        if (existingEmail != null
                && incomingEmail != null
                && !existingEmail.equals(incomingEmail)) {
            conflicts.add(
                    "Mevcut e-posta ile Excel e-postasi farkli. Mevcut: "
                            + existingEmail + ", Excel: " + incomingEmail
            );
        } else if (existingEmail == null && incomingEmail != null) {
            changes.add("E-posta eklenecek");
        }

        String existingPhone = AcademicianTextNormalizer.normalizeOptionalText(
                existing.getPhone()
        );
        String incomingPhone = row.getPhone();
        if (existingPhone != null
                && incomingPhone != null
                && !existingPhone.equals(incomingPhone)) {
            conflicts.add(
                    "Mevcut telefon ile Excel telefonu farkli. Mevcut: "
                            + existingPhone + ", Excel: " + incomingPhone
            );
        } else if (existingPhone == null && incomingPhone != null) {
            changes.add("Telefon eklenecek");
        }

        Set<String> mergedKeywords = mergedKeywords(existing, row);
        Set<String> existingKeywords = normalizedKeywords(existing);
        int newKeywordCount = mergedKeywords.size() - existingKeywords.size();
        if (newKeywordCount > 0) {
            changes.add(newKeywordCount + " anahtar kelime eklenecek");
        }

        if (!conflicts.isEmpty()) {
            return logicalRow.toDecision(
                    ImportStatus.CONFLICT,
                    existing,
                    conflicts,
                    List.of(),
                    Set.of()
            );
        }

        if (changes.isEmpty()) {
            return logicalRow.toDecision(
                    ImportStatus.DUPLICATE,
                    existing,
                    List.of(
                            "Mevcut kayitla ayni, guncellenecek yeni bilgi bulunamadi."
                    ),
                    List.of(),
                    mergedKeywords
            );
        }

        return logicalRow.toDecision(
                ImportStatus.UPDATE,
                existing,
                List.copyOf(changes),
                List.copyOf(changes),
                mergedKeywords
        );
    }

    private Set<String> mergedKeywords(
            Academician existing,
            AcademicianImportRow row
    ) {
        Set<String> keywords = new LinkedHashSet<>(normalizedKeywords(existing));
        keywords.addAll(AcademicianTextNormalizer.normalizeKeywords(
                row.getKeywords()
        ));
        return keywords;
    }

    private Set<String> normalizedKeywords(Academician existing) {
        return AcademicianTextNormalizer.normalizeKeywords(
                existing.getKeywords()
        );
    }

    private List<Academician> nullSafe(List<Academician> academicians) {
        return academicians == null ? List.of() : academicians;
    }

    private void addConflict(
            Map<CanonicalIdentity, List<String>> conflictsByIdentity,
            CanonicalIdentity identity,
            String message
    ) {
        conflictsByIdentity
                .computeIfAbsent(identity, ignored -> new ArrayList<>())
                .add(message);
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
        if (value == null || value.isBlank()) {
            return "";
        }

        return AcademicianTextNormalizer.canonicalizeIdentityPart(value);
    }

    public enum ImportStatus {
        NEW,
        UPDATE,
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

    private record SameFileAnalysis(
            List<LogicalImportRow> rows,
            Map<CanonicalIdentity, List<String>> conflictsByIdentity
    ) {
    }

    private record LogicalImportRow(
            CanonicalIdentity identity,
            AcademicianImportRow row
    ) {

        private AcademicianImportDecision toDecision(
                ImportStatus status,
                Academician existingAcademician,
                List<String> messages,
                List<String> changes,
                Set<String> mergedKeywords
        ) {
            return new AcademicianImportDecision(
                    row,
                    status,
                    existingAcademician,
                    List.copyOf(messages),
                    List.copyOf(changes),
                    Collections.unmodifiableSet(
                            new LinkedHashSet<>(mergedKeywords)
                    )
            );
        }
    }

    private class SameFileGroup {

        private final CanonicalIdentity identity;
        private final List<AcademicianImportRow> rows = new ArrayList<>();
        private final Set<String> emails = new LinkedHashSet<>();
        private final Set<String> phones = new LinkedHashSet<>();
        private final Set<String> keywords = new LinkedHashSet<>();

        private SameFileGroup(CanonicalIdentity identity) {
            this.identity = identity;
        }

        private void add(AcademicianImportRow row) {
            rows.add(row);
            if (row.getEmail() != null) {
                emails.add(row.getEmail());
            }
            if (row.getPhone() != null) {
                phones.add(row.getPhone());
            }
            keywords.addAll(AcademicianTextNormalizer.normalizeKeywords(
                    row.getKeywords()
            ));
        }

        private CanonicalIdentity identity() {
            return identity;
        }

        private List<String> conflictMessages() {
            List<String> messages = new ArrayList<>();
            if (emails.size() > 1) {
                messages.add(
                        "Dosya icinde ayni kisi icin farkli e-posta degerleri bulundu."
                );
            }
            if (phones.size() > 1) {
                messages.add(
                        "Dosya icinde ayni kisi icin farkli telefon degerleri bulundu."
                );
            }
            return messages;
        }

        private LogicalImportRow toLogicalRow() {
            AcademicianImportRow first = rows.getFirst();
            AcademicianImportRow row = new AcademicianImportRow(
                    first.getSheetName(),
                    first.getRowNumber()
            );
            row.setFirstName(first.getFirstName());
            row.setLastName(first.getLastName());
            row.setTitle(first.getTitle());
            row.setEmail(emails.stream().findFirst().orElse(null));
            row.setPhone(phones.stream().findFirst().orElse(null));
            row.setKeywords(new LinkedHashSet<>(keywords));

            for (AcademicianImportRow sourceRow : rows) {
                row.getWarnings().addAll(sourceRow.getWarnings());
            }

            return new LogicalImportRow(identity, row);
        }
    }
}
