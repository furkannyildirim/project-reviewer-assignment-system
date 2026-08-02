package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record CompanyImportPreviewResponse(
        String fileName,
        int sheetCount,
        int totalRowCount,
        int validRowCount,
        int invalidRowCount,
        int warningCount,
        List<String> detectedHeaders,
        List<String> ignoredHeaders,
        List<String> generalWarnings,
        List<String> generalInformationMessages,
        List<CompanyImportPreviewRowResponse> rows
) {
}
