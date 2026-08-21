package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.config.SecurityConfig;
import com.sakaryateknokent.reviewerassignment.dto.CompanyProjectSummaryResponse;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import com.sakaryateknokent.reviewerassignment.security.DbAwareJwtAuthenticationConverter;
import com.sakaryateknokent.reviewerassignment.security.RestSecurityErrorHandler;
import com.sakaryateknokent.reviewerassignment.service.CompanyExportService;
import com.sakaryateknokent.reviewerassignment.service.CompanyImportPreviewService;
import com.sakaryateknokent.reviewerassignment.service.CompanyImportService;
import com.sakaryateknokent.reviewerassignment.service.CompanyService;
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

@WebMvcTest(CompanyController.class)
@Import({
        SecurityConfig.class,
        DbAwareJwtAuthenticationConverter.class,
        RestSecurityErrorHandler.class
})
class CompanyProjectSummarySecurityTest {

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private CompanyExportService companyExportService;

    @MockitoBean
    private CompanyImportPreviewService companyImportPreviewService;

    @MockitoBean
    private CompanyImportService companyImportService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void projectSummaryAllowsUserAndAdminButRejectsAnonymous() throws Exception {
        when(companyService.getProjectSummary(7L)).thenReturn(
                new CompanyProjectSummaryResponse(7L, 0, 0, 0, 0)
        );
        String path = "/api/companies/7/project-summary";

        mockMvc.perform(get(path).with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk());
        mockMvc.perform(get(path).with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
