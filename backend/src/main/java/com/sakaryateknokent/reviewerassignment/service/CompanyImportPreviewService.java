package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewResponse;
import com.sakaryateknokent.reviewerassignment.dto.CompanyImportPreviewRowResponse;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.importing.CompanyExcelImportParser;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportPreview;
import com.sakaryateknokent.reviewerassignment.importing.CompanyImportRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyImportPreviewService {

    private final CompanyExcelImportParser parser;

    public CompanyImportPreviewResponse preview(MultipartFile file) {
        return preview(file, CompanyType.TGB);
    }

    public CompanyImportPreviewResponse preview(
            MultipartFile file,
            CompanyType companyType
    ) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Excel dosyasi bos olamaz");
        }

        try {
            CompanyImportPreview preview = parser.parse(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    companyType
            );
            return toResponse(preview);
        } catch (IOException exception) {
            throw new InvalidRequestException("Excel dosyasi okunamadi");
        }
    }

    private CompanyImportPreviewResponse toResponse(
            CompanyImportPreview preview
    ) {
        List<CompanyImportPreviewRowResponse> rowResponses =
                preview.getRows()
                        .stream()
                        .map(this::toRowResponse)
                        .toList();

        int validRowCount = 0;
        int warningCount = preview.getGeneralWarnings().size();

        for (CompanyImportRow row : preview.getRows()) {
            if (row.isValid()) {
                validRowCount++;
            }
            warningCount += row.getWarnings().size();
        }

        return new CompanyImportPreviewResponse(
                preview.getFileName(),
                preview.getSheetCount(),
                preview.getRows().size(),
                validRowCount,
                preview.getRows().size() - validRowCount,
                warningCount,
                List.copyOf(new LinkedHashSet<>(preview.getDetectedHeaders())),
                List.copyOf(new LinkedHashSet<>(preview.getIgnoredHeaders())),
                List.copyOf(preview.getGeneralWarnings()),
                List.copyOf(preview.getGeneralInformationMessages()),
                rowResponses
        );
    }

    private CompanyImportPreviewRowResponse toRowResponse(
            CompanyImportRow row
    ) {
        return new CompanyImportPreviewRowResponse(
                row.getSheetName(),
                row.getRowNumber(),
                row.getName(),
                row.getActivityArea(),
                row.getPhone(),
                row.getSecondaryPhone(),
                row.getEmail(),
                row.getContactFullName(),
                row.getContactPosition(),
                row.getContactPhone(),
                row.getContactEmail(),
                List.copyOf(row.getErrors()),
                List.copyOf(row.getWarnings()),
                row.isValid()
        );
    }
}
