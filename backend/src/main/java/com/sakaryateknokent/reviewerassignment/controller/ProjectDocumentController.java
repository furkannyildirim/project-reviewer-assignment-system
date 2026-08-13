package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.service.ProjectDocumentDownload;
import com.sakaryateknokent.reviewerassignment.service.ProjectDocumentDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectDocumentController {

    private final ProjectDocumentDownloadService documentDownloadService;

    @GetMapping("/{projectId}/document")
    public ResponseEntity<StreamingResponseBody> downloadProjectDocument(
            @PathVariable Long projectId
    ) {
        ProjectDocumentDownload document = documentDownloadService.openProjectDocument(projectId);
        StreamingResponseBody body = outputStream -> {
            try (var inputStream = document.content()) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .contentLength(document.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.originalFileName(), StandardCharsets.UTF_8)
                        .build().toString())
                .body(body);
    }
}
