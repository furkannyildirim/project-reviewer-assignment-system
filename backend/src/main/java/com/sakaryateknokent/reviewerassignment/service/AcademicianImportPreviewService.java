package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportPreviewRowResponse;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianExcelImportParser;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportPreview;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportClassifier.AcademicianImportDecision;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportClassifier.AcademicianImportPlan;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportClassifier.ImportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicianImportPreviewService {

    private final AcademicianExcelImportParser parser;
    private final AcademicianImportClassifier classifier;

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
            return toResponse(classifier.classify(preview));
        } catch (IOException exception) {
            throw new InvalidRequestException(
                    "Excel dosyasi okunamadi"
            );
        }
    }

    private AcademicianImportPreviewResponse toResponse(
            AcademicianImportPlan plan
    ) {
        List<AcademicianImportPreviewRowResponse> rowResponses =
                new ArrayList<>();
        AcademicianImportPreview preview = plan.preview();
        int newRowCount = 0;
        int updateRowCount = 0;
        int duplicateRowCount = 0;
        int conflictRowCount = 0;
        int invalidRowCount = 0;
        int warningCount = preview.getGeneralWarnings().size();

        for (AcademicianImportDecision decision : plan.decisions()) {
            ImportStatus status = decision.status();
            if (status == ImportStatus.NEW) {
                newRowCount++;
            } else if (status == ImportStatus.UPDATE) {
                updateRowCount++;
            } else if (status == ImportStatus.DUPLICATE) {
                duplicateRowCount++;
            } else if (status == ImportStatus.CONFLICT) {
                conflictRowCount++;
            } else if (status == ImportStatus.INVALID) {
                invalidRowCount++;
            }

            var row = decision.row();
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
                    status.name(),
                    status != ImportStatus.INVALID
                            && status != ImportStatus.CONFLICT,
                    status == ImportStatus.INVALID
                            ? List.of()
                            : List.copyOf(decision.messages()),
                    List.copyOf(row.getWarnings()),
                    List.copyOf(row.getErrors())
            ));
        }

        return new AcademicianImportPreviewResponse(
                preview.getFileName(),
                preview.getSheetCount(),
                preview.getRows().size(),
                newRowCount,
                updateRowCount,
                duplicateRowCount,
                conflictRowCount,
                newRowCount + updateRowCount + duplicateRowCount,
                invalidRowCount,
                warningCount,
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(preview.getDetectedHeaders())
                ),
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(preview.getIgnoredHeaders())
                ),
                List.copyOf(preview.getGeneralWarnings()),
                List.copyOf(preview.getGeneralInformationMessages()),
                rowResponses
        );
    }
}
