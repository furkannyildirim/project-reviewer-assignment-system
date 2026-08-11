package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record AcademicianImportResultResponse(
        String fileName,
        int totalDetectedRows,
        int createdCount,
        int updatedCount,
        int duplicateCount,
        int conflictCount,
        int invalidCount,
        int failedCount,
        int warningCount,
        List<AcademicianImportResultRowResponse> createdRows,
        List<AcademicianImportResultRowResponse> updatedRows,
        List<AcademicianImportResultRowResponse> duplicateRows,
        List<AcademicianImportResultRowResponse> conflictRows,
        List<AcademicianImportResultRowResponse> invalidRows,
        List<AcademicianImportResultRowResponse> failedRows,
        List<String> generalWarnings
) {
}
