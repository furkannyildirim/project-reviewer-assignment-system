package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.CreateProjectFromUploadRequest;
import com.sakaryateknokent.reviewerassignment.dto.ProjectListResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerAssignmentHistoryResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerAssignmentResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectResponse;
import com.sakaryateknokent.reviewerassignment.dto.ReviewerAssignmentRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateProjectRequest;
import com.sakaryateknokent.reviewerassignment.service.ProjectService;
import com.sakaryateknokent.reviewerassignment.service.ProjectCreationFromUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final ProjectService projectService;
    private final ProjectCreationFromUploadService projectCreationFromUploadService;

    @PostMapping("/from-upload")
    public ResponseEntity<ProjectResponse> createProjectFromUpload(
            @Valid @RequestBody CreateProjectFromUploadRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                projectCreationFromUploadService.create(request, authentication.getName())
        );
    }

    @GetMapping
    public ResponseEntity<Page<ProjectListResponse>> listProjects(
            @PageableDefault(size = 10, sort = "createdAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(projectService.listProjects(pageable));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    @GetMapping("/{projectId}/reviewer-assignments")
    public ResponseEntity<List<ProjectReviewerAssignmentResponse>>
    listActiveReviewerAssignments(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                projectService.listActiveReviewerAssignments(projectId)
        );
    }

    @GetMapping("/{projectId}/reviewer-assignments/history")
    public ResponseEntity<List<ProjectReviewerAssignmentHistoryResponse>>
    listReviewerAssignmentHistory(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                projectService.listReviewerAssignmentHistory(projectId)
        );
    }

    @PostMapping("/{projectId}/reviewer-assignments")
    public ResponseEntity<ProjectReviewerAssignmentResponse> assignReviewer(
            @PathVariable Long projectId,
            @Valid @RequestBody ReviewerAssignmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                projectService.assignReviewer(projectId, request, authentication.getName())
        );
    }

    @PostMapping("/{projectId}/reviewer-assignments/{assignmentId}/remove")
    public ResponseEntity<Void> removeReviewerAssignment(
            @PathVariable Long projectId,
            @PathVariable Long assignmentId
    ) {
        projectService.removeReviewerAssignment(projectId, assignmentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication
    ) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
        return ResponseEntity.ok(projectService.updateProject(
                projectId, request, authentication.getName(), isAdmin));
    }
}
