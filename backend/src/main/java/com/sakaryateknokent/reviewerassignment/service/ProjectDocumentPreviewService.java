package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.ProjectDocumentPreviewResponse;
import com.sakaryateknokent.reviewerassignment.enums.ProjectFormField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectDocumentPreviewService {

    private final ProjectUploadService projectUploadService;
    private final ProjectDocumentTextExtractionService textExtractionService;
    private final ProjectFormParserService formParserService;

    public ProjectDocumentPreviewResponse preview(
            UUID uploadId,
            String currentUserEmail
    ) {
        try (OpenedProjectUpload opened = projectUploadService.openUsableUpload(
                uploadId, currentUserEmail)) {
            ProjectUploadInfo metadata = opened.metadata();
            ExtractedProjectDocumentText extracted = textExtractionService.extract(
                    opened.inputStream(), metadata.contentType());
            ParsedProjectForm parsed = formParserService.parse(extracted);
            return toResponse(metadata, parsed);
        }
    }

    private ProjectDocumentPreviewResponse toResponse(
            ProjectUploadInfo metadata,
            ParsedProjectForm parsed
    ) {
        LocalDate endDate = parsed.startDate() != null
                && parsed.durationMonths() != null
                ? parsed.startDate().plusMonths(parsed.durationMonths())
                : null;

        return new ProjectDocumentPreviewResponse(
                metadata.uploadId(), metadata.originalFileName(),
                metadata.contentType(), metadata.fileSize(), metadata.expiresAt(),
                parsed.templateCode(), parsed.companyName(), parsed.name(),
                parsed.projectManager(), parsed.startDate(), parsed.durationMonths(),
                endDate, parsed.category(), parsed.sector(), parsed.summary(),
                parsed.researchAndDevelopmentNature(), parsed.innovativeAspect(),
                parsed.mainTechnologyArea(), parsed.projectDetail(),
                List.copyOf(parsed.keywords()), orderedFields(parsed.missingFields()),
                orderedFields(parsed.reviewRequiredFields()),
                List.copyOf(parsed.warnings())
        );
    }

    private List<ProjectFormField> orderedFields(
            java.util.Set<ProjectFormField> fields
    ) {
        return fields.stream()
                .sorted(Comparator.comparingInt(ProjectFormField::ordinal))
                .toList();
    }
}
