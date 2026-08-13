package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectDocumentDownloadServiceTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectFileStorageService fileStorageService = mock(ProjectFileStorageService.class);
    private final ProjectDocumentDownloadService service =
            new ProjectDocumentDownloadService(projectRepository, fileStorageService);

    @Test
    void opensPermanentFileUsingOnlyMetadataStoredOnProject() throws Exception {
        Project project = projectWithDocument();
        byte[] content = "document".getBytes();
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(fileStorageService.loadProjectFile("projects/123e4567-e89b-12d3-a456-426614174000.pdf"))
                .thenReturn(new ByteArrayInputStream(content));

        ProjectDocumentDownload download = service.openProjectDocument(7L);

        assertEquals("Türkçe Proje.pdf", download.originalFileName());
        assertEquals(content.length, download.fileSize());
        assertEquals("document", new String(download.content().readAllBytes()));
        verify(fileStorageService).loadProjectFile(project.getStorageKey());
    }

    @Test
    void returnsNotFoundWhenProjectDoesNotExist() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.openProjectDocument(99L));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void returnsNotFoundWhenProjectHasNoSourceDocument() {
        Project project = new Project();
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

        assertThrows(ResourceNotFoundException.class,
                () -> service.openProjectDocument(7L));
        verifyNoInteractions(fileStorageService);
    }

    private Project projectWithDocument() {
        Project project = new Project();
        project.setSourceFileId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        project.setStorageKey("projects/123e4567-e89b-12d3-a456-426614174000.pdf");
        project.setOriginalFileName("Türkçe Proje.pdf");
        project.setContentType("application/pdf");
        project.setFileSize(8L);
        return project;
    }
}
