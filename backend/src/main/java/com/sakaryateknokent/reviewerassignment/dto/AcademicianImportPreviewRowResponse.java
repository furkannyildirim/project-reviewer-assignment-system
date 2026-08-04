package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;
import java.util.Set;

public record AcademicianImportPreviewRowResponse(
        String sheetName,
        int rowNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        String title,
        Set<String> keywords,
        String status,
        boolean valid,
        List<String> messages,
        List<String> warnings,
        List<String> errors
) {
}
