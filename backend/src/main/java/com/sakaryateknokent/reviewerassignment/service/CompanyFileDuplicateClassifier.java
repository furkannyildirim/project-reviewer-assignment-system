package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportRow;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CompanyFileDuplicateClassifier {

    private static final String FILE_NAME_MESSAGE =
            "Dosya icinde ayni sirket adi birden fazla kullanilmis.";
    private static final String PARSER_FILE_EMAIL_MESSAGE =
            "Dosya icinde ayni e-posta birden fazla kullanilmis";

    private CompanyFileDuplicateClassifier() {
    }

    static FileDuplicateRows findDuplicates(
            List<CompanyImportRow> rows,
            CompanyType companyType
    ) {
        if (CompanyType.EXTERNAL.equals(companyType)) {
            return findExternalDuplicates(rows);
        }

        return findTgbDuplicates(rows);
    }

    static List<String> validationErrors(CompanyImportRow row) {
        return row.getErrors()
                .stream()
                .filter(error -> !PARSER_FILE_EMAIL_MESSAGE.equals(error))
                .toList();
    }

    private static FileDuplicateRows findTgbDuplicates(
            List<CompanyImportRow> rows
    ) {
        Map<String, Integer> nameCounts = new LinkedHashMap<>();

        for (CompanyImportRow row : rows) {
            if (!validationErrors(row).isEmpty()) {
                continue;
            }

            if (row.getName() != null) {
                increment(nameCounts, canonicalName(row.getName()));
            }
        }

        Set<String> duplicateNames = duplicateKeys(nameCounts);
        Map<CompanyImportRow, String> duplicateMessages =
                new IdentityHashMap<>();

        for (CompanyImportRow row : rows) {
            if (!validationErrors(row).isEmpty()) {
                continue;
            }

            if (row.getName() != null
                    && duplicateNames.contains(
                    canonicalName(row.getName())
            )) {
                duplicateMessages.put(row, FILE_NAME_MESSAGE);
            }
        }

        return new FileDuplicateRows(duplicateMessages);
    }

    private static FileDuplicateRows findExternalDuplicates(
            List<CompanyImportRow> rows
    ) {
        List<CompanyImportRow> validRows = rows.stream()
                .filter(row -> validationErrors(row).isEmpty())
                .toList();
        Map<String, List<CompanyImportRow>> rowsByCanonicalName =
                new LinkedHashMap<>();
        for (CompanyImportRow row : validRows) {
            rowsByCanonicalName
                    .computeIfAbsent(
                            canonicalName(row.getName()),
                            ignored -> new ArrayList<>()
                    )
                    .add(row);
        }
        Map<CompanyImportRow, String> duplicateMessages =
                new IdentityHashMap<>();

        for (List<CompanyImportRow> sameNameRows
                : rowsByCanonicalName.values()) {
            for (int firstIndex = 0;
                    firstIndex < sameNameRows.size();
                    firstIndex++) {
                CompanyImportRow first = sameNameRows.get(firstIndex);
                for (int secondIndex = firstIndex + 1;
                        secondIndex < sameNameRows.size();
                        secondIndex++) {
                    CompanyImportRow second = sameNameRows.get(secondIndex);
                    if (ExternalCompanyIdentityMatcher.matches(first, second)) {
                        duplicateMessages.put(first, FILE_NAME_MESSAGE);
                        duplicateMessages.put(second, FILE_NAME_MESSAGE);
                    }
                }
            }
        }

        return new FileDuplicateRows(duplicateMessages);
    }

    private static void increment(Map<String, Integer> counts, String key) {
        counts.put(key, counts.getOrDefault(key, 0) + 1);
    }

    private static Set<String> duplicateKeys(Map<String, Integer> counts) {
        Set<String> duplicates = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add(entry.getKey());
            }
        }

        return duplicates;
    }

    static String canonicalName(String name) {
        return ExternalCompanyIdentityMatcher.canonicalName(name);
    }

    static String canonicalAddress(String address) {
        return ExternalCompanyIdentityMatcher.canonicalAddress(address);
    }

    static String canonicalNameAddress(String name, String address) {
        return canonicalName(name) + "|" + canonicalAddress(address);
    }

    record FileDuplicateRows(
            Map<CompanyImportRow, String> messagesByRow
    ) {

        boolean duplicate(CompanyImportRow row) {
            return messagesByRow.containsKey(row);
        }

        String message(CompanyImportRow row) {
            return messagesByRow.getOrDefault(row, "");
        }
    }
}
