package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.ProjectUploadResponse;
import com.sakaryateknokent.reviewerassignment.service.ProjectUploadInfo;
import com.sakaryateknokent.reviewerassignment.service.ProjectUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/projects/uploads")
@RequiredArgsConstructor
public class ProjectUploadController {

    private final ProjectUploadService projectUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectUploadResponse> uploadTemporaryFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        ProjectUploadInfo upload = projectUploadService.createTemporaryUpload(
                file, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(upload));
    }

    private ProjectUploadResponse toResponse(ProjectUploadInfo upload) {
        return new ProjectUploadResponse(upload.uploadId(), upload.originalFileName(),
                upload.contentType(), upload.fileSize(), upload.expiresAt());
    }
}
