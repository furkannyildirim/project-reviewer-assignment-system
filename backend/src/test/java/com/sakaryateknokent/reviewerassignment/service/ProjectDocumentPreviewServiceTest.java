package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.ProjectDocumentPreviewResponse;
import com.sakaryateknokent.reviewerassignment.enums.ProjectFormField;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectDocumentPreviewServiceTest {

    private final ProjectUploadService uploadService = mock(ProjectUploadService.class);
    private final ProjectDocumentTextExtractionService extractionService =
            mock(ProjectDocumentTextExtractionService.class);
    private final ProjectFormParserService parserService = mock(ProjectFormParserService.class);
    private final ProjectDocumentPreviewService previewService =
            new ProjectDocumentPreviewService(
                    uploadService, extractionService, parserService);

    @Test
    void sendsUsableUploadThroughExtractionAndParserAndMapsSafeResponse() {
        UUID uploadId = UUID.randomUUID();
        CloseTrackingInputStream stream = new CloseTrackingInputStream(new byte[]{1, 2});
        ProjectUploadInfo metadata = metadata(uploadId);
        ExtractedProjectDocumentText extracted = new ExtractedProjectDocumentText(
                ProjectDocumentTextExtractionService.DOCX_CONTENT_TYPE, "extracted");
        ParsedProjectForm parsed = parsed(
                LocalDate.of(2026, 2, 15), 12,
                linkedSet(ProjectFormField.COMPANY_NAME, ProjectFormField.PROJECT_MANAGER),
                linkedSet(ProjectFormField.SECTOR),
                List.of("Şirket bilgisi tamamlanmalı"));
        when(uploadService.openUsableUpload(uploadId, "owner@example.com"))
                .thenReturn(new OpenedProjectUpload(metadata, stream));
        when(extractionService.extract(stream, metadata.contentType())).thenReturn(extracted);
        when(parserService.parse(extracted)).thenReturn(parsed);

        ProjectDocumentPreviewResponse response = previewService.preview(
                uploadId, "owner@example.com");

        assertEquals(uploadId, response.uploadId());
        assertEquals("project.docx", response.originalFileName());
        assertEquals("TT-FR-003.10", response.templateCode());
        assertEquals("Akıllı Üretim", response.name());
        assertEquals(LocalDate.of(2027, 2, 15), response.endDate());
        assertEquals(List.of("Yapay Zekâ", "Robotik"), response.keywords());
        assertEquals(List.of(ProjectFormField.COMPANY_NAME,
                ProjectFormField.PROJECT_MANAGER), response.missingFields());
        assertEquals(List.of(ProjectFormField.SECTOR), response.reviewRequiredFields());
        assertEquals(List.of("Şirket bilgisi tamamlanmalı"), response.warnings());
        assertTrue(stream.closed);
        verify(uploadService).openUsableUpload(uploadId, "owner@example.com");
        verify(extractionService).extract(stream, metadata.contentType());
        verify(parserService).parse(extracted);
    }

    @Test
    void leavesEndDateNullWhenDateOrDurationIsMissing() {
        UUID uploadId = UUID.randomUUID();
        CloseTrackingInputStream stream = new CloseTrackingInputStream(new byte[]{1});
        ExtractedProjectDocumentText extracted = new ExtractedProjectDocumentText(
                ProjectDocumentTextExtractionService.PDF_CONTENT_TYPE, "text");
        when(uploadService.openUsableUpload(uploadId, "owner@example.com"))
                .thenReturn(new OpenedProjectUpload(metadata(uploadId), stream));
        when(extractionService.extract(stream,
                ProjectDocumentTextExtractionService.DOCX_CONTENT_TYPE)).thenReturn(extracted);
        when(parserService.parse(extracted)).thenReturn(parsed(
                null, 12, linkedSet(ProjectFormField.START_DATE),
                linkedSet(), List.of()));

        ProjectDocumentPreviewResponse response = previewService.preview(
                uploadId, "owner@example.com");

        assertNull(response.endDate());
        assertTrue(stream.closed);
    }

    @Test
    void closesStreamWhenExtractionFails() {
        UUID uploadId = UUID.randomUUID();
        CloseTrackingInputStream stream = new CloseTrackingInputStream(new byte[]{1});
        ProjectUploadInfo metadata = metadata(uploadId);
        when(uploadService.openUsableUpload(uploadId, "owner@example.com"))
                .thenReturn(new OpenedProjectUpload(metadata, stream));
        when(extractionService.extract(stream, metadata.contentType()))
                .thenThrow(new InvalidRequestException("Belgeden metin cikarilamadi"));

        assertThrows(InvalidRequestException.class, () -> previewService.preview(
                uploadId, "owner@example.com"));

        assertTrue(stream.closed);
        verify(parserService, never()).parse(any());
    }

    @Test
    void closesStreamWhenParserFails() {
        UUID uploadId = UUID.randomUUID();
        CloseTrackingInputStream stream = new CloseTrackingInputStream(new byte[]{1});
        ProjectUploadInfo metadata = metadata(uploadId);
        ExtractedProjectDocumentText extracted = new ExtractedProjectDocumentText(
                metadata.contentType(), "unrelated");
        when(uploadService.openUsableUpload(uploadId, "owner@example.com"))
                .thenReturn(new OpenedProjectUpload(metadata, stream));
        when(extractionService.extract(stream, metadata.contentType())).thenReturn(extracted);
        when(parserService.parse(extracted))
                .thenThrow(new InvalidRequestException("Desteklenen proje formu taninamadi"));

        assertThrows(InvalidRequestException.class, () -> previewService.preview(
                uploadId, "owner@example.com"));

        assertTrue(stream.closed);
    }

    @Test
    void previewDoesNotConsumeUploadAndResponseExposesNoStorageDetails() {
        UUID uploadId = UUID.randomUUID();
        CloseTrackingInputStream stream = new CloseTrackingInputStream(new byte[]{1});
        ProjectUploadInfo metadata = metadata(uploadId);
        ExtractedProjectDocumentText extracted = new ExtractedProjectDocumentText(
                metadata.contentType(), "text");
        when(uploadService.openUsableUpload(uploadId, "owner@example.com"))
                .thenReturn(new OpenedProjectUpload(metadata, stream));
        when(extractionService.extract(stream, metadata.contentType())).thenReturn(extracted);
        when(parserService.parse(extracted)).thenReturn(parsed(
                LocalDate.of(2026, 1, 1), 1, linkedSet(), linkedSet(), List.of()));

        previewService.preview(uploadId, "owner@example.com");

        verify(uploadService, never()).markConsumed(any(), anyString());
        List<String> fields = Arrays.stream(
                        ProjectDocumentPreviewResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();
        assertFalse(fields.contains("storageKey"));
        assertFalse(fields.contains("storedFileName"));
        assertFalse(fields.contains("path"));
        assertFalse(fields.contains("uploadedBy"));
        assertFalse(fields.contains("extractedText"));
    }

    private ProjectUploadInfo metadata(UUID uploadId) {
        return new ProjectUploadInfo(uploadId, "project.docx",
                ProjectDocumentTextExtractionService.DOCX_CONTENT_TYPE, 2048,
                Instant.parse("2026-08-14T10:00:00Z"));
    }

    private ParsedProjectForm parsed(
            LocalDate startDate,
            Integer durationMonths,
            Set<ProjectFormField> missing,
            Set<ProjectFormField> reviewRequired,
            List<String> warnings
    ) {
        return new ParsedProjectForm(
                "TT-FR-003.10", "Örnek Şirket", "Akıllı Üretim",
                "Ayşe Şimşek", startDate, durationMonths, "Ar-Ge", "Yazılım",
                "Özet", "Ar-Ge niteliği", "Yenilikçi yön", "Finans Teknolojileri",
                "Proje detayı", linkedSet("Yapay Zekâ", "Robotik"),
                missing, reviewRequired, warnings
        );
    }

    @SafeVarargs
    private final <T> Set<T> linkedSet(T... values) {
        return new LinkedHashSet<>(List.of(values));
    }

    private static class CloseTrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private CloseTrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
