package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record CompanyImportPreviewRowResponse(
        String sheetName,
        int rowNumber,
        String name,
        String activityArea,
        String status,
        String city,
        String district,
        String osb,
        String address,
        String building,
        List<String> companyPhones,
        List<String> companyEmails,
        String contactFullName,
        String contactPosition,
        String contactPhone,
        String contactEmail,
        List<String> updateMessages,
        List<String> locationWarnings,
        List<String> errors,
        List<String> warnings,
        boolean valid
) {
}
