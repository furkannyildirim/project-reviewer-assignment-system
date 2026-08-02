package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record CompanyImportResultResponse(
        String fileName,
        int totalRowCount,
        int importedCount,
        int duplicateCount,
        int invalidCount,
        List<CompanyImportResultRowResponse> rows
) {
}
