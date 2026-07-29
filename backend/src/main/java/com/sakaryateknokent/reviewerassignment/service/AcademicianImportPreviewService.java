package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportPreviewRowResponse;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianExcelImportParser;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportPreview;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportRow;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicianImportPreviewService {

    private final AcademicianExcelImportParser parser;
    private final AcademicianRepository academicianRepository;

    public AcademicianImportPreviewResponse preview(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException(
                    "Excel dosyasi bos olamaz"
            );
        }

        try (InputStream inputStream = file.getInputStream()) {
            AcademicianImportPreview preview = parser.parse(
                    file.getOriginalFilename(),
                    inputStream
            );
            validateEmailDuplicates(preview.getRows());
            return toResponse(preview);
        } catch (IOException exception) {
            throw new InvalidRequestException(
                    "Excel dosyasi okunamadi"
            );
        }
    }

    private void validateEmailDuplicates(List<AcademicianImportRow> rows) {
        Map<String, Integer> emailCounts = new LinkedHashMap<>();

        for (AcademicianImportRow row : rows) {
            if (row.getEmail() == null) {
                continue;
            }

            emailCounts.merge(row.getEmail(), 1, Integer::sum);
        }

        Set<String> duplicateEmails = new LinkedHashSet<>();
        Set<String> lookupEmails = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : emailCounts.entrySet()) {
            lookupEmails.add(entry.getKey());
            if (entry.getValue() > 1) {
                duplicateEmails.add(entry.getKey());
            }
        }

        Set<String> existingEmails = lookupEmails.isEmpty()
                ? Set.of()
                : academicianRepository.findExistingEmails(lookupEmails);

        for (AcademicianImportRow row : rows) {
            String email = row.getEmail();
            if (email == null) {
                continue;
            }

            if (duplicateEmails.contains(email)) {
                row.getErrors().add(
                        "Dosya icinde ayni e-posta birden fazla kullanilmis"
                );
            }

            if (existingEmails.contains(email)) {
                row.getErrors().add(
                        "Bu e-posta adresiyle kayitli bir akademisyen zaten var"
                );
            }
        }
    }

    private AcademicianImportPreviewResponse toResponse(
            AcademicianImportPreview preview
    ) {
        List<AcademicianImportPreviewRowResponse> rowResponses =
                new ArrayList<>();
        int validRowCount = 0;
        int warningCount = preview.getGeneralWarnings().size();

        for (AcademicianImportRow row : preview.getRows()) {
            if (row.isValid()) {
                validRowCount++;
            }
            warningCount += row.getWarnings().size();

            rowResponses.add(new AcademicianImportPreviewRowResponse(
                    row.getSheetName(),
                    row.getRowNumber(),
                    row.getFirstName(),
                    row.getLastName(),
                    row.getEmail(),
                    row.getPhone(),
                    row.getTitle(),
                    Collections.unmodifiableSet(
                            new LinkedHashSet<>(row.getKeywords())
                    ),
                    row.isValid(),
                    List.copyOf(row.getWarnings()),
                    List.copyOf(row.getErrors())
            ));
        }

        return new AcademicianImportPreviewResponse(
                preview.getFileName(),
                preview.getSheetCount(),
                preview.getRows().size(),
                validRowCount,
                preview.getRows().size() - validRowCount,
                warningCount,
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(preview.getDetectedHeaders())
                ),
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(preview.getIgnoredHeaders())
                ),
                List.copyOf(preview.getGeneralWarnings()),
                rowResponses
        );
    }
}
