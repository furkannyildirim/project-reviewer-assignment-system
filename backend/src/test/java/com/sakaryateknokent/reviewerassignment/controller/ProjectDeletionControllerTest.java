package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.config.SecurityConfig;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import com.sakaryateknokent.reviewerassignment.security.DbAwareJwtAuthenticationConverter;
import com.sakaryateknokent.reviewerassignment.security.RestSecurityErrorHandler;
import com.sakaryateknokent.reviewerassignment.service.ProjectCreationFromUploadService;
import com.sakaryateknokent.reviewerassignment.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import({
        SecurityConfig.class,
        DbAwareJwtAuthenticationConverter.class,
        RestSecurityErrorHandler.class
})
class ProjectDeletionControllerTest {

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ProjectCreationFromUploadService projectCreationFromUploadService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deletionReturnsNoContentAndDoesNotParseRequestBody() throws Exception {
        mockMvc.perform(delete("/api/projects/11")
                        .with(user("user@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(11L);
    }

    @Test
    void deletionReturnsNotFoundForMissingProject() throws Exception {
        doThrow(new ResourceNotFoundException("Proje bulunamadi"))
                .when(projectService).deleteProject(99L);

        mockMvc.perform(delete("/api/projects/99")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Proje bulunamadi"));
    }

    @Test
    void deletionReturnsConflictForBusinessRuleViolation() throws Exception {
        doThrow(new DuplicateResourceException("Proje kalici olarak silinemez"))
                .when(projectService).deleteProject(11L);

        mockMvc.perform(delete("/api/projects/11")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Proje kalici olarak silinemez"));
    }

    @Test
    void deletionAllowsUserAndAdminButRejectsAnonymous() throws Exception {
        String path = "/api/projects/11";

        mockMvc.perform(delete(path)
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(path)
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(path))
                .andExpect(status().isUnauthorized());
    }
}
