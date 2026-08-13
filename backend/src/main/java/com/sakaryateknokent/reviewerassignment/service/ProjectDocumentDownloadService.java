package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectDocumentDownloadService {

    private final ProjectRepository projectRepository;
    private final ProjectFileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public ProjectDocumentDownload openProjectDocument(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proje bulunamadi"));

        if (project.getSourceFileId() == null
                || project.getStorageKey() == null
                || project.getOriginalFileName() == null
                || project.getContentType() == null
                || project.getFileSize() == null) {
            throw new ResourceNotFoundException("Proje kaynak belgesi bulunamadi");
        }

        return new ProjectDocumentDownload(
                project.getOriginalFileName(),
                project.getContentType(),
                project.getFileSize(),
                fileStorageService.loadProjectFile(project.getStorageKey())
        );
    }
}
