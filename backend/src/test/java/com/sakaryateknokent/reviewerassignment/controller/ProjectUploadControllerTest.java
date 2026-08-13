package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.ProjectUploadResponse;
import com.sakaryateknokent.reviewerassignment.service.ProjectUploadInfo;
import com.sakaryateknokent.reviewerassignment.service.ProjectUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectUploadControllerTest {

    private final ProjectUploadService uploadService = mock(ProjectUploadService.class);
    private final ProjectUploadController controller = new ProjectUploadController(uploadService);

    @Test
    void userUploadPassesSameFileAndAuthenticationNameAndReturnsSafeResponse() {
        MockMultipartFile file = new MockMultipartFile("file", "proposal.pdf",
                "application/pdf", "%PDF-".getBytes());
        UUID uploadId = UUID.randomUUID();
        when(uploadService.createTemporaryUpload(file, "user@example.com"))
                .thenReturn(uploadInfo(uploadId));

        ResponseEntity<ProjectUploadResponse> response = controller.uploadTemporaryFile(
                file, authentication("user@example.com", "ROLE_USER"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(uploadId, response.getBody().uploadId());
        assertEquals("proposal.pdf", response.getBody().originalFileName());
        assertEquals("application/pdf", response.getBody().contentType());
        verify(uploadService).createTemporaryUpload(file, "user@example.com");
    }

    @Test
    void adminCanUseSameUploadEndpointAndUploadIdComesFromService() {
        MockMultipartFile file = new MockMultipartFile("file", "proposal.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{1, 2});
        UUID serviceGeneratedId = UUID.randomUUID();
        when(uploadService.createTemporaryUpload(file, "admin@example.com"))
                .thenReturn(uploadInfo(serviceGeneratedId));

        ResponseEntity<ProjectUploadResponse> response = controller.uploadTemporaryFile(
                file, authentication("admin@example.com", "ROLE_ADMIN"));

        assertEquals(serviceGeneratedId, response.getBody().uploadId());
        verify(uploadService).createTemporaryUpload(file, "admin@example.com");
    }

    @Test
    void responseModelDoesNotExposeStorageKeyOrPhysicalPath() {
        List<String> fields = Arrays.stream(ProjectUploadResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertFalse(fields.contains("storageKey"));
        assertFalse(fields.contains("path"));
        assertFalse(fields.contains("storedFileName"));
        assertFalse(fields.contains("uploadedBy"));
    }

    private ProjectUploadInfo uploadInfo(UUID uploadId) {
        return new ProjectUploadInfo(uploadId, "proposal.pdf", "application/pdf", 42,
                Instant.parse("2026-08-14T10:00:00Z"));
    }

    private UsernamePasswordAuthenticationToken authentication(String email, String authority) {
        return new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
