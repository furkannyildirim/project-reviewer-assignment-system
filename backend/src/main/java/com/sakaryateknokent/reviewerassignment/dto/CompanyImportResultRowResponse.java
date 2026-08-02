package com.sakaryateknokent.reviewerassignment.dto;

import java.util.List;

public record CompanyImportResultRowResponse(
        String sheetName,
        int rowNumber,
        String name,
        String activityArea,
        String phone,
        String secondaryPhone,
        String email,
        String contactFullName,
        String contactPosition,
        String contactPhone,
        String contactEmail,
        String status,
        List<String> messages
) {
}
