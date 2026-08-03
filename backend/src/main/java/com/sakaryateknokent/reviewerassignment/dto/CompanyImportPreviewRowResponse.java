package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record CompanyImportPreviewRowResponse(
        String sheetName,
        int rowNumber,
        String name,
        String activityArea,
        List<String> companyPhones,
        List<String> companyEmails,
        String contactFullName,
        String contactPosition,
        String contactPhone,
        String contactEmail,
        List<String> errors,
        List<String> warnings,
        boolean valid
) {
}
