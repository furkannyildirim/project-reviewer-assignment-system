package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record AcademicianImportResultRowResponse(
        String sheetName,
        int rowNumber,
        String firstName,
        String lastName,
        String email,
        String title,
        String status,
        List<String> messages
) {
}
