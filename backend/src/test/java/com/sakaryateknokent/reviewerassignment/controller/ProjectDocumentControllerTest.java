package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.service.ProjectDocumentDownload;
import com.sakaryateknokent.reviewerassignment.service.ProjectDocumentDownloadService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectDocumentControllerTest {

    private final ProjectDocumentDownloadService service = mock(ProjectDocumentDownloadService.class);
    private final ProjectDocumentController controller = new ProjectDocumentController(service);

    @Test
    void returnsAttachmentHeadersAndStreamsDocument() throws Exception {
        byte[] content = "%PDF-test".getBytes();
        when(service.openProjectDocument(4L)).thenReturn(new ProjectDocumentDownload(
                "Türkçe Belge.pdf", "application/pdf", content.length,
                new ByteArrayInputStream(content)));

        ResponseEntity<StreamingResponseBody> response =
                controller.downloadProjectDocument(4L);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.getBody().writeTo(output);

        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertEquals(content.length, response.getHeaders().getContentLength());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .startsWith("attachment;"));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("filename*=UTF-8''"));
        assertArrayEquals(content, output.toByteArray());
        verify(service).openProjectDocument(4L);
    }
}
