package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record CompanyImportPreviewRowResponse(
        String sheetName,
        int rowNumber,
        String name,
        String activityArea,
        String phone,
        String secondaryPhone,
        String email,
        List<String> errors,
        List<String> warnings,
        boolean valid
) {
}
