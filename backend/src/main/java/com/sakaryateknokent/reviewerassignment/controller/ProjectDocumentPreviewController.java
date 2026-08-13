package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.ProjectDocumentPreviewResponse;
import com.sakaryateknokent.reviewerassignment.service.ProjectDocumentPreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/uploads")
@RequiredArgsConstructor
public class ProjectDocumentPreviewController {

    private final ProjectDocumentPreviewService previewService;

    @PostMapping("/{uploadId}/preview")
    public ResponseEntity<ProjectDocumentPreviewResponse> preview(
            @PathVariable UUID uploadId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                previewService.preview(uploadId, authentication.getName())
        );
    }
}
