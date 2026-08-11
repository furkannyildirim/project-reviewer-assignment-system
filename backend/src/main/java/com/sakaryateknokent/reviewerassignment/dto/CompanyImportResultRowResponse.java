package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record CompanyImportResultRowResponse(
        String sheetName,
        int rowNumber,
        String name,
        String activityArea,
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
        String status,
        List<String> messages
) {
}
