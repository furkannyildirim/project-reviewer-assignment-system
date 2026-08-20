package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.config.SecurityConfig;
import com.sakaryateknokent.reviewerassignment.dto.CreateProjectFromUploadRequest;
import com.sakaryateknokent.reviewerassignment.dto.ProjectListResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerAssignmentHistoryResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerAssignmentResponse;
import com.sakaryateknokent.reviewerassignment.dto.ReviewerAssignmentRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateProjectRequest;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.exception.GlobalExceptionHandler;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import com.sakaryateknokent.reviewerassignment.security.DbAwareJwtAuthenticationConverter;
import com.sakaryateknokent.reviewerassignment.security.RestSecurityErrorHandler;
import com.sakaryateknokent.reviewerassignment.service.ProjectService;
import com.sakaryateknokent.reviewerassignment.service.ProjectCreationFromUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(ProjectController.class)
@Import({
        SecurityConfig.class,
        DbAwareJwtAuthenticationConverter.class,
        RestSecurityErrorHandler.class
})
class ProjectControllerTest {

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ProjectCreationFromUploadService creationFromUploadService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ProjectController projectController;

    @Autowired
    private MockMvc securedMockMvc;

    @Test
    void createProjectFromUploadPassesOnlyRequestAndAuthenticationName() {
        CreateProjectFromUploadRequest request = createFromUploadRequest();
        when(creationFromUploadService.create(request, "user@example.com"))
                .thenReturn(projectResponse(2L));

        ResponseEntity<ProjectResponse> response = projectController.createProjectFromUpload(
                request, authentication("user@example.com", "ROLE_USER"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(creationFromUploadService).create(request, "user@example.com");
    }

    @Test
    void listProjectsReturnsPageFromService() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ProjectListResponse> page = new PageImpl<>(List.of(
                new ProjectListResponse(1L, "Proje", 2L, "Sirket", CompanyType.TGB,
                        "Kategori", "Sektor", LocalDate.now(), LocalDate.now(),
                        ProjectStatus.DRAFT, 0, null)), pageable, 1);
        when(projectService.listProjects(pageable)).thenReturn(page);

        ResponseEntity<Page<ProjectListResponse>> response = projectController.listProjects(pageable);

        assertEquals(1, response.getBody().getTotalElements());
        verify(projectService).listProjects(pageable);
    }

    @Test
    void getProjectByIdCallsService() {
        when(projectService.getProjectById(5L)).thenReturn(projectResponse(5L));

        ResponseEntity<ProjectResponse> response = projectController.getProjectById(5L);

        assertEquals(5L, response.getBody().id());
        verify(projectService).getProjectById(5L);
    }

    @Test
    void updateProjectPassesFalseForUserWithoutTrustingRequest() {
        UpdateProjectRequest request = updateRequest();
        when(projectService.updateProject(eq(5L), eq(request), eq("user@example.com"), eq(false)))
                .thenReturn(projectResponse(5L));

        projectController.updateProject(5L, request, authentication("user@example.com", "ROLE_USER"));

        verify(projectService).updateProject(5L, request, "user@example.com", false);
    }

    @Test
    void updateProjectPassesTrueForRoleAdminAuthority() {
        UpdateProjectRequest request = updateRequest();
        when(projectService.updateProject(eq(5L), eq(request), eq("admin@example.com"), eq(true)))
                .thenReturn(projectResponse(5L));

        projectController.updateProject(5L, request, authentication("admin@example.com", "ROLE_ADMIN"));

        verify(projectService).updateProject(5L, request, "admin@example.com", true);
    }

    @Test
    void assignReviewerAcceptsAuthenticatedUserAndAdminWithoutClientManagedFields() {
        ReviewerAssignmentRequest request = new ReviewerAssignmentRequest(21L);
        ProjectReviewerAssignmentResponse serviceResponse = assignmentResponse();
        when(projectService.assignReviewer(5L, request, "user@example.com"))
                .thenReturn(serviceResponse);
        when(projectService.assignReviewer(5L, request, "admin@example.com"))
                .thenReturn(serviceResponse);

        ResponseEntity<ProjectReviewerAssignmentResponse> userResponse =
                projectController.assignReviewer(
                        5L, request, authentication("user@example.com", "ROLE_USER"));
        ResponseEntity<ProjectReviewerAssignmentResponse> adminResponse =
                projectController.assignReviewer(
                        5L, request, authentication("admin@example.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.CREATED, userResponse.getStatusCode());
        assertEquals(HttpStatus.CREATED, adminResponse.getStatusCode());
        verify(projectService).assignReviewer(5L, request, "user@example.com");
        verify(projectService).assignReviewer(5L, request, "admin@example.com");
    }

    @Test
    void listsActiveReviewerAssignmentsWithSafeResponseFields() throws Exception {
        when(projectService.listActiveReviewerAssignments(5L))
                .thenReturn(List.of(assignmentResponse()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/projects/5/reviewer-assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignmentId").value(31))
                .andExpect(jsonPath("$[0].projectId").value(5))
                .andExpect(jsonPath("$[0].academicianId").value(21))
                .andExpect(jsonPath("$[0].academicianName").value("Ayse Demir"))
                .andExpect(jsonPath("$[0].academicianTitle").value("Prof. Dr."))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].assignedAt").exists())
                .andExpect(jsonPath("$[0].assignedByName").value("Ali Yilmaz"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].storageKey").doesNotExist());
    }

    @Test
    void returnsOkWithEmptyListWhenProjectHasNoActiveAssignments() {
        when(projectService.listActiveReviewerAssignments(5L)).thenReturn(List.of());

        ResponseEntity<List<ProjectReviewerAssignmentResponse>> response =
                projectController.listActiveReviewerAssignments(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
    }

    @Test
    void returnsNotFoundWhenAssignmentProjectDoesNotExist() throws Exception {
        when(projectService.listActiveReviewerAssignments(99L))
                .thenThrow(new ResourceNotFoundException("Proje bulunamadi"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/projects/99/reviewer-assignments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Proje bulunamadi"));
    }

    @Test
    void activeAssignmentListAllowsAuthenticatedUserAndAdminButRejectsAnonymous()
            throws Exception {
        when(projectService.listActiveReviewerAssignments(5L)).thenReturn(List.of());

        securedMockMvc.perform(get("/api/projects/5/reviewer-assignments")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk());
        securedMockMvc.perform(get("/api/projects/5/reviewer-assignments")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
        securedMockMvc.perform(get("/api/projects/5/reviewer-assignments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsReviewerAssignmentHistoryWithSafeResponseFields() throws Exception {
        when(projectService.listReviewerAssignmentHistory(5L))
                .thenReturn(List.of(historyResponse()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/projects/5/reviewer-assignments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignmentId").value(41))
                .andExpect(jsonPath("$[0].projectId").value(5))
                .andExpect(jsonPath("$[0].academicianId").value(21))
                .andExpect(jsonPath("$[0].academicianName").value("Ayse Demir"))
                .andExpect(jsonPath("$[0].academicianTitle").value("Prof. Dr."))
                .andExpect(jsonPath("$[0].status").value("REMOVED"))
                .andExpect(jsonPath("$[0].assignedAt").exists())
                .andExpect(jsonPath("$[0].endedAt").exists())
                .andExpect(jsonPath("$[0].assignedByName").value("Ali Yilmaz"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$[0].removedBy").doesNotExist());
    }

    @Test
    void returnsEmptyReviewerAssignmentHistoryList() {
        when(projectService.listReviewerAssignmentHistory(5L)).thenReturn(List.of());

        ResponseEntity<List<ProjectReviewerAssignmentHistoryResponse>> response =
                projectController.listReviewerAssignmentHistory(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
    }

    @Test
    void returnsNotFoundWhenHistoryProjectDoesNotExist() throws Exception {
        when(projectService.listReviewerAssignmentHistory(99L))
                .thenThrow(new ResourceNotFoundException("Proje bulunamadi"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/projects/99/reviewer-assignments/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Proje bulunamadi"));
    }

    @Test
    void reviewerAssignmentHistoryAllowsUserAndAdminButRejectsAnonymous()
            throws Exception {
        when(projectService.listReviewerAssignmentHistory(5L)).thenReturn(List.of());
        String path = "/api/projects/5/reviewer-assignments/history";

        securedMockMvc.perform(get(path)
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk());
        securedMockMvc.perform(get(path)
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
        securedMockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void removeReviewerAssignmentReturnsNoContent() {
        ResponseEntity<Void> response = projectController.removeReviewerAssignment(5L, 31L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(null, response.getBody());
        verify(projectService).removeReviewerAssignment(5L, 31L);
    }

    @Test
    void reviewerRemovalAllowsAuthenticatedUserAndAdminButRejectsAnonymous()
            throws Exception {
        String path = "/api/projects/5/reviewer-assignments/31/remove";

        securedMockMvc.perform(post(path)
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isNoContent());
        securedMockMvc.perform(post(path)
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNoContent());
        securedMockMvc.perform(post(path))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidFromUploadRequestWithExistingFieldErrorFormat() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/projects/from-upload")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.uploadId").exists())
                .andExpect(jsonPath("$.fieldErrors.companyId").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void rejectsReviewerAssignmentWithoutAcademicianId() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/projects/5/reviewer-assignments")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.academicianId").exists());
    }

    private UpdateProjectRequest updateRequest() {
        return new UpdateProjectRequest(2L, "Proje", null, LocalDate.of(2026, 1, 1),
                3, null, null, null, null, null, null, null, Set.of());
    }

    private CreateProjectFromUploadRequest createFromUploadRequest() {
        return new CreateProjectFromUploadRequest(UUID.randomUUID(), 2L, "Proje", null,
                LocalDate.of(2026, 1, 1), 3, null, null, null, null, null,
                null, null, Set.of());
    }

    private ProjectResponse projectResponse(Long id) {
        return new ProjectResponse(id, 2L, "Sirket", CompanyType.TGB, 3L,
                "Kullanici Adi", "Proje", null, LocalDate.of(2026, 1, 1), 3,
                LocalDate.of(2026, 4, 1), null, null, null, null, null, null, null,
                Set.of(), ProjectStatus.DRAFT, null, null, null, null, null, null);
    }

    private ProjectReviewerAssignmentResponse assignmentResponse() {
        return new ProjectReviewerAssignmentResponse(
                31L, 5L, 21L, "Ayse Demir", "Prof. Dr.",
                AssignmentStatus.ACTIVE, Instant.parse("2026-08-14T10:00:00Z"),
                "Ali Yilmaz"
        );
    }

    private ProjectReviewerAssignmentHistoryResponse historyResponse() {
        return new ProjectReviewerAssignmentHistoryResponse(
                41L,
                5L,
                21L,
                "Ayse Demir",
                "Prof. Dr.",
                AssignmentStatus.REMOVED,
                Instant.parse("2026-08-01T09:00:00Z"),
                Instant.parse("2026-08-14T10:00:00Z"),
                "Ali Yilmaz"
        );
    }

    private UsernamePasswordAuthenticationToken authentication(String email, String authority) {
        return new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
