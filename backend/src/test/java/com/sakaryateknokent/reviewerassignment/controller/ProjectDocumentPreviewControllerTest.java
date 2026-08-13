package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.ProjectDocumentPreviewResponse;
import com.sakaryateknokent.reviewerassignment.enums.ProjectFormField;
import com.sakaryateknokent.reviewerassignment.service.ProjectDocumentPreviewService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectDocumentPreviewControllerTest {

    private final ProjectDocumentPreviewService previewService =
            mock(ProjectDocumentPreviewService.class);
    private final ProjectDocumentPreviewController controller =
            new ProjectDocumentPreviewController(previewService);

    @Test
    void userPreviewPassesPathUploadIdAndAuthenticationNameAndReturnsOk() {
        UUID uploadId = UUID.randomUUID();
        when(previewService.preview(uploadId, "user@example.com"))
                .thenReturn(response(uploadId));

        ResponseEntity<ProjectDocumentPreviewResponse> response = controller.preview(
                uploadId, authentication("user@example.com", "ROLE_USER"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(uploadId, response.getBody().uploadId());
        verify(previewService).preview(uploadId, "user@example.com");
    }

    @Test
    void adminUsesSameEndpointWithoutClientRoleParameter() {
        UUID uploadId = UUID.randomUUID();
        when(previewService.preview(uploadId, "admin@example.com"))
                .thenReturn(response(uploadId));

        controller.preview(uploadId, authentication("admin@example.com", "ROLE_ADMIN"));

        verify(previewService).preview(uploadId, "admin@example.com");
    }

    @Test
    void previewMethodAcceptsNoRequestBodyOrClientSuppliedUploadIdentity() throws Exception {
        Method method = ProjectDocumentPreviewController.class.getMethod(
                "preview", UUID.class,
                org.springframework.security.core.Authentication.class);

        assertFalse(Arrays.stream(method.getParameters())
                .anyMatch(parameter -> parameter.isAnnotationPresent(RequestBody.class)));
        assertEquals(List.of(UUID.class,
                        org.springframework.security.core.Authentication.class),
                Arrays.stream(method.getParameterTypes()).toList());
    }

    private ProjectDocumentPreviewResponse response(UUID uploadId) {
        return new ProjectDocumentPreviewResponse(
                uploadId, "project.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                2048, Instant.parse("2026-08-14T10:00:00Z"), "TT-FR-003.10",
                "Örnek Şirket", "Proje", "Ayşe Şimşek",
                LocalDate.of(2026, 1, 1), 3, LocalDate.of(2026, 4, 1),
                "Ar-Ge", "Yazılım", "Özet", "Ar-Ge", "Yenilik",
                "Finans Teknolojileri", "Detay", List.of("Yapay Zekâ"),
                List.of(ProjectFormField.COMPANY_NAME), List.of(), List.of(), null
        );
    }

    private UsernamePasswordAuthenticationToken authentication(
            String email,
            String authority
    ) {
        return new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
