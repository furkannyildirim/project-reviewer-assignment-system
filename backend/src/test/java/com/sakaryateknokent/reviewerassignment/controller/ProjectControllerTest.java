package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.CreateProjectRequest;
import com.sakaryateknokent.reviewerassignment.dto.ProjectListResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectResponse;
import com.sakaryateknokent.reviewerassignment.dto.UpdateProjectRequest;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.exception.GlobalExceptionHandler;
import com.sakaryateknokent.reviewerassignment.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectControllerTest {

    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectController projectController = new ProjectController(projectService);

    @Test
    void createProjectPassesAuthenticatedUserNameAndReturnsCreated() {
        CreateProjectRequest request = createRequest();
        when(projectService.createProject(request, "user@example.com"))
                .thenReturn(projectResponse(1L));

        ResponseEntity<ProjectResponse> response = projectController.createProject(
                request, authentication("user@example.com", "ROLE_USER"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1L, response.getBody().id());
        verify(projectService).createProject(request, "user@example.com");
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
    void rejectsInvalidCreateRequestWithExistingFieldErrorFormat() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/projects")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.companyId").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    private CreateProjectRequest createRequest() {
        return new CreateProjectRequest(2L, "Proje", null, LocalDate.of(2026, 1, 1),
                3, null, null, null, null, null, null, null, Set.of());
    }

    private UpdateProjectRequest updateRequest() {
        return new UpdateProjectRequest(2L, "Proje", null, LocalDate.of(2026, 1, 1),
                3, null, null, null, null, null, null, null, Set.of());
    }

    private ProjectResponse projectResponse(Long id) {
        return new ProjectResponse(id, 2L, "Sirket", CompanyType.TGB, 3L,
                "Kullanici Adi", "Proje", null, LocalDate.of(2026, 1, 1), 3,
                LocalDate.of(2026, 4, 1), null, null, null, null, null, null, null,
                Set.of(), ProjectStatus.DRAFT, null, null, null, null, null, null);
    }

    private UsernamePasswordAuthenticationToken authentication(String email, String authority) {
        return new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
