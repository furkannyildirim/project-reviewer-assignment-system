package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;
import java.util.Set;

public record AcademicianImportPreviewResponse(
        String fileName,
        int sheetCount,
        int totalDetectedRows,
        int newRowCount,
        int updateRowCount,
        int duplicateRowCount,
        int conflictRowCount,
        int validRowCount,
        int invalidRowCount,
        int warningCount,
        Set<String> detectedHeaders,
        Set<String> ignoredHeaders,
        List<String> generalWarnings,
        List<String> generalInformationMessages,
        List<AcademicianImportPreviewRowResponse> rows
) {
}
