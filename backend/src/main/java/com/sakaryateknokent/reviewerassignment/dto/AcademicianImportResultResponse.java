package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record AcademicianImportResultResponse(
        String fileName,
        int totalDetectedRows,
        int createdCount,
        int skippedDuplicateCount,
        int skippedInvalidCount,
        int warningCount,
        List<AcademicianImportResultRowResponse> createdRows,
        List<AcademicianImportResultRowResponse> skippedRows,
        List<AcademicianImportResultRowResponse> invalidRows,
        List<String> generalWarnings
) {
}
