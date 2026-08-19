package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.config.SecurityConfig;
import com.sakaryateknokent.reviewerassignment.dto.AcademicianProjectSummaryResponse;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import com.sakaryateknokent.reviewerassignment.security.DbAwareJwtAuthenticationConverter;
import com.sakaryateknokent.reviewerassignment.security.RestSecurityErrorHandler;
import com.sakaryateknokent.reviewerassignment.service.AcademicianExportService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportPreviewService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcademicianController.class)
@Import({
        SecurityConfig.class,
        DbAwareJwtAuthenticationConverter.class,
        RestSecurityErrorHandler.class
})
class AcademicianProjectSummarySecurityTest {

    @MockitoBean
    private AcademicianService academicianService;

    @MockitoBean
    private AcademicianExportService academicianExportService;

    @MockitoBean
    private AcademicianImportPreviewService academicianImportPreviewService;

    @MockitoBean
    private AcademicianImportService academicianImportService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void projectSummaryAllowsUserAndAdminButRejectsAnonymous() throws Exception {
        when(academicianService.getProjectSummary(7L)).thenReturn(
                new AcademicianProjectSummaryResponse(7L, 0, 0, 0)
        );
        String path = "/api/academicians/7/project-summary";

        mockMvc.perform(get(path)
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk());
        mockMvc.perform(get(path)
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }
}
