package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportResultResponse;
import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportResultRowResponse;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianExcelImportParser;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportPreview;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportRow;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportClassifier.AcademicianImportDecision;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportClassifier.AcademicianImportPlan;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportClassifier.ImportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicianImportService {

    private static final String FAILED_DEFAULT_MESSAGE =
            "Kayit veritabanina yazilirken hata olustu. "
                    + "Diger kayitlarin aktarimina devam edildi.";
    private static final String FAILED_OPTIMISTIC_LOCKING_MESSAGE =
            "Kayit baska bir islem tarafindan guncellendi. "
                    + "Bu kayit aktarilmadi.";

    private final AcademicianExcelImportParser parser;
    private final AcademicianImportClassifier classifier;
    private final AcademicianImportPersistenceService persistenceService;

    public AcademicianImportResultResponse importCreateOnly(
            MultipartFile file
    ) {
        AcademicianImportPreview preview = parse(file);
        AcademicianImportPlan plan = classifier.classify(preview);

        List<AcademicianImportResultRowResponse> createdRows =
                new ArrayList<>();
        List<AcademicianImportResultRowResponse> updatedRows =
                new ArrayList<>();
        List<AcademicianImportResultRowResponse> duplicateRows =
                new ArrayList<>();
        List<AcademicianImportResultRowResponse> conflictRows =
                new ArrayList<>();
        List<AcademicianImportResultRowResponse> invalidRows =
                new ArrayList<>();
        List<AcademicianImportResultRowResponse> failedRows =
                new ArrayList<>();

        for (AcademicianImportDecision decision : plan.decisions()) {
            if (decision.status() == ImportStatus.NEW) {
                persistNew(decision, createdRows, failedRows);
            } else if (decision.status() == ImportStatus.UPDATE) {
                persistUpdate(decision, updatedRows, failedRows);
            } else if (decision.status() == ImportStatus.DUPLICATE) {
                duplicateRows.add(toResultRow(
                        decision,
                        "DUPLICATE",
                        decision.messages()
                ));
            } else if (decision.status() == ImportStatus.CONFLICT) {
                conflictRows.add(toResultRow(
                        decision,
                        "CONFLICT",
                        decision.messages()
                ));
            } else if (decision.status() == ImportStatus.INVALID) {
                invalidRows.add(toResultRow(
                        decision,
                        "INVALID",
                        decision.messages()
                ));
            }
        }

        int warningCount = preview.getGeneralWarnings().size()
                + plan.decisions().stream()
                .map(AcademicianImportDecision::row)
                .mapToInt(row -> row.getWarnings().size())
                .sum();

        return new AcademicianImportResultResponse(
                preview.getFileName(),
                preview.getRows().size(),
                createdRows.size(),
                updatedRows.size(),
                duplicateRows.size(),
                conflictRows.size(),
                invalidRows.size(),
                failedRows.size(),
                warningCount,
                List.copyOf(createdRows),
                List.copyOf(updatedRows),
                List.copyOf(duplicateRows),
                List.copyOf(conflictRows),
                List.copyOf(invalidRows),
                List.copyOf(failedRows),
                List.copyOf(preview.getGeneralWarnings())
        );
    }

    private void persistNew(
            AcademicianImportDecision decision,
            List<AcademicianImportResultRowResponse> createdRows,
            List<AcademicianImportResultRowResponse> failedRows
    ) {
        try {
            persistenceService.create(decision.row());
            createdRows.add(toResultRow(
                    decision,
                    "CREATED",
                    decision.messages()
            ));
        } catch (DataAccessException exception) {
            failedRows.add(toResultRow(
                    decision,
                    "FAILED",
                    List.of(failureMessage(exception))
            ));
        }
    }

    private void persistUpdate(
            AcademicianImportDecision decision,
            List<AcademicianImportResultRowResponse> updatedRows,
            List<AcademicianImportResultRowResponse> failedRows
    ) {
        try {
            persistenceService.update(decision);
            updatedRows.add(toResultRow(
                    decision,
                    "UPDATED",
                    decision.changes()
            ));
        } catch (DataAccessException exception) {
            failedRows.add(toResultRow(
                    decision,
                    "FAILED",
                    List.of(failureMessage(exception))
            ));
        }
    }

    private String failureMessage(DataAccessException exception) {
        if (exception instanceof OptimisticLockingFailureException
                || exception instanceof ObjectOptimisticLockingFailureException) {
            return FAILED_OPTIMISTIC_LOCKING_MESSAGE;
        }

        return FAILED_DEFAULT_MESSAGE;
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

    private AcademicianImportResultRowResponse toResultRow(
            AcademicianImportDecision decision,
            String status,
            List<String> messages
    ) {
        AcademicianImportRow row = decision.row();

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

}
